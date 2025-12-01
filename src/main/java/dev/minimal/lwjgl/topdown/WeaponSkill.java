package dev.minimal.lwjgl.topdown;

record WeaponSkill(
    String id,
    String displayName,
    SkillPattern pattern,
    int range,
    int width,
    String description
) {
}
