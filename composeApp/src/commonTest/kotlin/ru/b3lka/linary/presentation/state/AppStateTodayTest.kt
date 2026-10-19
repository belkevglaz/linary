package ru.b3lka.linary.presentation.state

import kotlinx.datetime.LocalDate
import ru.b3lka.linary.core.model.Diary
import ru.b3lka.linary.core.model.DiaryMedia
import ru.b3lka.linary.core.repository.DiaryRepository
import ru.b3lka.linary.core.repository.DiarySource
import kotlin.test.Test
import kotlin.test.assertEquals

class AppStateTodayTest {

    private val repository = object : DiaryRepository {
        override val source = DiarySource(DiarySource.Kind.Backup, "test")
        override suspend fun load() = Diary(emptyList(), emptyList())
        override suspend fun loadMediaBytes(media: DiaryMedia) = ByteArray(0)
    }

    private var date = LocalDate(2026, 10, 17)
    private val state = AppState(repository) { date }

    @Test
    fun selectionFollowsTodayAfterMidnight() {
        assertEquals(date, state.selectedDate)
        date = LocalDate(2026, 10, 18)
        state.refreshToday()
        assertEquals(date, state.today)
        assertEquals(date, state.selectedDate)
    }

    @Test
    fun otherSelectedDayIsKept() {
        val chosen = LocalDate(2026, 10, 1)
        state.selectDate(chosen)
        date = LocalDate(2026, 10, 18)
        state.refreshToday()
        assertEquals(date, state.today)
        assertEquals(chosen, state.selectedDate)
    }

    @Test
    fun todayButtonUsesCurrentDate() {
        state.section = Section.Feed
        date = LocalDate(2026, 10, 18)
        state.goToToday()
        assertEquals(Section.Calendar, state.section)
        assertEquals(date, state.selectedDate)
    }
}
