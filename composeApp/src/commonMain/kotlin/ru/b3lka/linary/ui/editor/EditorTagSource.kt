package ru.b3lka.linary.ui.editor

import ru.b3lka.linary.core.model.DiaryTag
import ru.b3lka.linary.core.model.TagChange

/**
 * Теги для редактора: все теги журнала и создание нового (возвращает его id или null при ошибке).
 */
class EditorTagSource(val all: List<DiaryTag>, val create: suspend (TagChange) -> Long?)
