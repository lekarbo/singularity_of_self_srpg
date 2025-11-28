package dev.minimal.lwjgl.topdown;

import java.util.Objects;

final class Unit {
    private final String id;
    private final UnitFaction faction;
    private GridPosition position;
    private boolean hasMovedThisTurn;

    Unit(String id, UnitFaction faction) {
        this.id = Objects.requireNonNull(id, "Unit id is required");
        this.faction = Objects.requireNonNull(faction, "Unit faction is required");
    }

    String id() {
        return id;
    }

    UnitFaction faction() {
        return faction;
    }

    boolean isPlaced() {
        return position != null;
    }

    GridPosition position() {
        return position;
    }

    void placeAt(GridPosition gridPosition) {
        this.position = Objects.requireNonNull(gridPosition, "gridPosition");
    }

    boolean hasMovedThisTurn() {
        return hasMovedThisTurn;
    }

    void setMovedThisTurn(boolean moved) {
        this.hasMovedThisTurn = moved;
    }

    void clearPlacement() {
        this.position = null;
    }
}
