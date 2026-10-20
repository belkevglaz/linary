package ru.b3lka.linary.desktop.data

import ru.b3lka.linary.core.model.TagChange
import ru.b3lka.linary.core.repository.DiaryWriteException
import java.io.File
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SqliteTagWriterTest {

    private fun database(): File {
        val file = File.createTempFile("linary-tags", ".sqlite")
        file.delete()
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { c ->
            c.createStatement().use {
                it.executeUpdate(
                    "CREATE TABLE Tags(DiaryTagId integer primary key autoincrement not null, " +
                        "Value varchar not null, Type integer not null, Color varchar, Icon varchar)",
                )
                it.executeUpdate("insert into Tags(Value, Type) values ('monitor', 0)")
            }
        }
        return file
    }

    @Test
    fun createAndRecolorTag() {
        val db = database()
        val created = TagChange(id = null, value = "work", color = "#FF1E88E5")
        val id = SqliteTagWriter.apply(db, created)
        assertTrue(SqliteTagWriter.contains(db, id, created))

        val recolored = TagChange(id = id, value = "job", color = null)
        assertEquals(id, SqliteTagWriter.apply(db, recolored))
        assertTrue(SqliteTagWriter.contains(db, id, recolored))

        val names = DriverManager.getConnection("jdbc:sqlite:${db.absolutePath}").use { c ->
            c.createStatement().use { st ->
                st.executeQuery("select Value from Tags order by DiaryTagId").use { rs ->
                    buildList { while (rs.next()) add(rs.getString(1)) }
                }
            }
        }
        assertEquals(listOf("monitor", "job"), names)
        db.delete()
    }

    @Test
    fun editingMissingTagIsConflict() {
        val db = database()
        val error = assertFailsWith<DiaryWriteException> {
            SqliteTagWriter.apply(db, TagChange(id = 999, value = "x", color = null))
        }
        assertEquals(DiaryWriteException.Reason.Conflict, error.reason)
        db.delete()
    }
}
