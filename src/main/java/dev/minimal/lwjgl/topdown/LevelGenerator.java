package dev.minimal.lwjgl.topdown;

import java.util.Random;

/**
 * Generates open-area arenas with scattered obstacles and hazards while guaranteeing a clear route between
 * the spawn and exit.
 */
final class LevelGenerator {
    private static final char WALL = '#';
    private static final char FLOOR = '.';
    private static final char START = 'S';
    private static final char EXIT = 'G';
    private static final char HAZARD = 'X';
    private static final char PLAYER_SPAWN = 'P';
    private static final char ENEMY_SPAWN = 'E';

    private final int width;
    private final int height;
    private final int tileSize;
    private final Random random;
    private final float hazardChance;

    LevelGenerator(int width, int height, int tileSize, float hazardChance, long seed) {
        if (width % 2 == 0 || height % 2 == 0) {
            throw new IllegalArgumentException("Maze dimensions must be odd to guarantee walls around passages");
        }
        this.width = width;
        this.height = height;
        this.tileSize = tileSize;
        this.hazardChance = hazardChance;
        this.random = new Random(seed);
    }

    Level generate() {
        char[][] grid = new char[height][width];
        boolean[][] walkway = new boolean[height][width];
        fillWithGround(grid);

        GridPosition start = new GridPosition(1, Math.max(1, height / 2));
        GridPosition exit = new GridPosition(width - 2, random.nextInt(1, height - 1));

        carveGuidedPath(grid, walkway, start, exit);
        placeObstacleClusters(grid, walkway);
        sprinkleHazards(grid, walkway);
        placeSpawnZones(grid, start, exit);

        grid[start.row()][start.col()] = START;
        grid[exit.row()][exit.col()] = EXIT;
        return new Level(toRows(grid), tileSize);
    }

    private void fillWithGround(char[][] grid) {
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                boolean isBorder = row == 0 || col == 0 || row == height - 1 || col == width - 1;
                grid[row][col] = isBorder ? WALL : FLOOR;
            }
        }
    }

    private void carveGuidedPath(char[][] grid, boolean[][] walkway, GridPosition start, GridPosition exit) {
        int col = start.col();
        int row = start.row();
        walkway[row][col] = true;

        while (col != exit.col() || row != exit.row()) {
            boolean moveHorizontally = random.nextBoolean();
            if (col == exit.col()) {
                moveHorizontally = false;
            } else if (row == exit.row()) {
                moveHorizontally = true;
            }

            if (moveHorizontally) {
                col += Integer.signum(exit.col() - col);
            } else {
                row += Integer.signum(exit.row() - row);
            }

            if (row <= 0 || row >= height - 1 || col <= 0 || col >= width - 1) {
                break;
            }
            walkway[row][col] = true;
            grid[row][col] = FLOOR;
        }
    }

    private void placeObstacleClusters(char[][] grid, boolean[][] walkway) {
        int clusters = Math.max(3, (width * height) / 150);
        for (int i = 0; i < clusters; i++) {
            int clusterWidth = random.nextInt(2, 5);
            int clusterHeight = random.nextInt(2, 5);
            int col = random.nextInt(1, width - clusterWidth - 1);
            int row = random.nextInt(1, height - clusterHeight - 1);
            for (int y = row; y < row + clusterHeight; y++) {
                for (int x = col; x < col + clusterWidth; x++) {
                    if (!walkway[y][x]) {
                        grid[y][x] = WALL;
                    }
                }
            }
        }

        // sprinkle isolated pillars
        int pillars = Math.max(5, (width * height) / 120);
        for (int i = 0; i < pillars; i++) {
            int col = random.nextInt(1, width - 1);
            int row = random.nextInt(1, height - 1);
            if (!walkway[row][col]) {
                grid[row][col] = WALL;
            }
        }
    }

    private void sprinkleHazards(char[][] grid, boolean[][] walkway) {
        for (int row = 1; row < height - 1; row++) {
            for (int col = 1; col < width - 1; col++) {
                if (walkway[row][col]) {
                    continue;
                }
                if (grid[row][col] == FLOOR && random.nextFloat() < hazardChance) {
                    grid[row][col] = HAZARD;
                }
            }
        }
    }

    private void placeSpawnZones(char[][] grid, GridPosition start, GridPosition exit) {
        carveSpawnBlock(grid, start, PLAYER_SPAWN, 1);
        carveSpawnBlock(grid, exit, ENEMY_SPAWN, -1);
    }

    private void carveSpawnBlock(char[][] grid, GridPosition anchor, char symbol, int horizontalDirection) {
        int blockWidth = 2;
        int blockHeight = 2;
        int startCol = anchor.col() + (horizontalDirection > 0 ? 1 : -blockWidth);
        startCol = clampInteriorCol(startCol, blockWidth);
        int startRow = clampInteriorRow(anchor.row() - blockHeight / 2, blockHeight);
        for (int row = startRow; row < startRow + blockHeight; row++) {
            for (int col = startCol; col < startCol + blockWidth; col++) {
                grid[row][col] = symbol;
            }
        }
    }

    private int clampInteriorCol(int desired, int span) {
        int min = 1;
        int max = span >= width ? min : width - span - 1;
        return Math.max(min, Math.min(desired, max));
    }

    private int clampInteriorRow(int desired, int span) {
        int min = 1;
        int max = span >= height ? min : height - span - 1;
        return Math.max(min, Math.min(desired, max));
    }

    private String[] toRows(char[][] grid) {
        String[] rows = new String[height];
        for (int row = 0; row < height; row++) {
            rows[row] = new String(grid[row]);
        }
        return rows;
    }
}
