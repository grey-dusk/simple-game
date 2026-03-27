package com.hexmerge.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HexCoordTest {

    @Test
    fun `s coordinate is computed as negative q minus r`() {
        val coord = HexCoord(2, -1)
        assertEquals(-1, coord.s)
    }

    @Test
    fun `s coordinate at origin is zero`() {
        assertEquals(0, HexCoord(0, 0).s)
    }

    @Test
    fun `neighbor returns correct offset for each direction`() {
        val origin = HexCoord(0, 0)
        assertEquals(HexCoord(1, 0), origin.neighbor(Direction.EAST))
        assertEquals(HexCoord(-1, 0), origin.neighbor(Direction.WEST))
        assertEquals(HexCoord(1, -1), origin.neighbor(Direction.NORTHEAST))
        assertEquals(HexCoord(-1, 1), origin.neighbor(Direction.SOUTHWEST))
        assertEquals(HexCoord(0, 1), origin.neighbor(Direction.SOUTHEAST))
        assertEquals(HexCoord(0, -1), origin.neighbor(Direction.NORTHWEST))
    }

    @Test
    fun `neighbors returns all 6 adjacent cells`() {
        val neighbors = HexCoord(0, 0).neighbors()
        assertEquals(6, neighbors.size)
        assertTrue(neighbors.contains(HexCoord(1, 0)))
        assertTrue(neighbors.contains(HexCoord(-1, 0)))
    }

    @Test
    fun `isValid returns true for origin`() {
        assertTrue(HexCoord(0, 0).isValid(3))
    }

    @Test
    fun `isValid returns true for boundary cell`() {
        assertTrue(HexCoord(3, 0).isValid(3))
        assertTrue(HexCoord(-2, -1).isValid(3))
    }

    @Test
    fun `isValid returns false for cell outside radius`() {
        assertFalse(HexCoord(4, 0).isValid(3))
        assertFalse(HexCoord(2, 2).isValid(3))
    }

    @Test
    fun `distanceTo origin is max of abs components`() {
        assertEquals(0, HexCoord(0, 0).distanceTo(HexCoord(0, 0)))
        assertEquals(3, HexCoord(3, 0).distanceTo(HexCoord(0, 0)))
        assertEquals(2, HexCoord(1, -1).distanceTo(HexCoord(0, 1)))
    }
}
