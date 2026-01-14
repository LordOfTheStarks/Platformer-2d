package levels;

import Main.Game;
import util.LoadSave;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static util.Helpmethods.*;

public class PotionManager {
    private BufferedImage[] frames; // 7-frame animation
    private int frameIndex = 0;
    private int tick = 0;
    private final int animSpeed = 10; // ticks per frame

    private final List<Potion> potions = new ArrayList<>();
    private final Random rnd = new Random();

    public PotionManager() {
        loadFrames();
    }

    private void loadFrames() {
        // 7 separate PNGs
        frames = new BufferedImage[7];
        frames[0] = LoadSave.getAtlas(LoadSave.POTION_1);
        frames[1] = LoadSave.getAtlas(LoadSave.POTION_2);
        frames[2] = LoadSave.getAtlas(LoadSave.POTION_3);
        frames[3] = LoadSave.getAtlas(LoadSave.POTION_4);
        frames[4] = LoadSave.getAtlas(LoadSave.POTION_5);
        frames[5] = LoadSave.getAtlas(LoadSave.POTION_6);
        frames[6] = LoadSave.getAtlas(LoadSave.POTION_7);
    }

    public void clear() {
        potions.clear();
    }

    /**
     * Spawn exactly one potion in the given level.
     * Picks a random x tile column, places the potion on the first solid tile's top (ground or platform).
     * Avoids spikes by checking SpikeManager bounds.
     */
    public void spawnForLevel(Level level, SpikeManager spikeManager) {
        potions.clear();
        int[][] data = level.getLevelData();
        if (data == null) return;

        int attempts = Game.TILES_WIDTH * 4;
        for (int i = 0; i < attempts && potions.size() < 1; i++) {
            int xt = rnd.nextInt(Game.TILES_WIDTH);
            int groundYTile = findGroundYTile(data, xt);
            if (groundYTile == -1) continue;

            // center horizontally on tile; sit on ground/platform top
            int px = xt * Game.TILES_SIZE + (Game.TILES_SIZE - Potion.W) / 2;
            int py = groundYTile * Game.TILES_SIZE - Potion.H - (int)(2 * Game.SCALE);

            // Must be a valid position (not embedded in solid)
            if (!CanMoveHere(px, py, Potion.W, Potion.H, data)) continue;

            // Avoid spikes overlap
            Rectangle potionRect = new Rectangle(px, py, Potion.W, Potion.H);
            boolean overlapsSpike = false;
            if (spikeManager != null) {
                for (Spike s : spikeManager.getSpikes()) {
                    if (potionRect.intersects(s.getBounds())) {
                        overlapsSpike = true;
                        break;
                    }
                }
            }
            if (overlapsSpike) continue;

            potions.add(new Potion(px, py));
        }
    }

    // Find first solid tile from top (matches Helpmethods.isSolid heuristic where index != 11)
    private int findGroundYTile(int[][] data, int xTile) {
        for (int y = 0; y < Game.TILES_HEIGHT; y++) {
            int idx = data[y][xTile];
            boolean solid = (idx != 11);
            if (solid) {
                return y;
            }
        }
        return -1;
    }

    /**
     * Checks player hitbox against the potion; removes if consumed and returns true.
     */
    public boolean consumeIfPlayerTouches(Rectangle2D.Float playerHB) {
        if (potions.isEmpty()) return false;
        Rectangle playerRect = new Rectangle((int)playerHB.x, (int)playerHB.y, (int)playerHB.width, (int)playerHB.height);
        Potion consumed = null;
        for (Potion p : potions) {
            if (playerRect.intersects(p.getBounds())) {
                consumed = p;
                break;
            }
        }
        if (consumed != null) {
            potions.remove(consumed);
            return true;
        }
        return false;
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

    public void draw(Graphics g) {
        if (frames == null || frames.length == 0) return;
        for (Potion p : potions) {
            g.drawImage(frames[frameIndex], p.getX(), p.getY(), Potion.W, Potion.H, null);
        }
    }
}