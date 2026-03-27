# HexMerge Design Document

> **Summary**: Technical design for HexMerge Android game, covering architecture, animations, UI layout, and game state management
>
> **Project**: HexMerge
> **Version**: 0.1.0
> **Author**: chan
> **Date**: 2026-03-27
> **Status**: Draft (retroactive from working implementation)
> **Planning Doc**: [hexmerge.plan.md](../../01-plan/features/hexmerge.plan.md)

---

## 1. Overview

### 1.1 Design Goals

- Smooth, responsive hex grid interaction at 60fps
- Clean separation of game logic (pure Kotlin) from UI (Compose)
- Minimal APK size (no game engine, pure Canvas rendering)
- State persistence that survives process death

### 1.2 Design Principles

- **Pure domain logic**: `model/` and `game/` packages have zero Android imports
- **Unidirectional data flow**: User swipe -> ViewModel -> GameLogic -> StateFlow -> Compose recomposition
- **Immutable state**: GameState is a data class, every move produces a new instance
- **Canvas-first rendering**: Custom hex drawing via Compose Canvas, no third-party game library

---

## 2. Architecture

### 2.1 Component Diagram

```
┌──────────────────────────────────────────────────────────┐
│                     MainActivity                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │                  GameScreen (Compose)               │  │
│  │  ┌──────────┐  ┌──────────────┐  ┌─────────────┐  │  │
│  │  │ ScoreBar │  │ HexGridCanvas│  │ NewGame Btn │  │  │
│  │  └──────────┘  │  (Canvas +   │  └─────────────┘  │  │
│  │                │  PointerInput)│                    │  │
│  │                └──────────────┘                    │  │
│  └──────────────────────┬─────────────────────────────┘  │
│                         │ collectAsState()                │
│  ┌──────────────────────┴─────────────────────────────┐  │
│  │              GameViewModel                          │  │
│  │  StateFlow<GameState>  +  DataStore persistence     │  │
│  └──────────────────────┬─────────────────────────────┘  │
│                         │                                 │
│  ┌──────────────────────┴─────────────────────────────┐  │
│  │              GameLogic (pure Kotlin)                │  │
│  │  move() + spawnTile() + isGameOver() + applyMove() │  │
│  └──────────────────────┬─────────────────────────────┘  │
│                         │                                 │
│  ┌──────────────────────┴─────────────────────────────┐  │
│  │              HexBoard + HexCoord (domain)          │  │
│  │  Cube coordinates, direction math, cell enumeration │  │
│  └────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

### 2.2 Data Flow

```
User Swipe (drag gesture)
    │
    ▼
angleToDirection(atan2(dy, dx))  ← 6 sectors of 60 degrees
    │
    ▼
GameViewModel.swipe(direction)
    │  if (isGameOver) return early
    ▼
GameLogic.applyMove(state, direction)
    ├── move(state, direction) → MoveResult
    │     ├── sortedForDirection() — process farthest tiles first
    │     └── per tile: slide until blocked, merge if equal + not already merged
    ├── spawnTile() — add 2 (90%) or 4 (10%) on random empty cell
    └── isGameOver() — full board + no adjacent equals
    │
    ▼
StateFlow.value = newState  →  Compose recomposition
    │
    ▼
DataStore.saveState() (async, fire-and-forget)
```

### 2.3 Dependencies

| Component | Depends On | Purpose |
|-----------|-----------|---------|
| GameScreen | GameViewModel | Observes StateFlow, renders board |
| GameViewModel | GameLogic, DataStore | Orchestrates moves, persists state |
| GameLogic | HexBoard, HexCoord | Pure game logic computation |
| HexBoard | HexCoord | Grid operations (allCells, emptyCells, adjacency) |
| HexCoord | Nothing | Cube coordinate math |

---

## 3. Data Model

### 3.1 Core Types

```kotlin
// Cube coordinate for hexagonal grid
data class HexCoord(val q: Int, val r: Int) {
    val s: Int get() = -q - r  // Cube constraint: q + r + s = 0
}

// 6 hex directions with offset deltas
enum class Direction(val dq: Int, val dr: Int) {
    EAST(1, 0), WEST(-1, 0),
    NORTHEAST(1, -1), SOUTHWEST(-1, 1),
    SOUTHEAST(0, 1), NORTHWEST(0, -1)
}

// Immutable game state
data class GameState(
    val board: Map<HexCoord, Int>,  // coord -> tile value (2, 4, 8, ...)
    val score: Int,
    val bestScore: Int,
    val isGameOver: Boolean,
    val radius: Int = 3             // Board radius (37 cells for r=3)
)

// Result of a single move operation
data class MoveResult(
    val newBoard: Map<HexCoord, Int>,
    val scoreGained: Int,
    val merges: List<MergeEvent>,
    val moved: Boolean
)

// Animation target for merge visualization
data class MergeEvent(
    val from: HexCoord,
    val to: HexCoord,
    val value: Int   // Post-merge value
)
```

### 3.2 State Machine

```
                    ┌─────────┐
           restart()│         │ newGame()
          ┌─────────┤  INIT   ├──────────┐
          │         │         │          │
          │         └─────────┘          │
          ▼                              ▼
    ┌───────────┐  swipe()  ┌───────────────┐
    │           │──────────▶│               │
    │  PLAYING  │           │  PLAYING      │
    │           │◀──────────│  (new state)  │
    └─────┬─────┘           └───────────────┘
          │
          │ isGameOver() == true
          ▼
    ┌───────────┐
    │ GAME OVER │──── restart() ──▶ PLAYING
    └───────────┘
```

### 3.3 Persistence (DataStore)

| Key | Type | Purpose |
|-----|------|---------|
| `game_state` | String (JSON) | Full board serialized via SerializableGameState |
| `best_score` | Int | Survives restart, updated on new highs |
| `game_over_count` | Int | Analytics: how many games completed |
| `restart_count` | Int | Analytics: immediate restarts after game over |

---

## 4. UI/UX Design

### 4.1 Screen Layout

```
┌──────────────────────────────────────┐
│          (status bar area)           │
│                                      │
│           H e x M e r g e           │  ← Title: 28sp Bold, #E8E8E8
│                                      │
│     ┌──────────┐  ┌──────────┐      │
│     │  SCORE   │  │   BEST   │      │  ← Score boxes: #16213E bg
│     │   142    │  │   580    │      │    Label: 12sp #888888
│     └──────────┘  └──────────┘      │    Value: 20sp Bold #E8E8E8
│                                      │
│            ╱ ╲ ╱ ╲ ╱ ╲              │
│           │   │   │   │             │
│          ╱ ╲ ╱ ╲ ╱ ╲ ╱ ╲           │  ← Hex grid: 1:1 aspect ratio
│         │   │ 2 │ 4 │   │          │    Empty: #16213E fill, #0F3460 stroke
│        ╱ ╲ ╱ ╲ ╱ ╲ ╱ ╲ ╱ ╲        │    Tiles: colored fill, white text
│       │   │   │   │ 8 │   │       │    Tile size: 0.92 * hexSize
│        ╲ ╱ ╲ ╱ ╲ ╱ ╲ ╱ ╲ ╱        │
│         │   │16 │   │   │          │
│          ╲ ╱ ╲ ╱ ╲ ╱ ╲ ╱           │
│           │   │   │   │             │
│            ╲ ╱ ╲ ╱ ╲ ╱              │
│                                      │
│          ┌──────────────┐            │
│          │   New Game   │            │  ← Button: #0F3460, 12dp radius
│          └──────────────┘            │
│                                      │
│      (game over overlay if needed)   │  ← "Game Over!" #E74C3C 24sp
│                                      │    "Score: 142" #E8E8E8 18sp
└──────────────────────────────────────┘

Background: #1A1A2E (dark navy)
```

### 4.2 Color System

| Value | Hex Color | Visual |
|-------|-----------|--------|
| 2 | `#2ECC71` | Green |
| 4 | `#27AE60` | Dark green |
| 8 | `#1ABC9C` | Teal |
| 16 | `#16A085` | Dark teal |
| 32 | `#3498DB` | Blue |
| 64 | `#2980B9` | Dark blue |
| 128 | `#9B59B6` | Purple |
| 256 | `#8E44AD` | Dark purple |
| 512 | `#E74C3C` | Red |
| 1024 | `#C0392B` | Dark red |
| 2048 | `#F39C12` | Gold |
| 4096+ | `#E67E22` | Orange |

### 4.3 Hex Grid Geometry

```
Coordinate system: Cube coordinates (pointy-top hexagons)

Pixel conversion:
  x = hexSize * (sqrt(3) * q + sqrt(3)/2 * r)
  y = hexSize * (3/2 * r)

Hex size calculation:
  hexSize = min(canvasWidth, canvasHeight) / (radius * 2 + 1) / 1.8

Hex vertex (pointy-top, 6 vertices):
  angle_deg = 60 * i - 30    (i = 0..5)
  x = center.x + size * cos(angle_rad)
  y = center.y + size * sin(angle_rad)
```

### 4.4 Swipe Direction Mapping

```
                  330°─────30°
                 ╱    EAST    ╲
          270°──╱               ╲──90°
         ╱  NORTHEAST    SOUTHEAST  ╲
  270°──╱                            ╲──90°
        ╲  NORTHWEST    SOUTHWEST   ╱
         ╲                         ╱
          210°──╲               ╱──150°
                 ╲    WEST    ╱
                  210°─────150°

Minimum drag distance: 20dp (prevents accidental taps)
```

---

## 5. Animation Design (Pending Implementation)

### 5.1 Merge Animation (FR-09)

```
Timeline: 200ms total, FastOutSlowInEasing

Phase 1 (0-150ms): Slide
  - Tile translates from source to destination hex
  - Use Animatable<Offset> per moving tile

Phase 2 (150-200ms): Pop
  - Merged tile scales from 1.0 -> 1.2 -> 1.0
  - Simultaneous with value text update

State flow:
  swipe() → compute MoveResult → start animations → on complete → update board
```

### 5.2 Spawn Animation (FR-10)

```
Timeline: 150ms, FastOutSlowInEasing

  - New tile scales from 0.0 -> 1.0
  - Slight overshoot: 0.0 -> 1.05 -> 1.0 (spring effect)
  - Triggered after merge animation completes
```

### 5.3 Animation Architecture

```
Current (no animation):
  swipe → applyMove → StateFlow.value = newState → instant recomposition

With animation:
  swipe → applyMove → get MoveResult
    → start slide Animatables for moved tiles
    → start scale Animatables for merged tiles
    → on all complete → StateFlow.value = newState (final positions)
    → start spawn Animatable for new tile
```

---

## 6. Error Handling

| Scenario | Handling | User Experience |
|----------|----------|-----------------|
| Serialization failure on save | Log.w(), continue | Silent, loses progress on kill |
| Corrupted JSON on load | Return null, start new game | Loses progress (acceptable for game) |
| Full board, no spawn possible | Return unchanged state | No visible issue |
| Swipe during game over | Guard clause, ignore | No response (intentional) |
| Invalid hex coordinate | isValid() check | Should never reach user |

---

## 7. Test Plan

### 7.1 Test Scope

| Type | Target | Tool | Status |
|------|--------|------|--------|
| Unit Test | HexCoord, HexBoard, Direction | JUnit 4 + kotlin-test | Done (94 tests) |
| Unit Test | GameLogic (move, merge, spawn, game over) | JUnit 4 + kotlin-test | Done |
| Unit Test | SerializableGameState round-trip | JUnit 4 + kotlin-test | Done |
| Unit Test | angleToDirection (swipe mapping) | JUnit 4 + kotlin-test | Done |
| Instrumented | GameViewModel persistence | AndroidJUnitRunner | Pending |
| Instrumented | Compose UI interactions | Compose test rules | Pending |

### 7.2 Key Test Cases

- [x] `move()` slides tile to edge when path clear
- [x] `move()` merges equal adjacent tiles, no double-merge
- [x] `move()` returns `moved=false` when no movement possible
- [x] `spawnTile()` no-op on full board
- [x] `isGameOver()` false when empty cells exist
- [x] `isGameOver()` true when full + no adjacent equals
- [x] `applyMove()` preserves bestScore, spawns after valid move
- [x] `SerializableGameState` round-trip with negative coords
- [x] `angleToDirection` maps all 6 sectors correctly
- [ ] ViewModel persists and restores state across process death
- [ ] Compose renders correct number of hex cells

---

## 8. Layer Structure

| Layer | Package | Responsibility | Android Deps? |
|-------|---------|---------------|:---:|
| Domain | `com.hexmerge.model` | HexCoord, Direction, HexBoard | No |
| Logic | `com.hexmerge.game` | GameState, GameLogic, MergeEvent | No |
| Presentation | `com.hexmerge.viewmodel` | GameViewModel, DataStore persistence | Yes |
| UI | `com.hexmerge.ui` | GameScreen, Canvas rendering, swipe | Yes |
| Entry | `com.hexmerge` | MainActivity | Yes |

```
Dependency direction (clean):
  model ← game ← viewmodel ← ui ← MainActivity
  (inner)                              (outer)
```

---

## 9. Implementation Order

### 9.1 Completed

1. [x] Domain model (HexCoord, Direction, HexBoard)
2. [x] Game logic (GameState, GameLogic, MergeEvent)
3. [x] ViewModel + DataStore persistence
4. [x] Compose UI (GameScreen, hex rendering, swipe input)
5. [x] Unit tests (94 passing)
6. [x] Paint allocation optimization
7. [x] Error logging for saveState
8. [x] Release signing config + GitHub Actions CI

### 9.2 Remaining (priority order)

1. [ ] Merge animation (FR-09) — slide + pop effect
2. [ ] Spawn animation (FR-10) — scale-in new tiles
3. [ ] Haptic feedback on merge (FR-11)
4. [ ] Instrumented tests (ViewModel + Compose)
5. [ ] Multi-device QA (phone, tablet, foldable)
6. [ ] Play Console submission
7. [ ] Undo last move (FR-12) — store previous state
8. [ ] Sound effects

---

## 10. File Structure

```
app/src/
├── main/kotlin/com/hexmerge/
│   ├── model/
│   │   └── HexGrid.kt          # HexCoord, Direction, HexBoard
│   ├── game/
│   │   ├── GameState.kt         # GameState, MoveResult, MergeEvent, Serializable
│   │   └── GameLogic.kt         # move, spawn, isGameOver, applyMove
│   ├── viewmodel/
│   │   └── GameViewModel.kt     # StateFlow, DataStore, swipe/restart
│   ├── ui/
│   │   └── GameScreen.kt        # Compose Canvas, hex drawing, swipe detection
│   └── MainActivity.kt          # Entry point, edge-to-edge
├── test/kotlin/com/hexmerge/
│   ├── model/
│   │   ├── HexCoordTest.kt      # 11 tests
│   │   └── HexBoardTest.kt      # 13 tests
│   ├── game/
│   │   ├── GameLogicTest.kt     # 15 tests
│   │   └── SerializableGameStateTest.kt  # 5 tests
│   └── ui/
│       └── AngleToDirectionTest.kt  # 11 tests (with boundary tests)
└── androidTest/                  # Instrumented tests (pending)
```

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-03-27 | Initial design (retroactive from implementation + eng review) | chan |
