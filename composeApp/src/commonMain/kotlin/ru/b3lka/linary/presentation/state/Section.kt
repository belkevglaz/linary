package ru.b3lka.linary.presentation.state

/**
 * Разделы — как в боковом меню Diarium (без «Карты», «Экспорта» и «Настроек»: клиент только читает).
 */
enum class Section {
    Calendar,
    Feed,
    Attachments,
    Search,
    Tags,
    OnThisDay,
}
