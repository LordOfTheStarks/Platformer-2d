package levels;

import Main.Game;

import java.awt.*;

public class Spike {
    // Store pixel-space coords for simpler drawing/collision
    private final int x;
    private final int y;
    private final int type; // 0..3

    // Render size — use a consistent size relative to tiles so all spikes share the same height.
    // This makes spikes look uniform even if the source sprite variants differ a little.
    public static final int SIZE = (int)(Game.TILES_SIZE * 0.5f); // half a tile high
    public static final int W = SIZE;
    public static final int H = SIZE;

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