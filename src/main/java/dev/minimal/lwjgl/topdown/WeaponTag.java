package dev.minimal.lwjgl.topdown;

enum WeaponTag {
    ROCK('R'),
    PAPER('P'),
    SCISSORS('S');

    private final char symbol;

    WeaponTag(char symbol) {
        this.symbol = symbol;
    }

    char symbol() {
        return symbol;
    }
}
