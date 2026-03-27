package com.hexmerge.game

import com.hexmerge.model.Direction
import com.hexmerge.model.HexBoard
import com.hexmerge.model.HexCoord
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameLogicTest {

    private val seededRandom = Random(42)

    @Test
    fun `newGame creates board with exactly 2 tiles`() {
        val state = GameLogic.newGame(random = seededRandom)
        assertEquals(2, state.board.size)
    }

    @Test
    fun `newGame starts with score 0`() {
        val state = GameLogic.newGame(random = seededRandom)
        assertEquals(0, state.score)
    }

    @Test
    fun `newGame is not game over`() {
        val state = GameLogic.newGame(random = seededRandom)
        assertFalse(state.isGameOver)
    }

    @Test
    fun `newGame uses default radius 3`() {
        val state = GameLogic.newGame(random = seededRandom)
        assertEquals(3, state.radius)
    }

    // --- move() ---

    @Test
    fun `move slides tile to edge when path is clear`() {
        val board = mapOf(HexCoord(0, 0) to 2)
        val state = GameState(board = board, radius = 3)
        val result = GameLogic.move(state, Direction.EAST)

        assertTrue(result.moved)
        assertEquals(1, result.newBoard.size)
        // Tile should have slid east to the boundary
        assertTrue(result.newBoard.containsKey(HexCoord(3, 0)))
    }

    @Test
    fun `move merges equal adjacent tiles`() {
        val board = mapOf(HexCoord(0, 0) to 2, HexCoord(1, 0) to 2)
        val state = GameState(board = board, radius = 3)
        val result = GameLogic.move(state, Direction.EAST)

        assertTrue(result.moved)
        assertEquals(1, result.newBoard.size)
        assertEquals(4, result.scoreGained)
    }

    @Test
    fun `move does not double-merge in single move`() {
        // Three 2s in a row: only the frontmost pair should merge
        val board = mapOf(
            HexCoord(-1, 0) to 2,
            HexCoord(0, 0) to 2,
            HexCoord(1, 0) to 2,
        )
        val state = GameState(board = board, radius = 3)
        val result = GameLogic.move(state, Direction.EAST)

        assertTrue(result.moved)
        assertEquals(2, result.newBoard.size)
        // One merged to 4, one remains as 2
        val values = result.newBoard.values.sorted()
        assertEquals(listOf(2, 4), values)
    }

    @Test
    fun `move returns moved false when no movement possible`() {
        // Single tile already at the east edge
        val board = mapOf(HexCoord(3, 0) to 2)
        val state = GameState(board = board, radius = 3)
        val result = GameLogic.move(state, Direction.EAST)

        assertFalse(result.moved)
        assertEquals(0, result.scoreGained)
    }

    @Test
    fun `move does not merge different values`() {
        val board = mapOf(HexCoord(0, 0) to 2, HexCoord(1, 0) to 4)
        val state = GameState(board = board, radius = 3)
        val result = GameLogic.move(state, Direction.EAST)

        assertTrue(result.moved)
        assertEquals(2, result.newBoard.size)
        assertEquals(0, result.scoreGained)
    }

    @Test
    fun `move calculates correct score for multiple merges`() {
        val board = mapOf(
            HexCoord(-2, 0) to 4,
            HexCoord(-1, 0) to 4,
            HexCoord(1, 0) to 8,
            HexCoord(2, 0) to 8,
        )
        val state = GameState(board = board, radius = 3)
        val result = GameLogic.move(state, Direction.EAST)

        assertTrue(result.moved)
        assertEquals(8 + 16, result.scoreGained)
    }

    // --- spawnTile() ---

    @Test
    fun `spawnTile adds exactly one tile`() {
        val state = GameState(board = mapOf(HexCoord(0, 0) to 2), radius = 3)
        val after = GameLogic.spawnTile(state, seededRandom)
        assertEquals(2, after.board.size)
    }

    @Test
    fun `spawnTile on full board returns unchanged state`() {
        val allCells = HexBoard.allCells(1)
        val board = allCells.associateWith { 2 }
        val state = GameState(board = board, radius = 1)
        val after = GameLogic.spawnTile(state)
        assertEquals(state, after)
    }

    @Test
    fun `spawnTile spawns value of 2 or 4`() {
        val state = GameState(board = emptyMap(), radius = 3)
        val after = GameLogic.spawnTile(state, seededRandom)
        val spawnedValue = after.board.values.first()
        assertTrue(spawnedValue == 2 || spawnedValue == 4)
    }

    // --- isGameOver() ---

    @Test
    fun `isGameOver returns false when empty cells exist`() {
        val state = GameState(board = mapOf(HexCoord(0, 0) to 2), radius = 3)
        assertFalse(GameLogic.isGameOver(state))
    }

    @Test
    fun `isGameOver returns false when board is full but merges exist`() {
        val allCells = HexBoard.allCells(1)
        // All cells have value 2, so adjacent merges exist
        val board = allCells.associateWith { 2 }
        val state = GameState(board = board, radius = 1)
        assertFalse(GameLogic.isGameOver(state))
    }

    @Test
    fun `isGameOver returns true when board is full and no merges possible`() {
        // Radius 1: 7 cells. Alternate values so no adjacent equals
        val cells = HexBoard.allCells(1)
        val values = listOf(2, 4, 8, 16, 32, 64, 128)
        val board = cells.zip(values).toMap()
        val state = GameState(board = board, radius = 1)
        assertTrue(GameLogic.isGameOver(state))
    }

    // --- applyMove() ---

    @Test
    fun `applyMove returns unchanged state when no movement`() {
        val board = mapOf(HexCoord(3, 0) to 2)
        val state = GameState(board = board, radius = 3)
        val after = GameLogic.applyMove(state, Direction.EAST, seededRandom)
        assertEquals(state, after)
    }

    @Test
    fun `applyMove spawns new tile after valid move`() {
        val board = mapOf(HexCoord(0, 0) to 2)
        val state = GameState(board = board, radius = 3)
        val after = GameLogic.applyMove(state, Direction.EAST, seededRandom)

        // Original 1 tile moved + 1 new tile spawned = 2 tiles
        assertEquals(2, after.board.size)
    }

    @Test
    fun `applyMove updates score after merge`() {
        val board = mapOf(HexCoord(0, 0) to 2, HexCoord(1, 0) to 2)
        val state = GameState(board = board, radius = 3)
        val after = GameLogic.applyMove(state, Direction.EAST, seededRandom)

        assertEquals(4, after.score)
    }

    @Test
    fun `applyMove updates bestScore when score exceeds it`() {
        val board = mapOf(HexCoord(0, 0) to 2, HexCoord(1, 0) to 2)
        val state = GameState(board = board, radius = 3, bestScore = 0)
        val after = GameLogic.applyMove(state, Direction.EAST, seededRandom)

        assertEquals(4, after.bestScore)
    }

    @Test
    fun `applyMove preserves bestScore when current score is lower`() {
        val board = mapOf(HexCoord(0, 0) to 2, HexCoord(1, 0) to 2)
        val state = GameState(board = board, radius = 3, bestScore = 100)
        val after = GameLogic.applyMove(state, Direction.EAST, seededRandom)

        assertEquals(100, after.bestScore)
    }
}
