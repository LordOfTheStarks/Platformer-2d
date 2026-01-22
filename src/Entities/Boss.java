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
 * Uses the undeadking.png sprite sheet for visuals.
 */
public class Boss extends Entity {
    private int[][] levelData;

    // Movement - faster than normal enemies (normal enemy speed is 0.5f)
    /** Speed multiplier for boss movement (faster than regular enemies) */
    private static final float BASE_SPEED_MULTIPLIER = 1.2f;
    private float xSpeed;
    private float baseSpeed = BASE_SPEED_MULTIPLIER * Game.SCALE;
    
    // Health system - 5 hearts
    private static final int BOSS_MAX_HEALTH = 5;
    private int health = BOSS_MAX_HEALTH;
    private int maxHealth = BOSS_MAX_HEALTH;
    
    // Damage cooldown to prevent one-shot
    private long lastDamageTakenMs = 0;
    private static final long DAMAGE_COOLDOWN_MS = 500; // 0.5 second invulnerability after hit
    
    // Death animation
    private static final int DEATH_ANIMATION_DURATION = 60; // Longer death animation for boss
    private boolean dying = false;
    private int deathAnimationTick = 0;
    private float deathFadeAlpha = 1.0f;
    
    // Attack system - projectiles
    /** Cooldown between projectile shots (milliseconds) */
    private static final long PROJECTILE_COOLDOWN_MS = 1500L;
    private List<Projectile> projectiles = new ArrayList<>();
    private long lastProjectileTime = 0;
    private float projectileSpeed = 3.0f * Game.SCALE;
    
    // Visual size for boss (larger than regular enemies)
    private static final int VISUAL_W = (int) (80f * Game.SCALE);
    private static final int VISUAL_H = (int) (80f * Game.SCALE);
    
    // Sprite sheet constants (undeadking.png is 96x256)
    // Using first frame only (96x64) for single character display
    private static final int SPRITE_WIDTH = 96;
    private static final int SPRITE_HEIGHT = 64;
    
    // Sprite
    private BufferedImage spriteSheet;
    private BufferedImage currentFrame;  // Single frame at a time
    private BufferedImage currentFrameMirrored;
    private boolean facingLeft = false;
    
    // Jump system for boss
    private float airSpeed = 0;
    private float gravity = 0.04f * Game.SCALE;
    private float jumpSpeed = -3.0f * Game.SCALE;
    private boolean inAir = false;
    private long lastJumpTime = 0;
    private static final long JUMP_COOLDOWN_MS = 2000; // Jump every 2 seconds
    
    // Player reference for targeting
    private Rectangle2D.Float playerHitBox;

    public Boss(float x, float y, int w, int h, int[][] levelData) {
        super(x, y, w, h);
        this.levelData = levelData;
        // Boss has a larger hitbox
        initHitBox(x, y, w - (int)(10*Game.SCALE), h - (int)(10*Game.SCALE));
        
        // Start moving right by default
        this.xSpeed = baseSpeed;
        
        // Load sprite
        loadSprite();
    }
    
    private void loadSprite() {
        spriteSheet = LoadSave.getAtlas(LoadSave.UNDEAD_KING);
        if (spriteSheet != null) {
            // Get first frame only - display single character
            currentFrame = spriteSheet.getSubimage(0, 0, SPRITE_WIDTH, SPRITE_HEIGHT);
            currentFrameMirrored = flipImage(currentFrame);
            System.out.println("[Boss] Loaded undead king sprite successfully.");
        } else {
            System.out.println("[Boss] Could not load undead king sprite, using fallback.");
        }
    }
    
    private BufferedImage flipImage(BufferedImage image) {
        BufferedImage flipped = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        AffineTransform transform = new AffineTransform();
        transform.setToScale(-1, 1);
        transform.translate(-image.getWidth(), 0);
        AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        op.filter(image, flipped);
        return flipped;
    }
    
    public void setPlayerHitBox(Rectangle2D.Float playerHB) {
        this.playerHitBox = playerHB;
    }

    public void update() {
        // If dying, only update death animation
        if (dying) {
            deathAnimationTick++;
            deathFadeAlpha = 1.0f - ((float)deathAnimationTick / DEATH_ANIMATION_DURATION);
            return;
        }

        // AI: Move toward player if we have a reference
        if (playerHitBox != null) {
            float playerCenterX = playerHitBox.x + playerHitBox.width / 2;
            float bossCenterX = hitBox.x + hitBox.width / 2;
            
            if (playerCenterX < bossCenterX - 20) {
                xSpeed = -baseSpeed;
                facingLeft = true;
            } else if (playerCenterX > bossCenterX + 20) {
                xSpeed = baseSpeed;
                facingLeft = false;
            }
        }

        // Use xSpeed for horizontal movement
        if (CanMoveHere(hitBox.x + xSpeed, hitBox.y, hitBox.width, hitBox.height, levelData)) {
            hitBox.x += xSpeed;
        } else {
            // Reverse direction if hitting wall
            xSpeed = -xSpeed;
            facingLeft = !facingLeft;
        }

        // Edge ahead check
        float probeX = xSpeed > 0 ? (hitBox.x + hitBox.width + 1) : (hitBox.x - 1);
        Rectangle2D.Float probeHB = new Rectangle2D.Float(probeX, hitBox.y, 1, hitBox.height);

        // Turn around at edges
        if (!IsOnFloor(probeHB, levelData)) {
            xSpeed = -xSpeed;
            facingLeft = !facingLeft;
        }

        // Apply gravity
        if (inAir) {
            airSpeed += gravity;
            if (CanMoveHere(hitBox.x, hitBox.y + airSpeed, hitBox.width, hitBox.height, levelData)) {
                hitBox.y += airSpeed;
            } else {
                if (airSpeed > 0) {
                    // Landed
                    inAir = false;
                    airSpeed = 0;
                } else {
                    // Hit ceiling
                    airSpeed = 0;
                }
            }
        } else {
            // Check if still on floor
            if (CanMoveHere(hitBox.x, hitBox.y + 1, hitBox.width, hitBox.height, levelData)) {
                inAir = true;
            }
        }
        
        // Try to jump periodically
        tryJump();
        
        // Shoot projectiles at player
        shootAtPlayer();
        
        // Update projectiles
        for (Projectile p : projectiles) {
            p.update();
        }
        projectiles.removeIf(p -> !p.isActive());
    }
    
    /**
     * Try to jump if cooldown has passed.
     */
    private void tryJump() {
        if (inAir || dying) return;
        
        long now = System.currentTimeMillis();
        if (now - lastJumpTime > JUMP_COOLDOWN_MS) {
            // Jump!
            inAir = true;
            airSpeed = jumpSpeed;
            lastJumpTime = now;
        }
    }
    
    private void shootAtPlayer() {
        if (playerHitBox == null || dying) return;
        
        long now = System.currentTimeMillis();
        if (now - lastProjectileTime > PROJECTILE_COOLDOWN_MS) {
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
        // Calculate draw position - center sprite on hitbox
        int drawW = VISUAL_W;
        int drawH = VISUAL_H;
        int drawX = (int) hitBox.x - cameraOffsetX - (drawW - (int)hitBox.width) / 2;
        int drawY = (int) hitBox.y - (drawH - (int)hitBox.height);
        
        // Apply death animation effects
        Graphics2D g2d = (Graphics2D) g;
        Composite originalComposite = g2d.getComposite();
        
        if (dying) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, deathFadeAlpha)));
        }
        
        // Draw the boss sprite - single frame only
        if (currentFrame != null) {
            BufferedImage frameToDraw = facingLeft ? currentFrameMirrored : currentFrame;
            g.drawImage(frameToDraw, drawX, drawY, drawW, drawH, null);
        } else {
            // Fallback to colored rectangle if sprite not loaded
            drawFallbackBoss(g, drawX, drawY, drawW, drawH);
        }
        
        // Health bar above boss
        if (!dying) {
            drawHealthBar(g, drawX, drawY - (int)(25 * Game.SCALE), drawW);
        }
        
        if (dying) {
            g2d.setComposite(originalComposite);
        }
        
        // Draw projectiles
        for (Projectile p : projectiles) {
            p.render(g, cameraOffsetX);
        }
    }
    
    private void drawFallbackBoss(Graphics g, int drawX, int drawY, int drawW, int drawH) {
        // Draw boss body (dark purple color to look menacing)
        Color bossColor = new Color(80, 20, 100);
        if (facingLeft) {
            bossColor = new Color(90, 25, 110);
        }
        
        // Body
        g.setColor(bossColor);
        g.fillRect(drawX, drawY, drawW, drawH);
        
        // Boss crown/horns
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
        
        // Boss label
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, (int)(14 * Game.SCALE)));
        FontMetrics fm = g.getFontMetrics();
        String label = "BOSS";
        int labelX = drawX + (drawW - fm.stringWidth(label)) / 2;
        int labelY = drawY + drawH / 2 + fm.getAscent() / 2;
        g.drawString(label, labelX, labelY);
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
        
        // Check damage cooldown to prevent one-shot
        long now = System.currentTimeMillis();
        if (now - lastDamageTakenMs < DAMAGE_COOLDOWN_MS) {
            return; // Still invulnerable from last hit
        }
        
        health = Math.max(0, health - amount);
        lastDamageTakenMs = now;
        
        // Play damage sound
        util.SoundManager.play(util.SoundManager.SoundEffect.BOSS_DAMAGE);
        
        // Start death animation if boss dies
        if (health <= 0) {
            dying = true;
            util.SoundManager.play(util.SoundManager.SoundEffect.BOSS_DEATH);
        }
    }
    
    public boolean isDead() {
        return dying && deathAnimationTick >= DEATH_ANIMATION_DURATION;
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
