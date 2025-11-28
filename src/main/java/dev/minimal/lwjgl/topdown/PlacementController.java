package dev.minimal.lwjgl.topdown;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class PlacementController {
    private final CursorController cursor;
    private final Squad squad;
    private final Set<GridPosition> spawnTiles;
    private int placedUnits;

    PlacementController(CursorController cursor, Squad squad, List<GridPosition> spawnTiles) {
        this.cursor = Objects.requireNonNull(cursor, "cursor");
        this.squad = Objects.requireNonNull(squad, "squad");
        this.spawnTiles = new HashSet<>(Objects.requireNonNull(spawnTiles, "spawnTiles"));
        this.placedUnits = 0;
    }

    PlacementResult attemptPlacement() {
        if (spawnTiles.isEmpty()) {
            return PlacementResult.INVALID;
        }
        GridPosition focus = cursor.gridPosition();
        if (!spawnTiles.contains(focus)) {
            return PlacementResult.INVALID;
        }
        if (isOccupied(focus)) {
            return PlacementResult.INVALID;
        }
        Unit unit = squad.nextUnplacedUnit();
        if (unit == null) {
            return PlacementResult.INVALID;
        }
        unit.placeAt(focus);
        placedUnits++;
        if (placedUnits >= squad.units().size()) {
            return PlacementResult.COMPLETED;
        }
        return PlacementResult.PLACED;
    }

    boolean isSpawnTile(GridPosition position) {
        return spawnTiles.contains(position);
    }

    int remainingUnits() {
        return Math.max(0, squad.units().size() - placedUnits);
    }

    boolean allUnitsPlaced() {
        return placedUnits >= squad.units().size();
    }

    private boolean isOccupied(GridPosition position) {
        for (Unit unit : squad.units()) {
            if (position.equals(unit.position())) {
                return true;
            }
        }
        return false;
    }

    Unit nextUnit() {
        return squad.nextUnplacedUnit();
    }

    Squad squad() {
        return squad;
    }

    enum PlacementResult {
        INVALID,
        PLACED,
        COMPLETED
    }
}
