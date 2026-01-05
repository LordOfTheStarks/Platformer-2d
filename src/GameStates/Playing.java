package GameStates;

import Entities.EnemyManager;
import Entities.Player;
import Main.Game;
import levels.LevelManager;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

import static Main.Game.*;

public class Playing extends State implements StateMethods{
    private Player player;
    private LevelManager levelManager;
    private EnemyManager enemyManager;
    private ui.PauseOverlay pauseOverlay;
    private boolean paused;

    public Playing(Game game) {
        super(game);
        init();
    }

    private void init() {
        levelManager = new LevelManager(game);
        player = new Player(100, 200,(int)(62.5*SCALE),(int)(46.25*SCALE));
        player.loadLevelData(levelManager.getCurrentLevel().getLevelData());

        enemyManager = new EnemyManager();
        enemyManager.spawnForLevel(levelManager.getCurrentLevel());

        pauseOverlay = new ui.PauseOverlay(game);
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
        enemyManager.update();
        pauseOverlay.update();

        handleBorderTransitions();
        handlePitDeath();
    }

    private void handleBorderTransitions() {
        // Go to next level when crossing right border
        if (playerRight() >= GAME_WIDTH) {
            if (!levelManager.isLastLevel()) {
                levelManager.nextLevel();
                // reload data references
                player.loadLevelData(levelManager.getCurrentLevel().getLevelData());
                enemyManager.spawnForLevel(levelManager.getCurrentLevel());
                // place player at left start
                setPlayerLeftStart();
            } else {
                // last level reached: return to menu or loop
                GameState.state = GameState.MENU;
            }
        }
    }

    private void handlePitDeath() {
        // If player falls below the visible game area, game over (return to menu)
        if (playerBottom() > GAME_HEIGHT + 200) {
            levelManager.resetToFirstLevel();
            player.loadLevelData(levelManager.getCurrentLevel().getLevelData());
            enemyManager.spawnForLevel(levelManager.getCurrentLevel());
            setPlayerLeftStart();
            GameState.state = GameState.MENU;
        }
    }

    private int playerLeft() {
        Rectangle2D.Float hb = player.getHitBox();
        return (int) hb.x;
    }

    private int playerRight() {
        Rectangle2D.Float hb = player.getHitBox();
        return (int) (hb.x + hb.width);
    }

    private int playerBottom() {
        Rectangle2D.Float hb = player.getHitBox();
        return (int) (hb.y + hb.height);
    }

    private void setPlayerLeftStart() {
        Rectangle2D.Float hb = player.getHitBox();
        hb.x = (int)(32 * SCALE);  // small margin
        hb.y = (int)(100 * SCALE); // safe height; gravity will settle to floor
    }

    @Override
    public void draw(Graphics g) {
        levelManager.draw(g);       // backgrounds + tiles
        player.render(g);
        enemyManager.draw(g);
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

    public void mouseDragged(MouseEvent e) {
        if (paused)
            pauseOverlay.mouseDragged(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A -> player.setLeft(true);
            case KeyEvent.VK_D -> player.setRight(true);
            case KeyEvent.VK_SPACE -> player.setJump(true);
            case KeyEvent.VK_ESCAPE -> paused = !paused;
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