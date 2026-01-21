package Entities;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
public class EnemyManager {
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    
    // Projectile shooting cooldown
    private long lastProjectileTime = 0;
    private final long projectileCooldown = 2000; // 2 seconds between shots

    public EnemyManager() { }

    public void spawnForLevel(levels.Level level) {
        enemies.clear();

        int[][] data = level.getLevelData();
        int levelWidth = level.getLevelWidth();

        // Match player's drawn size so enemies visually match player
        int h = (int)(46.25f * Main.Game.SCALE);
        int w = (int)(62.5f * Main.Game.SCALE);

        // Determine spawn positions based on level width and difficulty
        // Ordered by level progression for clarity
        int[] xTiles;
        
        if (levelWidth <= 50) {
            // Level 1 (50 tiles): 3 enemies, spread out for tutorial
            xTiles = new int[]{8, 24, 40};
        } else if (levelWidth <= 55) {
            // Level 3 (55 tiles): 4 enemies, strategic staircase positions
            xTiles = new int[]{6, 18, 30, 46};
        } else if (levelWidth <= 60) {
            // Level 2 (60 tiles): 5 enemies, platform challenge
            xTiles = new int[]{10, 20, 32, 44, 55};
        } else if (levelWidth <= 65) {
            // Level 4 (65 tiles): 6 enemies, gauntlet challenge
            xTiles = new int[]{7, 16, 28, 38, 48, 60};
        } else {
            // Level 5 (70 tiles): 7 enemies, final challenge with more combat
            xTiles = new int[]{8, 20, 28, 38, 48, 58, 66};
        }
        
        for (int i = 0; i < xTiles.length; i++) {
            int xt = xTiles[i];
            if (xt >= levelWidth) continue; // Safety check
            
            int yPixel = groundYPixel(data, xt);
            int variant = i % 2; // alternate between enemy1 and enemy2
            enemies.add(new Enemy(xt * Main.Game.TILES_SIZE, yPixel - h, w, h, variant, data));
        }
    }

    private int groundYPixel(int[][] data, int xTile) {
        // Start from bottom and find first solid tile
        for (int y = Main.Game.TILES_HEIGHT - 1; y >= 0; y--) {
            if (xTile < 0 || xTile >= data[0].length) continue;
            int idx = data[y][xTile];
            boolean solid = (idx != 11);
            if (solid) {
                // Found solid ground - check if there's space above for enemy
                // Need at least 2 tiles of air above for enemy to spawn
                int tilesAbove = 0;
                for (int checkY = y - 1; checkY >= 0 && checkY > y - 3; checkY--) {
                    if (data[checkY][xTile] == 11) {
                        tilesAbove++;
                    } else {
                        break;
                    }
                }
                
                // If enough space above, this is a good spawn point
                if (tilesAbove >= 2) {
                    return y * Main.Game.TILES_SIZE;
                }
            }
        }
        // Fallback to middle of screen if no good position found
        return (int)(Main.Game.GAME_HEIGHT * 0.5f);
    }

    public void update() {
        for (Enemy e : enemies) e.update();
        
        // Remove dead enemies
        enemies.removeIf(Enemy::isDead);
        
        // Shoot projectiles periodically from random enemies
        long now = System.currentTimeMillis();
        if (now - lastProjectileTime > projectileCooldown && !enemies.isEmpty()) {
            // Pick a random enemy to shoot
            int randomIndex = (int)(Math.random() * enemies.size());
            Enemy shooter = enemies.get(randomIndex);
            Rectangle2D.Float shooterHB = shooter.getHitBox();
            
            // Create projectile moving away from enemy
            float projectileX = shooterHB.x + shooterHB.width / 2;
            float projectileY = shooterHB.y + shooterHB.height / 2;
            float projectileSpeed = 2.0f * Main.Game.SCALE;
            
            // Shoot in a direction (could be toward player, but for simplicity shoot right for now)
            projectiles.add(new Projectile(projectileX, projectileY, projectileSpeed));
            lastProjectileTime = now;
        }
        
        // Update projectiles
        for (Projectile p : projectiles) p.update();
        
        // Remove inactive projectiles
        projectiles.removeIf(p -> !p.isActive());
    }

    public void draw(Graphics g, int cameraOffsetX) {
        for (Enemy e : enemies) e.render(g, cameraOffsetX);
        for (Projectile p : projectiles) p.render(g, cameraOffsetX);
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
        
        // Direct float-based collision detection without creating Rectangle objects
        for (Enemy e : enemies) {
            Rectangle2D.Float hb = e.getHitBox();
            // Check if rectangles intersect
            if (attackHitbox.intersects(hb)) {
                e.takeDamage(1);
            }
        }
    }
    
    /**
     * Check if any projectiles hit the player.
     * Returns damage dealt (1 per projectile hit).
     */
    public int checkProjectilePlayerCollision(Rectangle2D.Float playerHB) {
        int damage = 0;
        for (Projectile p : projectiles) {
            if (p.isActive() && p.getHitBox().intersects(playerHB)) {
                damage += p.getDamage();
                p.deactivate();
            }
        }
        return damage;
    }
}