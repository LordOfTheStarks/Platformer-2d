package GameStates;

import Entities.EnemyManager;
import Entities.Player;
import Main.Game;
import levels.LevelManager;
import levels.SpikeManager;
import ui.GoldUI;
import ui.HeartsUI;
import ui.DeathOverlay;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import levels.CoinManager;
import ui.PauseOverlay;

import static Main.Game.*;
public class Playing extends State implements StateMethods {
    private Player player;
    private LevelManager levelManager;
    private EnemyManager enemyManager;
    private SpikeManager spikeManager;
    private CoinManager coinManager;
    private PauseOverlay pauseOverlay;
    private DeathOverlay deathOverlay;
    private boolean paused;

    private int gold = 0;
    private final GoldUI goldUI = new GoldUI();
    private final HeartsUI heartsUI = new HeartsUI();

    // Enemy contact damage cooldown
    private long lastDamageMs = 0;
    private final long damageCooldownMs = 600;

    // When true, player is dead and DeathOverlay is shown.
    private boolean playerDead = false;

    // Track previous-frame bottom for optional velocity-based checks
    private int prevPlayerBottom = 0;

    // Track whether player was in-air last frame — used to detect landing events reliably
    private boolean prevInAir = false;
    
    // Camera system for side-scrolling
    private int cameraOffsetX = 0;

    public Playing(Game game) {
        super(game);
        init();
    }

    private void init() {
        levelManager = new LevelManager(game);
        player = new Player(100, 200, (int) (62.5 * SCALE), (int) (46.25 * SCALE));
        player.loadLevelData(levelManager.getCurrentLevel().getLevelData());

        enemyManager = new EnemyManager();
        enemyManager.spawnForLevel(levelManager.getCurrentLevel());

        spikeManager = new SpikeManager();
        spikeManager.spawnForLevel(levelManager.getCurrentLevel());

        coinManager = new CoinManager();
        // ensure coins do not spawn on spikes
        coinManager.spawnForLevel(levelManager.getCurrentLevel(), spikeManager);

        pauseOverlay = new PauseOverlay(game);
        deathOverlay = new DeathOverlay(game);

        // initialize trackers
        prevPlayerBottom = playerBottom();
        prevInAir = player.isInAir();
    }

    public void windowFocusLost() {
        player.resetBooleans();
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public int getGold() {
        return gold;
    }

    public void addGold(int amount) {
        if (amount > 0) gold += amount;
    }

    public boolean spendGold(int amount) {
        if (amount <= gold) {
            gold -= amount;
            return true;
        }
        return false;
    }

    @Override
    public void update() {
        // If death overlay active, only update overlay (freeze game world)
        if (deathOverlay.isActive()) {
            deathOverlay.update();
            return;
        }

        levelManager.update();
        player.update();
        
        // Update camera position to follow player
        updateCamera();
        
        enemyManager.update();
        spikeManagerUpdateAndCheck();
        enemyContactDamageCheck();
        
        // Check player attack collision with enemies
        if (player.isAttacking()) {
            Rectangle2D.Float attackHitbox = player.getAttackHitbox();
            enemyManager.checkPlayerAttackCollision(attackHitbox);
        }
        
        pauseOverlay.update();

        // update and collect coins
        coinManager.update();
        int collected = coinManager.collectIfPlayerTouches(player.getHitBox());
        if (collected > 0) addGold(collected);

        goldUI.update();
        heartsUI.update();

        handleBorderTransitions();
        handlePitDeath();

        // update previous-bottom and prevInAir trackers for next frame
        prevPlayerBottom = playerBottom();
        prevInAir = player.isInAir();
    }
    
    /**
     * Update camera position to follow the player with smooth scrolling.
     * Camera centers on player horizontally but respects level boundaries.
     */
    private void updateCamera() {
        Rectangle2D.Float playerHB = player.getHitBox();
        int playerCenterX = (int)(playerHB.x + playerHB.width / 2);
        
        // Center camera on player
        int desiredCameraX = playerCenterX - GAME_WIDTH / 2;
        
        // Get level width
        int levelWidth = levelManager.getCurrentLevel().getLevelWidth() * TILES_SIZE;
        
        // Clamp camera to level boundaries
        int maxCameraX = levelWidth - GAME_WIDTH;
        cameraOffsetX = Math.max(0, Math.min(desiredCameraX, maxCameraX));
    }

    private void enemyContactDamageCheck() {
        long now = System.currentTimeMillis();
        if (enemyManager.collidesWithPlayer(player.getHitBox())) {
            if (now - lastDamageMs > damageCooldownMs) {
                player.takeHeartDamage(1);
                lastDamageMs = now;
                if (player.getHearts() <= 0 && !playerDead) {
                    triggerDeath();
                }
            }
        }
    }

    private void spikeManagerUpdateAndCheck() {
        if (spikeManager.isPlayerOnSpike(player.getHitBox()) && !playerDead) {
            triggerDeath();
        }
    }

    private void handleBorderTransitions() {
        // Get the level width
        int levelWidth = levelManager.getCurrentLevel().getLevelWidth() * TILES_SIZE;
        
        // Transition when player reaches near the right edge of the level
        int threshold = levelWidth - (TILES_SIZE / 4);
        if (playerRight() >= threshold) {
            if (!levelManager.isLastLevel()) {
                levelManager.nextLevel();
                player.loadLevelData(levelManager.getCurrentLevel().getLevelData());
                enemyManager.spawnForLevel(levelManager.getCurrentLevel());
                spikeManager.spawnForLevel(levelManager.getCurrentLevel());
                coinManager.spawnForLevel(levelManager.getCurrentLevel(), spikeManager);
                setPlayerLeftStart();
                cameraOffsetX = 0; // Reset camera to start of new level
            } else {
                GameState.state = GameState.MENU;
            }
        }
    }

    /**
     * Pit death handling for level 2:
     * - Only applies in level 2 (index 1).
     * - Only triggers death when the player just landed (prevInAir == true && current inAir == false)
     *   within the pit columns and the landing point is below the platform height.
     */
    private void handlePitDeath() {
        int currentIdx = levelManager.getCurrentLevelIndex();
        if (currentIdx == 1 && !playerDead) {
            Rectangle2D.Float hb = player.getHitBox();
            int centerX = (int) (hb.x + hb.width / 2);
            int xt = centerX / TILES_SIZE;
            int[][] data = levelManager.getCurrentLevel().getLevelData();

            if (data != null && xt >= 0 && xt < Game.TILES_WIDTH) {
                // Pit center derived from LevelFactory.level2 layout
                int pitCenter = Game.TILES_WIDTH / 2;
                int pitLeft = pitCenter - 2;
                int pitRight = pitCenter + 2;

                // Only consider when player's horizontal center is over the pit columns
                if (xt >= pitLeft && xt <= pitRight) {
                    // Platform columns bridging the pit (per LevelFactory.level2)
                    int leftPlatformStart = pitCenter - 4;
                    int leftPlatformEnd = pitCenter - 1;
                    int rightPlatformStart = pitCenter + 1;
                    int rightPlatformEnd = pitCenter + 4;

                    // Find the platform row by scanning those platform columns and
                    // taking the deepest topmost solid among them; that represents platform height.
                    int platformRow = Integer.MIN_VALUE;
                    for (int x = leftPlatformStart; x <= leftPlatformEnd; x++) {
                        if (x < 0 || x >= Game.TILES_WIDTH) continue;
                        for (int y = 0; y < Game.TILES_HEIGHT; y++) {
                            if (data[y][x] != util.LevelFactory.AIR) {
                                if (y > platformRow) platformRow = y;
                                break;
                            }
                        }
                    }
                    for (int x = rightPlatformStart; x <= rightPlatformEnd; x++) {
                        if (x < 0 || x >= Game.TILES_WIDTH) continue;
                        for (int y = 0; y < Game.TILES_HEIGHT; y++) {
                            if (data[y][x] != util.LevelFactory.AIR) {
                                if (y > platformRow) platformRow = y;
                                break;
                            }
                        }
                    }

                    if (platformRow == Integer.MIN_VALUE) {
                        platformRow = Game.TILES_HEIGHT - 1; // fallback
                    }

                    // death threshold (pixels). Add a small margin so jumping over remains safe.
                    int deathThresholdY = platformRow * TILES_SIZE + (TILES_SIZE / 2);

                    int currBottom = playerBottom();
                    boolean currInAir = player.isInAir();

                    // Trigger death only when player was in-air last frame and is now not in-air (landed),
                    // AND the landing is below the death threshold.
                    if (prevInAir && !currInAir && currBottom > deathThresholdY) {
                        triggerDeath();
                        return;
                    } else {
                        // Player hasn't landed into the pit yet → safe
                        return;
                    }
                }
            }
        }

        // Fallback: if player falls far below the bottom of the screen, trigger death
        if (playerBottom() > GAME_HEIGHT + 200 && !playerDead) {
            triggerDeath();
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
        hb.x = (int) (32 * SCALE);
        hb.y = (int) (100 * SCALE);
    }

    private void triggerDeath() {
        playerDead = true;
        deathOverlay.activate();
        // Optionally stop sounds / play death sound here
    }

    private void respawn() {
        // Reset gold to zero on death
        gold = 0;

        // Reset everything to first level and respawn player
        levelManager.resetToFirstLevel();
        player.loadLevelData(levelManager.getCurrentLevel().getLevelData());
        enemyManager.spawnForLevel(levelManager.getCurrentLevel());
        spikeManager.spawnForLevel(levelManager.getCurrentLevel());
        coinManager.spawnForLevel(levelManager.getCurrentLevel(), spikeManager);
        setPlayerLeftStart();
        player.resetHeartsToFull();
        player.resetBooleans();
        
        // Reset camera to beginning
        cameraOffsetX = 0;

        // Clear death overlay and resume
        playerDead = false;
        deathOverlay.deactivate();

        // Reset trackers
        prevPlayerBottom = playerBottom();
        prevInAir = player.isInAir();
    }

    @Override
    public void draw(Graphics g) {
        levelManager.draw(g, cameraOffsetX);
        spikeManager.draw(g, cameraOffsetX);
        // draw coins under player (so player appears above)
        coinManager.draw(g, cameraOffsetX);
        player.render(g, cameraOffsetX);
        enemyManager.draw(g, cameraOffsetX);

        goldUI.draw(g, gold);
        heartsUI.draw(g, player.getHearts(), player.getMaxHearts());

        if (paused) {
            pauseOverlay.draw(g);
        }

        // Draw death overlay on top of everything if active
        if (deathOverlay.isActive()) {
            deathOverlay.draw(g);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (deathOverlay.isActive()) {
            // clicking also triggers respawn (after min time)
            if (deathOverlay.canRespawn() && e.getButton() == MouseEvent.BUTTON1) {
                respawn();
            }
            return;
        }

        if (e.getButton() == MouseEvent.BUTTON1) {
            player.setAttacking(true);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (paused)
            pauseOverlay.mouseReleased(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (paused)
            pauseOverlay.mousePressed(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (paused)
            pauseOverlay.mouseMoved(e);
    }

    public void mouseDragged(MouseEvent e) {
        if (paused)
            pauseOverlay.mouseDragged(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // If death overlay active, pressing Enter respawns (after min show time)
        if (deathOverlay.isActive()) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER && deathOverlay.canRespawn()) {
                respawn();
            }
            return;
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> player.setLeft(true);
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> player.setRight(true);
            case KeyEvent.VK_SPACE -> player.setJump(true);
            case KeyEvent.VK_ESCAPE -> paused = !paused;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (deathOverlay.isActive()) {
            // ignore other keys while dead
            return;
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> player.setLeft(false);
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> player.setRight(false);
            case KeyEvent.VK_SPACE -> player.setJump(false);
        }
    }
}