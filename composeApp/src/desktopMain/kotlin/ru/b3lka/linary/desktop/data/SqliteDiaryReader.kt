package ru.b3lka.linary.desktop.data

import org.sqlite.SQLiteConfig
import ru.b3lka.linary.core.model.Diary
import ru.b3lka.linary.core.model.DiaryEntry
import ru.b3lka.linary.core.model.DiaryMedia
import ru.b3lka.linary.core.model.DiaryTag
import ru.b3lka.linary.core.model.EntryTag
import ru.b3lka.linary.core.model.MediaType
import ru.b3lka.linary.core.repository.DiaryLoadException
import ru.b3lka.linary.core.time.Ticks
import java.io.File
import java.sql.Connection
import java.sql.ResultSet

/**
 * Чтение базы Diarium. Схема одинакова у бэкапа `.diary` и у расшифрованного `v4_database`.
 */
internal object SqliteDiaryReader {

    fun open(file: File): Connection {
        val config = SQLiteConfig().apply { setReadOnly(true) }
        return config.createConnection("jdbc:sqlite:${file.absolutePath}")
    }

    fun read(file: File): Diary = open(file).use { read(it) }

    fun read(connection: Connection): Diary {
        val tables = connection.tables()
        if ("Entries" !in tables) throw DiaryLoadException(DiaryLoadException.Reason.NotDiaryDatabase)

        val tags = if ("Tags" in tables) readTags(connection) else emptyMap()
        val entryTags = if ("EntryTags" in tables) readEntryTags(connection, tags) else emptyMap()
        val media = if ("Media" in tables) readMedia(connection) else emptyMap()
        val entries = connection.query("select * from Entries") { rs ->
            val id = rs.getLong("DiaryEntryId")
            DiaryEntry(
                id = id,
                dateTime = Ticks.toLocalDateTime(id),
                heading = rs.getString("Heading").orEmpty(),
                html = rs.getString("Text").orEmpty(),
                rating = rs.getInt("Rating"),
                latitude = rs.getDouble("Latitude"),
                longitude = rs.getDouble("Longitude"),
                tags = entryTags[id].orEmpty(),
                media = media[id].orEmpty().sortedBy { it.index },
            )
        }
        return Diary(entries, tags.values.toList())
    }

    private fun readTags(connection: Connection): Map<Long, DiaryTag> {
        val columns = connection.columns("Tags")
        return connection.query("select * from Tags") { rs ->
            DiaryTag(
                id = rs.getLong("DiaryTagId"),
                value = rs.getString("Value").orEmpty(),
                type = if ("Type" in columns) rs.getInt("Type") else 0,
                color = if ("Color" in columns) rs.getString("Color") else null,
                icon = if ("Icon" in columns) rs.getString("Icon") else null,
            )
        }.associateBy { it.id }
    }

    private fun readEntryTags(connection: Connection, tags: Map<Long, DiaryTag>): Map<Long, List<EntryTag>> {
        val hasTracking = "TrackingValue" in connection.columns("EntryTags")
        return connection.query("select * from EntryTags") { rs ->
            val tag = tags[rs.getLong("DiaryTagId")]
            val value = if (hasTracking) rs.getString("TrackingValue") else null
            rs.getLong("DiaryEntryId") to tag?.let { EntryTag(it, value) }
        }
            .mapNotNull { (entryId, tag) -> tag?.let { entryId to it } }
            .groupBy({ it.first }, { it.second })
    }

    private fun readMedia(connection: Connection): Map<Long, List<DiaryMedia>> =
        connection.query("select DiaryMediaId, Type, Name, FileEnding, \"Index\", DiaryEntryId from Media") { rs ->
            val ending = rs.getString("FileEnding").orEmpty().removePrefix(".")
            DiaryMedia(
                id = rs.getString("DiaryMediaId"),
                type = MediaType.of(rs.getInt("Type"), ending),
                name = rs.getString("Name").orEmpty(),
                ext = ending.ifBlank { "bin" },
                index = rs.getInt("Index"),
                entryId = rs.getLong("DiaryEntryId"),
            )
        }.groupBy { it.entryId }

    private fun <T> Connection.query(sql: String, map: (ResultSet) -> T): List<T> =
        createStatement().use { st ->
            st.executeQuery(sql).use { rs ->
                buildList { while (rs.next()) add(map(rs)) }
            }
        }

    private fun Connection.tables(): Set<String> =
        query("select name from sqlite_master where type='table'") { it.getString(1) }.toSet()

    private fun Connection.columns(table: String): Set<String> =
        query("pragma table_info(\"$table\")") { it.getString("name") }.toSet()
}
