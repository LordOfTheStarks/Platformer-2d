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

public class BoxManager {
    private static final int MAX_BOXES_PER_LEVEL = 6;
    private static final float COIN_DROP_CHANCE = 0.6f;
    private static final float HEART_DROP_CHANCE = 0.2f;

    private BufferedImage[] frames;
    private final List<Box> boxes = new ArrayList<>();
    private final Random rnd = new Random();

    public BoxManager() {
        loadFrames();
    }

    private void loadFrames() {
        BufferedImage sheet = LoadSave.getAtlas(LoadSave.BOXES_SHEET);
        if (sheet == null) return;
        int frameCount = sheet.getWidth() / 16;
        frames = new BufferedImage[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = sheet.getSubimage(i * 16, 0, 16, 16);
        }
    }

    public void clear() {
        boxes.clear();
    }

    public void spawnForLevel(Level level, SpikeManager spikeManager) {
        boxes.clear();
        int[][] data = level.getLevelData();
        if (data == null) return;

        int levelWidth = level.getLevelWidth();
        int spawnCount = Math.min(MAX_BOXES_PER_LEVEL, Math.max(1, levelWidth / 10));
        int attempts = spawnCount * 10;

        for (int i = 0; i < attempts && boxes.size() < spawnCount; i++) {
            int xt = rnd.nextInt(levelWidth);
            int groundYTile = findGroundYTile(data, xt);
            if (groundYTile == -1) continue;

            int px = xt * Game.TILES_SIZE + (Game.TILES_SIZE - Box.W) / 2;
            int py = groundYTile * Game.TILES_SIZE - Box.H;

            if (!CanMoveHere(px, py, Box.W, Box.H, data)) continue;

            Rectangle boxRect = new Rectangle(px, py, Box.W, Box.H);
            boolean overlapsSpike = false;
            if (spikeManager != null) {
                for (Spike s : spikeManager.getSpikes()) {
                    if (boxRect.intersects(s.getBounds())) {
                        overlapsSpike = true;
                        break;
                    }
                }
            }
            if (overlapsSpike) continue;

            boolean tooClose = false;
            for (Box b : boxes) {
                Rectangle r = b.getBounds();
                if (Math.abs(r.x - px) < Box.W * 1.2f && Math.abs(r.y - py) < Box.H * 1.2f) {
                    tooClose = true;
                    break;
                }
            }
            if (tooClose) continue;

            int frameIndex = (frames == null || frames.length == 0) ? 0 : rnd.nextInt(frames.length);
            boxes.add(new Box(px, py, frameIndex));
        }
    }

    private int findGroundYTile(int[][] data, int xTile) {
        for (int y = 0; y < Game.TILES_HEIGHT; y++) {
            if (xTile < 0 || xTile >= data[0].length) return -1;
            int idx = data[y][xTile];
            boolean solid = (idx != 11);
            if (solid) return y;
        }
        return -1;
    }

    public void checkPlayerAttackCollision(Rectangle2D.Float attackHitbox, CoinManager coinManager, HeartManager heartManager) {
        if (attackHitbox == null) return;

        List<Box> destroyed = new ArrayList<>();
        for (Box b : boxes) {
            if (attackHitbox.intersects(b.getBounds())) {
                destroyed.add(b);
                rollDrop(b, coinManager, heartManager);
            }
        }
        if (!destroyed.isEmpty()) boxes.removeAll(destroyed);
    }

    private void rollDrop(Box b, CoinManager coinManager, HeartManager heartManager) {
        float roll = rnd.nextFloat();
        if (roll < HEART_DROP_CHANCE) {
            if (heartManager != null) {
                int hx = b.getX() + (Box.W - Heart.W) / 2;
                int hy = b.getY() - Heart.H / 2;
                heartManager.addHeartAt(hx, hy);
            }
            return;
        }
        if (roll < HEART_DROP_CHANCE + COIN_DROP_CHANCE) {
            if (coinManager != null) {
                int cx = b.getX() + (Box.W - Coin.W) / 2;
                int cy = b.getY() - Coin.H / 2;
                coinManager.addCoinAt(cx, cy);
            }
        }
    }

    public void draw(Graphics g, int cameraOffsetX) {
        if (frames == null || frames.length == 0) {
            g.setColor(new Color(120, 80, 40));
            for (Box b : boxes) {
                int drawX = b.getX() - cameraOffsetX;
                g.fillRect(drawX, b.getY(), Box.W, Box.H);
            }
            return;
        }

        for (Box b : boxes) {
            int drawX = b.getX() - cameraOffsetX;
            int frame = b.getFrameIndex();
            if (frame < 0 || frame >= frames.length) frame = 0;
            g.drawImage(frames[frame], drawX, b.getY(), Box.W, Box.H, null);
        }
    }

    public void clearAll() {
        boxes.clear();
    }
}
