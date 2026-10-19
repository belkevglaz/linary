package ru.b3lka.linary.desktop.instance

import ru.b3lka.linary.desktop.data.PrivateTemp
import java.awt.EventQueue
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Один экземпляр приложения на пользователя.
 *
 * Первый запуск захватывает файловую блокировку `instance.lock` и слушает Unix-сокет `instance.sock`
 * в личной временной папке ([PrivateTemp]). Повторный запуск видит блокировку, просит работающий
 * экземпляр показать окно и завершается — две копии не пишут в дневник одновременно.
 * Блокировку снимает система при завершении процесса, даже аварийном.
 */
class SingleInstance private constructor(
    private val lock: FileLock?,
    private val server: ServerSocketChannel?,
) {
    /**
     * Вызывать [onActivate] (в потоке интерфейса), когда кто-то запускает приложение ещё раз.
     */
    fun listen(onActivate: () -> Unit) {
        // Блокировка держится, пока жив её FileChannel, поэтому ссылка на неё хранится всё время работы
        val server = server?.takeIf { lock?.isValid == true } ?: return
        Thread({ acceptLoop(server, onActivate) }, "single-instance").apply { isDaemon = true }.start()
    }

    private fun acceptLoop(server: ServerSocketChannel, onActivate: () -> Unit) {
        while (server.isOpen) {
            val client = runCatching { server.accept() }.getOrNull() ?: continue
            val message = client.use { readMessage(it) }
            if (message == ACTIVATE) EventQueue.invokeLater(onActivate)
        }
    }

    private fun readMessage(channel: SocketChannel): String {
        val buffer = ByteBuffer.allocate(MAX_MESSAGE)
        runCatching { channel.read(buffer) }
        return String(buffer.array(), 0, buffer.position(), Charsets.UTF_8).trim()
    }

    companion object {
        private const val LOCK_FILE = "instance.lock"
        private const val SOCKET_FILE = "instance.sock"
        private const val ACTIVATE = "activate"
        private const val MAX_MESSAGE = 64
        private const val CONNECT_ATTEMPTS = 20
        private const val CONNECT_DELAY_MS = 100L

        /**
         * Занять место единственного экземпляра. null — приложение уже запущено, ему передана просьба
         * показать окно, и этот процесс должен завершиться.
         *
         * Если проверить не удалось (нет Unix-сокетов, нет доступа к папке), приложение просто запускается.
         */
        fun acquire(): SingleInstance? {
            val dir = runCatching { PrivateTemp.dir.toPath() }.getOrNull() ?: return SingleInstance(null, null)
            val channel = runCatching {
                FileChannel.open(dir.resolve(LOCK_FILE), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
            }.getOrNull() ?: return SingleInstance(null, null)
            val lock = runCatching { channel.tryLock() }.getOrNull()
            if (lock == null) {
                channel.close()
                activateRunning(dir.resolve(SOCKET_FILE))
                return null
            }
            return SingleInstance(lock, openServer(dir.resolve(SOCKET_FILE)))
        }

        private fun openServer(socket: Path): ServerSocketChannel? = runCatching {
            // Сокет от упавшего процесса: блокировку мы получили, значит его владельца уже нет
            Files.deleteIfExists(socket)
            ServerSocketChannel.open(StandardProtocolFamily.UNIX).bind(UnixDomainSocketAddress.of(socket)).also {
                Runtime.getRuntime().addShutdownHook(Thread { runCatching { Files.deleteIfExists(socket) } })
            }
        }.getOrNull()

        /**
         * Первый экземпляр мог захватить блокировку, но ещё не открыть сокет — пробуем несколько раз.
         */
        private fun activateRunning(socket: Path) {
            repeat(CONNECT_ATTEMPTS) {
                val sent = runCatching {
                    SocketChannel.open(UnixDomainSocketAddress.of(socket)).use { channel ->
                        channel.write(ByteBuffer.wrap(ACTIVATE.toByteArray(Charsets.UTF_8)))
                    }
                }.isSuccess
                if (sent) return
                Thread.sleep(CONNECT_DELAY_MS)
            }
        }
    }
}
