package levels;

import Main.Game;
import util.LoadSave;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class SpikeManager {
    private BufferedImage[] spikeImgs; // 4 variants 16x16
    private final List<Spike> spikes = new ArrayList<>();

    public SpikeManager() {
        loadImages();
    }

    private void loadImages() {
        BufferedImage atlas = LoadSave.getAtlas(LoadSave.SPIKES); // 64x16
        spikeImgs = new BufferedImage[4];
        for (int i = 0; i < 4; i++) {
            spikeImgs[i] = atlas.getSubimage(i * 16, 0, 16, 16);
        }
    }

    public void clear() {
        spikes.clear();
    }

    public void spawnForLevel(Level level) {
        spikes.clear();
        int[][] data = level.getLevelData();

        // Choose a few x tile columns and place spikes on ground.
        int[] xTiles = {6, 10, 16, 21}; // tweak as desired per level layout
        for (int i = 0; i < xTiles.length; i++) {
            int xt = xTiles[i];
            int groundYTile = findGroundYTile(data, xt);
            if (groundYTile != -1) {
                // Pixel positions: center spike horizontally on the tile; bottom on ground top
                int px = xt * Game.TILES_SIZE + (Game.TILES_SIZE - Spike.W) / 2;
                int py = groundYTile * Game.TILES_SIZE - Spike.H; // bottom aligns with ground top
                int type = i % 4; // cycle through 4 spike variants
                spikes.add(new Spike(px, py, type));
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

    public void draw(Graphics g) {
        for (Spike s : spikes) {
            BufferedImage img = spikeImgs[s.getType()];
            g.drawImage(img, s.getX(), s.getY(), Spike.W, Spike.H, null);
        }
    }
}