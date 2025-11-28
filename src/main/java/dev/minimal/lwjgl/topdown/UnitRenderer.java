package dev.minimal.lwjgl.topdown;

import static org.lwjgl.opengl.GL11.GL_LINE_LOOP;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_FAN;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glVertex2f;

final class UnitRenderer {
    private UnitRenderer() {
    }

    static void draw(Level level, Unit unit) {
        if (level == null || unit == null || !unit.isPlaced()) {
            return;
        }
        GridPosition pos = unit.position();
        float baseX = level.cellCenterX(pos.col(), pos.row());
        float baseY = level.cellGroundY(pos.col(), pos.row());
        float spriteHeight = level.isoTileWidth() * 0.65f;
        float radius = spriteHeight * 0.35f;
        float shade = unit.faction() == UnitFaction.PLAYER ? 0.3f : 0.6f;
        float r = unit.faction() == UnitFaction.PLAYER ? 0.25f : 0.78f;
        float g = unit.faction() == UnitFaction.PLAYER ? 0.65f : 0.32f;
        float b = unit.faction() == UnitFaction.PLAYER ? 0.92f : 0.35f;

        // Shadow
        glColor3f(0f, 0f, 0f);
        glBegin(GL_TRIANGLE_FAN);
        glVertex2f(baseX, baseY - spriteHeight * 0.02f);
        int segments = 18;
        for (int i = 0; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            float x = baseX + (float) Math.cos(angle) * radius * 0.9f;
            float y = baseY - spriteHeight * 0.05f + (float) Math.sin(angle) * radius * 0.35f;
            glVertex2f(x, y);
        }
        glEnd();

        // Body column
        glColor3f(r, g, b);
        glBegin(GL_QUADS);
        glVertex2f(baseX - radius * 0.5f, baseY);
        glVertex2f(baseX + radius * 0.5f, baseY);
        glVertex2f(baseX + radius * 0.4f, baseY - spriteHeight);
        glVertex2f(baseX - radius * 0.4f, baseY - spriteHeight);
        glEnd();

        // Accent stripe
        glColor3f(r + shade, g + shade * 0.3f, b + shade * 0.2f);
        glBegin(GL_QUADS);
        float stripeTop = baseY - spriteHeight * 0.6f;
        float stripeBottom = stripeTop + spriteHeight * 0.12f;
        glVertex2f(baseX - radius * 0.45f, stripeTop);
        glVertex2f(baseX + radius * 0.45f, stripeTop);
        glVertex2f(baseX + radius * 0.35f, stripeBottom);
        glVertex2f(baseX - radius * 0.35f, stripeBottom);
        glEnd();

        // Head disk
        glColor3f(r + shade * 0.5f, g + shade * 0.4f, b + shade * 0.3f);
        float headCenterY = baseY - spriteHeight * 1.05f;
        float headRadius = radius * 0.65f;
        glBegin(GL_TRIANGLE_FAN);
        glVertex2f(baseX, headCenterY);
        for (int i = 0; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            float x = baseX + (float) Math.cos(angle) * headRadius;
            float y = headCenterY + (float) Math.sin(angle) * headRadius;
            glVertex2f(x, y);
        }
        glEnd();

        glColor3f(0f, 0f, 0f);
        glBegin(GL_LINE_LOOP);
        for (int i = 0; i < segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            float x = baseX + (float) Math.cos(angle) * headRadius;
            float y = headCenterY + (float) Math.sin(angle) * headRadius;
            glVertex2f(x, y);
        }
        glEnd();
    }
}
