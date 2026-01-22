package Entities;

import Main.Game;
import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * Projectile fired by the boss that can move in any direction.
 * Deals 1 heart of damage to the player.
 */
public class BossProjectile extends Projectile {
    private float xSpeed;
    private float ySpeed;
    private Rectangle2D.Float hitBox;
    private int damage = 1;
    private boolean active = true;

    // Visual properties - larger than regular projectiles
    private static final int WIDTH = (int)(12 * Game.SCALE);
    private static final int HEIGHT = (int)(12 * Game.SCALE);

    // Animation
    private int animTick = 0;
    private float pulseScale = 1.0f;

    public BossProjectile(float x, float y, float xSpeed, float ySpeed) {
        super(x, y, xSpeed); // Call parent constructor
        this.xSpeed = xSpeed;
        this.ySpeed = ySpeed;
        this.hitBox = new Rectangle2D.Float(x - WIDTH/2, y - HEIGHT/2, WIDTH, HEIGHT);
    }

    @Override
    public void update() {
        hitBox.x += xSpeed;
        hitBox.y += ySpeed;

        // Animate pulse effect
        animTick++;
        pulseScale = 1.0f + 0.2f * (float)Math.sin(animTick * 0.2);

        // Deactivate if too far off-screen
        int levelWidth = Game.GAME_WIDTH * 3; // Allow some buffer for scrolling levels
        if (hitBox.x < -200 || hitBox.x > levelWidth + 200 ||
            hitBox.y < -200 || hitBox.y > Game.GAME_HEIGHT + 200) {
            active = false;
        }
    }

    @Override
    public void render(Graphics g, int cameraOffsetX) {
        if (!active) return;

        int drawX = (int)hitBox.x - cameraOffsetX;
        int drawY = (int)hitBox.y;

        int scaledW = (int)(WIDTH * pulseScale);
        int scaledH = (int)(HEIGHT * pulseScale);
        int offsetX = (scaledW - WIDTH) / 2;
        int offsetY = (scaledH - HEIGHT) / 2;

        // Outer glow (purple)
        g.setColor(new Color(255, 120, 0, 90));
        g.fillOval(drawX - offsetX - 4, drawY - offsetY - 4, scaledW + 8, scaledH + 8);

        // Main projectile (dark purple core)
        g.setColor(new Color(200, 50, 0));
        g.fillOval(drawX - offsetX, drawY - offsetY, scaledW, scaledH);

        // Inner bright core
        g.setColor(new Color(255, 230, 120));
        int innerSize = scaledW / 2;
        g.fillOval(drawX - offsetX + innerSize/2, drawY - offsetY + innerSize/2, innerSize, innerSize);
    }

    @Override
    public Rectangle2D.Float getHitBox() {
        return hitBox;
    }

    @Override
    public int getDamage() {
        return damage;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void deactivate() {
        active = false;
    }
}
