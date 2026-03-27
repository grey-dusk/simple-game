package com.hexmerge

import com.hexmerge.game.GameLogic
import com.hexmerge.game.GameState
import com.hexmerge.model.Direction
import com.hexmerge.model.HexCoord
import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameLogicTest {

    private val radius = 3

    private fun stateWith(vararg tiles: Pair<HexCoord, Int>): GameState =
        GameState(board = mapOf(*tiles), radius = radius)

    // --- move() tests ---

    @Test
    fun `move east merges adjacent equal tiles`() {
        val state = stateWith(HexCoord(0, 0) to 2, HexCoord(1, 0) to 2)
        val result = GameLogic.move(state, Direction.EAST)
        assertTrue(result.moved)
        assertEquals(4, result.newBoard[HexCoord(3, 0)])
        assertEquals(4, result.scoreGained)
    }

    @Test
    fun `move west merges tiles`() {
        val state = stateWith(HexCoord(0, 0) to 2, HexCoord(-1, 0) to 2)
        val result = GameLogic.move(state, Direction.WEST)
        assertTrue(result.moved)
        assertEquals(4, result.scoreGained)
    }

    @Test
    fun `move northeast merges tiles`() {
        val state = stateWith(HexCoord(0, 0) to 4, HexCoord(1, -1) to 4)
        val result = GameLogic.move(state, Direction.NORTHEAST)
        assertTrue(result.moved)
        assertEquals(8, result.scoreGained)
    }

    @Test
    fun `move southeast merges tiles`() {
        val state = stateWith(HexCoord(0, 0) to 8, HexCoord(0, 1) to 8)
        val result = GameLogic.move(state, Direction.SOUTHEAST)
        assertTrue(result.moved)
        assertEquals(16, result.scoreGained)
    }

    @Test
    fun `move southwest merges tiles`() {
        val state = stateWith(HexCoord(0, 0) to 2, HexCoord(-1, 1) to 2)
        val result = GameLogic.move(state, Direction.SOUTHWEST)
        assertTrue(result.moved)
        assertEquals(4, result.scoreGained)
    }

    @Test
    fun `move northwest merges tiles`() {
        val state = stateWith(HexCoord(0, 0) to 16, HexCoord(0, -1) to 16)
        val result = GameLogic.move(state, Direction.NORTHWEST)
        assertTrue(result.moved)
        assertEquals(32, result.scoreGained)
    }

    @Test
    fun `tile merges only once per swipe`() {
        // Three 2s in a row: 2, 2, 2 going east
        // Should produce: _, 2, 4 (not 8)
        val state = stateWith(
            HexCoord(1, 0) to 2,
            HexCoord(2, 0) to 2,
            HexCoord(3, 0) to 2,
        )
        val result = GameLogic.move(state, Direction.EAST)
        assertTrue(result.moved)
        assertEquals(4, result.newBoard[HexCoord(3, 0)])
        assertEquals(2, result.newBoard[HexCoord(2, 0)])
    }

    @Test
    fun `no-op when nothing can move`() {
        // Single tile at the east edge
        val state = stateWith(HexCoord(3, 0) to 2)
        val result = GameLogic.move(state, Direction.EAST)
        assertFalse(result.moved)
        assertEquals(0, result.scoreGained)
    }

    @Test
    fun `tile slides to edge when path is clear`() {
        val state = stateWith(HexCoord(0, 0) to 2)
        val result = GameLogic.move(state, Direction.EAST)
        assertTrue(result.moved)
        assertEquals(2, result.newBoard[HexCoord(3, 0)])
        assertFalse(HexCoord(0, 0) in result.newBoard)
    }

    // --- spawnTile() tests ---

    @Test
    fun `spawnTile adds one tile to empty cells`() {
        val state = stateWith(HexCoord(0, 0) to 2)
        val afterSpawn = GameLogic.spawnTile(state, Random(42))
        assertEquals(2, afterSpawn.board.size)
    }

    @Test
    fun `spawnTile generates 2 or 4`() {
        val state = stateWith(HexCoord(0, 0) to 2)
        val afterSpawn = GameLogic.spawnTile(state, Random(42))
        val newTile = afterSpawn.board.entries.first { it.key != HexCoord(0, 0) }
        assertTrue(newTile.value == 2 || newTile.value == 4)
    }

    @Test
    fun `spawnTile does nothing when board is full`() {
        val allCells = com.hexmerge.model.HexBoard.allCells(radius)
        val fullBoard = allCells.associateWith { 2 }
        val state = GameState(board = fullBoard, radius = radius)
        val afterSpawn = GameLogic.spawnTile(state)
        assertEquals(state.board.size, afterSpawn.board.size)
    }

    // --- isGameOver() tests ---

    @Test
    fun `not game over with empty cells`() {
        val state = stateWith(HexCoord(0, 0) to 2)
        assertFalse(GameLogic.isGameOver(state))
    }

    @Test
    fun `not game over when full but merges possible`() {
        val allCells = com.hexmerge.model.HexBoard.allCells(radius)
        // All cells have value 2, so merges are possible
        val board = allCells.associateWith { 2 }
        val state = GameState(board = board, radius = radius)
        assertFalse(GameLogic.isGameOver(state))
    }

    @Test
    fun `game over when full and no merges possible`() {
        val allCells = com.hexmerge.model.HexBoard.allCells(radius)
        // Alternating values so no adjacent cells are equal
        var toggle = false
        val board = allCells.associateWith { coord ->
            toggle = !toggle
            if (toggle) 2 else 4
        }
        // Verify no adjacent equals exist, then check game over
        // Note: alternating might still have adjacent equals on hex grid
        // Use unique primes to guarantee no adjacent equals
        var prime = 2
        val primes = listOf(2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151, 157)
        val uniqueBoard = allCells.zip(primes).toMap()
        val state = GameState(board = uniqueBoard, radius = radius)
        assertTrue(GameLogic.isGameOver(state))
    }

    // --- applyMove() tests ---

    @Test
    fun `applyMove updates score and spawns new tile`() {
        val state = stateWith(HexCoord(0, 0) to 2, HexCoord(1, 0) to 2)
        val after = GameLogic.applyMove(state, Direction.EAST, Random(42))
        assertEquals(4, after.score)
        assertTrue(after.board.size >= 2) // merged tile + new spawn
    }

    @Test
    fun `applyMove returns same state if nothing moved`() {
        val state = stateWith(HexCoord(3, 0) to 2)
        val after = GameLogic.applyMove(state, Direction.EAST, Random(42))
        assertEquals(state, after)
    }

    @Test
    fun `newGame creates board with 2 tiles`() {
        val state = GameLogic.newGame(radius = 3, random = Random(42))
        assertEquals(2, state.board.size)
    }
}
