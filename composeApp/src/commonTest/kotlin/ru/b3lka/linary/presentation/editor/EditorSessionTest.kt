package ru.b3lka.linary.presentation.editor

import kotlinx.datetime.LocalDateTime
import ru.b3lka.linary.core.model.DiaryEntry
import ru.b3lka.linary.core.model.DiaryMedia
import ru.b3lka.linary.core.model.DiaryTag
import ru.b3lka.linary.core.model.EntryTag
import ru.b3lka.linary.core.model.MediaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditorSessionTest {
    private val time = LocalDateTime(2026, 10, 16, 12, 0)

    private fun media(id: String, index: Int) = DiaryMedia(id, MediaType.Image, id, "jpg", index, 1)

    private fun entry(vararg media: DiaryMedia) =
        DiaryEntry(1, time, "", "<p>x</p>", 0, 0.0, 0.0, emptyList(), media.toList())

    @Test
    fun moveAndMakeCover() {
        val session = EditorSession(entry(media("a", 0), media("b", 1), media("c", 2)), time)
        assertFalse(session.isDirty)

        session.makeCover(session.attachments[2])
        assertEquals(listOf("c", "a", "b"), session.mediaOrder)
        assertTrue(session.isDirty)

        session.move(session.attachments[0], 1)
        assertEquals(listOf("a", "c", "b"), session.mediaOrder)
        session.move(session.attachments[2], 1) // за край не уходит
        assertEquals(listOf("a", "c", "b"), session.mediaOrder)
    }

    @Test
    fun toggleTagsMarksSessionDirty() {
        val tag = DiaryTag(3, "work", 0, null, null)
        val tagged = DiaryEntry(1, time, "", "<p>x</p>", 0, 0.0, 0.0, listOf(EntryTag(tag, null)), emptyList())
        val session = EditorSession(tagged, time)
        assertEquals(listOf(3L), session.tagIds.toList())
        assertFalse(session.tagsChanged)

        session.toggleTag(7)
        assertTrue(session.tagsChanged)
        assertTrue(session.isDirty)

        session.toggleTag(7)
        assertFalse(session.tagsChanged)
        session.toggleTag(3)
        assertTrue(session.tagIds.isEmpty())
        assertTrue(session.tagsChanged)
    }

    @Test
    fun removedMediaIsReported() {
        val session = EditorSession(entry(media("a", 0), media("b", 1)), time)
        session.remove(session.attachments[0])
        assertEquals(setOf("a"), session.removedMediaIds)
        assertEquals(listOf("b"), session.mediaOrder)
    }

    @Test
    fun rebaseKeepsUserOrderAndAddsMediaFromOtherDevice() {
        val session = EditorSession(entry(media("a", 0), media("b", 1), media("c", 2)), time)
        session.makeCover(session.attachments[1]) // b, a, c
        session.remove(session.attachments[2]) // убрали c

        // На телефоне удалили «a» и добавили «d»
        session.rebase(entry(media("b", 0), media("c", 1), media("d", 2)))

        assertEquals(listOf("b", "d"), session.mediaOrder)
        assertEquals(setOf("c"), session.removedMediaIds)
    }
}
