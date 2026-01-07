package Entities;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class EnemyManager {
    private final List<Enemy> enemies = new ArrayList<>();

    public EnemyManager() { }

    public void spawnForLevel(levels.Level level) {
        enemies.clear();

        int[][] data = level.getLevelData();

        int h = (int)(32 * Main.Game.SCALE);
        int w = (int)(24 * Main.Game.SCALE);

        int[] xTiles = {5, 12, 20};
        for (int xt : xTiles) {
            int yPixel = groundYPixel(data, xt);
            enemies.add(new Enemy(xt * Main.Game.TILES_SIZE, yPixel - h, w, h, data));
        }
    }

    private int groundYPixel(int[][] data, int xTile) {
        for (int y = Main.Game.TILES_HEIGHT - 1; y >= 0; y--) {
            int idx = data[y][xTile];
            boolean solid = (idx != 11);
            if (solid) {
                return y * Main.Game.TILES_SIZE;
            }
        }
        return (int)(Main.Game.GAME_HEIGHT * 0.5f);
    }

    public void update() {
        for (Enemy e : enemies) e.update();
    }

    public void draw(Graphics g) {
        for (Enemy e : enemies) e.render(g);
    }

    // NEW: simple contact check to damage player
    public boolean collidesWithPlayer(Rectangle2D.Float playerHB) {
        Rectangle playerRect = new Rectangle((int)playerHB.x, (int)playerHB.y, (int)playerHB.width, (int)playerHB.height);
        for (Enemy e : enemies) {
            Rectangle2D.Float hb = e.getHitBox();
            Rectangle enemyRect = new Rectangle((int)hb.x, (int)hb.y, (int)hb.width, (int)hb.height);
            if (playerRect.intersects(enemyRect)) return true;
        }
        return false;
    }
}