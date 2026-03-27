package com.hexmerge.game

import com.hexmerge.model.HexCoord
import kotlin.test.Test
import kotlin.test.assertEquals

class SerializableGameStateTest {

    @Test
    fun `round-trip preserves board state`() {
        val original = GameState(
            board = mapOf(HexCoord(1, -2) to 8, HexCoord(0, 0) to 2),
            score = 42,
            bestScore = 100,
            isGameOver = false,
            radius = 3,
        )

        val serializable = SerializableGameState.from(original)
        val restored = serializable.toGameState()

        assertEquals(original.board, restored.board)
        assertEquals(original.score, restored.score)
        assertEquals(original.bestScore, restored.bestScore)
        assertEquals(original.isGameOver, restored.isGameOver)
        assertEquals(original.radius, restored.radius)
    }

    @Test
    fun `round-trip with empty board`() {
        val original = GameState(board = emptyMap(), score = 0, bestScore = 0)
        val restored = SerializableGameState.from(original).toGameState()
        assertEquals(original.board, restored.board)
    }

    @Test
    fun `round-trip preserves game over state`() {
        val original = GameState(
            board = mapOf(HexCoord(0, 0) to 2),
            score = 50,
            bestScore = 50,
            isGameOver = true,
        )
        val restored = SerializableGameState.from(original).toGameState()
        assertEquals(true, restored.isGameOver)
    }

    @Test
    fun `coordinate serialization format is q comma r`() {
        val state = GameState(board = mapOf(HexCoord(3, -1) to 4))
        val serializable = SerializableGameState.from(state)
        assertEquals(4, serializable.tiles["3,-1"])
    }

    @Test
    fun `negative coordinates survive round-trip`() {
        val board = mapOf(
            HexCoord(-3, 0) to 2,
            HexCoord(0, -3) to 4,
            HexCoord(-1, -2) to 8,
        )
        val original = GameState(board = board)
        val restored = SerializableGameState.from(original).toGameState()
        assertEquals(board, restored.board)
    }
}
