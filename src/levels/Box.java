package levels;

import Main.Game;
import java.awt.*;

public class Box {
    private final int x;
    private final int y;
    private final int frameIndex;

    public static final int W = (int)(16 * Game.SCALE);
    public static final int H = (int)(16 * Game.SCALE);

    public Box(int x, int y, int frameIndex) {
        this.x = x;
        this.y = y;
        this.frameIndex = frameIndex;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, W, H);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getFrameIndex() { return frameIndex; }
}
