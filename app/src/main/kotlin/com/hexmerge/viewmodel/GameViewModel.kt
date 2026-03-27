package com.hexmerge.viewmodel

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hexmerge.game.GameLogic
import com.hexmerge.game.GameState
import com.hexmerge.game.SerializableGameState
import com.hexmerge.model.Direction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private val Application.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hexmerge")

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.dataStore
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state

    private val _stats = MutableStateFlow(GameStats())
    val stats: StateFlow<GameStats> = _stats

    init {
        viewModelScope.launch {
            val loaded = loadState()
            _state.value = loaded ?: GameLogic.newGame()
            _stats.value = loadStats()
        }
    }

    fun swipe(direction: Direction) {
        val current = _state.value
        if (current.isGameOver) return

        val next = GameLogic.applyMove(current, direction)
        if (next != current) {
            _state.value = next
            viewModelScope.launch { saveState(next) }

            if (next.isGameOver) {
                _stats.value = _stats.value.copy(
                    gameOverCount = _stats.value.gameOverCount + 1,
                )
                viewModelScope.launch { saveStats(_stats.value) }
            }
        }
    }

    fun restart() {
        val prev = _state.value
        if (prev.isGameOver) {
            _stats.value = _stats.value.copy(
                immediateRestartCount = _stats.value.immediateRestartCount + 1,
            )
            viewModelScope.launch { saveStats(_stats.value) }
        }
        val newState = GameLogic.newGame().copy(bestScore = prev.bestScore)
        _state.value = newState
        viewModelScope.launch { saveState(newState) }
    }

    private suspend fun saveState(state: GameState) {
        try {
            val json = Json.encodeToString(
                SerializableGameState.serializer(),
                SerializableGameState.from(state),
            )
            dataStore.edit { prefs ->
                prefs[KEY_GAME_STATE] = json
                prefs[KEY_BEST_SCORE] = state.bestScore
            }
        } catch (e: Exception) {
            android.util.Log.w("GameViewModel", "Failed to save game state", e)
        }
    }

    private suspend fun loadState(): GameState? {
        return try {
            val prefs = dataStore.data.first()
            val json = prefs[KEY_GAME_STATE] ?: return null
            val bestScore = prefs[KEY_BEST_SCORE] ?: 0
            val serializable = Json.decodeFromString(SerializableGameState.serializer(), json)
            serializable.toGameState().copy(bestScore = bestScore)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun saveStats(stats: GameStats) {
        dataStore.edit { prefs ->
            prefs[KEY_GAME_OVER_COUNT] = stats.gameOverCount
            prefs[KEY_RESTART_COUNT] = stats.immediateRestartCount
        }
    }

    private suspend fun loadStats(): GameStats {
        val prefs = dataStore.data.first()
        return GameStats(
            gameOverCount = prefs[KEY_GAME_OVER_COUNT] ?: 0,
            immediateRestartCount = prefs[KEY_RESTART_COUNT] ?: 0,
        )
    }

    companion object {
        private val KEY_GAME_STATE = stringPreferencesKey("game_state")
        private val KEY_BEST_SCORE = intPreferencesKey("best_score")
        private val KEY_GAME_OVER_COUNT = intPreferencesKey("game_over_count")
        private val KEY_RESTART_COUNT = intPreferencesKey("restart_count")
    }
}

data class GameStats(
    val gameOverCount: Int = 0,
    val immediateRestartCount: Int = 0,
)
