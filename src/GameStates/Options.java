package GameStates;

import Main.Game;
import ui.PauseOverlay;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class Options extends State implements StateMethods {
    private PauseOverlay overlay;

    public Options(Game game) {
        super(game);
        // Reuse overlay UI (sound, volume, URM buttons)
        overlay = new PauseOverlay(game);
    }

    @Override
    public void update() {
        overlay.update();
    }

    @Override
    public void draw(Graphics g) {
        overlay.draw(g);
    }

    @Override
    public void mouseClicked(MouseEvent e) { }

    @Override
    public void mouseReleased(MouseEvent e) {
        overlay.mouseReleased(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        overlay.mousePressed(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        overlay.mouseMoved(e);
    }

    // Support dragging for volume sliders
    public void mouseDragged(MouseEvent e) {
        overlay.mouseDragged(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // ESC returns to menu
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            GameState.state = GameState.MENU;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) { }
}