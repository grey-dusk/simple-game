package com.hexmerge.game

import com.hexmerge.model.Direction
import com.hexmerge.model.HexBoard
import com.hexmerge.model.HexCoord
import kotlin.random.Random

object GameLogic {

    fun newGame(radius: Int = GameState.BOARD_RADIUS, random: Random = Random): GameState {
        val empty = GameState(radius = radius)
        val withFirst = spawnTile(empty, random)
        return spawnTile(withFirst, random)
    }

    fun move(state: GameState, direction: Direction): MoveResult {
        val board = state.board.toMutableMap()
        val merged = mutableSetOf<HexCoord>()
        val mergeEvents = mutableListOf<MergeEvent>()
        var scoreGained = 0
        var anyMoved = false

        val occupiedCells = board.keys.toList()
        val sorted = HexBoard.sortedForDirection(occupiedCells, direction)

        for (coord in sorted) {
            val value = board[coord] ?: continue
            var current = coord
            var next = HexCoord(current.q + direction.dq, current.r + direction.dr)

            while (next.isValid(state.radius) && next !in board) {
                current = next
                next = HexCoord(current.q + direction.dq, current.r + direction.dr)
            }

            if (next.isValid(state.radius)
                && board[next] == value
                && next !in merged
            ) {
                board.remove(coord)
                board[next] = value * 2
                merged.add(next)
                mergeEvents.add(MergeEvent(from = coord, to = next, value = value * 2))
                scoreGained += value * 2
                anyMoved = true
            } else if (current != coord) {
                board.remove(coord)
                board[current] = value
                anyMoved = true
            }
        }

        return MoveResult(
            newBoard = board,
            scoreGained = scoreGained,
            merges = mergeEvents,
            moved = anyMoved,
        )
    }

    fun spawnTile(state: GameState, random: Random = Random): GameState {
        val empty = HexBoard.emptyCells(state.board, state.radius)
        if (empty.isEmpty()) return state

        val coord = empty[random.nextInt(empty.size)]
        val value = if (random.nextFloat() < 0.9f) 2 else 4
        return state.copy(board = state.board + (coord to value))
    }

    fun isGameOver(state: GameState): Boolean {
        if (HexBoard.emptyCells(state.board, state.radius).isNotEmpty()) return false
        return !HexBoard.hasAdjacentEqual(state.board, state.radius)
    }

    fun applyMove(state: GameState, direction: Direction, random: Random = Random): GameState {
        val result = move(state, direction)
        if (!result.moved) return state

        val newScore = state.score + result.scoreGained
        val newBest = maxOf(state.bestScore, newScore)
        val afterSpawn = spawnTile(
            state.copy(
                board = result.newBoard,
                score = newScore,
                bestScore = newBest,
            ),
            random,
        )
        return afterSpawn.copy(isGameOver = isGameOver(afterSpawn))
    }
}
