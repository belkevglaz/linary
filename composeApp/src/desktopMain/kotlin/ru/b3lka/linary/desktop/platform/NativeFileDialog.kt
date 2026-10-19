package ru.b3lka.linary.desktop.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Системный диалог выбора файлов: `kdialog` (KDE, LXQt) или `zenity` (GNOME, XFCE, Cinnamon и др.).
 * Запускается отдельным процессом, поэтому окно приложения не зависает, пока диалог открыт.
 */
internal object NativeFileDialog {
    private const val EXIT_OK = 0
    private const val EXIT_CANCEL = 1
    private const val WAIT_HOURS = 24L

    /**
     * Фильтр файлов: [patterns] — шаблоны вида `*.jpg` или точные имена (`v4_database`).
     */
    class Filter(val name: String, val patterns: List<String>)

    internal enum class Tool(val command: String) { KDialog("kdialog"), Zenity("zenity") }

    /**
     * Выбранные файлы; пустой список — пользователь отменил. null — системного диалога нет
     * или он не запустился.
     */
    suspend fun openFiles(title: String, filter: Filter, multiple: Boolean, start: File?): List<File>? =
        firstAvailable { tool -> command(tool, title, filter, multiple, start) }

    /**
     * Путь для сохранения [suggested] (папка и предлагаемое имя). Пустой список — отмена,
     * null — системного диалога нет. Перезапись существующего файла диалоги подтверждают сами.
     */
    suspend fun saveFile(title: String, suggested: File): List<File>? =
        firstAvailable { tool -> saveCommand(tool, title, suggested) }

    private suspend fun firstAvailable(command: (Tool) -> List<String>): List<File>? = withContext(Dispatchers.IO) {
        tools().firstNotNullOfOrNull { tool -> runCatching { run(command(tool)) }.getOrNull() }
    }

    private fun run(command: List<String>): List<File>? {
        val process = ProcessBuilder(command)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor(WAIT_HOURS, TimeUnit.HOURS)
        return when (process.exitValue()) {
            EXIT_OK -> output.lines().filter { it.isNotBlank() }.map(::File)
            EXIT_CANCEL -> emptyList()
            else -> null
        }
    }

    internal fun command(tool: Tool, title: String, filter: Filter, multiple: Boolean, start: File?): List<String> {
        val dir = (start?.takeIf { it.isDirectory } ?: File(System.getProperty("user.home"))).absolutePath
        val patterns = filter.patterns.flatMap { listOf(it, it.uppercase()) }.distinct().joinToString(" ")
        return when (tool) {
            Tool.KDialog -> buildList {
                addAll(listOf("kdialog", "--title", title, "--getopenfilename", dir, "$patterns|${filter.name}"))
                if (multiple) addAll(listOf("--multiple", "--separate-output"))
            }
            Tool.Zenity -> buildList {
                addAll(listOf("zenity", "--file-selection", "--title=$title", "--filename=$dir/"))
                add("--file-filter=${filter.name} | $patterns")
                if (multiple) addAll(listOf("--multiple", "--separator=\n"))
            }
        }
    }

    internal fun saveCommand(tool: Tool, title: String, suggested: File): List<String> = when (tool) {
        Tool.KDialog -> listOf("kdialog", "--title", title, "--getsavefilename", suggested.absolutePath)
        Tool.Zenity -> listOf(
            "zenity",
            "--file-selection",
            "--save",
            "--title=$title",
            "--filename=${suggested.absolutePath}",
        )
    }

    /**
     * Доступные утилиты в порядке предпочтения для текущего рабочего стола.
     */
    private fun tools(): List<Tool> {
        val desktop = System.getenv("XDG_CURRENT_DESKTOP").orEmpty().uppercase()
        val preferred = if ("KDE" in desktop || "LXQT" in desktop) {
            listOf(Tool.KDialog, Tool.Zenity)
        } else {
            listOf(Tool.Zenity, Tool.KDialog)
        }
        return preferred.filter { onPath(it.command) }
    }

    private fun onPath(command: String): Boolean =
        System.getenv("PATH").orEmpty().split(File.pathSeparator).any { File(it, command).canExecute() }
}
