package Inputs;

import GameStates.GameState;
import Main.GamePanel;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import static util.Constants.Directions.*;

public class KeyboardInputs implements KeyListener {
    private GamePanel gamePanel;

    public KeyboardInputs(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
    }

    @Override
    public void keyTyped(KeyEvent e) { }

    @Override
    public void keyPressed(KeyEvent e) {
        switch(GameState.state){
            case MENU:
                gamePanel.getGame().getMenu().keyPressed(e);
                break;
            case PLAYING:
                gamePanel.getGame().getPlaying().keyPressed(e);
                break;
            case OPTIONS:
                gamePanel.getGame().getOptions().keyPressed(e);
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch(GameState.state){
            case MENU:
                gamePanel.getGame().getMenu().keyReleased(e);
                break;
            case PLAYING:
                gamePanel.getGame().getPlaying().keyReleased(e);
                break;
            case OPTIONS:
                gamePanel.getGame().getOptions().keyReleased(e);
                break;
        }
    }
}