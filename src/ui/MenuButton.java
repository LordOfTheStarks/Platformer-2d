package ui;

import GameStates.GameState;
import Main.Game;
import util.*;
import util.Constants.*;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static util.Constants.UI.Buttons.*;
import static util.LoadSave.*;

public class MenuButton {
    private int x,y,row,index;
    private int xOffset = B_WIDTH/2;
    private boolean mouseOver,mousePressed;
    private Rectangle buttonHitBox;
    private GameState state;
    private BufferedImage[] buttonImages;
    public MenuButton(int x, int y, int row, GameState state){
       this.x = x;
       this.y = y;
       this.row = row;
       this.state= state;
       loadImages();
       initButtonHitBox();
    }
    private void initButtonHitBox() {
        buttonHitBox = new Rectangle(x-xOffset,y,B_WIDTH,B_HEIGHT);
    }
    private void loadImages() {
        buttonImages = new BufferedImage[3];
        BufferedImage tempImage = LoadSave.getAtlas(BUTTONS);
        for(int i = 0;i<buttonImages.length; i++){
            buttonImages[i] = tempImage.getSubimage(i*B_WIDTH_DEFAULT,row*B_HEIGHT_DEFAULT,B_WIDTH_DEFAULT,B_HEIGHT_DEFAULT);
        }
    }
    public void draw(Graphics g){
          g.drawImage(buttonImages[index],x-xOffset,y,B_WIDTH,B_HEIGHT,null);
    }
    public void update(){
        index=0;
        if(mouseOver){
            index = 1;
        }else if(mousePressed){
            index = 2;
        }
    }

    public void setMouseOver(boolean mouseOver) {
        this.mouseOver = mouseOver;
    }
    public void setMousePressed(boolean mousePressed) {
        this.mousePressed = mousePressed;
    }

    public boolean isMouseOver() {
        return mouseOver;
    }

    public boolean isMousePressed() {
        return mousePressed;
    }

    public Rectangle getButtonHitBox() {
        return buttonHitBox;
    }
    public void applyGameState(){
        GameState.state = state;
    }
    public void resetBooleans(){
        mouseOver = false;
        mousePressed = false;
    }
}
