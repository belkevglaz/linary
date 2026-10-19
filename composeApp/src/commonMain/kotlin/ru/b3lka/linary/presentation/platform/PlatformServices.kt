package ru.b3lka.linary.presentation.platform

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import ru.b3lka.linary.core.model.DiaryMedia

/**
 * То, что каждая платформа реализует сама.
 */
interface PlatformServices {

    /**
     * Версия установленного приложения; null — запуск из исходников (`./gradlew run`).
     */
    val appVersion: String? get() = null

    /**
     * Декодирует картинку. Если задан [maxSize], уменьшает её так, чтобы большая сторона была не больше.
     */
    fun decodeImage(bytes: ByteArray, maxSize: Int?): ImageBitmap?

    /**
     * Открывает вложение (видео, аудио) во внешнем приложении.
     */
    fun openExternally(media: DiaryMedia, bytes: ByteArray)

    /**
     * Открывает ссылку в браузере по умолчанию.
     */
    fun openUrl(url: String)

    /**
     * Диалог выбора картинок. Пустой список — пользователь ничего не выбрал.
     */
    suspend fun chooseImages(title: String, filterName: String): List<ImageSource>

    /**
     * Картинки из буфера обмена: скриншот или скопированные файлы. Пустой список — картинок там нет.
     */
    fun clipboardImages(): List<ImageSource>

    /**
     * Предлагает выбрать, куда сохранить файл [fileName], и записывает [bytes]. false — отмена.
     */
    suspend fun saveFile(title: String, fileName: String, bytes: ByteArray): Boolean

    /**
     * Модификатор, принимающий перетаскиваемые картинки (файлы из файлового менеджера, картинки из браузера).
     * [onActive] — над областью сейчас что-то перетаскивают; [onDrop] — получены картинки.
     */
    fun imageDropTarget(onActive: (Boolean) -> Unit, onDrop: (List<ImageSource>) -> Unit): Modifier
}
