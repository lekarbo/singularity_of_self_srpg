package dev.minimal.lwjgl.topdown;

final class CursorController {
    private Level level;
    private int col;
    private int row;

    CursorController(Level level) {
        attach(level);
    }

    void attach(Level level) {
        this.level = level;
        if (level != null) {
            GridPosition start = level.startPosition();
            this.col = start.col();
            this.row = start.row();
        } else {
            this.col = 0;
            this.row = 0;
        }
    }

    void moveTo(GridPosition position) {
        if (level == null || position == null) {
            return;
        }
        if (!level.isInside(position.col(), position.row())) {
            return;
        }
        this.col = position.col();
        this.row = position.row();
    }

    boolean move(Direction direction) {
        if (level == null || direction == null) {
            return false;
        }
        int nextCol = col + direction.dx;
        int nextRow = row + direction.dy;
        if (!level.isInside(nextCol, nextRow)) {
            return false;
        }
        col = nextCol;
        row = nextRow;
        return true;
    }

    GridPosition gridPosition() {
        return new GridPosition(col, row);
    }

    void render() {
        if (level == null) {
            return;
        }
        IsoTileHighlightRenderer.draw(
            level,
            col,
            row,
            0.2f,
            0.6f,
            0.95f,
            0.35f,
            0.85f,
            0.95f,
            1f,
            0.9f
        );
    }
}
