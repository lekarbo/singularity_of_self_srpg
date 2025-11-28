package dev.minimal.lwjgl.topdown;

final class Player {
    private static final float MOVE_DURATION = 0.18f;

    private final float spriteHeight;
    private Level level;

    private int currentCol;
    private int currentRow;
    private int targetCol;
    private int targetRow;

    private float currentX;
    private float currentY;
    private float startX;
    private float startY;
    private float targetX;
    private float targetY;
    private float moveProgress;
    private boolean moving;
    private float animationTime;

    Player(Level level) {
        this.level = level;
        this.spriteHeight = level.isoTileWidth() * 0.85f;
        teleportTo(level.startPosition());
    }

    void reset(Level level) {
        this.level = level;
        teleportTo(level.startPosition());
    }

    private void teleportTo(GridPosition position) {
        this.currentCol = position.col();
        this.currentRow = position.row();
        this.targetCol = currentCol;
        this.targetRow = currentRow;
        this.moving = false;
        syncPositionToCell();
    }

    void snapTo(GridPosition position) {
        teleportTo(position);
    }

    void haltImmediately() {
        moving = false;
        syncPositionToCell();
    }

    private void syncPositionToCell() {
        currentX = footXFor(currentCol, currentRow);
        currentY = footYFor(currentCol, currentRow);
        startX = currentX;
        startY = currentY;
        targetX = currentX;
        targetY = currentY;
        moveProgress = 0f;
        animationTime = 0f;
    }

    boolean requestMove(Direction direction) {
        if (moving || level == null) {
            return false;
        }
        int nextCol = currentCol + direction.dx;
        int nextRow = currentRow + direction.dy;
        if (!level.isInside(nextCol, nextRow)) {
            return false;
        }
        if (!level.isWalkable(nextCol, nextRow)) {
            return false;
        }
        targetCol = nextCol;
        targetRow = nextRow;
        startX = currentX;
        startY = currentY;
        targetX = footXFor(targetCol, targetRow);
        targetY = footYFor(targetCol, targetRow);
        moveProgress = 0f;
        moving = true;
        return true;
    }

    boolean update(float deltaSeconds) {
        animationTime += deltaSeconds;
        if (!moving) {
            return false;
        }
        moveProgress += deltaSeconds / MOVE_DURATION;
        if (moveProgress >= 1f) {
            moveProgress = 1f;
            currentCol = targetCol;
            currentRow = targetRow;
            currentX = targetX;
            currentY = targetY;
            moving = false;
            return true;
        }
        float t = moveProgress;
        currentX = startX + (targetX - startX) * t;
        currentY = startY + (targetY - startY) * t;
        return false;
    }

    void render() {
        PlayerSprite.drawIsoRunner(currentX, currentY, spriteHeight, animationTime, moving);
    }

    int col() {
        return currentCol;
    }

    int row() {
        return currentRow;
    }

    boolean isMoving() {
        return moving;
    }

    private float footXFor(int col, int row) {
        return level.cellCenterX(col, row);
    }

    private float footYFor(int col, int row) {
        return level.cellGroundY(col, row);
    }
}
