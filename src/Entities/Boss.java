package Entities;

import Main.Game;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import static util.Helpmethods.*;
import java.awt.image.BufferedImage;
import util.LoadSave;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.util.ArrayList;
import java.util.List;

/**
 * Boss enemy with 5 hearts, faster movement, and projectile attacks.
 * Deals 1 heart of damage to the player on contact.
 */
public class Boss extends Entity {
    private int[][] levelData;

    // Movement - faster than normal enemies
    private float xSpeed;
    private float baseSpeed = 1.2f * Game.SCALE; // Faster than normal enemies (0.5f)
    
    // Health system - 5 hearts
    private int health = 5;
    private int maxHealth = 5;
    
    // Death animation
    private boolean dying = false;
    private int deathAnimationTick = 0;
    private final int deathAnimationDuration = 60; // Longer death animation for boss
    private float deathFadeAlpha = 1.0f;
    
    // Attack system - projectiles
    private List<Projectile> projectiles = new ArrayList<>();
    private long lastProjectileTime = 0;
    private final long projectileCooldown = 1500; // 1.5 seconds between shots (faster than regular enemies)
    private float projectileSpeed = 3.0f * Game.SCALE;
    
    // Visual size for boss (larger than regular enemies)
    private static final int VISUAL_W = (int) (80f * Game.SCALE);
    private static final int VISUAL_H = (int) (60f * Game.SCALE);
    
    // Animation
    private int animIndex = 0;
    private int animTick = 0;
    private final int animSpeed = 6; // Faster animation
    
    // Player reference for targeting
    private Rectangle2D.Float playerHitBox;

    public Boss(float x, float y, int w, int h, int[][] levelData) {
        super(x, y, w, h);
        this.levelData = levelData;
        // Boss has a larger hitbox
        initHitBox(x, y, w - (int)(10*Game.SCALE), h - (int)(10*Game.SCALE));
        
        // Start moving right by default
        this.xSpeed = baseSpeed;
    }
    
    public void setPlayerHitBox(Rectangle2D.Float playerHB) {
        this.playerHitBox = playerHB;
    }

    public void update() {
        // If dying, only update death animation
        if (dying) {
            deathAnimationTick++;
            deathFadeAlpha = 1.0f - ((float)deathAnimationTick / deathAnimationDuration);
            return;
        }
        
        // Animate
        animTick++;
        if (animTick >= animSpeed) {
            animTick = 0;
            animIndex++;
            if (animIndex >= 4) animIndex = 0; // 4 frame animation cycle
        }

        // AI: Move toward player if we have a reference
        if (playerHitBox != null) {
            float playerCenterX = playerHitBox.x + playerHitBox.width / 2;
            float bossCenterX = hitBox.x + hitBox.width / 2;
            
            if (playerCenterX < bossCenterX - 20) {
                xSpeed = -baseSpeed;
            } else if (playerCenterX > bossCenterX + 20) {
                xSpeed = baseSpeed;
            }
        }

        // Use xSpeed for horizontal movement
        if (CanMoveHere(hitBox.x + xSpeed, hitBox.y, hitBox.width, hitBox.height, levelData)) {
            hitBox.x += xSpeed;
        } else {
            // Reverse direction if hitting wall
            xSpeed = -xSpeed;
        }

        // Edge ahead check
        float probeX = xSpeed > 0 ? (hitBox.x + hitBox.width + 1) : (hitBox.x - 1);
        Rectangle2D.Float probeHB = new Rectangle2D.Float(probeX, hitBox.y, 1, hitBox.height);

        // Turn around at edges
        if (!IsOnFloor(probeHB, levelData)) {
            xSpeed = -xSpeed;
        }

        // Gravity
        if (CanMoveHere(hitBox.x, hitBox.y + 1, hitBox.width, hitBox.height, levelData)) {
            hitBox.y += 1;
        }
        
        // Shoot projectiles at player
        shootAtPlayer();
        
        // Update projectiles
        for (Projectile p : projectiles) {
            p.update();
        }
        projectiles.removeIf(p -> !p.isActive());
    }
    
    private void shootAtPlayer() {
        if (playerHitBox == null || dying) return;
        
        long now = System.currentTimeMillis();
        if (now - lastProjectileTime > projectileCooldown) {
            // Calculate direction to player
            float playerCenterX = playerHitBox.x + playerHitBox.width / 2;
            float playerCenterY = playerHitBox.y + playerHitBox.height / 2;
            float bossCenterX = hitBox.x + hitBox.width / 2;
            float bossCenterY = hitBox.y + hitBox.height / 2;
            
            float dx = playerCenterX - bossCenterX;
            float dy = playerCenterY - bossCenterY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            
            if (distance > 0) {
                // Normalize and apply speed
                float dirX = (dx / distance) * projectileSpeed;
                float dirY = (dy / distance) * projectileSpeed;
                
                // Create projectile
                projectiles.add(new BossProjectile(bossCenterX, bossCenterY, dirX, dirY));
                lastProjectileTime = now;
                
                // Play attack sound
                util.SoundManager.play(util.SoundManager.SoundEffect.BOSS_ATTACK);
            }
        }
    }

    public void render(Graphics g, int cameraOffsetX) {
        // Draw boss body
        int drawX = (int) hitBox.x - cameraOffsetX;
        int drawY = (int) hitBox.y;
        int drawW = (int) hitBox.width;
        int drawH = (int) hitBox.height;
        
        // Apply death animation effects
        Graphics2D g2d = (Graphics2D) g;
        Composite originalComposite = g2d.getComposite();
        
        if (dying) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, deathFadeAlpha)));
        }
        
        // Draw boss body (dark purple color to look menacing)
        Color bossColor = new Color(80, 20, 100);
        if (xSpeed < 0) {
            // Facing left - slightly different shade
            bossColor = new Color(90, 25, 110);
        }
        
        // Body
        g.setColor(bossColor);
        g.fillRect(drawX, drawY, drawW, drawH);
        
        // Boss crown/horns (to distinguish from regular enemies)
        g.setColor(new Color(200, 150, 50)); // Gold color
        int crownHeight = (int)(10 * Game.SCALE);
        int crownWidth = (int)(15 * Game.SCALE);
        // Left horn
        int[] xPointsL = {drawX + drawW/4 - crownWidth/2, drawX + drawW/4, drawX + drawW/4 + crownWidth/2};
        int[] yPointsL = {drawY, drawY - crownHeight, drawY};
        g.fillPolygon(xPointsL, yPointsL, 3);
        // Right horn
        int[] xPointsR = {drawX + 3*drawW/4 - crownWidth/2, drawX + 3*drawW/4, drawX + 3*drawW/4 + crownWidth/2};
        int[] yPointsR = {drawY, drawY - crownHeight, drawY};
        g.fillPolygon(xPointsR, yPointsR, 3);
        
        // Eyes (glowing red)
        g.setColor(Color.RED);
        int eyeSize = (int)(8 * Game.SCALE);
        int eyeY = drawY + drawH / 3;
        g.fillOval(drawX + drawW/3 - eyeSize/2, eyeY, eyeSize, eyeSize);
        g.fillOval(drawX + 2*drawW/3 - eyeSize/2, eyeY, eyeSize, eyeSize);
        
        // Eye glow
        g.setColor(new Color(255, 100, 100, 100));
        g.fillOval(drawX + drawW/3 - eyeSize, eyeY - eyeSize/2, eyeSize * 2, eyeSize * 2);
        g.fillOval(drawX + 2*drawW/3 - eyeSize, eyeY - eyeSize/2, eyeSize * 2, eyeSize * 2);
        
        // Health bar above boss
        if (!dying) {
            drawHealthBar(g, drawX, drawY - (int)(25 * Game.SCALE), drawW);
        }
        
        // Boss label
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, (int)(14 * Game.SCALE)));
        FontMetrics fm = g.getFontMetrics();
        String label = "BOSS";
        int labelX = drawX + (drawW - fm.stringWidth(label)) / 2;
        int labelY = drawY + drawH / 2 + fm.getAscent() / 2;
        g.drawString(label, labelX, labelY);
        
        if (dying) {
            g2d.setComposite(originalComposite);
        }
        
        // Draw projectiles
        for (Projectile p : projectiles) {
            p.render(g, cameraOffsetX);
        }
    }
    
    private void drawHealthBar(Graphics g, int x, int y, int width) {
        int barHeight = (int)(8 * Game.SCALE);
        
        // Background (dark)
        g.setColor(new Color(50, 50, 50));
        g.fillRect(x, y, width, barHeight);
        
        // Health (red gradient)
        float healthPercent = (float) health / maxHealth;
        int healthWidth = (int)(width * healthPercent);
        
        // Color based on health
        Color healthColor;
        if (healthPercent > 0.6f) {
            healthColor = new Color(0, 200, 0);
        } else if (healthPercent > 0.3f) {
            healthColor = new Color(200, 200, 0);
        } else {
            healthColor = new Color(200, 0, 0);
        }
        
        g.setColor(healthColor);
        g.fillRect(x, y, healthWidth, barHeight);
        
        // Border
        g.setColor(Color.WHITE);
        g.drawRect(x, y, width, barHeight);
        
        // Heart icons for health
        int heartSize = (int)(10 * Game.SCALE);
        int heartY = y - heartSize - 2;
        for (int i = 0; i < maxHealth; i++) {
            int heartX = x + i * (heartSize + 2);
            if (i < health) {
                g.setColor(Color.RED);
            } else {
                g.setColor(Color.DARK_GRAY);
            }
            // Simple heart shape
            g.fillOval(heartX, heartY, heartSize, heartSize);
        }
    }

    // Health API
    public void takeDamage(int amount) {
        if (amount <= 0 || dying) return;
        health = Math.max(0, health - amount);
        
        // Play damage sound
        util.SoundManager.play(util.SoundManager.SoundEffect.BOSS_DAMAGE);
        
        // Start death animation if boss dies
        if (health <= 0) {
            dying = true;
            util.SoundManager.play(util.SoundManager.SoundEffect.BOSS_DEATH);
        }
    }
    
    public boolean isDead() {
        return dying && deathAnimationTick >= deathAnimationDuration;
    }
    
    public boolean isDying() {
        return dying;
    }
    
    public int getHealth() {
        return health;
    }
    
    public int getMaxHealth() {
        return maxHealth;
    }
    
    public List<Projectile> getProjectiles() {
        return projectiles;
    }
    
    /**
     * Check if any of the boss's projectiles hit the player.
     * Returns damage dealt.
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
    
    /**
     * Check if boss body collides with player.
     */
    public boolean collidesWithPlayer(Rectangle2D.Float playerHB) {
        if (dying) return false;
        return hitBox.intersects(playerHB);
    }
}
