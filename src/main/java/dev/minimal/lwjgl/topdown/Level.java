package dev.minimal.lwjgl.topdown;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_LINE_LOOP;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_FAN;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glVertex2f;

final class Level {
    private final TileType[][] tiles;
    private final GridPosition start;
    private final GridPosition exit;
    private final int tileSize;
    private final float isoTileWidth;
    private final float isoTileHeight;
    private final float halfIsoWidth;
    private final float halfIsoHeight;

    private final int columns;
    private final int rows;
    private final int pixelWidth;
    private final int pixelHeight;
    private final float originX;
    private final float originY;
    private final List<GridPosition> playerSpawnTiles;
    private final List<GridPosition> enemySpawnTiles;

    Level(String[] layout, int tileSize) {
        if (layout.length == 0) {
            throw new IllegalArgumentException("Level layout requires rows");
        }
        this.tileSize = tileSize;
        this.rows = layout.length;
        this.columns = layout[0].length();
        this.isoTileWidth = tileSize;
        this.isoTileHeight = tileSize * 0.6f;
        this.halfIsoWidth = isoTileWidth / 2f;
        this.halfIsoHeight = isoTileHeight / 2f;
        float minCenterX = -(rows - 1) * halfIsoWidth;
        float maxCenterX = (columns - 1) * halfIsoWidth;
        float widthSpan = (maxCenterX - minCenterX) + isoTileWidth;
        float horizontalMargin = isoTileWidth * 2f;
        this.originX = horizontalMargin - minCenterX;

        float hudHeight = 64f;
        float wallHeight = isoTileHeight * 2.2f;
        float topPadding = isoTileHeight * 3f;
        float bottomPadding = isoTileHeight * 6f;
        this.originY = hudHeight + topPadding + wallHeight;
        float boardDepth = (columns + rows - 2) * halfIsoHeight + isoTileHeight;
        this.pixelWidth = (int) Math.ceil(widthSpan + horizontalMargin * 2f);
        this.pixelHeight = (int) Math.ceil(originY + boardDepth + bottomPadding);
        this.tiles = new TileType[rows][columns];
        this.playerSpawnTiles = new ArrayList<>();
        this.enemySpawnTiles = new ArrayList<>();

        GridPosition startPos = null;
        GridPosition exitPos = null;

        for (int row = 0; row < rows; row++) {
            String line = layout[row];
            if (line.length() != columns) {
                throw new IllegalStateException("Inconsistent level width at row " + row);
            }
            for (int col = 0; col < columns; col++) {
                TileType tile = TileType.fromSymbol(line.charAt(col));
                tiles[row][col] = tile;
                if (tile == TileType.START) {
                    startPos = new GridPosition(col, row);
                } else if (tile == TileType.EXIT) {
                    exitPos = new GridPosition(col, row);
                } else if (tile == TileType.PLAYER_SPAWN) {
                    playerSpawnTiles.add(new GridPosition(col, row));
                } else if (tile == TileType.ENEMY_SPAWN) {
                    enemySpawnTiles.add(new GridPosition(col, row));
                }
            }
        }

        if (startPos == null || exitPos == null) {
            throw new IllegalStateException("Level must define both a start (S) and exit (G)");
        }
        this.start = startPos;
        this.exit = exitPos;
    }

    List<GridPosition> playerSpawnTiles() {
        return Collections.unmodifiableList(playerSpawnTiles);
    }

    List<GridPosition> enemySpawnTiles() {
        return Collections.unmodifiableList(enemySpawnTiles);
    }

    GridPosition startPosition() {
        return start;
    }

    GridPosition exitPosition() {
        return exit;
    }

    boolean isWalkable(int col, int row) {
        TileType tile = tileAt(col, row);
        return !tile.blocksMovement;
    }

    TileType tileAt(int col, int row) {
        if (!isInside(col, row)) {
            return TileType.WALL;
        }
        return tiles[row][col];
    }

    boolean isInside(int col, int row) {
        return col >= 0 && col < columns && row >= 0 && row < rows;
    }

    int tileSize() {
        return tileSize;
    }

    int columns() {
        return columns;
    }

    int rows() {
        return rows;
    }

    float isoTileWidth() {
        return isoTileWidth;
    }

    float isoTileHeight() {
        return isoTileHeight;
    }

    int pixelWidth() {
        return pixelWidth;
    }

    int pixelHeight() {
        return pixelHeight;
    }

    void render() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                drawTile(col, row, tiles[row][col]);
            }
        }
    }

    float cellCenterX(int col, int row) {
        return originX + (col - row) * halfIsoWidth;
    }

    float cellCenterY(int col, int row) {
        return originY + (col + row) * halfIsoHeight;
    }

    float cellGroundY(int col, int row) {
        return cellCenterY(col, row) + halfIsoHeight;
    }

    private void drawTile(int col, int row, TileType tile) {
        float centerX = cellCenterX(col, row);
        float centerY = cellCenterY(col, row);
        if (tile == TileType.WALL) {
            drawRaisedBlock(centerX, centerY, tile);
        } else {
            drawFloorDiamond(centerX, centerY, tile);
        }
    }

    private void drawFloorDiamond(float centerX, float centerY, TileType tile) {
        float leftX = centerX - halfIsoWidth;
        float rightX = centerX + halfIsoWidth;
        float topY = centerY - halfIsoHeight;
        float bottomY = centerY + halfIsoHeight;
        float baseShade = 1.05f;
        glColor3f(clampColor(tile.r * baseShade), clampColor(tile.g * baseShade), clampColor(tile.b * baseShade));
        glBegin(GL_TRIANGLE_FAN);
        glVertex2f(centerX, topY);
        glVertex2f(rightX, centerY);
        glVertex2f(centerX, bottomY);
        glVertex2f(leftX, centerY);
        glEnd();

        glColor3f(tile.r * 0.65f, tile.g * 0.65f, tile.b * 0.65f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(centerX, topY);
        glVertex2f(rightX, centerY);
        glVertex2f(centerX, bottomY);
        glVertex2f(leftX, centerY);
        glEnd();

        if (tile == TileType.HAZARD) {
            drawHazardGlyph(centerX, centerY);
        } else if (tile == TileType.START) {
            drawStartMarker(centerX, centerY);
        } else if (tile == TileType.EXIT) {
            drawExitMarker(centerX, centerY);
        }
    }

    private void drawHazardGlyph(float centerX, float centerY) {
        float insetX = halfIsoWidth * 0.45f;
        float insetY = halfIsoHeight * 0.6f;
        glColor3f(1f, 0.95f, 0.95f);
        glBegin(GL_LINES);
        glVertex2f(centerX - insetX, centerY - insetY);
        glVertex2f(centerX + insetX, centerY + insetY);
        glVertex2f(centerX + insetX, centerY - insetY);
        glVertex2f(centerX - insetX, centerY + insetY);
        glEnd();
    }

    private void drawStartMarker(float centerX, float centerY) {
        float topY = centerY - halfIsoHeight * 0.5f;
        float bottomY = centerY + halfIsoHeight * 0.4f;
        float halfWidth = halfIsoWidth * 0.35f;
        glColor3f(0.95f, 1f, 1f);
        glBegin(GL_TRIANGLES);
        glVertex2f(centerX, topY);
        glVertex2f(centerX - halfWidth, bottomY);
        glVertex2f(centerX + halfWidth, bottomY);
        glEnd();
    }

    private void drawExitMarker(float centerX, float centerY) {
        float w = halfIsoWidth * 0.4f;
        float h = halfIsoHeight * 0.7f;
        glColor3f(0.98f, 1f, 0.9f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(centerX, centerY - h);
        glVertex2f(centerX + w, centerY);
        glVertex2f(centerX, centerY + h);
        glVertex2f(centerX - w, centerY);
        glEnd();
    }

    private void drawRaisedBlock(float centerX, float centerY, TileType tile) {
        float height = isoTileHeight * 2.2f;
        float leftX = centerX - halfIsoWidth;
        float rightX = centerX + halfIsoWidth;
        float topY = centerY - halfIsoHeight;
        float bottomY = centerY + halfIsoHeight;

        float roofCenterY = centerY - height;
        float roofTopY = roofCenterY - halfIsoHeight;
        float roofBottomY = roofCenterY + halfIsoHeight;

        glColor3f(0.05f, 0.05f, 0.08f);
        glBegin(GL_TRIANGLE_FAN);
        glVertex2f(centerX, topY);
        glVertex2f(rightX, centerY);
        glVertex2f(centerX, bottomY);
        glVertex2f(leftX, centerY);
        glEnd();

        // Left front face
        glColor3f(tile.r * 0.65f, tile.g * 0.65f, tile.b * 0.7f);
        glBegin(GL_QUADS);
        glVertex2f(centerX, bottomY);
        glVertex2f(leftX, centerY);
        glVertex2f(leftX, roofCenterY);
        glVertex2f(centerX, roofBottomY);
        glEnd();

        // Right front face
        glColor3f(tile.r * 0.75f, tile.g * 0.75f, tile.b * 0.8f);
        glBegin(GL_QUADS);
        glVertex2f(rightX, centerY);
        glVertex2f(centerX, bottomY);
        glVertex2f(centerX, roofBottomY);
        glVertex2f(rightX, roofCenterY);
        glEnd();

        // Roof
        glColor3f(clampColor(tile.r * 1.15f), clampColor(tile.g * 1.15f), clampColor(tile.b * 1.15f));
        glBegin(GL_TRIANGLE_FAN);
        glVertex2f(centerX, roofTopY);
        glVertex2f(rightX, roofCenterY);
        glVertex2f(centerX, roofBottomY);
        glVertex2f(leftX, roofCenterY);
        glEnd();

        glColor3f(0f, 0f, 0f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(centerX, roofTopY);
        glVertex2f(rightX, roofCenterY);
        glVertex2f(centerX, roofBottomY);
        glVertex2f(leftX, roofCenterY);
        glEnd();
    }

    private float clampColor(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
