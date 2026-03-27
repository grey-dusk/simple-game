package com.hexmerge.game

import com.hexmerge.model.HexCoord
import kotlinx.serialization.Serializable

data class GameState(
    val board: Map<HexCoord, Int> = emptyMap(),
    val score: Int = 0,
    val bestScore: Int = 0,
    val isGameOver: Boolean = false,
    val radius: Int = BOARD_RADIUS,
) {
    companion object {
        const val BOARD_RADIUS = 3
    }
}

data class MoveResult(
    val newBoard: Map<HexCoord, Int>,
    val scoreGained: Int,
    val merges: List<MergeEvent>,
    val moved: Boolean,
)

data class MergeEvent(
    val from: HexCoord,
    val to: HexCoord,
    val value: Int,
)

@Serializable
data class SerializableGameState(
    val tiles: Map<String, Int>,
    val score: Int,
    val bestScore: Int,
    val isGameOver: Boolean,
    val radius: Int,
) {
    fun toGameState(): GameState = GameState(
        board = tiles.mapKeys { (key, _) ->
            val (q, r) = key.split(",").map { it.toInt() }
            HexCoord(q, r)
        },
        score = score,
        bestScore = bestScore,
        isGameOver = isGameOver,
        radius = radius,
    )

    companion object {
        fun from(state: GameState): SerializableGameState = SerializableGameState(
            tiles = state.board.mapKeys { (coord, _) -> "${coord.q},${coord.r}" },
            score = state.score,
            bestScore = state.bestScore,
            isGameOver = state.isGameOver,
            radius = state.radius,
        )
    }
}
