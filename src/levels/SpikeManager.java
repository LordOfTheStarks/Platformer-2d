package levels;

import Main.Game;
import util.LoadSave;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.awt.RenderingHints;
import java.util.Collections;
public class SpikeManager {
    // Use a single spike image for all spikes
    private BufferedImage spikeImg;
    private final List<Spike> spikes = new ArrayList<>();

    public SpikeManager() {
        loadImages();
    }

    private void loadImages() {
        BufferedImage atlas = LoadSave.getAtlas(LoadSave.SPIKES); // expected 64x16 (4 x 16x16)
        if (atlas != null && atlas.getWidth() >= 16 && atlas.getHeight() >= 16) {
            // Choose first variant (index 0) and use it for all spikes
            spikeImg = atlas.getSubimage(0, 0, 16, 16);
        } else {
            spikeImg = null;
        }
    }

    public void clear() {
        spikes.clear();
    }

    public void spawnForLevel(Level level) {
        spikes.clear();
        int[][] data = level.getLevelData();
        if (data == null) return;

        // Choose a few x tile columns and place spikes on ground.
        int[] xTiles = {6, 10, 16, 21}; // tweak as desired per level layout
        for (int i = 0; i < xTiles.length; i++) {
            int xt = xTiles[i];
            int groundYTile = findGroundYTile(data, xt);
            if (groundYTile != -1) {
                // Pixel positions: center spike horizontally on the tile; bottom on ground top
                int px = xt * Game.TILES_SIZE + (Game.TILES_SIZE - Spike.W) / 2;
                int py = groundYTile * Game.TILES_SIZE - Spike.H; // bottom aligns with ground top
                // Use type 0 for all spikes (keeps type present but uniform visually)
                spikes.add(new Spike(px, py, 0));
            }
        }
    }

    private int findGroundYTile(int[][] data, int xTile) {
        // Find first solid tile from top; we treat value != 11 as solid (matches Helpmethods.isSolid)
        for (int y = 0; y < Game.TILES_HEIGHT; y++) {
            int idx = data[y][xTile];
            boolean solid = (idx != 11);
            if (solid) {
                return y;
            }
        }
        return -1;
    }

    public boolean isPlayerOnSpike(Rectangle2D.Float playerHB) {
        Rectangle playerRect = new Rectangle((int)playerHB.x, (int)playerHB.y, (int)playerHB.width, (int)playerHB.height);
        for (Spike s : spikes) {
            if (playerRect.intersects(s.getBounds())) {
                return true;
            }
        }
        return false;
    }

    public void draw(Graphics g, int cameraOffsetX) {
        Graphics2D g2 = (Graphics2D) g;
        Object prevHint = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        for (Spike s : spikes) {
            int drawX = s.getX() - cameraOffsetX;
            int drawY = s.getY();
            
            // soft shadow to give depth
            g2.setColor(new Color(0, 0, 0, 90));
            int shadowW = (int)(Spike.W * 0.6f);
            int shadowH = Math.max(2, Spike.H / 6);
            int shadowX = drawX + (Spike.W - shadowW) / 2;
            int shadowY = drawY + Spike.H - (shadowH / 2);
            g2.fillOval(shadowX, shadowY, shadowW, shadowH);

            if (spikeImg != null) {
                g2.drawImage(spikeImg, drawX, drawY, Spike.W, Spike.H, null);
            } else {
                // Fallback: draw a simple triangle if sprite missing
                int[] xs = { drawX, drawX + Spike.W/2, drawX + Spike.W };
                int[] ys = { drawY + Spike.H, drawY, drawY + Spike.H };
                g2.setColor(Color.GRAY);
                g2.fillPolygon(xs, ys, 3);
            }
        }

        // Restore previous hint safely
        if (prevHint != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, prevHint);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        }
    }

    // Expose spikes so other systems (coins, etc.) can avoid them.
    public List<Spike> getSpikes() {
        return Collections.unmodifiableList(spikes);
    }
}