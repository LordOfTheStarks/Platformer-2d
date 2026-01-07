package GameStates;

import Entities.EnemyManager;
import Entities.Player;
import Main.Game;
import levels.LevelManager;
import levels.SpikeManager;
import ui.GoldUI;
import ui.HeartsUI;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

import static Main.Game.*;

public class Playing extends State implements StateMethods{
    private Player player;
    private LevelManager levelManager;
    private EnemyManager enemyManager;
    private SpikeManager spikeManager;
    private ui.PauseOverlay pauseOverlay;
    private boolean paused;

    private int gold = 0;
    private final GoldUI goldUI = new GoldUI();
    private final HeartsUI heartsUI = new HeartsUI();

    // Enemy contact damage cooldown
    private long lastDamageMs = 0;
    private final long damageCooldownMs = 600;

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

        spikeManager = new SpikeManager();
        spikeManager.spawnForLevel(levelManager.getCurrentLevel());

        pauseOverlay = new ui.PauseOverlay(game);
    }

    public void windowFocusLost() {
        player.resetBooleans();
    }
    public Player getPlayer(){
        return player;
    }

    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }

    public int getGold() { return gold; }
    public void addGold(int amount) { if (amount > 0) gold += amount; }
    public boolean spendGold(int amount) { if (amount <= gold) { gold -= amount; return true; } return false; }

    @Override
    public void update() {
        levelManager.update();
        player.update();
        enemyManager.update();
        spikeManagerUpdateAndCheck();
        enemyContactDamageCheck();
        pauseOverlay.update();

        goldUI.update();
        heartsUI.update();

        handleBorderTransitions();
        handlePitDeath();
    }

    private void enemyContactDamageCheck() {
        long now = System.currentTimeMillis();
        if (enemyManager.collidesWithPlayer(player.getHitBox())) {
            if (now - lastDamageMs > damageCooldownMs) {
                player.takeHeartDamage(1);
                lastDamageMs = now;
                if (player.getHearts() <= 0) {
                    // Game over -> return to menu and reset to first level + hearts full
                    levelManager.resetToFirstLevel();
                    player.loadLevelData(levelManager.getCurrentLevel().getLevelData());
                    enemyManager.spawnForLevel(levelManager.getCurrentLevel());
                    spikeManager.spawnForLevel(levelManager.getCurrentLevel());
                    setPlayerLeftStart();
                    player.resetHeartsToFull();
                    GameState.state = GameState.MENU;
                }
            }
        }
    }

    private void spikeManagerUpdateAndCheck() {
        if (spikeManager.isPlayerOnSpike(player.getHitBox())) {
            levelManager.resetToFirstLevel();
            player.loadLevelData(levelManager.getCurrentLevel().getLevelData());
            enemyManager.spawnForLevel(levelManager.getCurrentLevel());
            spikeManager.spawnForLevel(levelManager.getCurrentLevel());
            setPlayerLeftStart();
            player.resetHeartsToFull();
            GameState.state = GameState.MENU;
        }
    }

    private void handleBorderTransitions() {
        int threshold = GAME_WIDTH - (TILES_SIZE / 4);
        if (playerRight() >= threshold) {
            if (!levelManager.isLastLevel()) {
                levelManager.nextLevel();
                player.loadLevelData(levelManager.getCurrentLevel().getLevelData());
                enemyManager.spawnForLevel(levelManager.getCurrentLevel());
                spikeManager.spawnForLevel(levelManager.getCurrentLevel());
                setPlayerLeftStart();
            } else {
                GameState.state = GameState.MENU;
            }
        }
    }

    private void handlePitDeath() {
        if (playerBottom() > GAME_HEIGHT + 200) {
            levelManager.resetToFirstLevel();
            player.loadLevelData(levelManager.getCurrentLevel().getLevelData());
            enemyManager.spawnForLevel(levelManager.getCurrentLevel());
            spikeManager.spawnForLevel(levelManager.getCurrentLevel());
            setPlayerLeftStart();
            player.resetHeartsToFull();
            GameState.state = GameState.MENU;
        }
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
        hb.x = (int)(32 * SCALE);
        hb.y = (int)(100 * SCALE);
    }

    @Override
    public void draw(Graphics g) {
        levelManager.draw(g);
        spikeManager.draw(g);
        player.render(g);
        enemyManager.draw(g);

        goldUI.draw(g, gold);
        heartsUI.draw(g, player.getHearts(), player.getMaxHearts());

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