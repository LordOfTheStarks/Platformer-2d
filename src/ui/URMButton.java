package ui;

import java.awt.*;
import java.awt.image.BufferedImage;

import static util.LoadSave.URM_BUTTONS;
import static util.LoadSave.getAtlas;
import static util.Constants.UI.URMButton.*;

public class URMButton extends PauseButtons {
    // 3 rows: play, menu, quit; 3 cols: normal, hover, pressed
    private BufferedImage[][] imgs;
    private int rowIndex;
    private int colIndex;
    private boolean mouseOver, mousePressed;

    public URMButton(int x, int y, int rowIndex) {
        super(x, y, URM_SIZE, URM_SIZE);
        this.rowIndex = rowIndex;
        loadImages();
    }

    private void loadImages() {
        BufferedImage atlas = getAtlas(URM_BUTTONS);
        int tileW = atlas.getWidth() / 3;
        int tileH = atlas.getHeight() / 3;

        imgs = new BufferedImage[3][3];
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                imgs[r][c] = atlas.getSubimage(c * tileW, r * tileH, tileW, tileH);
            }
        }
    }

    public void update() {
        colIndex = 0;
        if (mouseOver) colIndex = 1;
        if (mousePressed) colIndex = 2;
    }

    public void draw(Graphics g) {
        g.drawImage(imgs[rowIndex][colIndex], x, y, width, height, null);
    }

    public void setMouseOver(boolean mouseOver) {
        this.mouseOver = mouseOver;
    }

    public void setMousePressed(boolean mousePressed) {
        this.mousePressed = mousePressed;
    }

    public boolean isMousePressed() {
        return mousePressed;
    }

    public void reset() {
        mouseOver = false;
        mousePressed = false;
    }
}