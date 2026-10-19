package ru.b3lka.linary.core.model

import kotlinx.datetime.LocalDate

/**
 * Загруженный журнал целиком.
 */
class Diary(
    entries: List<DiaryEntry>,
    val tags: List<DiaryTag>,
) {
    /**
     * Записи от новых к старым.
     */
    val entries: List<DiaryEntry> = entries.sortedByDescending { it.id }

    /**
     * Записи по дням, внутри дня — от новых к старым.
     */
    val byDate: Map<LocalDate, List<DiaryEntry>> = this.entries.groupBy { it.date }

    /**
     * Записи дня по времени создания (от ранних к поздним).
     */
    fun entriesOn(date: LocalDate): List<DiaryEntry> = byDate[date]?.asReversed().orEmpty()

    /**
     * Сколько записей у каждого тега.
     */
    val tagCounts: Map<Long, Int> by lazy {
        this.entries.flatMap { e -> e.tags.map { it.tag.id } }.groupingBy { it }.eachCount()
    }
}
