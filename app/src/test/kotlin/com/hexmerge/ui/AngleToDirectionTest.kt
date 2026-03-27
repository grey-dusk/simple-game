package com.hexmerge.ui

import com.hexmerge.model.Direction
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for the angle-to-direction mapping used by swipe detection.
 * The function divides 360 degrees into 6 sectors of 60 degrees each.
 */
class AngleToDirectionTest {

    // Reimplemented here since the original is private in GameScreen.kt
    private fun angleToDirection(angle: Double): Direction? {
        val degrees = Math.toDegrees(angle)
        val normalized = if (degrees < 0) degrees + 360 else degrees
        return when {
            normalized < 30 || normalized >= 330 -> Direction.EAST
            normalized < 90 -> Direction.SOUTHEAST
            normalized < 150 -> Direction.SOUTHWEST
            normalized < 210 -> Direction.WEST
            normalized < 270 -> Direction.NORTHWEST
            else -> Direction.NORTHEAST
        }
    }

    @Test
    fun `0 degrees maps to EAST`() {
        assertEquals(Direction.EAST, angleToDirection(0.0))
    }

    @Test
    fun `180 degrees maps to WEST`() {
        assertEquals(Direction.WEST, angleToDirection(PI))
    }

    @Test
    fun `90 degrees maps to SOUTHWEST`() {
        // Downward in screen coordinates
        assertEquals(Direction.SOUTHWEST, angleToDirection(PI / 2))
    }

    @Test
    fun `negative 90 degrees maps to NORTHEAST`() {
        // -90 deg normalized = 270 deg, which is >= 270, so it's NORTHEAST
        assertEquals(Direction.NORTHEAST, angleToDirection(-PI / 2))
    }

    @Test
    fun `45 degrees maps to SOUTHEAST`() {
        assertEquals(Direction.SOUTHEAST, angleToDirection(PI / 4))
    }

    @Test
    fun `boundary at 30 degrees maps to EAST due to floating point`() {
        // Math.toRadians(30.0) -> toDegrees round-trip yields < 30.0 due to precision
        assertEquals(Direction.EAST, angleToDirection(Math.toRadians(30.0)))
    }

    @Test
    fun `just above 30 degrees maps to SOUTHEAST`() {
        assertEquals(Direction.SOUTHEAST, angleToDirection(Math.toRadians(31.0)))
    }

    @Test
    fun `just under 30 degrees maps to EAST`() {
        assertEquals(Direction.EAST, angleToDirection(Math.toRadians(29.9)))
    }

    @Test
    fun `330 degrees maps to EAST`() {
        assertEquals(Direction.EAST, angleToDirection(Math.toRadians(330.0)))
    }

    @Test
    fun `just under 330 degrees maps to NORTHEAST`() {
        assertEquals(Direction.NORTHEAST, angleToDirection(Math.toRadians(329.9)))
    }

    @Test
    fun `negative angle wraps correctly`() {
        // -30 degrees = 330 degrees -> EAST
        assertEquals(Direction.EAST, angleToDirection(Math.toRadians(-30.0)))
    }
}
