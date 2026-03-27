package com.hexmerge.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HexBoardTest {

    @Test
    fun `allCells with radius 3 returns 37 cells`() {
        val cells = HexBoard.allCells(3)
        assertEquals(37, cells.size)
    }

    @Test
    fun `allCells with radius 1 returns 7 cells`() {
        val cells = HexBoard.allCells(1)
        assertEquals(7, cells.size)
    }

    @Test
    fun `allCells with radius 0 returns 1 cell`() {
        val cells = HexBoard.allCells(0)
        assertEquals(1, cells.size)
        assertEquals(HexCoord(0, 0), cells[0])
    }

    @Test
    fun `allCells contains only valid coordinates`() {
        val cells = HexBoard.allCells(3)
        assertTrue(cells.all { it.isValid(3) })
    }

    @Test
    fun `emptyCells returns all cells when board is empty`() {
        val empty = HexBoard.emptyCells(emptyMap(), 3)
        assertEquals(37, empty.size)
    }

    @Test
    fun `emptyCells excludes occupied cells`() {
        val board = mapOf(HexCoord(0, 0) to 2, HexCoord(1, 0) to 4)
        val empty = HexBoard.emptyCells(board, 3)
        assertEquals(35, empty.size)
        assertFalse(empty.contains(HexCoord(0, 0)))
        assertFalse(empty.contains(HexCoord(1, 0)))
    }

    @Test
    fun `emptyCells returns empty list when board is full`() {
        val allCells = HexBoard.allCells(1)
        val board = allCells.associateWith { 2 }
        val empty = HexBoard.emptyCells(board, 1)
        assertTrue(empty.isEmpty())
    }

    @Test
    fun `hasAdjacentEqual returns true when adjacent cells have same value`() {
        val board = mapOf(HexCoord(0, 0) to 2, HexCoord(1, 0) to 2)
        assertTrue(HexBoard.hasAdjacentEqual(board, 3))
    }

    @Test
    fun `hasAdjacentEqual returns false when no adjacent cells have same value`() {
        val board = mapOf(HexCoord(0, 0) to 2, HexCoord(1, 0) to 4)
        assertFalse(HexBoard.hasAdjacentEqual(board, 3))
    }

    @Test
    fun `hasAdjacentEqual returns false for single tile`() {
        val board = mapOf(HexCoord(0, 0) to 2)
        assertFalse(HexBoard.hasAdjacentEqual(board, 3))
    }

    @Test
    fun `hasAdjacentEqual ignores non-adjacent equal values`() {
        val board = mapOf(HexCoord(0, 0) to 2, HexCoord(3, 0) to 2)
        assertFalse(HexBoard.hasAdjacentEqual(board, 3))
    }

    @Test
    fun `sortedForDirection EAST sorts by q descending`() {
        val cells = listOf(HexCoord(0, 0), HexCoord(2, 0), HexCoord(-1, 0))
        val sorted = HexBoard.sortedForDirection(cells, Direction.EAST)
        assertEquals(listOf(HexCoord(2, 0), HexCoord(0, 0), HexCoord(-1, 0)), sorted)
    }

    @Test
    fun `sortedForDirection WEST sorts by q ascending`() {
        val cells = listOf(HexCoord(0, 0), HexCoord(2, 0), HexCoord(-1, 0))
        val sorted = HexBoard.sortedForDirection(cells, Direction.WEST)
        assertEquals(listOf(HexCoord(-1, 0), HexCoord(0, 0), HexCoord(2, 0)), sorted)
    }

    @Test
    fun `sortedForDirection SOUTHEAST sorts by r descending`() {
        val cells = listOf(HexCoord(0, 0), HexCoord(0, 2), HexCoord(0, -1))
        val sorted = HexBoard.sortedForDirection(cells, Direction.SOUTHEAST)
        assertEquals(listOf(HexCoord(0, 2), HexCoord(0, 0), HexCoord(0, -1)), sorted)
    }
}
