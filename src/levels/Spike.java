package levels;

import Main.Game;

import java.awt.*;

public class Spike {
    // Store pixel-space coords for simpler drawing/collision
    private final int x;
    private final int y;
    private final int type; // 0..3

    // Render size (scaled from 16x16)
    public static final int W = (int)(16 * Game.SCALE);
    public static final int H = (int)(16 * Game.SCALE);

    public Spike(int x, int y, int type) {
        this.x = x;
        this.y = y; // y is top-left where bottom of spike sits on ground top
        this.type = type;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, W, H);
    }

    public int getType() {
        return type;
    }

    public int getX() { return x; }
    public int getY() { return y; }
}