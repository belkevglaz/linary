package ru.b3lka.linary.desktop.platform

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class NativeFileDialogTest {
    private val filter = NativeFileDialog.Filter("Images", listOf("*.jpg", "*.png"))
    private val start: File = Files.createTempDirectory("linary-dialog").toFile()

    @Test
    fun kdialogCommand() {
        val command = NativeFileDialog.command(NativeFileDialog.Tool.KDialog, "Add", filter, multiple = true, start)
        assertEquals(
            listOf(
                "kdialog",
                "--title",
                "Add",
                "--getopenfilename",
                start.absolutePath,
                "*.jpg *.JPG *.png *.PNG|Images",
                "--multiple",
                "--separate-output",
            ),
            command,
        )
    }

    @Test
    fun saveCommands() {
        val target = File(start, "photo.jpg")
        assertEquals(
            listOf("kdialog", "--title", "Save", "--getsavefilename", target.absolutePath),
            NativeFileDialog.saveCommand(NativeFileDialog.Tool.KDialog, "Save", target),
        )
        assertEquals(
            listOf("zenity", "--file-selection", "--save", "--title=Save", "--filename=${target.absolutePath}"),
            NativeFileDialog.saveCommand(NativeFileDialog.Tool.Zenity, "Save", target),
        )
    }

    @Test
    fun zenityCommand() {
        val command = NativeFileDialog.command(NativeFileDialog.Tool.Zenity, "Add", filter, multiple = false, start)
        assertEquals(
            listOf(
                "zenity",
                "--file-selection",
                "--title=Add",
                "--filename=${start.absolutePath}/",
                "--file-filter=Images | *.jpg *.JPG *.png *.PNG",
            ),
            command,
        )
    }
}
