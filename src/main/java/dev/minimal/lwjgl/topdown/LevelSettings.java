package dev.minimal.lwjgl.topdown;

import java.util.concurrent.ThreadLocalRandom;

record LevelSettings(int width, int height, int tileSize, float hazardDensity, long seed, boolean lockSeed) {
    private static final int MIN_DIMENSION = 5;
    private static final int MIN_TILE_SIZE = 16;
    private static final int MAX_TILE_SIZE = 128;
    private static final float MIN_HAZARD = 0f;
    private static final float MAX_HAZARD = 0.6f;

    LevelSettings {
        width = ensureOdd(Math.max(width, MIN_DIMENSION));
        height = ensureOdd(Math.max(height, MIN_DIMENSION));
        tileSize = Math.max(MIN_TILE_SIZE, Math.min(tileSize, MAX_TILE_SIZE));
        hazardDensity = clamp(hazardDensity);
    }

    LevelSettings withWidthDelta(int delta) {
        return new LevelSettings(width + delta, height, tileSize, hazardDensity, seed, lockSeed);
    }

    LevelSettings withHeightDelta(int delta) {
        return new LevelSettings(width, height + delta, tileSize, hazardDensity, seed, lockSeed);
    }

    LevelSettings withTileSizeDelta(int delta) {
        return new LevelSettings(width, height, tileSize + delta, hazardDensity, seed, lockSeed);
    }

    LevelSettings withHazardDelta(float delta) {
        return new LevelSettings(width, height, tileSize, hazardDensity + delta, seed, lockSeed);
    }

    LevelSettings withSeed(long newSeed) {
        return new LevelSettings(width, height, tileSize, hazardDensity, newSeed, true);
    }

    LevelSettings withRandomSeed() {
        long newSeed = ThreadLocalRandom.current().nextLong();
        return new LevelSettings(width, height, tileSize, hazardDensity, newSeed, false);
    }

    LevelSettings withSeedLock(boolean locked) {
        return new LevelSettings(width, height, tileSize, hazardDensity, seed, locked);
    }

    static LevelSettings fromConfig(GameConfig config) {
        return config.toLevelSettings();
    }

    private static int ensureOdd(int value) {
        return (value & 1) == 1 ? value : value + 1;
    }

    private static float clamp(float value) {
        return Math.max(MIN_HAZARD, Math.min(MAX_HAZARD, value));
    }
}
