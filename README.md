# LWJGL SRPG Prototype — Isometric Squad Tactics

This fork of the isometric hazard runner is the starting point for a lightweight strategy RPG. Instead of piloting a single hero to an exit, players will field an N-unit squad against an M-unit enemy team on a shared grid. Turns will feel chess-like—snappy tile-to-tile movement, deterministic ability ranges, and no hidden stats—while combat outcomes rely on weapon-specific skills that follow a rock/paper/scissors relationship.

The immediate goal is to keep the polished isometric renderer, HUD, and tooling, then layer on squad placement + movement. Combat, abilities, and campaign structure will build on that foundation once we can comfortably position multiple characters on the board.

## Prerequisites

- JDK 17+ (a local copy is auto-provisioned via the helper scripts)
- `bash`, `curl`, and `unzip`
- OpenGL-compatible GPU/driver
- Internet connectivity the first time you bootstrap the local toolchain

## Setup and virtual toolchain

Everything needed to build/run the sample lives inside the repository so experiments remain isolated.

```bash
./scripts/create-jdk.sh     # downloads Temurin JDK 17 into .java-env/
./scripts/create-env.sh     # downloads Gradle 8.7 into .gradle-env/
```

You rarely need to call those directly because every other helper ensures the toolchain exists before starting.

```bash
./scripts/gradle.sh tasks   # run any Gradle task with the local toolchain
./scripts/run.sh            # compile + launch the game loop
```

The first execution fetches LWJGL artifacts from Maven Central; subsequent runs are fully offline.

## Tactics-focused control goals

Current build still exposes the original movement controls while we stand up the SRPG-specific systems. Over the next iterations they will evolve into:

- **Cursor + squad selection** (`WASD`/arrows or mouse): move a cursor, highlight tiles, and select friendly units.
- **Placement phase**: drag/drop or key-select spawn tiles for the player party while the AI populates its slots.
- **Turn actions**: Select a unit → preview reachable tiles → confirm a move → choose a weapon skill and target.
- **Reset/testing shortcuts**: keep `R` for fast regeneration while the systems remain under heavy iteration.

## Architecture checkpoints

| Component | Responsibility | Notes |
| --- | --- | --- |
| Component | Responsibility | SRPG evolution |
| --- | --- | --- |
| `TopDownPlatformerGame` | Owns window, loop, input handling, HUD, and high-level state | Will become the tactical battle driver with explicit phases: placement → turns → victory |
| `LevelSettings` + `Level` | Grid data + iso renderer | Gains spawn zones, blocking terrain tags, and influence overlays |
| `LevelGenerator` | Arena creation | Later iterations swap to authored encounter maps + faction deployment markers |
| `Player`/`PlayerSprite` | Single avatar movement | Promotion into a reusable `Unit` renderer + animation hooks shared by player/enemy squads |
| `TileType`, `Direction`, `GameStatus`, `GridPosition` | Grid primitives | Extended with helper math for range, adjacency, and weapon skill footprints |

Key advantages we keep from the base project:

- **Separated concerns** between update/render/input to isolate tactical state machines.
- **Deterministic, data-driven grids** that make tile math predictable and easy to unit-test.
- **Immediate-mode iso rendering** that stays approachable while we experiment with UI layers.
- **Modular components** so new systems (cursor controller, unit rosters, ability resolvers, AI brains) live in dedicated classes with clear APIs, making them trivial to reuse or swap out across missions and future prototypes.

## SRPG development roadmap

1. **Squad placement + cursor-driven movement (current focus)**  
   - Represent multiple friendly + enemy units on the grid.  
   - Implement a cursor/highlight system for selecting spawn tiles.  
   - Add a placement phase UI (confirm, rotate camera later).  
   - Validate pathfinding + legal movement previews before shifting turns into place. This means adding a flood-fill or A*/BFS routine that respects blocking tiles + unit occupancy, surfacing its result as translucent tiles/arrows, and only allowing confirmations that match those reachable destinations so turn-handling logic inherits a trustworthy move resolver.

2. **Turn controller + initiative**  
   - Replace the continuous loop with explicit turn phases (player team → enemy AI).  
   - Track per-unit state (moved/acted flags) and enforce one move + one ability per turn.  
   - Add simple AI that mirrors player move rules to validate the system.

3. **Weapon-driven ability kit**  
   - Introduce weapon definitions with embedded skills (cone, line, single-target) and rock/paper/scissors tags.  
   - Render target previews before confirmation; apply deterministic outcomes (push, stun, break weapon).  
   - Expand the HUD to show a unit’s weapon, available skills, and the R/P/S icon for quick reads.

4. **Encounter authoring**  
   - Replace procedural arenas with hand-authored maps that embed spawn zones, obstacles, and scripted objectives.  
   - Build a lightweight level description format (JSON/TOML) that feeds the existing `Level` parser.  
   - Support multiple encounters and a simple mission select loop.

5. **UX + polish**  
   - Dedicated overlays for turn order, ability explanations, and combat log.  
   - Audio/visual cues for move confirmation, ability resolution, and end-of-turn markers.  
   - Optional mouse controls and key rebinding once the keyboard loop is stable.

6. **Technical hardening**  
   - Extract pathfinding, ability resolution, and AI routines into testable classes.  
   - Wire up CI to run `./scripts/gradle.sh test` and validate new content.  
   - Profile for hotspots before layering on effects or networking experiments.

## From-scratch SRPG build checklist

1. **Engine bootstrap** — Provision a Gradle/LWJGL project, wire GLFW window creation, and stand up an OpenGL 2.1 render loop with deterministic timestep handling.
2. **Grid + rendering core** — Implement the `Level` data model, tile serialization format, and immediate-mode isometric renderer capable of drawing floors, walls, spawn zones, and overlays.
3. **Input + cursor controller** — Abstract keyboard/mouse input into a cursor/highlight system that can navigate tiles independent of any specific unit logic.
4. **Unit system** — Create reusable `Unit` and `UnitSprite` classes for both player and enemy squads, including animation hooks, occupancy checks, and squad roster management.
5. **Movement + pathfinding** — Integrate BFS/A* reachability that respects blocking tiles and occupied cells, render translucent previews, and gate movement confirmations through that solver.
6. **Turn/phase manager** — Replace the free-running loop with explicit placement, player, AI, victory, and defeat phases, tracking per-unit move/act state each round.
7. **Ability kit + combat resolution** — Define weapons with embedded skills, target patterns, and deterministic damage/effects; extend the HUD with targeting previews and combat summaries.
8. **Encounter authoring pipeline** — Build JSON/TOML mission files with spawn zones, objectives, scripts, and load them through a selector screen or CLI prompt.
9. **AI behaviors** — Implement baseline AI that mirrors legal movement/ability rules, then add heuristics (threat evaluation, focus fire, objective chasing) as the roster grows.
10. **Polish + tooling** — Layer on debug overlays, profiling hooks, CI test runs, audio cues, and optional mouse bindings to make iteration smoother for future prototypes.

## TODO

- [ ] Draft the JSON/TOML mission schema and spike a loader that translates it into `Level` tiles.
- [ ] Prototype the cursor/highlight controller that decouples tile focus from the player avatar.
- [ ] Extract `Player` logic into a generalized `Unit` component shared by both factions.
- [ ] Stand up the flood-fill movement preview and surface it as translucent highlights.
- [ ] Add JUnit coverage around pathfinding and ability resolution to prep for CI.

## Config tweaks while iterating

All helper scripts still work the same way (`./scripts/run.sh`, `./scripts/gradle.sh build`). Until authored encounters arrive, continue using the config overlay (`Tab`/`M`) to resize the board or adjust tile sizes. Hazard density currently influences how many blocking pillars spawn—treat that as a stand-in for terrain density until the SRPG-specific generator lands.

## Repository layout

```
srpg-isometric/
├── build.gradle.kts
├── scripts/               # toolchain + run helpers
├── src/main/java/dev/minimal/lwjgl/topdown/
│   ├── Direction.java
│   ├── GameConfig.java
│   ├── GameStatus.java
│   ├── GridPosition.java
│   ├── Level.java
│   ├── LevelGenerator.java
│   ├── LevelSettings.java
│   ├── Main.java
│   ├── Player.java
│   ├── PlayerSprite.java
│   ├── TileType.java
│   └── TopDownPlatformerGame.java
└── README.md
```

Use this project as a reference when bootstrapping future LWJGL experiments that need deterministic grid movement, procedural arenas, or a lightweight 2D isometric renderer.
