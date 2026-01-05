package GameStates;

import Entities.Player;
import Main.Game;
import levels.LevelManager;
import ui.PauseOverlay;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import static Main.Game.*;

public class Playing extends State implements StateMethods{
    private Player player;
    private LevelManager levelManager;
    private PauseOverlay pauseOverlay;
    private boolean paused;

    public Playing(Game game) {
        super(game);
        init();
    }

    private void init() {
        levelManager = new LevelManager(game);
        player = new Player(200,200,(int)(62.5*SCALE),(int)(46.25*SCALE));
        player.loadLevelData(levelManager.getCurrentLevel().getLevelData());
        pauseOverlay = new PauseOverlay(game);
    }

    public void windowFocusLost() {
        player.resetBooleans();
    }
    public Player getPlayer(){
        return player;
    }

    public boolean isPaused() {
        return paused;
    }
    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    @Override
    public void update() {
        levelManager.update();
        player.update();

        pauseOverlay.update();
    }

    @Override
    public void draw(Graphics g) {
        levelManager.draw(g);
        player.render(g);
        if(paused){
            pauseOverlay.draw(g);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(e.getButton()==MouseEvent.BUTTON1){
            player.setAttacking(true);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if(paused)
            pauseOverlay.mouseReleased(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if(paused)
            pauseOverlay.mousePressed(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if(paused)
            pauseOverlay.mouseMoved(e);
    }

    // Added to support slider dragging while paused
    public void mouseDragged(MouseEvent e) {
        if (paused)
            pauseOverlay.mouseDragged(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> System.out.println("W key");
            case KeyEvent.VK_A ->{
                player.setLeft(true);
                System.out.println("A key");
            }
            case KeyEvent.VK_D -> {
                player.setRight(true);
                System.out.println("D key");
            }
            case KeyEvent.VK_S -> System.out.println("S key");
            case KeyEvent.VK_SPACE -> {
                player.setJump(true);
                System.out.println("Jump key");
            }
            case KeyEvent.VK_ESCAPE -> {
                paused = !paused;
                System.out.println("Escape key");
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A -> player.setLeft(false);
            case KeyEvent.VK_D -> player.setRight(false);
            case KeyEvent.VK_SPACE -> player.setJump(false);
        }
    }
}