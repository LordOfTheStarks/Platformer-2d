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

        // Match player's drawn size so enemies visually match player
        int h = (int)(46.25f * Main.Game.SCALE);
        int w = (int)(62.5f * Main.Game.SCALE);

        int[] xTiles = {5, 12, 20};
        for (int i = 0; i < xTiles.length; i++) {
            int xt = xTiles[i];
            int yPixel = groundYPixel(data, xt);
            int variant = i % 2; // alternate between enemy1 and enemy2
            enemies.add(new Enemy(xt * Main.Game.TILES_SIZE, yPixel - h, w, h, variant, data));
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
        
        // Remove dead enemies
        enemies.removeIf(Enemy::isDead);
    }

    public void draw(Graphics g, int cameraOffsetX) {
        for (Enemy e : enemies) e.render(g, cameraOffsetX);
    }

    // simple contact check to damage player
    public boolean collidesWithPlayer(Rectangle2D.Float playerHB) {
        Rectangle playerRect = new Rectangle((int)playerHB.x, (int)playerHB.y, (int)playerHB.width, (int)playerHB.height);
        for (Enemy e : enemies) {
            Rectangle2D.Float hb = e.getHitBox();
            Rectangle enemyRect = new Rectangle((int)hb.x, (int)hb.y, (int)hb.width, (int)hb.height);
            if (playerRect.intersects(enemyRect)) return true;
        }
        return false;
    }
    
    // Check if player's attack hitbox hits any enemy
    public void checkPlayerAttackCollision(Rectangle2D.Float attackHitbox) {
        if (attackHitbox == null) return;
        
        Rectangle attackRect = new Rectangle((int)attackHitbox.x, (int)attackHitbox.y, 
                                            (int)attackHitbox.width, (int)attackHitbox.height);
        for (Enemy e : enemies) {
            Rectangle2D.Float hb = e.getHitBox();
            Rectangle enemyRect = new Rectangle((int)hb.x, (int)hb.y, (int)hb.width, (int)hb.height);
            if (attackRect.intersects(enemyRect)) {
                e.takeDamage(1);
            }
        }
    }
}