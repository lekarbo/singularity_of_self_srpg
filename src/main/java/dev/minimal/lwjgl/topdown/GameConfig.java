package dev.minimal.lwjgl.topdown;

import java.util.Locale;
import java.util.Objects;

final class GameConfig {
    private static final int DEFAULT_WIDTH = 25;
    private static final int DEFAULT_HEIGHT = 19;
    private static final int DEFAULT_TILE_SIZE = 48;
    private static final float DEFAULT_HAZARD_DENSITY = 0.08f;

    private final int width;
    private final int height;
    private final int tileSize;
    private final float hazardDensity;
    private final long seed;
    private final boolean seedLocked;

    GameConfig(int width, int height, int tileSize, float hazardDensity, long seed, boolean seedLocked) {
        this.width = makeOdd(width);
        this.height = makeOdd(height);
        this.tileSize = tileSize;
        this.hazardDensity = Math.min(Math.max(hazardDensity, 0f), 0.9f);
        this.seed = seed;
        this.seedLocked = seedLocked;
    }

    int width() {
        return width;
    }

    int height() {
        return height;
    }

    int tileSize() {
        return tileSize;
    }

    float hazardDensity() {
        return hazardDensity;
    }

    long seed() {
        return seed;
    }

    boolean seedLocked() {
        return seedLocked;
    }

    LevelSettings toLevelSettings() {
        return new LevelSettings(width, height, tileSize, hazardDensity, seed, seedLocked);
    }

    static GameConfig fromArgs(String[] args) {
        int width = DEFAULT_WIDTH;
        int height = DEFAULT_HEIGHT;
        int tileSize = DEFAULT_TILE_SIZE;
        float hazard = DEFAULT_HAZARD_DENSITY;
        Long seed = null;

        for (String arg : Objects.requireNonNullElse(args, new String[0])) {
            if (arg == null || arg.isBlank()) {
                continue;
            }
            String cleaned = arg.startsWith("--") ? arg.substring(2) : arg;
            String[] parts = cleaned.split("=", 2);
            if (parts.length != 2) {
                continue;
            }
            String key = parts[0].trim().toLowerCase(Locale.ROOT);
            String value = parts[1].trim();
            switch (key) {
                case "width" -> width = parsePositiveInt(value, width);
                case "height" -> height = parsePositiveInt(value, height);
                case "tilesize" -> tileSize = parsePositiveInt(value, tileSize);
                case "hazard" -> hazard = parseFloat(value, hazard);
                case "seed" -> seed = parseLong(value, null);
                default -> {
                }
            }
        }

        boolean customSeed = seed != null;
        long resolvedSeed = customSeed ? seed : System.nanoTime();
        return new GameConfig(width, height, tileSize, hazard, resolvedSeed, customSeed);
    }

    private static int parsePositiveInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static Long parseLong(String value, Long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static int makeOdd(int input) {
        return (input & 1) == 1 ? input : input + 1;
    }
}
