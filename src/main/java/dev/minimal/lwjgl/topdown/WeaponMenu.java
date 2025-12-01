package dev.minimal.lwjgl.topdown;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class WeaponMenu {
    private final List<Weapon> weapons;
    private int selectedIndex;

    WeaponMenu(List<Weapon> weapons) {
        if (weapons == null || weapons.isEmpty()) {
            throw new IllegalArgumentException("Weapon menu requires at least one weapon");
        }
        this.weapons = new ArrayList<>();
        for (Weapon weapon : weapons) {
            if (weapon != null) {
                this.weapons.add(weapon);
            }
        }
        if (this.weapons.isEmpty()) {
            throw new IllegalArgumentException("Weapon menu requires at least one valid weapon");
        }
        this.selectedIndex = 0;
    }

    List<Weapon> weapons() {
        return Collections.unmodifiableList(weapons);
    }

    Weapon selected() {
        if (weapons.isEmpty()) {
            return null;
        }
        return weapons.get(Math.min(selectedIndex, weapons.size() - 1));
    }

    void moveSelection(int delta) {
        if (weapons.isEmpty()) {
            return;
        }
        selectedIndex = Math.floorMod(selectedIndex + delta, weapons.size());
    }

    int selectedIndex() {
        return selectedIndex;
    }

    void setSelectedIndex(int index) {
        if (weapons.isEmpty()) {
            selectedIndex = 0;
            return;
        }
        selectedIndex = Math.max(0, Math.min(index, weapons.size() - 1));
    }
}
