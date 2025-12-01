package dev.minimal.lwjgl.topdown;

import java.util.List;

record Weapon(
    String id,
    String displayName,
    WeaponTag tag,
    int damage,
    int defense,
    List<WeaponSkill> skills
) {
}
