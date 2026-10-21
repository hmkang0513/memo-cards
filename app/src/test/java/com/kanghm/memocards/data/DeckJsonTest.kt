package com.kanghm.memocards.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeckJsonTest {
    @Test
    fun `progress와 모르는 키가 없어도 가져올 수 있다`() {
        val text = """
            {"version":1,"unknown":"x","decks":[{"name":"d","cards":[
              {"uid":"u1","front":"f","back":"b","tag":"무시"}
            ]}]}
        """.trimIndent()
        val file = deckJsonFormat.decodeFromString(DeckFile.serializer(), text)
        val card = file.decks.single().cards.single()
        assertEquals("u1", card.uid)
        assertNull(card.progress)
        assertNull(card.reading)
    }

    @Test
    fun `내보낸 JSON을 다시 읽으면 진행 기록이 유지된다`() {
        val original = DeckFile(
            decks = listOf(
                DeckJson(
                    name = "d",
                    cards = listOf(
                        CardJson("u1", "f", "b", progress = ProgressJson(knownCount = 3, unknownCount = 1, dueAt = 99)),
                    ),
                ),
            ),
        )
        val text = deckJsonFormat.encodeToString(DeckFile.serializer(), original)
        assertEquals(original, deckJsonFormat.decodeFromString(DeckFile.serializer(), text))
    }
}
