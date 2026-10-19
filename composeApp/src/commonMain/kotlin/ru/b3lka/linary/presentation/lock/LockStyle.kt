package ru.b3lka.linary.presentation.lock

/**
 * Вид экрана блокировки.
 */
enum class LockStyle {
    /**
     * Непрозрачный фон: под ним ничего не видно.
     */
    Solid,

    /**
     * Размытое окно приложения под полупрозрачной вуалью.
     */
    Blurred,
}
