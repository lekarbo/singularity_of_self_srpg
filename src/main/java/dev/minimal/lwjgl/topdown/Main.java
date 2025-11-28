package dev.minimal.lwjgl.topdown;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        GameConfig config = GameConfig.fromArgs(args);
        new TopDownPlatformerGame(config).run();
    }
}
