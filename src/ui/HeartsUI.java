package ui;

import Main.Game;
import util.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;

public class HeartsUI {
    private BufferedImage heartEmpty;
    private BufferedImage[] heartFullFrames;

    // Animation
    private int frameIndex = 0;
    private int tick = 0;
    private final int animSpeed = 10; // ticks per frame

    // Drawing
    private final int margin = (int)(8 * Game.SCALE);
    private final int heartSize = (int)(16 * Game.SCALE);
    private final int spacing = (int)(4 * Game.SCALE);

    public HeartsUI(){
        loadHearts();
    }

    private void loadHearts(){
        heartEmpty = LoadSave.getAtlas(LoadSave.HEART_EMPTY);
        BufferedImage sheet = LoadSave.getAtlas(LoadSave.HEART_FULL_SHEET);

        // 96x16 => 6 frames of 16x16
        heartFullFrames = new BufferedImage[6];
        for (int i = 0; i < 6; i++) {
            heartFullFrames[i] = sheet.getSubimage(i * 16, 0, 16, 16);
        }
    }

    public void update(){
        tick++;
        if (tick >= animSpeed) {
            tick = 0;
            frameIndex++;
            if (frameIndex >= heartFullFrames.length) frameIndex = 0;
        }
    }

    // Draw hearts under the gold icon (top-left)
    public void draw(Graphics g, int currentHearts, int maxHearts){
        int coinDrawSize = (int)(20 * Game.SCALE);
        int baseX = margin;
        int baseY = margin + coinDrawSize + (int)(6 * Game.SCALE);

        for (int i = 0; i < maxHearts; i++) {
            int x = baseX + i * (heartSize + spacing);
            if (i < currentHearts) {
                g.drawImage(heartFullFrames[frameIndex], x, baseY, heartSize, heartSize, null);
            } else {
                g.drawImage(heartEmpty, x, baseY, heartSize, heartSize, null);
            }
        }
    }
}