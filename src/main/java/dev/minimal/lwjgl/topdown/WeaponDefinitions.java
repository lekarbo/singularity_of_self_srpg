package dev.minimal.lwjgl.topdown;

import java.util.List;
import java.util.Map;

final class WeaponDefinitions {
    private static final Weapon PLAYER_LANCE = new Weapon(
        "player-lance",
        "Aegis Lance",
        WeaponTag.ROCK,
        8,
        5,
        List.of(
            new WeaponSkill("lance-thrust", "Focused Thrust", SkillPattern.SINGLE_TARGET, 3, 1,
                "Stab a single tile up to 3 spaces away."),
            new WeaponSkill("lance-sweep", "Shield Sweep", SkillPattern.CONE, 2, 2,
                "Push foes in a short cone while dealing light damage.")
        )
    );

    private static final Weapon ENEMY_PISTOL = new Weapon(
        "enemy-pistol",
        "Pulse Pistol",
        WeaponTag.SCISSORS,
        6,
        3,
        List.of(
            new WeaponSkill("pistol-shot", "Pulse Shot", SkillPattern.SINGLE_TARGET, 4, 1,
                "Single target ranged shot."),
            new WeaponSkill("pistol-burst", "Line Burst", SkillPattern.LINE, 3, 1,
                "Fires a piercing beam through aligned tiles.")
        )
    );

    private static final Map<UnitFaction, Weapon> DEFAULT_WEAPONS = Map.of(
        UnitFaction.PLAYER, PLAYER_LANCE,
        UnitFaction.ENEMY, ENEMY_PISTOL
    );

    private static final List<Weapon> CATALOG = List.of(
        PLAYER_LANCE,
        ENEMY_PISTOL
    );

    private WeaponDefinitions() {
    }

    static Weapon forFaction(UnitFaction faction) {
        return DEFAULT_WEAPONS.getOrDefault(faction, PLAYER_LANCE);
    }

    static List<Weapon> catalog() {
        return CATALOG;
    }
}
