package ru.b3lka.linary.presentation.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.b3lka.linary.core.html.DiariumHtmlWriter
import ru.b3lka.linary.core.model.DiaryEntry
import ru.b3lka.linary.core.model.DiaryMedia
import ru.b3lka.linary.core.model.EntryChange
import ru.b3lka.linary.core.repository.DiaryWriteException
import ru.b3lka.linary.presentation.editor.EditorSession

/**
 * Правка, сохранение и удаление записей.
 */
class EditingState internal constructor(private val app: AppState) {

    var editor by mutableStateOf<EditorSession?>(null)
        private set
    var saving by mutableStateOf(false)
        private set

    /**
     * Ошибка последнего сохранения или удаления; показывается диалогом.
     */
    var error by mutableStateOf<DiaryWriteException?>(null)

    /**
     * Запись, удаление которой ждёт подтверждения.
     */
    var pendingDelete by mutableStateOf<DiaryEntry?>(null)
        private set

    /**
     * HTML последней попытки сохранения — для «Сохранить всё равно» после конфликта.
     */
    private var lastSavedHtml: String? = null

    /**
     * Можно ли после ошибки сохранить текст редактора поверх версии с другого устройства.
     */
    val canSaveOverConflict: Boolean
        get() = error?.reason == DiaryWriteException.Reason.Conflict && editor != null && lastSavedHtml != null

    /**
     * Открывает редактор для записи.
     */
    fun startEditing(entry: DiaryEntry) {
        if (!app.canEdit) return
        leaveAttachments()
        app.selectEntry(entry)
        editor = EditorSession(entry, entry.dateTime)
    }

    /**
     * Новая запись на день [date] с текущим временем. В один день может быть несколько записей.
     */
    fun startNewEntry(date: LocalDate = app.selectedDate) {
        if (!app.canEdit) return
        leaveAttachments()
        app.selectDate(date)
        editor = EditorSession(original = null, dateTime = LocalDateTime(date, app.now().time))
    }

    fun cancelEditing() {
        editor = null
    }

    /**
     * Сохраняет запись. [editorHtml] — HTML из редактора; он приводится к формату Diarium.
     */
    suspend fun save(editorHtml: String) {
        val session = editor ?: return
        if (session.preparingMedia > 0) return
        lastSavedHtml = editorHtml
        writing {
            val change = EntryChange(
                original = session.original,
                dateTime = session.dateTime,
                html = DiariumHtmlWriter.normalize(editorHtml),
                rating = session.rating,
                addedMedia = session.addedMedia.toList(),
                removedMediaIds = session.removedMediaIds,
                mediaOrder = session.mediaOrder,
            )
            val id = app.repository.saveEntry(change)
            editor = null
            lastSavedHtml = null
            app.select(change.dateTime.date, id)
            app.reload()
        }
    }

    /**
     * После конфликта: перечитать дневник и сохранить текст редактора поверх версии с другого устройства.
     * Если запись там удалили, она сохранится заново.
     */
    suspend fun saveOverConflict() {
        if (!canSaveOverConflict) return
        val session = editor ?: return
        val html = lastSavedHtml ?: return
        error = null
        app.reload()
        val fresh = session.original?.let { old -> app.diary?.entries?.firstOrNull { it.id == old.id } }
        session.rebase(fresh)
        save(html)
    }

    /**
     * Делает фото [media] обложкой его записи (переносит на первое место) и сразу сохраняет.
     */
    suspend fun setCover(media: DiaryMedia) {
        val entry = app.diary?.entries?.firstOrNull { it.id == media.entryId } ?: return
        val order = entry.media.sortedBy { it.index }.map { it.id }
        if (order.firstOrNull() == media.id || !app.canEdit) return
        lastSavedHtml = null
        writing {
            app.repository.saveEntry(
                EntryChange(
                    original = entry,
                    dateTime = entry.dateTime,
                    html = entry.html,
                    rating = entry.rating,
                    mediaOrder = listOf(media.id) + order.filter { it != media.id },
                ),
            )
            app.reload()
        }
    }

    /**
     * Просит подтвердить удаление [entry].
     */
    fun requestDelete(entry: DiaryEntry) {
        if (app.canEdit) pendingDelete = entry
    }

    fun cancelDelete() {
        pendingDelete = null
    }

    /**
     * Удаляет запись, удаление которой подтвердили. Если она открыта в редакторе, редактор закрывается.
     * После удаления выбирается другая запись того же дня или сам день.
     */
    suspend fun confirmDelete() {
        val entry = pendingDelete ?: return
        pendingDelete = null
        lastSavedHtml = null
        writing {
            app.repository.deleteEntry(entry)
            if (editor?.original?.id == entry.id) editor = null
            app.select(entry.date, app.entriesOf(entry.date).lastOrNull { it.id != entry.id }?.id)
            app.reload()
        }
    }

    /**
     * Выполняет запись; любая ошибка — в [error], приложение не падает.
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun writing(block: suspend () -> Unit) {
        saving = true
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: DiaryWriteException) {
            error = e
        } catch (e: Exception) {
            error = DiaryWriteException(DiaryWriteException.Reason.Unexpected, e.message ?: e::class.simpleName, e)
        } finally {
            saving = false
        }
    }

    private fun leaveAttachments() {
        if (app.section == Section.Attachments) app.section = Section.Feed
    }
}
