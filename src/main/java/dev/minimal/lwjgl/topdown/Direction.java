package dev.minimal.lwjgl.topdown;

import org.lwjgl.glfw.GLFW;

enum Direction {
    UP(0, -1, GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP),
    DOWN(0, 1, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN),
    LEFT(-1, 0, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT),
    RIGHT(1, 0, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT);

    final int dx;
    final int dy;
    private final int[] keyCodes;

    Direction(int dx, int dy, int... keyCodes) {
        this.dx = dx;
        this.dy = dy;
        this.keyCodes = keyCodes;
    }

    static Direction fromKey(int key) {
        for (Direction direction : values()) {
            for (int code : direction.keyCodes) {
                if (code == key) {
                    return direction;
                }
            }
        }
        return null;
    }
}
