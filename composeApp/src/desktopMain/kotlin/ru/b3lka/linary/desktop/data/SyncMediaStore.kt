package ru.b3lka.linary.desktop.data

import ru.b3lka.linary.core.model.NewMedia
import ru.b3lka.linary.desktop.crypto.SyncCrypto
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Файлы вложений в папке синхронизации: `v4_Media/<GUID>` = AES(файл) и `v4_mediaindex` =
 * gzip(JSON-массив GUID, по возрастанию). По индексу телефон узнаёт, какие вложения уже есть в облаке.
 *
 * Удалённые вложения здесь не трогаются: телефон сам удаляет из облака файлы, на которые
 * больше не ссылается база.
 */
internal class SyncMediaStore(private val dir: File, private val crypto: () -> SyncCrypto) {

    private val mediaDir get() = File(dir, MEDIA_DIR)
    private val indexFile get() = File(dir, INDEX_FILE)

    /**
     * Записывает файлы [media] и добавляет их в индекс. Возвращает функцию, которая всё откатывает.
     */
    fun add(media: List<NewMedia>): () -> Unit {
        if (media.isEmpty()) return {}
        mediaDir.mkdirs()
        val written = mutableListOf<File>()
        val oldIndex = indexFile.takeIf { it.isFile }?.readBytes()
        val undo = {
            written.forEach { it.delete() }
            if (oldIndex != null) replaceAtomically(indexFile, oldIndex)
        }
        runCatching {
            media.forEach { m ->
                val file = File(mediaDir, m.id)
                replaceAtomically(file, crypto().encrypt(m.bytes))
                written += file
            }
            val ids = (oldIndex?.let(::parseIndex).orEmpty() + media.map { it.id }).toSortedSet()
            replaceAtomically(indexFile, SyncCrypto.gzip(formatIndex(ids).toByteArray()))
        }.onFailure { undo() }.getOrThrow()
        return undo
    }

    private companion object {
        const val MEDIA_DIR = "v4_Media"
        const val INDEX_FILE = "v4_mediaindex"
        val guid = Regex("\"([0-9A-Fa-f-]{36})\"")

        fun parseIndex(gzipped: ByteArray): List<String> =
            guid.findAll(SyncCrypto.gunzip(gzipped).decodeToString()).map { it.groupValues[1] }.toList()

        fun formatIndex(ids: Collection<String>): String = ids.joinToString(",", "[", "]") { "\"$it\"" }

        /**
         * Пишет во временный файл рядом и переименовывает его поверх [target].
         * Имя `.~…` Dropbox считает временным и не синхронизирует.
         */
        fun replaceAtomically(target: File, bytes: ByteArray) {
            val tmp = File(target.parentFile, ".~${target.name}.tmp")
            tmp.writeBytes(bytes)
            Files.move(
                tmp.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        }
    }
}
