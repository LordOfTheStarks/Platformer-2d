package ui;

import Main.Game;
import util.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;

public class GoldUI {
    // Animation
    private BufferedImage[] frames;
    private int frameIndex = 0;
    private int tick = 0;
    private final int animSpeed = 12; // ticks per frame

    // Drawing
    private final int margin = (int)(8 * Game.SCALE);
    private final int coinDrawSize = (int)(20 * Game.SCALE); // scale 20x20 to game scale
    private final Font font = new Font("Arial", Font.BOLD, Math.max(12, (int)(16 * Game.SCALE)));
    private final Color fontColor = new Color(245, 235, 130);

    public GoldUI() {
        loadCoinFrames();
    }

    private void loadCoinFrames() {
        BufferedImage sheet = LoadSave.getAtlas(LoadSave.COIN_SHEET);
        // sheet is 180x20 => 9 frames of 20x20
        frames = new BufferedImage[9];
        for (int i = 0; i < 9; i++) {
            frames[i] = sheet.getSubimage(i * 20, 0, 20, 20);
        }
    }

    public void update() {
        tick++;
        if (tick >= animSpeed) {
            tick = 0;
            frameIndex++;
            if (frameIndex >= frames.length) frameIndex = 0;
        }
    }

    public void draw(Graphics g, int goldAmount) {
        // Draw coin icon
        g.drawImage(frames[frameIndex], margin, margin, coinDrawSize, coinDrawSize, null);

        // Draw gold amount to the right of the icon
        int offsetX = margin + coinDrawSize + (int)(8 * Game.SCALE);
        int baselineY = margin + coinDrawSize - (int)(6 * Game.SCALE);

        g.setColor(Color.black); // subtle outline for readability
        g.setFont(font);
        g.drawString(String.valueOf(goldAmount), offsetX + 1, baselineY + 1);

        g.setColor(fontColor);
        g.drawString(String.valueOf(goldAmount), offsetX, baselineY);
    }
}