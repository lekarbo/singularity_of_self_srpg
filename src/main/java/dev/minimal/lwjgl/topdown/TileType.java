package dev.minimal.lwjgl.topdown;

enum TileType {
    FLOOR('.', 0.18f, 0.18f, 0.2f, false),
    WALL('#', 0.09f, 0.09f, 0.1f, true),
    START('S', 0.24f, 0.32f, 0.54f, false),
    EXIT('G', 0.36f, 0.75f, 0.34f, false),
    HAZARD('X', 0.83f, 0.27f, 0.27f, false),
    PLAYER_SPAWN('P', 0.38f, 0.58f, 0.82f, false),
    ENEMY_SPAWN('E', 0.72f, 0.32f, 0.32f, false);

    final char symbol;
    final float r;
    final float g;
    final float b;
    final boolean blocksMovement;

    TileType(char symbol, float r, float g, float b, boolean blocksMovement) {
        this.symbol = symbol;
        this.r = r;
        this.g = g;
        this.b = b;
        this.blocksMovement = blocksMovement;
    }

    static TileType fromSymbol(char symbol) {
        for (TileType type : values()) {
            if (type.symbol == symbol) {
                return type;
            }
        }
        return FLOOR;
    }
}
