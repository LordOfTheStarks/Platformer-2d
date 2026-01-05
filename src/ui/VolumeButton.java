package ui;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static util.Constants.UI.VolumeButton.*;
import static util.LoadSave.VOLUME_BUTTONS;
import static util.LoadSave.getAtlas;

public class VolumeButton extends PauseButtons {
    // Master volume [0..1] for both music and sfx
    private float value = 1.0f;

    private int minX, maxX; // track range in screen coords
    private int sliderX;    // knob center X

    private boolean mouseOver, mousePressed;

    // Atlas parts
    private BufferedImage[] knobImgs;   // 3 states: default, hover, pressed
    private BufferedImage trackImg;     // static slide graphic

    public VolumeButton(int x, int y) {
        // Bounds represent the track interaction area
        super(x, y, TRACK_W, TRACK_H);
        loadImages();
        createBounds();

        // Range for knob movement over the track
        minX = x;
        maxX = x + width;
        sliderX = maxX; // start at 100%
    }

    private void loadImages() {
        BufferedImage atlas = getAtlas(VOLUME_BUTTONS);
        // Atlas is 299x44
        // Slice knobs: 3 tiles 28x44 at x=0,28,56
        knobImgs = new BufferedImage[3];
        for (int i = 0; i < 3; i++) {
            int sx = i * 28;
            knobImgs[i] = atlas.getSubimage(sx, 0, 28, 44);
        }
        // Slice track: 215x44 at x=84
        trackImg = atlas.getSubimage(84, 0, 215, 44);
    }

    public void update() {
        // Keep sliderX consistent with current value
        sliderX = (int)(minX + value * (maxX - minX));
    }

    public void draw(Graphics g) {
        // Draw the static track scaled to constants
        g.drawImage(trackImg, x, y, TRACK_W, TRACK_H, null);

        // Determine knob visual state
        int idx = 0;
        if (mouseOver) idx = 1;
        if (mousePressed) idx = 2;

        // Draw knob centered vertically on the track at sliderX
        int knobX = sliderX - KNOB_SIZE / 2;
        int knobY = y + TRACK_H / 2 - KNOB_SIZE / 2;

        g.drawImage(knobImgs[idx], knobX, knobY, KNOB_SIZE, KNOB_SIZE, null);
    }

    public void mousePressed(MouseEvent e) {
        if (bounds.contains(e.getX(), e.getY())) {
            mousePressed = true;
            setValueByMouse(e.getX());
        }
    }

    public void mouseReleased(MouseEvent e) {
        mousePressed = false;
    }

    public void mouseMoved(MouseEvent e) {
        mouseOver = bounds.contains(e.getX(), e.getY());
    }

    public void mouseDragged(MouseEvent e) {
        if (mousePressed) {
            setValueByMouse(e.getX());
        }
    }

    private void setValueByMouse(int mouseX) {
        if (mouseX < minX) mouseX = minX;
        if (mouseX > maxX) mouseX = maxX;
        value = (float)(mouseX - minX) / (float)(maxX - minX);
    }

    public float getValue() {
        return value;
    }
}