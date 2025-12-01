package dev.minimal.lwjgl.topdown;

import java.util.List;

record Weapon(
    String id,
    String displayName,
    WeaponTag tag,
    List<WeaponSkill> skills
) {
}
