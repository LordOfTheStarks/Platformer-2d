package Entities;

import Main.Game;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import static util.Helpmethods.*;
import java.awt.image.BufferedImage;
import util.LoadSave;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
public class Enemy extends Entity {
    private int[][] levelData;

    // movement expressed as continuous xSpeed (sub-pixel)
    private float xSpeed;
    private float baseSpeed = 0.5f * Game.SCALE;
    
    // Health system
    private int health = 1;

    // Animation frames per variant
    private static BufferedImage[][] enemyFrames; // [variant][frameIndex]
    private static BufferedImage[][] enemyFramesFlipped;
    private static boolean triedLoadingImages = false;
    private static boolean imagesAvailable = false;
    private static boolean printedDebug = false;

    // per-instance animation state
    private int animIndex = 0;
    private int animTick = 0;
    private final int animSpeed = 8; // ticks per frame

    private final int variant; // 0 or 1

    // Desired visual size to match the Player sprite drawing
    private static final int VISUAL_W = (int) (62.5f * Game.SCALE);
    private static final int VISUAL_H = (int) (46.25f * Game.SCALE);

    public Enemy(float x, float y, int w, int h, int variant, int[][] levelData) {
        super(x, y, w, h);
        this.levelData = levelData;
        this.variant = Math.max(0, Math.min(1, variant));
        // Slightly smaller hitbox for more forgiving collisions (we already pass in sensible w/h)
        initHitBox(x, y, w - (int)(10*Game.SCALE), h - (int)(10*Game.SCALE));

        loadEnemyFramesIfNeeded();

        // start moving right by default
        this.xSpeed = baseSpeed;
    }

    /**
     * Load enemy sprite atlases and slice into square frames (frameW = imgHeight).
     * We support both single-frame images and horizontal strips.
     */
    private void loadEnemyFramesIfNeeded() {
        if (triedLoadingImages) return;
        triedLoadingImages = true;

        try {
            URL r1 = LoadSave.class.getResource(LoadSave.ENEMY_1);
            URL r2 = LoadSave.class.getResource(LoadSave.ENEMY_2);
            if (!printedDebug) {
                System.out.println("[Enemy] Resource ENEMY_1 path: " + LoadSave.ENEMY_1 + " -> " + r1);
                System.out.println("[Enemy] Resource ENEMY_2 path: " + LoadSave.ENEMY_2 + " -> " + r2);
            }

            BufferedImage a1 = LoadSave.getAtlas(LoadSave.ENEMY_1);
            BufferedImage a2 = LoadSave.getAtlas(LoadSave.ENEMY_2);

            if (a1 == null && a2 == null) {
                System.out.println("[Enemy] Warning: enemy images not found (ENEMY_1/ENEMY_2). Falling back to rectangles.");
                imagesAvailable = false;
                printedDebug = true;
                return;
            }

            List<BufferedImage[]> variants = new ArrayList<>(2);
            variants.add(sliceAtlasToFrames(a1));
            variants.add(sliceAtlasToFrames(a2));

            // convert to arrays
            enemyFrames = new BufferedImage[variants.size()][];
            for (int i = 0; i < variants.size(); i++) {
                enemyFrames[i] = variants.get(i);
            }

            // Precompute flipped frames
            enemyFramesFlipped = new BufferedImage[enemyFrames.length][];
            for (int v = 0; v < enemyFrames.length; v++) {
                BufferedImage[] frames = enemyFrames[v];
                if (frames == null) {
                    enemyFramesFlipped[v] = null;
                    continue;
                }
                enemyFramesFlipped[v] = new BufferedImage[frames.length];
                for (int f = 0; f < frames.length; f++) {
                    BufferedImage img = frames[f];
                    if (img == null) {
                        enemyFramesFlipped[v][f] = null;
                        continue;
                    }
                    AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
                    tx.translate(-img.getWidth(), 0);
                    AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                    try {
                        enemyFramesFlipped[v][f] = op.filter(img, null);
                    } catch (Exception ex) {
                        enemyFramesFlipped[v][f] = null;
                    }
                }
            }

            imagesAvailable = true;
            System.out.println("[Enemy] Loaded enemy images OK.");
            printedDebug = true;
        } catch (Exception e) {
            imagesAvailable = false;
            System.out.println("[Enemy] Warning: error loading enemy images, falling back to rectangles.");
            e.printStackTrace();
            printedDebug = true;
        }
    }

    // Slice atlas into square frames. If atlas==null returns a one-element array with null.
    private BufferedImage[] sliceAtlasToFrames(BufferedImage atlas) {
        if (atlas == null) return new BufferedImage[] { null };
        int h = atlas.getHeight();
        int w = atlas.getWidth();
        if (h <= 0) return new BufferedImage[] { atlas };

        int frameW = h; // assume square frames stacked horizontally
        int frameCount = Math.max(1, w / frameW);
        BufferedImage[] frames = new BufferedImage[frameCount];
        for (int i = 0; i < frameCount; i++) {
            int sx = i * frameW;
            if (sx + frameW <= w) {
                frames[i] = atlas.getSubimage(sx, 0, frameW, h);
            } else {
                int remaining = w - sx;
                if (remaining > 0) {
                    BufferedImage tmp = new BufferedImage(frameW, h, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = tmp.createGraphics();
                    g2.drawImage(atlas.getSubimage(sx, 0, remaining, h), 0, 0, null);
                    g2.dispose();
                    frames[i] = tmp;
                } else {
                    frames[i] = null;
                }
            }
        }
        return frames;
    }

    public void update() {
        // Animate
        if (imagesAvailable && enemyFrames != null && variant < enemyFrames.length && enemyFrames[variant] != null) {
            animTick++;
            if (animTick >= animSpeed) {
                animTick = 0;
                animIndex++;
                if (animIndex >= enemyFrames[variant].length) animIndex = 0;
            }
        }

        // Use xSpeed for horizontal movement
        if (CanMoveHere(hitBox.x + xSpeed, hitBox.y, hitBox.width, hitBox.height, levelData)) {
            hitBox.x += xSpeed;
        } else {
            // reverse direction smoothly
            xSpeed = -xSpeed;
        }

        // Edge ahead check: build a thin probe hitbox one pixel ahead
        float probeX = xSpeed > 0 ? (hitBox.x + hitBox.width + 1) : (hitBox.x - 1);
        Rectangle2D.Float probeHB = new Rectangle2D.Float(probeX, hitBox.y, 1, hitBox.height);

        // If there is no floor directly under the probe, turn around (reverse xSpeed)
        if (!IsOnFloor(probeHB, levelData)) {
            xSpeed = -xSpeed;
        }

        // Gravity: move down if there is space
        if (CanMoveHere(hitBox.x, hitBox.y + 1, hitBox.width, hitBox.height, levelData)) {
            hitBox.y += 1;
        }
    }

    public void render(Graphics g, int cameraOffsetX) {
        // If images aren't available draw fallback rectangle
        if (!imagesAvailable || enemyFrames == null) {
            drawFallback(g, cameraOffsetX);
            return;
        }

        BufferedImage[] frames = variant < enemyFrames.length ? enemyFrames[variant] : null;
        BufferedImage[] framesFlipped = variant < enemyFramesFlipped.length ? enemyFramesFlipped[variant] : null;
        if (frames == null || frames.length == 0 || frames[0] == null) {
            drawFallback(g, cameraOffsetX);
            return;
        }

        int frameIdx = animIndex % frames.length;
        BufferedImage srcImg = (xSpeed >= 0) ? frames[frameIdx] : (framesFlipped != null ? framesFlipped[frameIdx] : frames[frameIdx]);

        if (srcImg == null) {
            drawFallback(g, cameraOffsetX);
            return;
        }

        int srcW = Math.max(1, srcImg.getWidth());
        int srcH = Math.max(1, srcImg.getHeight());

        // Use desired visual size = player's drawn size
        int targetW = VISUAL_W;
        int targetH = VISUAL_H;

        // Compute scale to fit the sprite inside the target box while preserving aspect ratio
        float scale = Math.min((float) targetW / srcW, (float) targetH / srcH);
        if (scale <= 0f) {
            drawFallback(g, cameraOffsetX);
            return;
        }

        int drawW = Math.max(1, Math.round(srcW * scale));
        int drawH = Math.max(1, Math.round(srcH * scale));

        // Bottom-align sprite to the enemy's hitbox bottom
        int drawX = (int) hitBox.x + ((int)hitBox.width - drawW) / 2 - cameraOffsetX;
        int drawY = (int) (hitBox.y + hitBox.height - drawH);

        g.drawImage(srcImg, drawX, drawY, drawW, drawH, null);
    }

    private void drawFallback(Graphics g, int cameraOffsetX) {
        // Visible debugging fallback: colored rectangle with "E" label so you can see enemies
        g.setColor(new Color(200, 40, 40));
        int x = Math.max(0, (int) hitBox.x - cameraOffsetX);
        int y = Math.max(0, (int) hitBox.y);
        int w = Math.max(8, (int) hitBox.width);
        int h = Math.max(8, (int) hitBox.height);
        g.fillRect(x, y, w, h);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, (int)(12 * Game.SCALE))));
        FontMetrics fm = g.getFontMetrics();
        String s = "E";
        int tx = x + (w - fm.stringWidth(s)) / 2;
        int ty = y + (h + fm.getAscent()) / 2 - 2;
        g.drawString(s, tx, ty);
    }
    
    // Health API
    public void takeDamage(int amount) {
        if (amount <= 0) return;
        health = Math.max(0, health - amount);
        // Play damage sound
        util.SoundManager.play(util.SoundManager.SoundEffect.ENEMY_DAMAGE);
        // Play death sound if enemy dies
        if (health <= 0) {
            util.SoundManager.play(util.SoundManager.SoundEffect.ENEMY_DEATH);
        }
    }
    
    public boolean isDead() {
        return health <= 0;
    }
    
    public int getHealth() {
        return health;
    }
}



