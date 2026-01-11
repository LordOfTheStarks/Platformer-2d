package levels;

import Main.Game;
import java.awt.*;

public class Coin {
    private final int x;
    private final int y;

    // Render size (source sheet frames are 20x20). Keep consistent with UI scale.
    public static final int W = (int)(20 * Game.SCALE);
    public static final int H = (int)(20 * Game.SCALE);

    public Coin(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, W, H);
    }

    public int getX() { return x; }
    public int getY() { return y; }
}