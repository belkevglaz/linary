package ru.b3lka.linary.desktop.data

import org.sqlite.SQLiteConfig
import ru.b3lka.linary.core.model.TagChange
import ru.b3lka.linary.core.repository.DiaryWriteException
import java.io.File
import java.sql.Connection
import java.sql.PreparedStatement

/**
 * Запись тегов в таблицу `Tags` журнала: `DiaryTagId` (autoincrement), `Value`, `Type` (0 — обычный тег),
 * `Color` (`#AARRGGBB` или NULL), `Icon` (NULL).
 */
internal object SqliteTagWriter {
    private const val TYPE_PLAIN = 0

    /**
     * Создаёт тег или меняет название и цвет существующего; возвращает его идентификатор.
     * Если изменяемого тега в базе уже нет (удалили на другом устройстве) — Conflict.
     */
    fun apply(file: File, change: TagChange): Long =
        SQLiteConfig().createConnection("jdbc:sqlite:${file.absolutePath}").use { c ->
            change.id?.let { update(c, it, change) } ?: insert(c, change)
        }

    /**
     * Есть ли в базе [file] тег [id] с названием и цветом из [change].
     */
    fun contains(file: File, id: Long, change: TagChange): Boolean =
        SqliteDiaryReader.open(file).use { c ->
            c.prepareStatement("select Value, Color from Tags where DiaryTagId = ?").use { st ->
                st.setLong(1, id)
                st.executeQuery().use { rs ->
                    rs.next() && rs.getString(1) == change.value && rs.getString(2) == change.color
                }
            }
        }

    private fun insert(c: Connection, change: TagChange): Long {
        c.prepareStatement("insert into Tags(Value, Type, Color, Icon) values (?, ?, ?, null)").use { st ->
            st.bind(change.value, TYPE_PLAIN, change.color).executeUpdate()
        }
        return c.createStatement().use { st ->
            st.executeQuery("select last_insert_rowid()").use { rs -> if (rs.next()) rs.getLong(1) else error("no id") }
        }
    }

    private fun update(c: Connection, id: Long, change: TagChange): Long {
        val updated = c.prepareStatement("update Tags set Value = ?, Color = ? where DiaryTagId = ?").use { st ->
            st.bind(change.value, change.color, id).executeUpdate()
        }
        if (updated == 0) throw DiaryWriteException(DiaryWriteException.Reason.Conflict, "tag $id")
        return id
    }

    /**
     * Подставляет параметры запроса по порядку — по одному на каждый `?` слева направо (null — NULL).
     */
    private fun PreparedStatement.bind(vararg values: Any?): PreparedStatement = apply {
        values.forEachIndexed { i, value -> setObject(i + 1, value) }
    }
}
