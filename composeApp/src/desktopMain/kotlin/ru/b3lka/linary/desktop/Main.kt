package ru.b3lka.linary.desktop

import androidx.compose.ui.window.application
import ru.b3lka.linary.desktop.instance.SingleInstance
import ru.b3lka.linary.desktop.settings.LegacyMigration
import ru.b3lka.linary.desktop.ui.LinaryApplication

/**
 * Точка входа. Если приложение уже запущено, показывает его окно и сразу завершается.
 */
fun main(args: Array<String>) {
    LegacyMigration.run()
    val instance = SingleInstance.acquire() ?: return
    application { LinaryApplication(args.firstOrNull(), instance) }
}
