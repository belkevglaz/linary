package ru.b3lka.linary.presentation.editor

import ru.b3lka.linary.core.model.DiaryMedia
import ru.b3lka.linary.core.model.MediaType
import ru.b3lka.linary.core.model.NewMedia

/**
 * Вложение в редакторе: уже сохранённое в дневнике или добавленное в этой правке.
 */
sealed interface Attachment {
    val id: String
    val isImage: Boolean

    /**
     * Вложение, которое уже есть в дневнике.
     */
    class Saved(val media: DiaryMedia) : Attachment {
        override val id: String get() = media.id
        override val isImage: Boolean get() = media.type == MediaType.Image
    }

    /**
     * Новая картинка, ещё не сохранённая.
     */
    class Added(val media: NewMedia) : Attachment {
        override val id: String get() = media.id
        override val isImage: Boolean get() = true
    }
}
