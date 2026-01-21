package Entities;

import Main.Game;
import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * Projectile fired by enemies.
 */
public class Projectile {
    private Rectangle2D.Float hitBox;
    private float xSpeed;
    private int damage = 1;
    private boolean active = true;
    
    // Visual properties
    private static final int WIDTH = (int)(8 * Game.SCALE);
    private static final int HEIGHT = (int)(8 * Game.SCALE);
    
    public Projectile(float x, float y, float xSpeed) {
        this.xSpeed = xSpeed;
        this.hitBox = new Rectangle2D.Float(x, y, WIDTH, HEIGHT);
    }
    
    public void update() {
        hitBox.x += xSpeed;
        
        // Deactivate if off-screen
        if (hitBox.x < -100 || hitBox.x > Game.GAME_WIDTH + 100) {
            active = false;
        }
    }
    
    public void render(Graphics g, int cameraOffsetX) {
        if (!active) return;
        
        int drawX = (int)hitBox.x - cameraOffsetX;
        int drawY = (int)hitBox.y;
        
        // Draw simple projectile (red circle)
        g.setColor(new Color(255, 100, 100));
        g.fillOval(drawX, drawY, WIDTH, HEIGHT);
        
        // Add glow effect
        g.setColor(new Color(255, 150, 150, 100));
        g.fillOval(drawX - 2, drawY - 2, WIDTH + 4, HEIGHT + 4);
    }
    
    public Rectangle2D.Float getHitBox() {
        return hitBox;
    }
    
    public int getDamage() {
        return damage;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void deactivate() {
        active = false;
    }
}
