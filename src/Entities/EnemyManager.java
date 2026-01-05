package Entities;

import Main.Game;
import levels.Level;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EnemyManager {
    private final List<Enemy> enemies = new ArrayList<>();

    public EnemyManager() { }

    public void spawnForLevel(Level level) {
        enemies.clear();

        int[][] data = level.getLevelData();

        // Spawn enemies at a few x positions, placed on the ground using tile data
        int h = (int)(32 * Game.SCALE);
        int w = (int)(24 * Game.SCALE);

        int[] xTiles = {5, 12, 20}; // tile columns to place enemies at
        for (int xt : xTiles) {
            int yPixel = groundYPixel(data, xt);
            enemies.add(new Enemy(xt * Game.TILES_SIZE, yPixel - h, w, h, data));
        }
    }

    private int groundYPixel(int[][] data, int xTile) {
        // Find first solid tile from bottom up at xTile; place enemy on top of it.
        for (int y = Game.TILES_HEIGHT - 1; y >= 0; y--) {
            int idx = data[y][xTile];
            boolean solid = (idx != 11); // 11 is air in your Helpmethods
            if (solid) {
                return y * Game.TILES_SIZE;
            }
        }
        // If no ground found, default to a reasonable mid-height
        return (int)(Game.GAME_HEIGHT * 0.5f);
    }

    public void update() {
        for (Enemy e : enemies) e.update();
    }

    public void draw(Graphics g) {
        for (Enemy e : enemies) e.render(g);
    }
}