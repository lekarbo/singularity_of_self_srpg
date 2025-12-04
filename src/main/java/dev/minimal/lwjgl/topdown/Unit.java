package dev.minimal.lwjgl.topdown;

import java.util.Locale;
import java.util.Objects;

final class Unit {
    private final String id;
    private final UnitFaction faction;
    private GridPosition position;
    private boolean hasMovedThisTurn;
    private final int maxHp;
    private final int maxSp;
    private int hp;
    private int sp;
    private Weapon weapon;
    private static final int DEFAULT_HP = 30;
    private static final int DEFAULT_SP = 10;

    Unit(String id, UnitFaction faction) {
        this(id, faction, DEFAULT_HP, DEFAULT_SP);
    }

    Unit(String id, UnitFaction faction, int maxHp, int maxSp) {
        this.id = Objects.requireNonNull(id, "Unit id is required");
        this.faction = Objects.requireNonNull(faction, "Unit faction is required");
        this.maxHp = Math.max(1, maxHp);
        this.maxSp = Math.max(0, maxSp);
        this.hp = this.maxHp;
        this.sp = this.maxSp;
        this.weapon = WeaponDefinitions.forFaction(faction);
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

    Weapon weapon() {
        return weapon;
    }

    void equipWeapon(Weapon weapon) {
        this.weapon = Objects.requireNonNull(weapon, "weapon");
    }

    int maxHp() {
        return maxHp;
    }

    int maxSp() {
        return maxSp;
    }

    int hp() {
        return hp;
    }

    int sp() {
        return sp;
    }

    String displayName() {
        if (id == null || id.isBlank()) {
            return "";
        }
        String[] segments = id.split("-");
        StringBuilder builder = new StringBuilder();
        for (String segment : segments) {
            if (segment == null || segment.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(capitalize(segment));
        }
        return builder.toString();
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.length() == 1) {
            return normalized.toUpperCase(Locale.ROOT);
        }
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }
}
