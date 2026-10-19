package ru.b3lka.linary.desktop.tray.dbus

import java.io.File
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Минимальное подключение к сессионной шине D-Bus через Unix-сокет — ровно то, что нужно значку в трее:
 * вызвать метод, ответить на вызов, получить сигнал. Без сторонних библиотек.
 *
 * Входящие сообщения читает отдельный поток; [onMethodCall] и [onSignal] вызываются в нём, поэтому
 * внутри них нельзя ждать ответа на собственный [call] — это делается в другом потоке.
 */
class DBusConnection private constructor(private val channel: SocketChannel) : AutoCloseable {
    private val serials = AtomicLong(0)
    private val pending = ConcurrentHashMap<Long, CompletableFuture<DBusMessage>>()
    private val writeLock = Any()

    @Volatile
    var onMethodCall: (DBusMessage) -> Unit = { call ->
        if (call.expectsReply) replyError(call, UNKNOWN_METHOD, "No such method ${call.member}")
    }

    @Volatile
    var onSignal: (DBusMessage) -> Unit = {}

    lateinit var uniqueName: String
        private set

    private fun start() {
        Thread(::readLoop, "dbus-reader").apply { isDaemon = true }.start()
        uniqueName = callBus("Hello").args().first() as String
    }

    /**
     * Вызывает метод и ждёт ответа. Ошибка D-Bus или тайм-аут — [DBusException].
     */
    fun call(
        destination: String,
        path: String,
        iface: String,
        member: String,
        signature: String = "",
        timeoutMs: Long = CALL_TIMEOUT_MS,
        body: DBusWriter.() -> Unit = {},
    ): DBusMessage {
        val serial = serials.incrementAndGet()
        val future = CompletableFuture<DBusMessage>()
        pending[serial] = future
        val fields = mapOf(
            DBusMessage.FIELD_PATH to path,
            DBusMessage.FIELD_INTERFACE to iface,
            DBusMessage.FIELD_MEMBER to member,
            DBusMessage.FIELD_DESTINATION to destination,
            DBusMessage.FIELD_SIGNATURE to signature.ifEmpty { null },
        )
        send(DBusMessage(DBusMessage.METHOD_CALL, serial, fields, DBusWriter().apply(body).toByteArray()))
        val reply = runCatching { future.get(timeoutMs, TimeUnit.MILLISECONDS) }
            .getOrElse { throw DBusException(TIMEOUT, "No reply to $member") }
            .also { pending.remove(serial) }
        if (reply.type == DBusMessage.ERROR) {
            val text = reply.args().firstOrNull() as? String ?: ""
            throw DBusException(reply.errorName ?: FAILED, text)
        }
        return reply
    }

    /**
     * Ответ на вызов [call] (если вызывающий его ждёт).
     */
    fun reply(call: DBusMessage, signature: String = "", body: DBusWriter.() -> Unit = {}) {
        if (!call.expectsReply) return
        val fields = mapOf(
            DBusMessage.FIELD_REPLY_SERIAL to call.serial,
            DBusMessage.FIELD_DESTINATION to call.sender,
            DBusMessage.FIELD_SIGNATURE to signature.ifEmpty { null },
        )
        val bytes = DBusWriter().apply(body).toByteArray()
        send(DBusMessage(DBusMessage.METHOD_RETURN, serials.incrementAndGet(), fields, bytes))
    }

    fun replyError(call: DBusMessage, name: String, message: String) {
        if (!call.expectsReply) return
        val fields = mapOf(
            DBusMessage.FIELD_REPLY_SERIAL to call.serial,
            DBusMessage.FIELD_DESTINATION to call.sender,
            DBusMessage.FIELD_ERROR_NAME to name,
            DBusMessage.FIELD_SIGNATURE to "s",
        )
        val body = DBusWriter().apply { string(message) }.toByteArray()
        send(DBusMessage(DBusMessage.ERROR, serials.incrementAndGet(), fields, body))
    }

    /**
     * Вызов метода самой шины (`org.freedesktop.DBus`).
     */
    fun callBus(member: String, signature: String = "", body: DBusWriter.() -> Unit = {}): DBusMessage =
        call(BUS_NAME, BUS_PATH, BUS_INTERFACE, member, signature, body = body)

    /**
     * Подписка на сигналы по правилу вида `type='signal',interface='…'`.
     */
    fun addMatch(rule: String) {
        callBus("AddMatch", "s") { string(rule) }
    }

    override fun close() {
        runCatching { channel.close() }
        pending.values.forEach { it.completeExceptionally(DBusException(FAILED, "Connection closed")) }
    }

    private fun send(message: DBusMessage) {
        val buffer = ByteBuffer.wrap(message.encode())
        synchronized(writeLock) {
            while (buffer.hasRemaining()) channel.write(buffer)
        }
    }

    private fun readLoop() {
        runCatching {
            while (channel.isOpen) dispatch(DBusMessage.decode(readMessage()))
        }
        close()
    }

    private fun dispatch(message: DBusMessage) {
        when (message.type) {
            DBusMessage.METHOD_RETURN, DBusMessage.ERROR ->
                message.replySerial?.let { pending.remove(it)?.complete(message) }
            DBusMessage.METHOD_CALL -> runCatching { onMethodCall(message) }.onFailure {
                replyError(message, FAILED, it.message ?: it.toString())
            }
            DBusMessage.SIGNAL -> runCatching { onSignal(message) }
        }
    }

    private fun readMessage(): ByteArray {
        val start = readFully(DBusMessage.FIXED_HEADER)
        val rest = readFully(DBusMessage.totalLength(start) - DBusMessage.FIXED_HEADER)
        return start + rest
    }

    private fun readFully(count: Int): ByteArray {
        val buffer = ByteBuffer.allocate(count)
        while (buffer.hasRemaining()) {
            if (channel.read(buffer) < 0) throw DBusException(FAILED, "Connection closed")
        }
        return buffer.array()
    }

    companion object {
        const val BUS_NAME = "org.freedesktop.DBus"
        const val BUS_PATH = "/org/freedesktop/DBus"
        const val BUS_INTERFACE = "org.freedesktop.DBus"
        const val UNKNOWN_METHOD = "org.freedesktop.DBus.Error.UnknownMethod"
        private const val FAILED = "org.freedesktop.DBus.Error.Failed"
        private const val TIMEOUT = "org.freedesktop.DBus.Error.NoReply"
        private const val CALL_TIMEOUT_MS = 3000L
        private const val AUTH_LINE_MAX = 512

        /**
         * Подключение к сессионной шине или null, если её нет или она недоступна.
         */
        fun session(): DBusConnection? = runCatching {
            val socket = sessionSocket() ?: return null
            val channel = SocketChannel.open(StandardProtocolFamily.UNIX)
            channel.connect(UnixDomainSocketAddress.of(socket))
            authenticate(channel)
            DBusConnection(channel).also { it.start() }
        }.getOrNull()

        /**
         * Путь к сокету из `DBUS_SESSION_BUS_ADDRESS` (`unix:path=…`) или `$XDG_RUNTIME_DIR/bus`.
         * Абстрактные сокеты (`unix:abstract=…`) Java не поддерживает.
         */
        private fun sessionSocket(): Path? {
            val fromAddress = System.getenv("DBUS_SESSION_BUS_ADDRESS").orEmpty().split(';')
                .filter { it.startsWith("unix:") }
                .firstNotNullOfOrNull { address ->
                    address.removePrefix("unix:").split(',')
                        .firstOrNull { it.startsWith("path=") }
                        ?.removePrefix("path=")
                        ?.let(::unescape)
                }
            val path = fromAddress ?: System.getenv("XDG_RUNTIME_DIR")?.let { "$it/bus" }
            return path?.let { Path.of(it) }?.takeIf { Files.exists(it) }
        }

        private fun unescape(value: String): String =
            Regex("%([0-9a-fA-F]{2})").replace(value) { it.groupValues[1].toInt(HEX).toChar().toString() }

        /**
         * SASL EXTERNAL: шина узнаёт пользователя по сокету, мы называем свой uid.
         */
        private fun authenticate(channel: SocketChannel) {
            val uid = Files.getAttribute(File("/proc/self").toPath(), "unix:uid") as Int
            val hexUid = uid.toString().toByteArray().joinToString("") { "%02x".format(it) }
            write(channel, "\u0000AUTH EXTERNAL $hexUid\r\n")
            val answer = readLine(channel)
            if (!answer.startsWith("OK")) throw DBusException(FAILED, "Authentication rejected: $answer")
            write(channel, "BEGIN\r\n")
        }

        private fun write(channel: SocketChannel, text: String) {
            val buffer = ByteBuffer.wrap(text.toByteArray(Charsets.US_ASCII))
            while (buffer.hasRemaining()) channel.write(buffer)
        }

        private fun readLine(channel: SocketChannel): String {
            val line = StringBuilder()
            val one = ByteBuffer.allocate(1)
            var c = ' '
            while (c != '\n' && line.length < AUTH_LINE_MAX) {
                one.clear()
                c = if (channel.read(one) < 0) '\n' else one.get(0).toInt().toChar()
                if (c != '\r' && c != '\n') line.append(c)
            }
            return line.toString()
        }

        private const val HEX = 16
    }
}
