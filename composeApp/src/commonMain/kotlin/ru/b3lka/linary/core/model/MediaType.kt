package ru.b3lka.linary.core.model

/**
 * Тип вложения записи.
 */
enum class MediaType {
    Image,
    Audio,
    Video,
    Other,
    ;

    companion object {
        private const val CODE_IMAGE = 0
        private const val CODE_AUDIO = 3
        private const val CODE_VIDEO = 4

        private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "bmp")
        private val videoExtensions = setOf("mp4", "mov", "webm", "mkv", "3gp")
        private val audioExtensions = setOf("opus", "ogg", "m4a", "mp3", "aac", "wav")

        /**
         * Определяет тип по коду из колонки `Media.Type` (0 — фото, 3 — аудио, 4 — видео),
         * а для неизвестных кодов — по расширению файла.
         */
        fun of(code: Int, fileEnding: String): MediaType = when (code) {
            CODE_IMAGE -> Image
            CODE_AUDIO -> Audio
            CODE_VIDEO -> Video
            else -> byExtension(fileEnding.lowercase().removePrefix("."))
        }

        private fun byExtension(ext: String): MediaType = when (ext) {
            in imageExtensions -> Image
            in videoExtensions -> Video
            in audioExtensions -> Audio
            else -> Other
        }
    }
}
