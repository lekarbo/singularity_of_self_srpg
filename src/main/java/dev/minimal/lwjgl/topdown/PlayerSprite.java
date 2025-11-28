package dev.minimal.lwjgl.topdown;

import static org.lwjgl.opengl.GL11.GL_LINE_LOOP;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_FAN;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glVertex2f;

final class PlayerSprite {
    private PlayerSprite() {
    }

    static void drawIsoRunner(float footX, float footY, float height, float time, boolean moving) {
        float bob = moving ? (float) Math.sin(time * 12f) * height * 0.04f : 0f;
        float torsoTop = footY - height + bob;
        float torsoBottom = footY - height * 0.25f + bob;
        float shoulderWidth = height * 0.18f;
        float hipWidth = shoulderWidth * 1.25f;
        float headRadius = height * 0.16f;
        float legLength = height * 0.55f;
        float armLength = height * 0.4f;
        float step = moving ? (float) Math.sin(time * 9.5f) * height * 0.09f : 0f;

        drawShadow(footX, footY, height);

        // Back leg
        glColor3f(0.14f, 0.18f, 0.24f);
        drawSkewedLimb(footX - hipWidth * 0.35f - step, footY, height * 0.07f, legLength, -height * 0.08f);

        // Torso
        glColor3f(0.22f, 0.45f, 0.86f);
        glBegin(GL_QUADS);
        glVertex2f(footX - hipWidth, torsoBottom);
        glVertex2f(footX + hipWidth, torsoBottom);
        glVertex2f(footX + shoulderWidth, torsoTop);
        glVertex2f(footX - shoulderWidth, torsoTop);
        glEnd();

        // Chest stripe
        glColor3f(0.15f, 0.26f, 0.52f);
        glBegin(GL_QUADS);
        float stripeY = torsoTop + (torsoBottom - torsoTop) * 0.45f;
        glVertex2f(footX - hipWidth, stripeY - height * 0.02f);
        glVertex2f(footX + hipWidth, stripeY - height * 0.02f);
        glVertex2f(footX + hipWidth * 0.95f, stripeY + height * 0.02f);
        glVertex2f(footX - hipWidth * 0.95f, stripeY + height * 0.02f);
        glEnd();

        // Front leg
        glColor3f(0.18f, 0.22f, 0.3f);
        drawSkewedLimb(footX + hipWidth * 0.35f + step, footY, height * 0.075f, legLength, height * 0.06f);

        // Arms (swing opposite legs)
        float armSwing = moving ? -step * 0.8f : 0f;
        float armBaseY = torsoTop + (torsoBottom - torsoTop) * 0.2f;
        glColor3f(0.22f, 0.45f, 0.86f);
        drawSkewedLimb(footX - shoulderWidth * 1.2f, armBaseY, height * 0.05f, armLength, -height * 0.08f + armSwing);
        glColor3f(0.28f, 0.55f, 0.98f);
        drawSkewedLimb(footX + shoulderWidth * 1.1f, armBaseY, height * 0.05f, armLength, height * 0.08f - armSwing);

        // Head
        float headCenterY = torsoTop - headRadius * 0.6f;
        glColor3f(0.96f, 0.86f, 0.76f);
        glBegin(GL_TRIANGLE_FAN);
        glVertex2f(footX, headCenterY);
        int segments = 20;
        for (int i = 0; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            float x = footX + (float) Math.cos(angle) * headRadius;
            float y = headCenterY + (float) Math.sin(angle) * headRadius;
            glVertex2f(x, y);
        }
        glEnd();

        // Visor
        glColor3f(0.1f, 0.1f, 0.18f);
        float visorWidth = headRadius * 1.2f;
        float visorHeight = headRadius * 0.55f;
        glBegin(GL_QUADS);
        glVertex2f(footX - visorWidth * 0.5f, headCenterY - visorHeight * 0.3f);
        glVertex2f(footX + visorWidth * 0.5f, headCenterY - visorHeight * 0.3f);
        glVertex2f(footX + visorWidth * 0.55f, headCenterY + visorHeight * 0.4f);
        glVertex2f(footX - visorWidth * 0.55f, headCenterY + visorHeight * 0.4f);
        glEnd();

        glColor3f(0.88f, 0.92f, 1f);
        glBegin(GL_LINES);
        glVertex2f(footX - visorWidth * 0.35f, headCenterY - visorHeight * 0.05f);
        glVertex2f(footX - visorWidth * 0.05f, headCenterY + visorHeight * 0.25f);
        glEnd();

        glColor3f(0f, 0f, 0f);
        glBegin(GL_LINE_LOOP);
        for (int i = 0; i < segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            float x = footX + (float) Math.cos(angle) * headRadius;
            float y = headCenterY + (float) Math.sin(angle) * headRadius;
            glVertex2f(x, y);
        }
        glEnd();
    }

    private static void drawShadow(float footX, float footY, float height) {
        float radiusX = height * 0.32f;
        float radiusY = height * 0.14f;
        float baseY = footY - height * 0.04f;
        glColor3f(0f, 0f, 0f);
        glBegin(GL_TRIANGLE_FAN);
        glVertex2f(footX, baseY);
        int segments = 18;
        for (int i = 0; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            float x = footX + (float) Math.cos(angle) * radiusX;
            float y = baseY + (float) Math.sin(angle) * radiusY;
            glVertex2f(x, y);
        }
        glEnd();
    }

    private static void drawSkewedLimb(float footX, float footY, float width, float length, float lean) {
        float half = width / 2f;
        glBegin(GL_QUADS);
        glVertex2f(footX - half, footY);
        glVertex2f(footX + half, footY);
        glVertex2f(footX + half + lean, footY - length);
        glVertex2f(footX - half + lean, footY - length);
        glEnd();
    }
}
