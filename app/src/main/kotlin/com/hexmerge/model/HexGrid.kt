package com.hexmerge.model

import kotlin.math.abs
import kotlin.math.max

data class HexCoord(val q: Int, val r: Int) {
    val s: Int get() = -q - r

    fun neighbor(direction: Direction): HexCoord =
        HexCoord(q + direction.dq, r + direction.dr)

    fun neighbors(): List<HexCoord> =
        Direction.entries.map { neighbor(it) }

    fun isValid(radius: Int): Boolean =
        max(abs(q), max(abs(r), abs(s))) <= radius

    fun distanceTo(other: HexCoord): Int =
        max(abs(q - other.q), max(abs(r - other.r), abs(s - other.s)))
}

enum class Direction(val dq: Int, val dr: Int) {
    EAST(1, 0),
    WEST(-1, 0),
    NORTHEAST(1, -1),
    SOUTHWEST(-1, 1),
    SOUTHEAST(0, 1),
    NORTHWEST(0, -1);
}

object HexBoard {
    fun allCells(radius: Int): List<HexCoord> {
        val cells = mutableListOf<HexCoord>()
        for (q in -radius..radius) {
            for (r in -radius..radius) {
                val coord = HexCoord(q, r)
                if (coord.isValid(radius)) {
                    cells.add(coord)
                }
            }
        }
        return cells
    }

    fun emptyCells(board: Map<HexCoord, Int>, radius: Int): List<HexCoord> =
        allCells(radius).filter { it !in board }

    fun hasAdjacentEqual(board: Map<HexCoord, Int>, radius: Int): Boolean {
        for ((coord, value) in board) {
            for (neighbor in coord.neighbors()) {
                if (neighbor.isValid(radius) && board[neighbor] == value) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Returns cells sorted for processing in the given swipe direction.
     * Tiles farthest in the swipe direction are processed first.
     *
     * For each direction, we sort by the component that increases
     * along that direction (descending = farthest first).
     */
    fun sortedForDirection(cells: List<HexCoord>, direction: Direction): List<HexCoord> =
        when (direction) {
            Direction.EAST -> cells.sortedByDescending { it.q }
            Direction.WEST -> cells.sortedBy { it.q }
            Direction.NORTHEAST -> cells.sortedByDescending { it.q - it.r }
            Direction.SOUTHWEST -> cells.sortedBy { it.q - it.r }
            Direction.SOUTHEAST -> cells.sortedByDescending { it.r }
            Direction.NORTHWEST -> cells.sortedBy { it.r }
        }
}
