package levels;

import Main.Game;
import java.awt.*;

public class Potion {
    private final int x;
    private final int y;

    // Render size; adjust if your potion frames differ in native size
    public static final int W = (int)(16 * Game.SCALE);
    public static final int H = (int)(16 * Game.SCALE);

    public Potion(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, W, H);
    }

    public int getX() { return x; }
    public int getY() { return y; }
}