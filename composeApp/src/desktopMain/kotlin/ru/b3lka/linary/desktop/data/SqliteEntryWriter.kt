package ru.b3lka.linary.desktop.data

import org.sqlite.SQLiteConfig
import ru.b3lka.linary.core.model.DiaryEntry
import ru.b3lka.linary.core.model.EntryChange
import ru.b3lka.linary.core.repository.DiaryWriteException
import ru.b3lka.linary.core.time.Ticks
import java.io.File
import java.sql.Connection
import java.sql.PreparedStatement

/**
 * Запись изменений в SQLite-файл журнала Diarium.
 */
internal object SqliteEntryWriter {

    /**
     * Таблицы со ссылками на запись; удаляются вместе с ней (если есть в этой версии базы).
     */
    private val dependentTables = listOf("EntryTags", "Events", "Media")

    /**
     * Применяет [change] к базе [file] и возвращает идентификатор записи.
     *
     * Для изменённой записи сначала проверяет, что в базе она та же, что была открыта
     * в редакторе: если её успели поменять на другом устройстве, бросает Conflict.
     */
    fun apply(file: File, change: EntryChange): Long =
        SQLiteConfig().createConnection("jdbc:sqlite:${file.absolutePath}").use { c ->
            c.autoCommit = false
            val id = change.original?.let { update(c, it.id, change) } ?: insert(c, change)
            applyMedia(c, id, change)
            change.tagIds?.let { applyTags(c, id, it) }
            c.commit()
            id
        }

    /**
     * Проверяет, что в базе [file] у записи [id] нужные текст и оценка.
     */
    fun contains(file: File, id: Long, html: String, rating: Int): Boolean =
        SqliteDiaryReader.open(file).use { c ->
            c.prepareStatement("select Text, Rating from Entries where DiaryEntryId = ?").use { st ->
                st.setLong(1, id)
                st.executeQuery().use { rs -> rs.next() && rs.getString(1) == html && rs.getInt(2) == rating }
            }
        }

    /**
     * Идентификаторы вложений записи [entryId] в базе [file] в порядке `Index`.
     */
    fun mediaIds(file: File, entryId: Long): List<String> =
        SqliteDiaryReader.open(file).use { mediaIds(it, entryId) }

    /**
     * Удаляет запись вместе с её тегами, событиями и строками вложений.
     * Файлы вложений в `v4_Media` остаются: их удалит телефон при следующей синхронизации,
     * как он делает с любыми вложениями, на которые больше нет ссылок.
     */
    fun delete(file: File, entry: DiaryEntry) {
        SQLiteConfig().createConnection("jdbc:sqlite:${file.absolutePath}").use { c ->
            c.autoCommit = false
            checkUnchanged(c, entry.id, entry.html, entry.rating)
            val tables = c.metaData.getTables(null, null, "%", null).use { rs ->
                buildSet { while (rs.next()) add(rs.getString("TABLE_NAME")) }
            }
            (dependentTables.filter { it in tables } + "Entries").forEach { table ->
                c.prepareStatement("delete from $table where DiaryEntryId = ?").use { st ->
                    st.setLong(1, entry.id)
                    st.executeUpdate()
                }
            }
            c.commit()
        }
    }

    /**
     * Есть ли в базе [file] запись [id].
     */
    fun exists(file: File, id: Long): Boolean = SqliteDiaryReader.open(file).use { exists(it, id) }

    /**
     * Запись в базе должна совпадать с той, что видел пользователь, иначе её изменили на другом устройстве.
     */
    private fun checkUnchanged(c: Connection, id: Long, html: String, rating: Int) {
        val current = c.prepareStatement("select Text, Rating from Entries where DiaryEntryId = ?").use { st ->
            st.setLong(1, id)
            st.executeQuery().use { rs -> if (rs.next()) rs.getString(1) to rs.getInt(2) else null }
        }
        if (current != html to rating) {
            throw DiaryWriteException(DiaryWriteException.Reason.Conflict, "entry $id")
        }
    }

    private fun update(c: Connection, id: Long, change: EntryChange): Long {
        val original = requireNotNull(change.original)
        checkUnchanged(c, id, original.html, original.rating)
        c.prepareStatement("update Entries set Text = ?, Rating = ? where DiaryEntryId = ?").use { st ->
            st.bind(change.html, change.rating, id).executeUpdate()
        }
        return id
    }

    private fun insert(c: Connection, change: EntryChange): Long {
        var id = Ticks.fromLocalDateTime(change.dateTime)
        // Идентификатор — это время в тиках; при совпадении сдвигаемся на тик
        while (exists(c, id)) id++
        c.prepareStatement(
            "insert into Entries (DiaryEntryId, Heading, Text, Rating, Latitude, Longitude) values (?, '', ?, ?, 0, 0)",
        ).use { st ->
            st.bind(id, change.html, change.rating).executeUpdate()
        }
        return id
    }

    /**
     * Идентификаторы тегов записи [entryId] в базе [file].
     */
    fun tagIds(file: File, entryId: Long): Set<Long> = SqliteDiaryReader.open(file).use { tagIds(it, entryId) }

    /**
     * Приводит теги записи к [tagIds]: лишние связи удаляет, недостающие добавляет (без значения трекера).
     * У оставшихся тегов значение трекера не трогается.
     */
    private fun applyTags(c: Connection, entryId: Long, tagIds: Set<Long>) {
        val current = tagIds(c, entryId)
        (current - tagIds).forEach { tagId ->
            c.prepareStatement("delete from EntryTags where DiaryEntryId = ? and DiaryTagId = ?").use { st ->
                st.bind(entryId, tagId).executeUpdate()
            }
        }
        (tagIds - current).forEach { tagId ->
            c.prepareStatement("insert into EntryTags (DiaryEntryId, DiaryTagId, TrackingValue) values (?, ?, null)")
                .use { st -> st.bind(entryId, tagId).executeUpdate() }
        }
    }

    private fun tagIds(c: Connection, entryId: Long): Set<Long> =
        c.prepareStatement("select DiaryTagId from EntryTags where DiaryEntryId = ?").use { st ->
            st.setLong(1, entryId)
            st.executeQuery().use { rs -> buildSet { while (rs.next()) add(rs.getLong(1)) } }
        }

    /**
     * Убирает удалённые вложения и добавляет новые в конец (`Index` продолжает нумерацию).
     * Сами файлы в `v4_Media` пишет репозиторий; колонка `Data` у синхронизируемой базы пустая.
     */
    private fun applyMedia(c: Connection, entryId: Long, change: EntryChange) {
        change.removedMediaIds.forEach { mediaId ->
            c.prepareStatement("delete from Media where DiaryMediaId = ? and DiaryEntryId = ?").use { st ->
                st.setString(1, mediaId)
                st.setLong(2, entryId)
                st.executeUpdate()
            }
        }
        if (change.addedMedia.isNotEmpty()) insertMedia(c, entryId, change)
        change.mediaOrder?.let { reorderMedia(c, entryId, it) }
    }

    private fun insertMedia(c: Connection, entryId: Long, change: EntryChange) {
        var index = c.prepareStatement("select coalesce(max(\"Index\"), -1) + 1 from Media where DiaryEntryId = ?")
            .use { st ->
                st.setLong(1, entryId)
                st.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
            }
        change.addedMedia.forEach { media ->
            c.prepareStatement(
                "insert into Media (DiaryMediaId, Type, Data, Name, FileEnding, \"Index\", DiaryEntryId) " +
                    "values (?, ?, null, ?, ?, ?, ?)",
            ).use { st ->
                st.bind(media.id, MEDIA_TYPE_IMAGE, media.name, ".${media.ext}", index++, entryId).executeUpdate()
            }
        }
    }

    /**
     * Перенумеровывает `Index` вложений записи в порядке [order]; вложения не из списка — в конец.
     */
    private fun reorderMedia(c: Connection, entryId: Long, order: List<String>) {
        val current = mediaIds(c, entryId)
        val ordered = order.filter { it in current } + current.filter { it !in order }
        c.prepareStatement("update Media set \"Index\" = ? where DiaryMediaId = ? and DiaryEntryId = ?").use { st ->
            ordered.forEachIndexed { index, id ->
                st.bind(index, id, entryId).executeUpdate()
            }
        }
    }

    private fun mediaIds(c: Connection, entryId: Long): List<String> =
        c.prepareStatement("select DiaryMediaId from Media where DiaryEntryId = ? order by \"Index\"").use { st ->
            st.setLong(1, entryId)
            st.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.getString(1)) } }
        }

    private const val MEDIA_TYPE_IMAGE = 0

    /**
     * Подставляет параметры запроса по порядку — по одному на каждый `?` слева направо.
     */
    private fun PreparedStatement.bind(vararg values: Any): PreparedStatement = apply {
        values.forEachIndexed { i, value -> setObject(i + 1, value) }
    }

    private fun exists(c: Connection, id: Long): Boolean =
        c.prepareStatement("select 1 from Entries where DiaryEntryId = ?").use { st ->
            st.setLong(1, id)
            st.executeQuery().use { it.next() }
        }
}
