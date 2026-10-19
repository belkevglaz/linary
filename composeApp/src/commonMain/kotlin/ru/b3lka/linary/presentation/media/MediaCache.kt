package ru.b3lka.linary.presentation.media

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import ru.b3lka.linary.core.model.DiaryMedia
import ru.b3lka.linary.core.repository.DiaryRepository
import ru.b3lka.linary.presentation.platform.PlatformServices

/**
 * Кэш декодированных картинок, вытесняющий давно не показанные (LRU), с ограничением по памяти:
 * миниатюры занимают десятки килобайт, а фото в полном размере — несколько мегабайт каждое.
 *
 * К самой карте обращаются только из UI-потока (через produceState),
 * расшифровка и декодирование идут в [Dispatchers.Default].
 */
class MediaCache(
    private val repository: DiaryRepository,
    private val platform: PlatformServices,
    private val budgetBytes: Long = DEFAULT_BUDGET_BYTES,
) {
    private val cache = LinkedHashMap<String, ImageBitmap>()
    private var usedBytes = 0L
    private val limiter = Semaphore(PARALLEL_DECODES)

    fun peek(media: DiaryMedia, maxSize: Int?): ImageBitmap? = touch(key(media, maxSize))

    suspend fun image(media: DiaryMedia, maxSize: Int?): ImageBitmap? {
        val k = key(media, maxSize)
        touch(k)?.let { return it }
        val bitmap = limiter.withPermit {
            withContext(Dispatchers.Default) {
                runCatching { platform.decodeImage(repository.loadMediaBytes(media), maxSize) }.getOrNull()
            }
        } ?: return null
        cache.remove(k)?.let { usedBytes -= it.byteSize }
        cache[k] = bitmap
        usedBytes += bitmap.byteSize
        // Самую свежую картинку не вытесняем, даже если она одна больше бюджета
        while (usedBytes > budgetBytes && cache.size > 1) {
            val eldest = cache.keys.first()
            cache.remove(eldest)?.let { usedBytes -= it.byteSize }
        }
        return bitmap
    }

    suspend fun bytes(media: DiaryMedia): ByteArray =
        withContext(Dispatchers.Default) { repository.loadMediaBytes(media) }

    /**
     * Картинка из кэша; при попадании переносится в конец очереди вытеснения.
     */
    private fun touch(key: String): ImageBitmap? {
        val bitmap = cache.remove(key) ?: return null
        cache[key] = bitmap
        return bitmap
    }

    private fun key(media: DiaryMedia, maxSize: Int?) = "${media.id}@${maxSize ?: 0}"

    private val ImageBitmap.byteSize: Long get() = width.toLong() * height * BYTES_PER_PIXEL

    private companion object {
        const val DEFAULT_BUDGET_BYTES = 256L * 1024 * 1024
        const val BYTES_PER_PIXEL = 4
        const val PARALLEL_DECODES = 4
    }
}
