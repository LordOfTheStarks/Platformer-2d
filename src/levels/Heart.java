package levels;

import Main.Game;
import java.awt.*;

/**
 * Heart pickup that restores player health.
 */
public class Heart {
    private final int x;
    private final int y;

    // Render size (consistent with UI scale)
    public static final int W = (int)(16 * Game.SCALE);
    public static final int H = (int)(16 * Game.SCALE);

    public Heart(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, W, H);
    }

    public int getX() { return x; }
    public int getY() { return y; }
}
