package dev.minimal.lwjgl.topdown;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_LINE_LOOP;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_FAN;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glVertex2f;

final class IsoTileHighlightRenderer {
    private IsoTileHighlightRenderer() {
    }

    static void draw(Level level, int col, int row, float fillR, float fillG, float fillB, float fillAlpha,
                     float outlineR, float outlineG, float outlineB, float outlineAlpha) {
        if (level == null || !level.isInside(col, row)) {
            return;
        }
        float centerX = level.cellCenterX(col, row);
        float centerY = level.cellCenterY(col, row);
        float halfWidth = level.isoTileWidth() / 2f;
        float halfHeight = level.isoTileHeight() / 2f;
        float leftX = centerX - halfWidth;
        float rightX = centerX + halfWidth;
        float topY = centerY - halfHeight;
        float bottomY = centerY + halfHeight;

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glColor4f(fillR, fillG, fillB, fillAlpha);
        glBegin(GL_TRIANGLE_FAN);
        glVertex2f(centerX, topY);
        glVertex2f(rightX, centerY);
        glVertex2f(centerX, bottomY);
        glVertex2f(leftX, centerY);
        glEnd();

        glColor4f(outlineR, outlineG, outlineB, outlineAlpha);
        glBegin(GL_LINE_LOOP);
        glVertex2f(centerX, topY);
        glVertex2f(rightX, centerY);
        glVertex2f(centerX, bottomY);
        glVertex2f(leftX, centerY);
        glEnd();

        glDisable(GL_BLEND);
    }
}
