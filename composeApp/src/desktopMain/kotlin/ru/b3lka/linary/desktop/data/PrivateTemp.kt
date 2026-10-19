package ru.b3lka.linary.desktop.data

import ru.b3lka.linary.desktop.APP_ID
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

/**
 * Временные файлы с расшифрованными данными (база, вложения для внешнего плеера).
 *
 * Лежат в `$XDG_RUNTIME_DIR/linary` — это tmpfs в памяти, доступная только пользователю
 * и очищаемая при выходе из сеанса; если её нет — в `/tmp/linary-<user>`. Права — только владелец.
 * Имена начинаются с PID процесса: при выходе удаляется всё своё, а при запуске — то, что осталось
 * от уже не работающих процессов (после падения или `kill`).
 */
internal object PrivateTemp {
    private val pid = ProcessHandle.current().pid()
    private val posix = "posix" in FileSystems.getDefault().supportedFileAttributeViews()

    val dir: File by lazy {
        createDir().also { dir ->
            removeStale(dir)
            Runtime.getRuntime().addShutdownHook(Thread { removeOwn(dir) })
        }
    }

    /**
     * Папка этого процесса для файлов, которые открываются во внешних программах.
     */
    val filesDir: File by lazy { File(dir, "$pid-files").apply { mkdirs() }.also { restrict(it.toPath(), DIR_MODE) } }

    fun file(prefix: String, suffix: String): File {
        val path = Files.createTempFile(dir.toPath(), "$pid-$prefix", suffix)
        restrict(path, FILE_MODE)
        return path.toFile()
    }

    private fun createDir(): File {
        val runtime = System.getenv("XDG_RUNTIME_DIR")?.let(::File)?.takeIf { it.isDirectory && it.canWrite() }
        val preferred = runtime?.let { File(it, APP_DIR) }
            ?: File(System.getProperty("java.io.tmpdir"), "$APP_DIR-${System.getProperty("user.name")}")
        preferred.mkdirs()
        // Чужая папка с тем же именем в общем /tmp — не используем
        val owned = runCatching {
            Files.getOwner(preferred.toPath()).name == System.getProperty("user.name")
        }.getOrDefault(false)
        val dir = if (owned) preferred.toPath() else Files.createTempDirectory(APP_DIR)
        restrict(dir, DIR_MODE)
        return dir.toFile()
    }

    private fun removeStale(dir: File) {
        dir.listFiles().orEmpty().forEach { file ->
            val owner = file.name.substringBefore('-').toLongOrNull() ?: return@forEach
            if (owner != pid && !ProcessHandle.of(owner).isPresent) file.deleteRecursively()
        }
    }

    private fun removeOwn(dir: File) {
        dir.listFiles { file -> file.name.startsWith("$pid-") }.orEmpty().forEach { it.deleteRecursively() }
    }

    private fun restrict(path: Path, mode: String) {
        if (posix) runCatching { Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(mode)) }
    }

    private const val APP_DIR = APP_ID
    private const val DIR_MODE = "rwx------"
    private const val FILE_MODE = "rw-------"
}
