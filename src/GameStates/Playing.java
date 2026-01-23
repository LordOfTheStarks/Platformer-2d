package GameStates;

import Entities.Boss;
import Entities.EnemyManager;
import Entities.Player;
import Main.Game;
import levels.BoxManager;
import levels.LevelManager;
import levels.SpikeManager;
import ui.GoldUI;
import ui.HeartsUI;
import ui.DeathOverlay;
import ui.VictoryOverlay;

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
    private BoxManager boxManager;
    private CoinManager coinManager;
    private levels.HeartManager heartManager;
    private PauseOverlay pauseOverlay;
    private DeathOverlay deathOverlay;
    private VictoryOverlay victoryOverlay;
    private boolean paused;

    // Boss system
    private Boss boss;
    private boolean bossDefeated = false;

    private boolean bossIntroActive = false;
    private long bossIntroStartMs = 0;
    private static final long BOSS_INTRO_DURATION_MS = 4000;

    private int gold = 0;
    private final GoldUI goldUI = new GoldUI();
    private final HeartsUI heartsUI = new HeartsUI();

    // Enemy contact damage cooldown (also used for spike damage)
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

    // Background music started flag
    private boolean musicStarted = false;

    // Developer tools
    private boolean devImmunity = false;  // Toggle with F1 - makes player immune to damage

    // Start screen (controls)
    private boolean showControlsScreen = true;

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

        boxManager = new BoxManager();
        boxManager.spawnForLevel(levelManager.getCurrentLevel(), spikeManager);

        coinManager = new CoinManager();
        // ensure coins do not spawn on spikes
        coinManager.spawnForLevel(levelManager.getCurrentLevel(), spikeManager);

        heartManager = new levels.HeartManager();
        heartManager.spawnForLevel(levelManager.getCurrentLevel(), spikeManager);

        pauseOverlay = new PauseOverlay(game);
        deathOverlay = new DeathOverlay(game);
        victoryOverlay = new VictoryOverlay(game);

        // Boss starts as null (spawned when entering boss level)
        boss = null;
        bossDefeated = false;

        // initialize trackers
        prevPlayerBottom = playerBottom();
        prevInAir = player.isInAir();

        // Start background music
        if (!musicStarted) {
            util.SoundManager.startBackgroundMusic();
            musicStarted = true;
        }
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
        // Show controls screen at game start
        if (showControlsScreen) {
            return;
        }

        // If victory overlay active, only update overlay
        if (victoryOverlay.isActive()) {
            victoryOverlay.update();
            return;
        }

        // If death overlay active, only update overlay (freeze game world)
        if (deathOverlay.isActive()) {
            deathOverlay.update();
            return;
        }

        levelManager.update();
        player.update();

        // Update camera position to follow player
        updateCamera();

        // Handle boss level differently
        if (levelManager.isBossLevel()) {
            if (bossIntroActive) {
                updateBossIntro();
            } else {
                updateBossLevel();
            }
        } else {
            enemyManager.update();
            enemyContactDamageCheck();
        }

        spikeManagerUpdateAndCheck();

        // Check player attack collisions (enemies, boss, boxes)
        if (player.isAttacking()) {
            Rectangle2D.Float attackHitbox = player.getAttackHitbox();
            if (!levelManager.isBossLevel()) {
                enemyManager.checkPlayerAttackCollision(attackHitbox);
            }
            if (boss != null && !boss.isDying() && attackHitbox != null && boss.getHitBox().intersects(attackHitbox)) {
                boss.takeDamage(1);
            }
            if (boxManager != null) {
                boxManager.checkPlayerAttackCollision(attackHitbox, coinManager, heartManager);
            }
        }

        pauseOverlay.update();

        // update and collect coins
        coinManager.update();
        int collected = coinManager.collectIfPlayerTouches(player.getHitBox());
        if (collected > 0) addGold(collected);

        // update and collect hearts
        heartManager.update();
        int heartsCollected = heartManager.collectIfPlayerTouches(player.getHitBox());
        if (heartsCollected > 0) {
            player.healHearts(heartsCollected);
        }

        goldUI.update();
        heartsUI.update();

        handleBorderTransitions();
        handlePitDeath();

        // update previous-bottom and prevInAir trackers for next frame
        prevPlayerBottom = playerBottom();
        prevInAir = player.isInAir();
    }

    /**
     * Update logic specific to the boss level.
     */
    private void updateBossLevel() {
        // Spawn boss if not already spawned
        if (boss == null && !bossDefeated) {
            spawnBoss();
        }

        // Update boss
        if (boss != null) {
            boss.setPlayerHitBox(player.getHitBox());
            boss.update();

            // Check if boss is dead
            if (boss.isDead()) {
                bossDefeated = true;
                boss = null;
                triggerVictory();
                return;
            }

            // Check boss damage to player
            long now = System.currentTimeMillis();

            // Boss contact damage (1 heart)
//            if (!boss.isDying() && boss.collidesWithPlayer(player.getHitBox())) {
//                applyDamageToPlayer(1, now);
//            }

            // Boss projectile damage (1 heart each)
            int projectileDamage = boss.checkProjectilePlayerCollision(player.getHitBox());
            if (projectileDamage > 0) {
                applyDamageToPlayer(projectileDamage, now);
            }
        }
    }

    /**
     * Spawn the boss in the boss arena.
     */
    private void spawnBoss() {
        int[][] levelData = levelManager.getCurrentLevel().getLevelData();

        int levelWidthPx = levelManager.getCurrentLevel().getLevelWidth() * TILES_SIZE;

        int bossW = (int)(64 * SCALE);
        int bossH = (int)(64 * SCALE);

        int bossX = (levelWidthPx / 2) - (bossW / 2);

        int groundY = (TILES_HEIGHT - 2) * TILES_SIZE;
        int bossY = groundY - bossH - (int)(40 * SCALE);

        boss = new Boss(bossX, bossY, bossW, bossH, levelData);
    }



    /**
     * Trigger victory when boss is defeated.
     */
    private void triggerVictory() {
        victoryOverlay.activate();
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

        // Clamp camera to level boundaries (handle short levels)
        int maxCameraX = Math.max(0, levelWidth - GAME_WIDTH);
        cameraOffsetX = Math.max(0, Math.min(desiredCameraX, maxCameraX));
    }

    private void enemyContactDamageCheck() {
        long now = System.currentTimeMillis();

        // Check contact damage from enemies
        if (enemyManager.collidesWithPlayer(player.getHitBox())) {
            applyDamageToPlayer(1, now);
        }

        // Check projectile damage
        int projectileDamage = enemyManager.checkProjectilePlayerCollision(player.getHitBox());
        if (projectileDamage > 0) {
            applyDamageToPlayer(projectileDamage, now);
        }
    }

    /**
     * Apply damage to the player with cooldown check.
     * Respects dev immunity mode.
     */
    private void applyDamageToPlayer(int damage, long currentTime) {
        // Skip damage if dev immunity is active
        if (devImmunity) return;

        if (currentTime - lastDamageMs > damageCooldownMs) {
            player.takeHeartDamage(damage);
            lastDamageMs = currentTime;
            if (player.getHearts() <= 0 && !playerDead) {
                triggerDeath();
            }
        }
    }

    private void spikeManagerUpdateAndCheck() {
        // Skip spike damage if dev immunity is active
        if (devImmunity) return;

        long now = System.currentTimeMillis();
        if (spikeManager.isPlayerOnSpike(player.getHitBox())) {
            if (now - lastDamageMs > damageCooldownMs) {
                player.takeHeartDamage(1);
                lastDamageMs = now;
                if (player.getHearts() <= 0 && !playerDead) {
                    triggerDeath();
                }
            }
        }
    }

    private void handleBorderTransitions() {
        // Don't allow transition out of boss arena unless boss is defeated
        if (levelManager.isBossLevel()) {
            // Boss level has walls - no transition
            return;
        }

        // Get the level width
        int levelWidth = levelManager.getCurrentLevel().getLevelWidth() * TILES_SIZE;

        // Transition when player reaches near the right edge of the level
        int threshold = levelWidth - (TILES_SIZE / 4);
        if (playerRight() >= threshold) {
            if (!levelManager.isLastLevel()) {
                levelManager.nextLevel();
                player.loadLevelData(levelManager.getCurrentLevel().getLevelData());

                // Check if entering boss level
                if (levelManager.isBossLevel()) {
                    // Clear regular enemies for boss level
                    enemyManager = new EnemyManager(); // Reset to empty
                    // Spawn extra hearts for boss fight (3 hearts as required)
                    heartManager.spawnBossArenaHearts(levelManager.getCurrentLevel(), spikeManager);
                    // Boss will be spawned in updateBossLevel()
                    boss = null;
                    bossDefeated = false;
                    // Use special spawn position for boss arena (avoid left wall)
                    setBossArenaPlayerStart();

                    player.resetHeartsToFull();
                    bossIntroActive = true;
                    bossIntroStartMs = System.currentTimeMillis();
                    boss = null;
                    bossDefeated = false;

                } else {
                    enemyManager.spawnForLevel(levelManager.getCurrentLevel());
                    heartManager.spawnForLevel(levelManager.getCurrentLevel(), spikeManager);
                    setPlayerLeftStart();
                }

                spikeManager.spawnForLevel(levelManager.getCurrentLevel());
                coinManager.spawnForLevel(levelManager.getCurrentLevel(), spikeManager);
                boxManager.spawnForLevel(levelManager.getCurrentLevel(), spikeManager);
                cameraOffsetX = 0; // Reset camera to start of new level
            } else {
                GameState.state = GameState.MENU;
            }
        }
    }

    /**
     * Handle death when player falls off the map (through gaps or below screen).
     * Works on all levels - triggers death when player falls below the visible area.
     */
    private void handlePitDeath() {
        if (playerDead) return;

        // Skip pit death if dev immunity is active
        if (devImmunity) return;

        // Death threshold: past the very bottom of the visible game area
        // GAME_HEIGHT is the full height, so death triggers just past that
        int deathThreshold = GAME_HEIGHT + 10;

        if (playerBottom() > deathThreshold) {
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

    /**
     * Set player spawn position for boss arena.
     * Spawns player to the right of the left wall (tiles 0-1 are walls).
     */
    private void setBossArenaPlayerStart() {
        // Constants for boss arena spawn position
        final int BOSS_ARENA_SPAWN_X_TILE = 3;        // Past the wall at tiles 0-1
        final int BOSS_ARENA_SPAWN_Y_OFFSET_TILES = 4; // Offset from bottom of arena

        Rectangle2D.Float hb = player.getHitBox();
        hb.x = (int) (BOSS_ARENA_SPAWN_X_TILE * TILES_SIZE + 10 * SCALE);
        hb.y = (int) ((TILES_HEIGHT - BOSS_ARENA_SPAWN_Y_OFFSET_TILES) * TILES_SIZE);
    }

    private void triggerDeath() {
        playerDead = true;
        deathOverlay.activate();
        // Play death sound
        util.SoundManager.play(util.SoundManager.SoundEffect.PLAYER_DEATH);
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
        boxManager.spawnForLevel(levelManager.getCurrentLevel(), spikeManager);
        heartManager.spawnForLevel(levelManager.getCurrentLevel(), spikeManager);
        setPlayerLeftStart();
        player.resetHeartsToFull();
        player.resetBooleans();

        // Reset boss state
        boss = null;
        bossDefeated = false;

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
        boxManager.draw(g, cameraOffsetX);
        // draw coins and hearts under player (so player appears above)
        coinManager.draw(g, cameraOffsetX);
        heartManager.draw(g, cameraOffsetX);
        player.render(g, cameraOffsetX);

        // Draw enemies or boss depending on level
        if (levelManager.isBossLevel()) {
            if (boss != null) {
                boss.render(g, cameraOffsetX);
            }
            // Draw "BOSS ARENA" indicator
            drawBossArenaIndicator(g);

            if (bossIntroActive) {
                drawBossIntroText(g);
            }

        } else {
            enemyManager.draw(g, cameraOffsetX);
        }

        goldUI.draw(g, gold);
        heartsUI.draw(g, player.getHearts(), player.getMaxHearts());

        // Draw dev immunity indicator if active
        if (devImmunity) {
            drawDevModeIndicator(g);
        }

        if (paused) {
            pauseOverlay.draw(g);
        }

        // Draw death overlay on top of everything if active
        if (deathOverlay.isActive()) {
            deathOverlay.draw(g);
        }

        // Draw victory overlay on top of everything if active
        if (victoryOverlay.isActive()) {
            victoryOverlay.draw(g);
        }

        // Show controls screen at game start
        if (showControlsScreen) {
            drawControlsScreen(g);
        }

    }

    /**
     * Draw the boss arena indicator at the top of the screen.
     */
    private void drawBossArenaIndicator(Graphics g) {
        if (bossDefeated) return;

        Graphics2D g2 = (Graphics2D) g;

        // Background bar
        g2.setColor(new Color(50, 0, 50, 200));
        int barWidth = (int)(300 * SCALE);
        int barHeight = (int)(30 * SCALE);
        int barX = (GAME_WIDTH - barWidth) / 2;
        int barY = (int)(10 * SCALE);
        g2.fillRect(barX, barY, barWidth, barHeight);

        // Border
        g2.setColor(new Color(150, 50, 150));
        g2.drawRect(barX, barY, barWidth, barHeight);

        // Text
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, (int)(16 * SCALE)));
        String text = "BOSS ARENA";
        FontMetrics fm = g2.getFontMetrics();
        int textX = barX + (barWidth - fm.stringWidth(text)) / 2;
        int textY = barY + (barHeight + fm.getAscent()) / 2 - 2;
        g2.drawString(text, textX, textY);
    }

    /**
     * Draw the developer mode indicator when immunity is active.
     */
    private void drawDevModeIndicator(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        // Draw in top-right corner
        int padding = (int)(10 * SCALE);
        int boxWidth = (int)(150 * SCALE);
        int boxHeight = (int)(25 * SCALE);
        int boxX = GAME_WIDTH - boxWidth - padding;
        int boxY = padding;

        // Semi-transparent green background
        g2.setColor(new Color(0, 150, 0, 180));
        g2.fillRect(boxX, boxY, boxWidth, boxHeight);

        // Border
        g2.setColor(new Color(0, 255, 0));
        g2.drawRect(boxX, boxY, boxWidth, boxHeight);

        // Text
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, (int)(12 * SCALE)));
        String text = "DEV: IMMUNITY ON";
        FontMetrics fm = g2.getFontMetrics();
        int textX = boxX + (boxWidth - fm.stringWidth(text)) / 2;
        int textY = boxY + (boxHeight + fm.getAscent()) / 2 - 2;
        g2.drawString(text, textX, textY);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // Handle victory overlay
        if (victoryOverlay.isActive()) {
            if (victoryOverlay.canContinue() && e.getButton() == MouseEvent.BUTTON1) {
                victoryOverlay.continueToMenu();
                resetGameState();
            }
            return;
        }

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
        if (showControlsScreen) {
            showControlsScreen = false;
            return;
        }

        // Handle victory overlay
        if (victoryOverlay.isActive()) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER && victoryOverlay.canContinue()) {
                victoryOverlay.continueToMenu();
                resetGameState();
            }
            return;
        }

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
            // Developer tools
            case KeyEvent.VK_F1 -> {
                devImmunity = !devImmunity;
                System.out.println("[DEV] Immunity " + (devImmunity ? "ENABLED" : "DISABLED"));
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (victoryOverlay.isActive() || deathOverlay.isActive()) {
            // ignore other keys while in overlays
            return;
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> player.setLeft(false);
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> player.setRight(false);
            case KeyEvent.VK_SPACE -> player.setJump(false);
        }
    }

    /**
     * Reset game state after victory or when returning to menu.
     */
    private void resetGameState() {
        gold = 0;
        boss = null;
        bossDefeated = false;
        levelManager.resetToFirstLevel();
        player.loadLevelData(levelManager.getCurrentLevel().getLevelData());
        enemyManager.spawnForLevel(levelManager.getCurrentLevel());
        spikeManager.spawnForLevel(levelManager.getCurrentLevel());
        coinManager.spawnForLevel(levelManager.getCurrentLevel(), spikeManager);
        boxManager.spawnForLevel(levelManager.getCurrentLevel(), spikeManager);
        heartManager.spawnForLevel(levelManager.getCurrentLevel(), spikeManager);
        setPlayerLeftStart();
        player.resetHeartsToFull();
        player.resetBooleans();
        cameraOffsetX = 0;
        playerDead = false;
    }

    private void updateBossIntro() {
        long now = System.currentTimeMillis();
        if (now - bossIntroStartMs >= BOSS_INTRO_DURATION_MS) {
            bossIntroActive = false;
        }
    }

    private void drawBossIntroText(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        String text = "GET READY FOR THE BOSS!";
        g2.setFont(new Font("SansSerif", Font.BOLD, (int)(32 * SCALE)));
        FontMetrics fm = g2.getFontMetrics();

        int x = (GAME_WIDTH - fm.stringWidth(text)) / 2;
        int y = (GAME_HEIGHT / 2);

        g2.setColor(Color.BLACK);
        g2.drawString(text, x + 2, y + 2);

        g2.setColor(Color.WHITE);
        g2.drawString(text, x, y);

        g2.dispose();
    }

    private void drawControlsScreen(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        // Background
        g2.setColor(new Color(0, 0, 0, 220));
        g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        g2.setColor(Color.WHITE);

        int centerX = GAME_WIDTH / 2;
        int y = (int)(120 * SCALE);

        g2.setFont(new Font("SansSerif", Font.BOLD, (int)(28 * SCALE)));
        drawCenteredText(g2, "CONTROLS", centerX, y);
        y += 50 * SCALE;

        g2.setFont(new Font("SansSerif", Font.PLAIN, (int)(18 * SCALE)));
        drawCenteredText(g2, "A / D  - Move", centerX, y); y += 28 * SCALE;
        drawCenteredText(g2, "SPACE  - Jump", centerX, y); y += 28 * SCALE;
        drawCenteredText(g2, "LMB  - Attack", centerX, y); y += 28 * SCALE;
        drawCenteredText(g2, "ESC  - Pause", centerX, y); y += 50 * SCALE;

        g2.setFont(new Font("SansSerif", Font.BOLD, (int)(20 * SCALE)));
        drawCenteredText(g2, "Press any key to start", centerX, y);

        g2.dispose();
    }

    private void drawCenteredText(Graphics2D g2, String text, int centerX, int y) {
        FontMetrics fm = g2.getFontMetrics();
        int x = centerX - fm.stringWidth(text) / 2;
        g2.drawString(text, x, y);
    }


}
