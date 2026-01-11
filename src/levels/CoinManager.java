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

public class CoinManager {
    private BufferedImage[] frames; // animation frames from coin sheet
    private int frameIndex = 0;
    private int tick = 0;
    private final int animSpeed = 10; // ticks per frame

    private final List<Coin> coins = new ArrayList<>();
    private final Random rnd = new Random();

    public CoinManager() {
        loadFrames();
    }

    private void loadFrames() {
        BufferedImage sheet = LoadSave.getAtlas(LoadSave.COIN_SHEET);
        if (sheet == null) return;
        int frameCount = sheet.getWidth() / 20;
        frames = new BufferedImage[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = sheet.getSubimage(i * 20, 0, 20, 20);
        }
    }

    public void clear() {
        coins.clear();
    }

    /**
     * Spawn a number of coins in the given level.
     * We pick random x tile columns and place the coin on the first solid tile's top (so it sits on ground).
     * Avoid placing coins where spikes exist by checking the spikeManager's spike bounds.
     */
    public void spawnForLevel(Level level, SpikeManager spikeManager) {
        coins.clear();
        int[][] data = level.getLevelData();
        if (data == null) return;

        int spawnCount = Math.max(5, Game.TILES_WIDTH / 6); // tweak how many coins you'd like per level
        int attempts = spawnCount * 6;

        for (int i = 0; i < attempts && coins.size() < spawnCount; i++) {
            int xt = rnd.nextInt(Game.TILES_WIDTH);
            int groundYTile = findGroundYTile(data, xt);
            if (groundYTile == -1) continue;

            int px = xt * Game.TILES_SIZE + (Game.TILES_SIZE - Coin.W) / 2;
            int py = groundYTile * Game.TILES_SIZE - Coin.H - (int)(4 * Game.SCALE);

            if (!CanMoveHere(px, py, Coin.W, Coin.H, data)) continue;

            // Skip placement if this coin would overlap any spike
            Rectangle coinRect = new Rectangle(px, py, Coin.W, Coin.H);
            boolean overlapsSpike = false;
            if (spikeManager != null) {
                for (Spike s : spikeManager.getSpikes()) {
                    if (coinRect.intersects(s.getBounds())) {
                        overlapsSpike = true;
                        break;
                    }
                }
            }
            if (overlapsSpike) continue;

            // Avoid placing coins too close to each other
            boolean tooClose = false;
            for (Coin c : coins) {
                Rectangle r = c.getBounds();
                if (Math.abs(r.x - px) < Coin.W * 1.2f && Math.abs(r.y - py) < Coin.H * 1.2f) {
                    tooClose = true;
                    break;
                }
            }
            if (!tooClose) coins.add(new Coin(px, py));
        }
    }

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
     * Checks player hitbox against coins; removes collected coins and returns how many were collected.
     */
    public int collectIfPlayerTouches(Rectangle2D.Float playerHB) {
        Rectangle playerRect = new Rectangle((int)playerHB.x, (int)playerHB.y, (int)playerHB.width, (int)playerHB.height);
        int collected = 0;
        List<Coin> removed = new ArrayList<>();
        for (Coin c : coins) {
            if (playerRect.intersects(c.getBounds())) {
                collected++;
                removed.add(c);
            }
        }
        if (!removed.isEmpty()) coins.removeAll(removed);
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

    public void draw(Graphics g) {
        if (frames == null || frames.length == 0) return;
        for (Coin c : coins) {
            g.drawImage(frames[frameIndex], c.getX(), c.getY(), Coin.W, Coin.H, null);
        }
    }

    public void clearAll() {
        coins.clear();
    }
}