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
 *
 * Complex AI behavior:
 * - Patrols the arena when player is far
 * - Charges aggressively when player is close
 * - Jumps to reach platforms and dodge attacks
 * - Shoots projectiles at player
 */
public class Boss extends Entity {
    private int[][] levelData;

    // Movement - faster than normal enemies (normal enemy speed is 0.5f)
    /** Speed multiplier for boss movement (faster than regular enemies) */
    private static final float BASE_SPEED_MULTIPLIER = 0.8f;
    private static final float CHARGE_SPEED_MULTIPLIER = 1.3f;
    private float xSpeed;
    private float baseSpeed = BASE_SPEED_MULTIPLIER * Game.SCALE;
    private float chargeSpeed = CHARGE_SPEED_MULTIPLIER * Game.SCALE;

    // AI State machine
    private enum BossState {
        PATROL,     // Moving back and forth
        CHASE,      // Chasing player aggressively
        ATTACK,     // Preparing or executing attack
        RETREAT     // Moving away after taking damage
    }
    private BossState state = BossState.PATROL;
    private long stateChangeTime = 0;
    private static final long STATE_DURATION_MS = 2000; // Min time in each state

    // Patrol parameters
    private float patrolLeftBound;
    private float patrolRightBound;
    private boolean patrollingRight = true;

    private BossState prevState = null;

    private float flyTargetY;
    private long nextFlyTargetChangeMs = 0;

    private long nextAllowedAttackMs = 0;

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
    private static final long PROJECTILE_COOLDOWN_MS = 2500L;
    private List<BossProjectile> projectiles = new ArrayList<>();
    private long lastProjectileTime = 0;
    private float projectileSpeed = 2.0f * Game.SCALE;

    // Visual size for boss (larger than regular enemies)
    private static final int VISUAL_W = (int) (80f * Game.SCALE);
    private static final int VISUAL_H = (int) (80f * Game.SCALE);

    // Sprite sheet constants (undeadking.png is 96x256)
    // Using first frame only (96x64) for single character display
    private static final int SPRITE_WIDTH = 96;
    private static final int SPRITE_HEIGHT = 64;

    // Sprites
    private BufferedImage spriteSheet;
    private BufferedImage currentFrame;  // Single frame at a time
    private BufferedImage currentFrameMirrored;
    private boolean facingLeft = false;

    // Player reference for targeting
    private Rectangle2D.Float playerHitBox;

    // Flying vertical movement
    private float ySpeed = 0f;
    private float maxFlySpeed = 3.0f * Game.SCALE;
    private float verticalSmoothing = 0.10f;

    private int minFlyY;
    private int maxFlyY;

    public Boss(float x, float y, int w, int h, int[][] levelData) {
        super(x, y, w, h);
        this.levelData = levelData;
        // Boss has a larger hitbox
        initHitBox(x, y, w - (int)(10*Game.SCALE), h - (int)(10*Game.SCALE));

        // Set patrol bounds based on arena
        // Arena is 45 tiles wide: tiles 0-1 are left wall, tiles 43-44 are right wall
        // Patrol area: tiles 3-41 (inside the walls with margin)
        patrolLeftBound = 3 * Game.TILES_SIZE;
        patrolRightBound = 41 * Game.TILES_SIZE;

        // Start moving right by default
        this.xSpeed = baseSpeed;
        patrollingRight = true;
        state = BossState.PATROL;
        stateChangeTime = System.currentTimeMillis();

        // Load sprite
        loadSprite();

        maxFlyY = (Game.TILES_HEIGHT - 4) * Game.TILES_SIZE - (int) this.hitBox.height; // near ground
        minFlyY = Math.max(0, (Game.TILES_HEIGHT - 10) * Game.TILES_SIZE - (int) this.hitBox.height); // not too high

        flyTargetY = hitBox.y;
        nextFlyTargetChangeMs = System.currentTimeMillis() + 800;

        nextAllowedAttackMs = System.currentTimeMillis() + 2000;
    }

    private BufferedImage[] idleFrames, attackFrames, flyingFrames, hurtFrames, deathFrames;
    private BufferedImage[] idleFramesM, attackFramesM, flyingFramesM, hurtFramesM, deathFramesM;


    private static final int FRAME_W = 64;
    private static final int FRAME_H = 64;

    private BufferedImage[] loadFramesFromSheet(String path, int frames) {
        BufferedImage sheet = LoadSave.getAtlas(path);
        if (sheet == null) return new BufferedImage[0];

        int sheetW = sheet.getWidth();
        int sheetH = sheet.getHeight();

        int frameW = sheetW / frames;
        int frameH = sheetH;

        BufferedImage[] out = new BufferedImage[frames];
        for (int i = 0; i < frames; i++) {
            int x = i * frameW;

            // страховка на всякий случай
            int w = Math.min(frameW, sheetW - x);
            out[i] = sheet.getSubimage(x, 0, w, frameH);
        }
        return out;
    }





    private BufferedImage flip(BufferedImage img) {
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        AffineTransform tx = new AffineTransform();
        tx.scale(-1, 1);
        tx.translate(-img.getWidth(), 0);
        new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR).filter(img, out);
        return out;
    }

    private BufferedImage[] mirrorFrames(BufferedImage[] src) {
        BufferedImage[] out = new BufferedImage[src.length];
        for (int i = 0; i < src.length; i++) out[i] = flip(src[i]);
        return out;
    }

    private void loadSprite() {
        idleFrames   = loadFramesFromSheet(LoadSave.BOSS_IDLE, 4);
        flyingFrames = loadFramesFromSheet(LoadSave.BOSS_FLYING, 4);
        attackFrames = loadFramesFromSheet(LoadSave.BOSS_ATTACK, 8);
        hurtFrames   = loadFramesFromSheet(LoadSave.BOSS_HURT, 4);
        deathFrames  = loadFramesFromSheet(LoadSave.BOSS_DEATH, 7);

        idleFramesM   = mirrorFrames(idleFrames);
        flyingFramesM = mirrorFrames(flyingFrames);
        attackFramesM = mirrorFrames(attackFrames);
        hurtFramesM   = mirrorFrames(hurtFrames);
        deathFramesM  = mirrorFrames(deathFrames);
    }


    private BufferedImage getFrameForState() {
        BufferedImage[] frames;
        BufferedImage[] framesM;

        if (dying) {
            frames = deathFrames;
            framesM = deathFramesM;
        } else {
            switch (state) {
                case ATTACK -> { frames = attackFrames; framesM = attackFramesM; }
                case CHASE, RETREAT -> { frames = flyingFrames; framesM = flyingFramesM; }
                default -> { frames = idleFrames; framesM = idleFramesM; }
            }
        }

        if (frames == null || frames.length == 0) return null;

        int idx = animIndex % frames.length;
        if (facingLeft && framesM != null && framesM.length > idx) return framesM[idx];
        return frames[idx];
    }


    private int animTick = 0;
    private int animIndex = 0;
    private int animSpeed = 8;

    private void advanceAnimation() {
        animTick++;
        if (animTick >= animSpeed) {
            animTick = 0;
            animIndex++;
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

        // Update AI state
        updateAIState();

        if (state != prevState) {
            animIndex = 0;
            animTick = 0;
            prevState = state;
        }

        // Execute behavior based on state
        switch (state) {
            case PATROL -> executePatrol();
            case CHASE -> executeChase();
            case ATTACK -> executeAttack();
            case RETREAT -> executeRetreat();
        }

        // Apply horizontal movement
        applyMovement();

        applyFlyingVerticalMovement();

        // Shoot projectiles at player
        shootAtPlayer();

        // Update projectiles
        for (BossProjectile  p : projectiles) {
            p.update();
        }
        projectiles.removeIf(p -> !p.isActive());

        advanceAnimation();
    }

    /**
     * Update AI state based on player distance and timing.
     */
    private void updateAIState() {
        if (playerHitBox == null) {
            state = BossState.PATROL;
            return;
        }

        float playerCenterX = playerHitBox.x + playerHitBox.width / 2;
        float bossCenterX = hitBox.x + hitBox.width / 2;
        float distance = Math.abs(playerCenterX - bossCenterX);

        long now = System.currentTimeMillis();
        long timeInState = now - stateChangeTime;

        // State transitions
        switch (state) {
            case PATROL -> {
                // Switch to chase if player is within medium range
                if (distance < 300 * Game.SCALE) {
                    state = BossState.CHASE;
                    stateChangeTime = now;
                }
            }
            case CHASE -> {
                if (distance < 130 * Game.SCALE && timeInState > 800 && now >= nextAllowedAttackMs) {
                    if (Math.random() < 0.20) {
                        state = BossState.ATTACK;
                        stateChangeTime = now;

                        nextAllowedAttackMs = now + 3000 + (long)(Math.random() * 2000);
                    }
                }

            }
            case ATTACK -> {
                // Return to chase after attack duration
                if (timeInState > 800) {
                    state = BossState.CHASE;
                    stateChangeTime = now;
                }
            }
            case RETREAT -> {
                // Return to chase after retreat
                if (timeInState > 1000) {
                    state = BossState.CHASE;
                    stateChangeTime = now;
                }
            }
        }
    }

    /**
     * Patrol back and forth in the arena.
     */
    private void executePatrol() {
        float currentSpeed = baseSpeed;

        // Patrol between bounds
        if (patrollingRight) {
            xSpeed = currentSpeed;
            facingLeft = false;
            if (hitBox.x + hitBox.width >= patrolRightBound) {
                patrollingRight = false;
            }
        } else {
            xSpeed = -currentSpeed;
            facingLeft = true;
            if (hitBox.x <= patrolLeftBound) {
                patrollingRight = true;
            }
        }
    }

    /**
     * Chase player aggressively.
     */
    private void executeChase() {
        if (playerHitBox == null) return;

        float playerCenterX = playerHitBox.x + playerHitBox.width / 2;
        float bossCenterX = hitBox.x + hitBox.width / 2;

        // Move faster toward player
        if (playerCenterX < bossCenterX - 70) {
            xSpeed = -chargeSpeed;
            facingLeft = true;
        } else if (playerCenterX > bossCenterX + 70) {
            xSpeed = chargeSpeed;
            facingLeft = false;
        } else {
            // Very close - slow down for accuracy
            xSpeed = 0;
        }
    }

    /**
     * Execute attack (charge at player).
     */
    private void executeAttack() {
        if (playerHitBox == null) return;

        float playerCenterX = playerHitBox.x + playerHitBox.width / 2;
        float bossCenterX = hitBox.x + hitBox.width / 2;

        // Charge at full speed
        if (playerCenterX < bossCenterX) {
            xSpeed = -chargeSpeed * 1.3f;
            facingLeft = true;
        } else {
            xSpeed = chargeSpeed * 1.3f;
            facingLeft = false;
        }
    }

    /**
     * Retreat after taking damage.
     */
    private void executeRetreat() {
        if (playerHitBox == null) return;

        float playerCenterX = playerHitBox.x + playerHitBox.width / 2;
        float bossCenterX = hitBox.x + hitBox.width / 2;

        // Move away from player
        if (playerCenterX < bossCenterX) {
            xSpeed = chargeSpeed;  // Move right (away from player on left)
            facingLeft = true;  // Still face player
        } else {
            xSpeed = -chargeSpeed;  // Move left (away from player on right)
            facingLeft = false;  // Still face player
        }
    }

    /**
     * Apply horizontal movement with collision checking.
     */
    private void applyMovement() {
        // Apply xSpeed with collision
        if (bossCanMoveHere(hitBox.x + xSpeed, hitBox.y, hitBox.width, hitBox.height)) {
            hitBox.x += xSpeed;
        } else {
            if (state == BossState.PATROL) patrollingRight = !patrollingRight;
            xSpeed = -xSpeed * 0.5f;
        }

        // Clamp to patrol bounds
        if (hitBox.x < patrolLeftBound) {
            hitBox.x = patrolLeftBound;
            patrollingRight = true;
        }
        if (hitBox.x + hitBox.width > patrolRightBound) {
            hitBox.x = patrolRightBound - hitBox.width;
            patrollingRight = false;
        }
    }

    private void shootAtPlayer() {
        if (playerHitBox == null || dying) return;

        long now = System.currentTimeMillis();

        if (Math.random() > 0.25) return;

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

    private void applyFlyingVerticalMovement() {
        if (playerHitBox == null || dying) return;

        long now = System.currentTimeMillis();

        if (now >= nextFlyTargetChangeMs) {
            float base = playerHitBox.y;

            float randomOffset = (float)((Math.random() * 160) - 80); // [-80..+80] px
            flyTargetY = base + randomOffset;

            flyTargetY = Math.max(minFlyY, Math.min(flyTargetY, maxFlyY));

            nextFlyTargetChangeMs = now + 800 + (long)(Math.random() * 700);
        }

        float diff = flyTargetY - hitBox.y;

        float smoothing = 0.04f;
        float maxSpeed = 1.5f * Game.SCALE;

        float desiredYSpeed = diff * smoothing;

        if (desiredYSpeed > maxSpeed) desiredYSpeed = maxSpeed;
        if (desiredYSpeed < -maxSpeed) desiredYSpeed = -maxSpeed;

        if (bossCanMoveHere(hitBox.x, hitBox.y + desiredYSpeed, hitBox.width, hitBox.height)) {
            hitBox.y += desiredYSpeed;
            ySpeed = desiredYSpeed;
        } else {
            ySpeed = 0;
        }
    }


    private boolean bossCanMoveHere(float x, float y, float width, float height) {
        return !bossIsSolid(x, y) &&
                !bossIsSolid(x + width, y) &&
                !bossIsSolid(x, y + height) &&
                !bossIsSolid(x + width, y + height) &&
                !bossIsSolid(x, y + height / 2f) &&
                !bossIsSolid(x + width, y + height / 2f);
    }

    private boolean bossIsSolid(float x, float y) {
        if (x < 0) return true;
        if (y < 0) return true;
        if (y >= Game.GAME_HEIGHT) return false;

        int xIndex = (int) (x / Game.TILES_SIZE);
        int yIndex = (int) (y / Game.TILES_SIZE);

        if (yIndex < 0 || yIndex >= levelData.length) return false;
        if (xIndex < 0 || xIndex >= levelData[0].length) return true;

        int value = levelData[yIndex][xIndex];

        // IMPORTANT: PLATFORM должен быть НЕ solid для босса
        if (value == util.LevelFactory.AIR) return false;
        if (value == util.LevelFactory.PLATFORM) return false;

        return true; // земля, стены, колонны — solid
    }



    public void render(Graphics g, int cameraOffsetX) {
        int drawW = VISUAL_W;
        int drawH = VISUAL_H;
        int drawX = (int) hitBox.x - cameraOffsetX - (drawW - (int) hitBox.width) / 2;
        int drawY = (int) hitBox.y - (drawH - (int) hitBox.height);

        Graphics2D g2d = (Graphics2D) g;
        Composite originalComposite = g2d.getComposite();

        if (dying) {
            g2d.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER,
                    Math.max(0f, deathFadeAlpha)
            ));
        }

        BufferedImage[] frames;
        BufferedImage[] framesM;

        if (dying) {
            frames = deathFrames;
            framesM = deathFramesM;
        } else {
            switch (state) {
                case ATTACK -> { frames = attackFrames; framesM = attackFramesM; }
                case CHASE, RETREAT -> { frames = flyingFrames; framesM = flyingFramesM; }
                default -> { frames = idleFrames; framesM = idleFramesM; }
            }
        }

        BufferedImage frameToDraw = null;
        if (frames != null && frames.length > 0) {
            int idx = animIndex % frames.length;
            frameToDraw = facingLeft && framesM != null && framesM.length > idx
                    ? framesM[idx]
                    : frames[idx];
        }

        if (frameToDraw != null) {
            g.drawImage(frameToDraw, drawX, drawY, drawW, drawH, null);
        } else {
            drawFallbackBoss(g, drawX, drawY, drawW, drawH);
        }

        // Health bar
        if (!dying) {
            drawHealthBar(g, drawX, drawY - (int) (25 * Game.SCALE), drawW);
        }

        // restore alpha
        if (dying) {
            g2d.setComposite(originalComposite);
        }

        // projectiles
        for (BossProjectile p : projectiles) {
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

        // Switch to retreat state when hit
        state = BossState.RETREAT;
        stateChangeTime = now;

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

    public List<BossProjectile> getProjectiles() {
        return projectiles;
    }

    /**
     * Check if any of the boss's projectiles hit the player.
     * Returns damage dealt.
     */
    public int checkProjectilePlayerCollision(Rectangle2D.Float playerHB) {
        int damage = 0;
        for (BossProjectile  p : projectiles) {
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