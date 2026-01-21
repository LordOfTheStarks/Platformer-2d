package GameStates;

import Entities.Boss;
import Entities.EnemyManager;
import Entities.Player;
import Main.Game;
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
    private CoinManager coinManager;
    private levels.HeartManager heartManager;
    private PauseOverlay pauseOverlay;
    private DeathOverlay deathOverlay;
    private VictoryOverlay victoryOverlay;
    private boolean paused;
    
    // Boss system
    private Boss boss;
    private boolean bossDefeated = false;

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
            updateBossLevel();
        } else {
            enemyManager.update();
            enemyContactDamageCheck();
        }
        
        spikeManagerUpdateAndCheck();
        
        // Check player attack collision with enemies (non-boss levels)
        if (player.isAttacking() && !levelManager.isBossLevel()) {
            Rectangle2D.Float attackHitbox = player.getAttackHitbox();
            enemyManager.checkPlayerAttackCollision(attackHitbox);
        }
        
        // Check player attack collision with boss
        if (player.isAttacking() && boss != null && !boss.isDying()) {
            Rectangle2D.Float attackHitbox = player.getAttackHitbox();
            if (attackHitbox != null && boss.getHitBox().intersects(attackHitbox)) {
                boss.takeDamage(1);
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
            if (!boss.isDying() && boss.collidesWithPlayer(player.getHitBox())) {
                applyDamageToPlayer(1, now);
            }
            
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
        // Boss spawn position constants (tiles from edge)
        final int BOSS_SPAWN_OFFSET_X_TILES = 10;
        final int BOSS_SPAWN_OFFSET_Y_TILES = 5;
        
        int[][] levelData = levelManager.getCurrentLevel().getLevelData();
        int levelWidth = levelManager.getCurrentLevel().getLevelWidth();
        
        // Spawn boss on the right side of the arena
        int bossX = (levelWidth - BOSS_SPAWN_OFFSET_X_TILES) * TILES_SIZE;
        int bossY = (TILES_HEIGHT - BOSS_SPAWN_OFFSET_Y_TILES) * TILES_SIZE;
        
        int bossW = (int)(60 * SCALE);
        int bossH = (int)(50 * SCALE);
        
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
     */
    private void applyDamageToPlayer(int damage, long currentTime) {
        if (currentTime - lastDamageMs > damageCooldownMs) {
            player.takeHeartDamage(damage);
            lastDamageMs = currentTime;
            if (player.getHearts() <= 0 && !playerDead) {
                triggerDeath();
            }
        }
    }

    private void spikeManagerUpdateAndCheck() {
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
                } else {
                    enemyManager.spawnForLevel(levelManager.getCurrentLevel());
                    heartManager.spawnForLevel(levelManager.getCurrentLevel(), spikeManager);
                }
                
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
            int levelWidth = levelManager.getCurrentLevel().getLevelWidth();

            if (data != null && xt >= 0 && xt < levelWidth) {
                // Pit center derived from LevelFactory.level2 layout (now at tile 30 for 60-tile level)
                int pitCenter = 30;
                int pitLeft = pitCenter - 3;
                int pitRight = pitCenter + 3;

                // Only consider when player's horizontal center is over the pit columns
                if (xt >= pitLeft && xt <= pitRight) {
                    // Platform columns bridging the pit (per LevelFactory.level2)
                    int leftPlatformStart = pitCenter - 5;
                    int leftPlatformEnd = pitCenter - 1;
                    int rightPlatformStart = pitCenter + 1;
                    int rightPlatformEnd = pitCenter + 5;

                    // Find the platform row by scanning those platform columns and
                    // taking the deepest topmost solid among them; that represents platform height.
                    int platformRow = Integer.MIN_VALUE;
                    for (int x = leftPlatformStart; x <= leftPlatformEnd; x++) {
                        if (x < 0 || x >= levelWidth) continue;
                        for (int y = 0; y < Game.TILES_HEIGHT; y++) {
                            if (data[y][x] != util.LevelFactory.AIR) {
                                if (y > platformRow) platformRow = y;
                                break;
                            }
                        }
                    }
                    for (int x = rightPlatformStart; x <= rightPlatformEnd; x++) {
                        if (x < 0 || x >= levelWidth) continue;
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
        } else {
            enemyManager.draw(g, cameraOffsetX);
        }

        goldUI.draw(g, gold);
        heartsUI.draw(g, player.getHearts(), player.getMaxHearts());

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
        heartManager.spawnForLevel(levelManager.getCurrentLevel(), spikeManager);
        setPlayerLeftStart();
        player.resetHeartsToFull();
        player.resetBooleans();
        cameraOffsetX = 0;
        playerDead = false;
    }
}