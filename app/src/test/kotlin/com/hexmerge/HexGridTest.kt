package com.hexmerge

import com.hexmerge.model.Direction
import com.hexmerge.model.HexBoard
import com.hexmerge.model.HexCoord
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HexGridTest {

    @Test
    fun `center cell has 6 neighbors`() {
        val center = HexCoord(0, 0)
        assertEquals(6, center.neighbors().size)
    }

    @Test
    fun `neighbors are correct for center`() {
        val center = HexCoord(0, 0)
        val expected = setOf(
            HexCoord(1, 0), HexCoord(-1, 0),
            HexCoord(1, -1), HexCoord(-1, 1),
            HexCoord(0, 1), HexCoord(0, -1),
        )
        assertEquals(expected, center.neighbors().toSet())
    }

    @Test
    fun `neighbor in east direction`() {
        val coord = HexCoord(1, 0)
        assertEquals(HexCoord(2, 0), coord.neighbor(Direction.EAST))
    }

    @Test
    fun `neighbor in southwest direction`() {
        val coord = HexCoord(0, 0)
        assertEquals(HexCoord(-1, 1), coord.neighbor(Direction.SOUTHWEST))
    }

    @Test
    fun `center is valid for any radius`() {
        assertTrue(HexCoord(0, 0).isValid(1))
        assertTrue(HexCoord(0, 0).isValid(3))
    }

    @Test
    fun `edge cell is valid for radius 3`() {
        assertTrue(HexCoord(3, 0).isValid(3))
        assertTrue(HexCoord(-2, -1).isValid(3))
    }

    @Test
    fun `cell outside radius is invalid`() {
        assertFalse(HexCoord(4, 0).isValid(3))
        assertFalse(HexCoord(2, 2).isValid(3))
    }

    @Test
    fun `distance from center to edge is radius`() {
        val center = HexCoord(0, 0)
        assertEquals(3, center.distanceTo(HexCoord(3, 0)))
        assertEquals(3, center.distanceTo(HexCoord(0, -3)))
    }

    @Test
    fun `distance between adjacent cells is 1`() {
        assertEquals(1, HexCoord(0, 0).distanceTo(HexCoord(1, 0)))
    }

    @Test
    fun `allCells for radius 1 has 7 cells`() {
        assertEquals(7, HexBoard.allCells(1).size)
    }

    @Test
    fun `allCells for radius 3 has 37 cells`() {
        assertEquals(37, HexBoard.allCells(3).size)
    }

    @Test
    fun `emptyCells returns all cells for empty board`() {
        val empty = emptyMap<HexCoord, Int>()
        assertEquals(37, HexBoard.emptyCells(empty, 3).size)
    }

    @Test
    fun `emptyCells excludes occupied cells`() {
        val board = mapOf(HexCoord(0, 0) to 2, HexCoord(1, 0) to 4)
        assertEquals(35, HexBoard.emptyCells(board, 3).size)
    }

    @Test
    fun `hasAdjacentEqual detects equal neighbors`() {
        val board = mapOf(HexCoord(0, 0) to 2, HexCoord(1, 0) to 2)
        assertTrue(HexBoard.hasAdjacentEqual(board, 3))
    }

    @Test
    fun `hasAdjacentEqual returns false when no equal neighbors`() {
        val board = mapOf(HexCoord(0, 0) to 2, HexCoord(1, 0) to 4)
        assertFalse(HexBoard.hasAdjacentEqual(board, 3))
    }

    @Test
    fun `sortedForDirection east sorts by q descending`() {
        val cells = listOf(HexCoord(0, 0), HexCoord(2, 0), HexCoord(1, 0))
        val sorted = HexBoard.sortedForDirection(cells, Direction.EAST)
        assertEquals(listOf(HexCoord(2, 0), HexCoord(1, 0), HexCoord(0, 0)), sorted)
    }

    @Test
    fun `sortedForDirection west sorts by q ascending`() {
        val cells = listOf(HexCoord(0, 0), HexCoord(2, 0), HexCoord(-1, 0))
        val sorted = HexBoard.sortedForDirection(cells, Direction.WEST)
        assertEquals(listOf(HexCoord(-1, 0), HexCoord(0, 0), HexCoord(2, 0)), sorted)
    }
}
