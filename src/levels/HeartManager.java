package levels;

import Main.Game;
import util.LoadSave;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import static util.Helpmethods.*;

/**
 * Manages heart pickups in levels.
 * Places one heart per level in strategic positions.
 */
public class HeartManager {
    private BufferedImage[] frames; // animation frames from heart sheet
    private int frameIndex = 0;
    private int tick = 0;
    private final int animSpeed = 10; // ticks per frame

    private final List<Heart> hearts = new ArrayList<>();

    public HeartManager() {
        loadFrames();
    }

    private void loadFrames() {
        BufferedImage sheet = LoadSave.getAtlas(LoadSave.HEART_FULL_SHEET);
        if (sheet == null) return;
        // Heart sheet is 96x16 with 6 frames of 16x16 each
        int frameCount = 6;
        frames = new BufferedImage[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = sheet.getSubimage(i * 16, 0, 16, 16);
        }
    }

    public void clear() {
        hearts.clear();
    }

    /**
     * Spawn one heart in the given level at a strategic position.
     * Avoids spikes and ensures it's on solid ground.
     */
    public void spawnForLevel(Level level, SpikeManager spikeManager) {
        hearts.clear();
        int[][] data = level.getLevelData();
        if (data == null) return;

        int levelWidth = level.getLevelWidth();
        
        // Place heart at approximately 70% through the level
        // This rewards players for progressing through the level
        int targetX = (int)(levelWidth * 0.7f);
        
        // Find a valid position near the target
        for (int attempt = 0; attempt < 10; attempt++) {
            int xt = targetX + attempt - 5; // Try nearby tiles
            if (xt < 0 || xt >= levelWidth) continue;
            
            int groundYTile = findGroundYTile(data, xt);
            if (groundYTile == -1) continue;

            int px = xt * Game.TILES_SIZE + (Game.TILES_SIZE - Heart.W) / 2;
            int py = groundYTile * Game.TILES_SIZE - Heart.H - (int)(4 * Game.SCALE);

            if (!CanMoveHere(px, py, Heart.W, Heart.H, data)) continue;

            // Skip placement if this heart would overlap any spike
            Rectangle heartRect = new Rectangle(px, py, Heart.W, Heart.H);
            boolean overlapsSpike = false;
            if (spikeManager != null) {
                for (Spike s : spikeManager.getSpikes()) {
                    if (heartRect.intersects(s.getBounds())) {
                        overlapsSpike = true;
                        break;
                    }
                }
            }
            if (!overlapsSpike) {
                hearts.add(new Heart(px, py));
                return; // Successfully placed one heart
            }
        }
        
        // Fallback: place at a safe starting position if no good spot found
        int fallbackX = 10;
        int groundYTile = findGroundYTile(data, fallbackX);
        if (groundYTile != -1) {
            int px = fallbackX * Game.TILES_SIZE + (Game.TILES_SIZE - Heart.W) / 2;
            int py = groundYTile * Game.TILES_SIZE - Heart.H - (int)(4 * Game.SCALE);
            hearts.add(new Heart(px, py));
        }
    }

    private int findGroundYTile(int[][] data, int xTile) {
        for (int y = 0; y < Game.TILES_HEIGHT; y++) {
            if (xTile < 0 || xTile >= data[0].length) return -1;
            int idx = data[y][xTile];
            boolean solid = (idx != 11);
            if (solid) {
                return y;
            }
        }
        return -1;
    }

    /**
     * Checks player hitbox against hearts; removes collected hearts and returns how many were collected.
     */
    public int collectIfPlayerTouches(Rectangle2D.Float playerHB) {
        Rectangle playerRect = new Rectangle((int)playerHB.x, (int)playerHB.y, (int)playerHB.width, (int)playerHB.height);
        int collected = 0;
        List<Heart> removed = new ArrayList<>();
        for (Heart h : hearts) {
            if (playerRect.intersects(h.getBounds())) {
                collected++;
                removed.add(h);
                // Play collection sound (reuse coin sound or add specific heart sound)
                util.SoundManager.play(util.SoundManager.SoundEffect.COIN_COLLECT);
            }
        }
        if (!removed.isEmpty()) hearts.removeAll(removed);
        return collected;
    }

    public void update() {
        if (frames == null || frames.length == 0) return;
        tick++;
        if (tick >= animSpeed) {
            tick = 0;
            frameIndex++;
            if (frameIndex >= frames.length) frameIndex = 0;
        }
    }

    public void draw(Graphics g, int cameraOffsetX) {
        if (frames == null || frames.length == 0) return;
        for (Heart h : hearts) {
            int drawX = h.getX() - cameraOffsetX;
            g.drawImage(frames[frameIndex], drawX, h.getY(), Heart.W, Heart.H, null);
        }
    }

    public void clearAll() {
        hearts.clear();
    }
}
