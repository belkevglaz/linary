package ru.b3lka.linary.presentation.state

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import ru.b3lka.linary.core.model.Diary
import ru.b3lka.linary.core.model.DiaryEntry
import ru.b3lka.linary.core.model.DiaryMedia
import ru.b3lka.linary.core.repository.DiaryLoadException
import ru.b3lka.linary.core.repository.DiaryRepository
import ru.b3lka.linary.core.time.YearMonth

/**
 * Состояние экрана дневника: загруженный журнал, раздел, выбор, поиск. Живёт, пока открыт
 * один и тот же источник. Правка и удаление записей — в [editing].
 *
 * @param currentDate сегодняшняя дата; подменяется в тестах.
 */
class AppState(
    val repository: DiaryRepository,
    private val currentDate: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) {
    /**
     * Сегодня. Приложение может работать сутками (в трее), поэтому дата обновляется: см. [refreshToday].
     */
    var today by mutableStateOf(currentDate())
        private set

    var diary by mutableStateOf<Diary?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<DiaryLoadException?>(null)
        private set

    var section by mutableStateOf(Section.Calendar)
    var selectedDate by mutableStateOf(today)
        private set
    var selectedEntryId by mutableStateOf<Long?>(null)
        private set
    var query by mutableStateOf("")
    var tagFilter by mutableStateOf<Long?>(null)
    var viewer by mutableStateOf<ViewerState?>(null)

    /**
     * Дневник изменился на диске (например, Dropbox докачал правки с телефона) и ждёт перечитывания.
     */
    var externalChangePending by mutableStateOf(false)
        private set

    val editing = EditingState(this)

    val canEdit: Boolean get() = repository.isWritable && diary != null

    val selectedEntry: DiaryEntry? by derivedStateOf {
        diary?.entries?.firstOrNull { it.id == selectedEntryId }
    }

    /**
     * Месяцы для календаря: от первой записи до текущего месяца.
     */
    val calendarMonths: List<YearMonth> by derivedStateOf {
        val last = YearMonth.of(today)
        val first = diary?.entries?.lastOrNull()?.let { YearMonth.of(it.date) } ?: last
        YearMonth.range(minOf(first, last), last)
    }

    /**
     * Записи для списка слева в текущем разделе.
     */
    val listedEntries: List<DiaryEntry> by derivedStateOf {
        val all = diary?.entries.orEmpty()
        when (section) {
            Section.Calendar, Section.Feed, Section.Attachments -> all
            Section.Search -> search(all, query.trim())
            Section.Tags -> tagFilter?.let { id -> all.filter { e -> e.tags.any { it.tag.id == id } } }.orEmpty()
            Section.OnThisDay -> all.filter { isSameDayInPreviousYear(it.date) }
        }
    }

    /**
     * Перечитывает журнал. Выбранная запись сохраняется; если её больше нет — остаётся выбранный день.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun reload() {
        loading = true
        error = null
        externalChangePending = false
        try {
            val hadDiary = diary != null
            val loaded = repository.load()
            diary = loaded
            val keep = selectedEntryId?.let { id -> loaded.entries.firstOrNull { it.id == id } }
            when {
                keep != null -> selectEntry(keep)
                hadDiary -> selectDate(selectedDate)
                else -> selectDate(today)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: DiaryLoadException) {
            error = e
        } catch (e: Exception) {
            // Неожиданная ошибка не должна ронять приложение
            error = DiaryLoadException(DiaryLoadException.Reason.Unexpected, e.message ?: e::class.simpleName, e)
        } finally {
            loading = false
        }
    }

    /**
     * Отмечает, что дневник изменился на диске. Перечитывание — [reloadIfIdle].
     */
    fun onExternalChange() {
        externalChangePending = true
    }

    /**
     * Перечитывает дневник, если он изменился на диске и сейчас ничего не редактируется:
     * иначе правка «уехала» бы из-под редактора. После закрытия редактора вызывается снова.
     */
    suspend fun reloadIfIdle() {
        if (externalChangePending && isIdle) reload()
    }

    /**
     * Ничего не редактируется, не сохраняется и не загружается.
     */
    private val isIdle: Boolean get() = editing.editor == null && !editing.saving && !loading

    fun selectDate(date: LocalDate) {
        selectedDate = date
        selectedEntryId = diary?.byDate?.get(date)?.firstOrNull()?.id
    }

    fun selectEntry(entry: DiaryEntry) {
        selectedDate = entry.date
        selectedEntryId = entry.id
    }

    /**
     * Выбор дня и конкретной записи (её может ещё не быть в загруженном журнале — например, только что сохранённой).
     */
    internal fun select(date: LocalDate, entryId: Long?) {
        selectedDate = date
        selectedEntryId = entryId
    }

    fun openEntryOf(media: DiaryMedia) {
        val entry = diary?.entries?.firstOrNull { it.id == media.entryId } ?: return
        viewer = null
        section = Section.Feed
        selectEntry(entry)
    }

    fun goToToday() {
        refreshToday()
        section = Section.Calendar
        selectDate(today)
    }

    /**
     * Наступили новые сутки — обновляет [today]. Если был выбран прежний «сегодня»,
     * выбор переходит на новый день (кроме времени, пока запись редактируется).
     */
    fun refreshToday() {
        val date = currentDate()
        if (date == today) return
        val previous = today
        today = date
        if (selectedDate == previous && editing.editor == null) selectDate(date)
    }

    /**
     * Записи дня по времени создания.
     */
    fun entriesOf(date: LocalDate): List<DiaryEntry> = diary?.entriesOn(date).orEmpty()

    fun now(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    /**
     * Переход к соседней записи: [direction] = +1 — более ранняя, -1 — более поздняя.
     */
    fun stepEntry(direction: Int) {
        val list = if (section == Section.Calendar) diary?.entries.orEmpty() else listedEntries
        val i = list.indexOfFirst { it.id == selectedEntryId }
        val next = if (i < 0) list.firstOrNull() else list.getOrNull(i + direction)
        next?.let { selectEntry(it) }
    }

    private fun search(all: List<DiaryEntry>, q: String): List<DiaryEntry> {
        if (q.isEmpty()) return emptyList()
        return all.filter { e ->
            e.plainText.contains(q, ignoreCase = true) ||
                e.heading.contains(q, ignoreCase = true) ||
                e.tags.any { it.tag.value.contains(q, ignoreCase = true) }
        }
    }

    private fun isSameDayInPreviousYear(date: LocalDate): Boolean =
        date.monthNumber == today.monthNumber && date.dayOfMonth == today.dayOfMonth && date.year < today.year
}
