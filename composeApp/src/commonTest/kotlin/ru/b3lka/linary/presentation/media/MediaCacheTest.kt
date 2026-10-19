package ru.b3lka.linary.presentation.media

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.runBlocking
import ru.b3lka.linary.core.model.Diary
import ru.b3lka.linary.core.model.DiaryMedia
import ru.b3lka.linary.core.model.MediaType
import ru.b3lka.linary.core.repository.DiaryRepository
import ru.b3lka.linary.core.repository.DiarySource
import ru.b3lka.linary.presentation.platform.ImageSource
import ru.b3lka.linary.presentation.platform.PlatformServices
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MediaCacheTest {

    private val repository = object : DiaryRepository {
        override val source = DiarySource(DiarySource.Kind.Backup, "test")
        override suspend fun load() = Diary(emptyList(), emptyList())
        override suspend fun loadMediaBytes(media: DiaryMedia) = ByteArray(0)
    }

    /**
     * «Декодирует» любую картинку в квадрат 100×100 (40 000 байт).
     */
    private val platform = object : PlatformServices {
        override fun decodeImage(bytes: ByteArray, maxSize: Int?) = ImageBitmap(SIDE, SIDE)
        override fun openExternally(media: DiaryMedia, bytes: ByteArray) = Unit
        override fun openUrl(url: String) = Unit
        override suspend fun chooseImages(title: String, filterName: String) = emptyList<ImageSource>()
        override fun clipboardImages() = emptyList<ImageSource>()
        override suspend fun saveFile(title: String, fileName: String, bytes: ByteArray) = false
        override fun imageDropTarget(onActive: (Boolean) -> Unit, onDrop: (List<ImageSource>) -> Unit) = Modifier
    }

    private fun media(id: String) = DiaryMedia(id, MediaType.Image, id, "jpg", 0, 0)

    @Test
    fun leastRecentlyUsedImageIsEvictedWhenBudgetIsExceeded() = runBlocking<Unit> {
        val cache = MediaCache(repository, platform, budgetBytes = 2L * SIDE * SIDE * 4)
        cache.image(media("a"), null)
        cache.image(media("b"), null)
        assertNotNull(cache.peek(media("a"), null)) // «a» теперь использовалась последней

        cache.image(media("c"), null)

        assertNull(cache.peek(media("b"), null))
        assertNotNull(cache.peek(media("a"), null))
        assertNotNull(cache.peek(media("c"), null))
    }

    private companion object {
        const val SIDE = 100
    }
}
