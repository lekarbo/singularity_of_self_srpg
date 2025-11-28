package dev.minimal.lwjgl.topdown;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

final class Squad {
    private final UnitFaction faction;
    private final List<Unit> units;

    Squad(UnitFaction faction, List<Unit> units) {
        this.faction = Objects.requireNonNull(faction, "faction");
        this.units = new ArrayList<>();
        for (Unit unit : Objects.requireNonNull(units, "units")) {
            if (unit.faction() != faction) {
                throw new IllegalArgumentException("Unit faction mismatch");
            }
            this.units.add(unit);
        }
    }

    static Squad create(UnitFaction faction, int unitCount) {
        if (unitCount < 0) {
            throw new IllegalArgumentException("unitCount must be non-negative");
        }
        List<Unit> units = new ArrayList<>(unitCount);
        for (int i = 0; i < unitCount; i++) {
            String id = faction.name().toLowerCase(Locale.ROOT) + "-" + (i + 1);
            units.add(new Unit(id, faction));
        }
        return new Squad(faction, units);
    }

    UnitFaction faction() {
        return faction;
    }

    List<Unit> units() {
        return Collections.unmodifiableList(units);
    }

    Unit nextUnplacedUnit() {
        for (Unit unit : units) {
            if (!unit.isPlaced()) {
                return unit;
            }
        }
        return null;
    }

    boolean allUnitsPlaced() {
        return nextUnplacedUnit() == null;
    }

    void clearPlacements() {
        for (Unit unit : units) {
            unit.clearPlacement();
        }
    }
}
