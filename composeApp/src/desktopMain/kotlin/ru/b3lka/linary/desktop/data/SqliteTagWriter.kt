package ru.b3lka.linary.desktop.data

import org.sqlite.SQLiteConfig
import ru.b3lka.linary.core.model.TagChange
import ru.b3lka.linary.core.repository.DiaryWriteException
import java.io.File

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
            val id = change.id
            if (id == null) {
                c.prepareStatement("insert into Tags(Value, Type, Color, Icon) values (?, ?, ?, null)").use { st ->
                    st.setString(1, change.value)
                    st.setInt(2, TYPE_PLAIN)
                    st.setString(3, change.color)
                    st.executeUpdate()
                }
                c.createStatement().use { st ->
                    st.executeQuery("select last_insert_rowid()").use { rs ->
                        rs.next()
                        rs.getLong(1)
                    }
                }
            } else {
                val sql = "update Tags set Value = ?, Color = ? where DiaryTagId = ?"
                val updated = c.prepareStatement(sql).use { st ->
                    st.setString(1, change.value)
                    st.setString(2, change.color)
                    st.setLong(3, id)
                    st.executeUpdate()
                }
                if (updated == 0) throw DiaryWriteException(DiaryWriteException.Reason.Conflict, "tag $id")
                id
            }
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
}
