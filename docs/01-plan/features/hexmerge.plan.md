# HexMerge Planning Document

> **Summary**: A 2048-style number merge puzzle game on a hexagonal grid for Android
>
> **Project**: HexMerge
> **Version**: 0.1.0
> **Author**: chan
> **Date**: 2026-03-27
> **Status**: Draft (retroactive — implementation already in progress)

---

## 1. Overview

### 1.1 Purpose

HexMerge is a casual puzzle game where players swipe to merge numbered tiles on a hexagonal grid. Same-value tiles merge into their sum (2+2=4, 4+4=8, etc.). The hexagonal grid adds 6 directional swipes instead of the traditional 4, creating deeper strategy than standard 2048.

### 1.2 Background

2048-style merge games are proven casual game mechanics with strong retention. The hex grid variant is differentiated enough to stand on its own while being immediately familiar to players who know the genre. Target platform is Android (Google Play Store).

### 1.3 Related Documents

- Design: `docs/02-design/features/hexmerge.design.md` (to be created)

---

## 2. Scope

### 2.1 In Scope

- [x] Hexagonal grid board (radius 3, 37 cells)
- [x] 6-directional swipe input (E, W, NE, NW, SE, SW)
- [x] Tile spawning (90% chance of 2, 10% chance of 4)
- [x] Tile merging and score calculation
- [x] Game over detection (no empty cells + no adjacent equal tiles)
- [x] Score and best score display
- [x] Game state persistence (DataStore)
- [x] New Game / Restart
- [ ] Merge animations (tile slide + pop)
- [ ] New tile spawn animation
- [ ] Haptic feedback on merge
- [ ] Sound effects
- [ ] Undo last move
- [ ] Google Play Games leaderboard integration

### 2.2 Out of Scope

- Online multiplayer
- In-app purchases / monetization (v1 is free, no ads)
- iOS version
- Custom themes / skins
- Tutorial / onboarding flow (game is self-explanatory)

---

## 3. Requirements

### 3.1 Functional Requirements

| ID | Requirement | Priority | Status |
|----|-------------|----------|--------|
| FR-01 | Hex grid renders correctly with radius-3 board | High | Done |
| FR-02 | 6-directional swipe detection from drag gestures | High | Done |
| FR-03 | Tiles slide in swipe direction, merge on collision with equal value | High | Done |
| FR-04 | New tile (2 or 4) spawns after each valid move | High | Done |
| FR-05 | Game over when board full and no merges possible | High | Done |
| FR-06 | Score tracks sum of merged tile values | High | Done |
| FR-07 | Best score persists across sessions | High | Done |
| FR-08 | Game state saves/restores on app kill/reopen | High | Done |
| FR-09 | Merge animation (tile slides to target, pop effect) | Medium | Pending |
| FR-10 | Spawn animation (scale-in for new tiles) | Medium | Pending |
| FR-11 | Haptic feedback on successful merge | Low | Pending |
| FR-12 | Undo last move (single step) | Low | Pending |

### 3.2 Non-Functional Requirements

| Category | Criteria | Measurement Method |
|----------|----------|-------------------|
| Performance | 60fps during swipe + animation | Android Studio profiler |
| Performance | Move computation < 5ms (37-cell board) | Logcat timing |
| Battery | No background CPU usage when idle | Battery profiler |
| Size | APK < 10MB | Build output |
| Compatibility | Android 8.0+ (API 26) | minSdk in build.gradle |

---

## 4. Success Criteria

### 4.1 Definition of Done

- [x] Core game loop: swipe, merge, spawn, game over
- [x] Score tracking and persistence
- [ ] Merge/spawn animations feel smooth
- [ ] No visual glitches on common Android screen sizes
- [ ] Game Over state has clear restart path
- [ ] Published to Google Play Store

### 4.2 Quality Criteria

- [ ] Unit tests for GameLogic (merge, move, game over detection)
- [ ] Unit tests for HexBoard (coordinate math, adjacency)
- [ ] Manual QA on 3+ device sizes
- [ ] No ANR or crash in 30 min play session

---

## 5. Risks and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Swipe direction ambiguous at hex boundaries | Medium | Medium | 60-degree sectors with clear angle mapping (implemented) |
| Performance on low-end devices with animations | Medium | Low | Canvas-based rendering, no heavy Compose animations |
| Play Console review delays (new account, 7+ days) | High | High | Submit early, develop in parallel |
| Hex grid rendering miscalculation | High | Low | Pointy-top hex math with standard cube coordinates (implemented) |

---

## 6. Architecture Considerations

### 6.1 Project Level Selection

| Level | Characteristics | Recommended For | Selected |
|-------|-----------------|-----------------|:--------:|
| **Starter** | Simple structure | Static sites, portfolios | ☐ |
| **Dynamic** | Feature-based modules, state management | Apps with persistence, game logic | ☒ |
| **Enterprise** | Strict layer separation, DI, microservices | High-traffic systems | ☐ |

### 6.2 Key Architectural Decisions

| Decision | Options | Selected | Rationale |
|----------|---------|----------|-----------|
| Platform | Android Native / Flutter / KMP | Android Native (Kotlin) | Simplest path to Play Store, single platform target |
| UI Framework | Compose / XML Views | Jetpack Compose | Modern, declarative, good for custom Canvas drawing |
| Rendering | Compose Canvas / OpenGL / Custom View | Compose Canvas | Sufficient for 2D hex grid, no GPU pipeline needed |
| State Mgmt | StateFlow / LiveData / Compose State | StateFlow + ViewModel | Clean unidirectional data flow |
| Persistence | DataStore / Room / SharedPrefs | DataStore Preferences | Lightweight key-value, no relational data needed |
| Serialization | Gson / Moshi / kotlinx.serialization | kotlinx.serialization | Kotlin-native, no reflection |
| Build | Gradle KTS | Gradle KTS | Standard Android |

### 6.3 Architecture

```
Selected Level: Dynamic

Package Structure:
com.hexmerge/
  model/          # Domain: HexCoord, Direction, HexBoard
  game/           # Logic: GameState, GameLogic, MergeEvent
  viewmodel/      # Presentation: GameViewModel, persistence
  ui/             # Compose: GameScreen, hex rendering
  MainActivity.kt # Entry point
```

---

## 7. Convention Prerequisites

### 7.1 Existing Project Conventions

- [x] Kotlin coding conventions (standard)
- [x] Package-by-feature structure
- [ ] `CLAUDE.md` with project conventions
- [ ] Detekt / ktlint configuration
- [ ] Test conventions

### 7.2 Conventions to Define/Verify

| Category | Current State | To Define | Priority |
|----------|---------------|-----------|:--------:|
| **Naming** | Kotlin standard | PascalCase classes, camelCase functions | Medium |
| **Folder structure** | Defined | model/game/viewmodel/ui packages | Done |
| **Color constants** | In GameScreen.kt | Extract to theme or constants file | Low |
| **State management** | StateFlow pattern | Keep unidirectional flow | Done |

### 7.3 Environment Variables Needed

None — this is a standalone Android app with no backend or API keys.

---

## 8. Next Steps

1. [ ] Create design document (`hexmerge.design.md`) — detail animations, responsive layout, visual design
2. [ ] Add merge/spawn animations
3. [ ] Unit tests for GameLogic and HexBoard
4. [ ] QA on multiple screen sizes
5. [ ] Play Console account setup and first submission
6. [ ] Design review for visual polish

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-03-27 | Initial draft (retroactive from implementation) | chan |
| 0.2 | 2026-03-27 | Eng review: +signing/CI, +tests as v1 req, +Paint fix, +error logging | chan |

## GSTACK REVIEW REPORT

| Review | Trigger | Why | Runs | Status | Findings |
|--------|---------|-----|------|--------|----------|
| CEO Review | `/plan-ceo-review` | Scope & strategy | 0 | — | — |
| Codex Review | `/codex review` | Independent 2nd opinion | 0 | — | — |
| Eng Review | `/plan-eng-review` | Architecture & tests (required) | 1 | CLEAR (PLAN) | 3 issues, 1 critical gap |
| Design Review | `/plan-design-review` | UI/UX gaps | 0 | — | — |

- **UNRESOLVED:** 0
- **VERDICT:** ENG CLEARED — ready to implement
