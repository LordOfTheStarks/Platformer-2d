package ui;

import Main.Game;
import GameStates.GameState;
import java.awt.*;

/**
 * Victory overlay displayed when the boss is defeated.
 * Shows "WELL DONE!" message and prompts player to continue.
 */
public class VictoryOverlay {
    private final Game game;

    private boolean active = false;
    private long activatedAt = 0L;
    
    /** Minimum time to show before allowing continue (milliseconds) */
    private static final long MIN_SHOW_DURATION_MS = 2000L;

    private final Font bigFont;
    private final Font mediumFont;
    private final Font smallFont;
    
    // Animation variables
    private int animTick = 0;
    private float pulseScale = 1.0f;
    private int starParticleCount = 50;
    private float[] starX;
    private float[] starY;
    private float[] starSpeed;
    private float[] starSize;

    public VictoryOverlay(Game game) {
        this.game = game;
        bigFont = new Font("Serif", Font.BOLD, (int)(50 * Game.SCALE));
        mediumFont = new Font("SansSerif", Font.BOLD, (int)(24 * Game.SCALE));
        smallFont = new Font("SansSerif", Font.PLAIN, (int)(16 * Game.SCALE));
        
        // Initialize star particles
        starX = new float[starParticleCount];
        starY = new float[starParticleCount];
        starSpeed = new float[starParticleCount];
        starSize = new float[starParticleCount];
        initStars();
    }
    
    private void initStars() {
        for (int i = 0; i < starParticleCount; i++) {
            starX[i] = (float)(Math.random() * Game.GAME_WIDTH);
            starY[i] = (float)(Math.random() * Game.GAME_HEIGHT);
            starSpeed[i] = (float)(1 + Math.random() * 3);
            starSize[i] = (float)(2 + Math.random() * 4);
        }
    }

    public void activate() {
        active = true;
        activatedAt = System.currentTimeMillis();
        initStars();
    }

    public void deactivate() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    public boolean canContinue() {
        return System.currentTimeMillis() - activatedAt >= MIN_SHOW_DURATION_MS;
    }

    public void update() {
        if (!active) return;
        
        animTick++;
        pulseScale = 1.0f + 0.1f * (float)Math.sin(animTick * 0.05);
        
        // Update star particles (falling effect)
        for (int i = 0; i < starParticleCount; i++) {
            starY[i] += starSpeed[i];
            if (starY[i] > Game.GAME_HEIGHT) {
                starY[i] = 0;
                starX[i] = (float)(Math.random() * Game.GAME_WIDTH);
            }
        }
    }

    public void draw(Graphics g) {
        if (!active) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = Game.GAME_WIDTH;
        int h = Game.GAME_HEIGHT;

        // Golden gradient background
        GradientPaint gradient = new GradientPaint(
            0, 0, new Color(20, 10, 40, 230),
            0, h, new Color(40, 20, 60, 230)
        );
        g2.setPaint(gradient);
        g2.fillRect(0, 0, w, h);
        
        // Draw star particles
        for (int i = 0; i < starParticleCount; i++) {
            int alpha = 150 + (int)(Math.random() * 105);
            g2.setColor(new Color(255, 215, 0, alpha)); // Gold stars
            int size = (int)starSize[i];
            g2.fillOval((int)starX[i], (int)starY[i], size, size);
        }

        // "WELL DONE!" text with golden glow
        String title = "WELL DONE!";
        g2.setFont(bigFont);
        FontMetrics fm = g2.getFontMetrics();
        int titleW = fm.stringWidth(title);
        int titleX = (w - (int)(titleW * pulseScale)) / 2;
        int titleY = h / 3;

        // Glow effect
        for (int i = 3; i >= 1; i--) {
            g2.setColor(new Color(255, 200, 0, 40 * i));
            g2.setFont(bigFont.deriveFont(bigFont.getSize2D() * pulseScale));
            FontMetrics fmScaled = g2.getFontMetrics();
            int scaledW = fmScaled.stringWidth(title);
            int scaledX = (w - scaledW) / 2;
            for (int dx = -i; dx <= i; dx++) {
                for (int dy = -i; dy <= i; dy++) {
                    g2.drawString(title, scaledX + dx * 2, titleY + dy * 2);
                }
            }
        }

        // Main title (golden)
        g2.setFont(bigFont.deriveFont(bigFont.getSize2D() * pulseScale));
        FontMetrics fmScaled = g2.getFontMetrics();
        int scaledW = fmScaled.stringWidth(title);
        int scaledX = (w - scaledW) / 2;
        g2.setColor(new Color(255, 215, 0)); // Gold
        g2.drawString(title, scaledX, titleY);
        
        // Victory message
        String message = "You have defeated the Boss!";
        g2.setFont(mediumFont);
        FontMetrics fm2 = g2.getFontMetrics();
        int msgW = fm2.stringWidth(message);
        int msgX = (w - msgW) / 2;
        int msgY = titleY + fm.getHeight() + 20;
        
        g2.setColor(Color.WHITE);
        g2.drawString(message, msgX, msgY);
        
        // Trophy/crown icon (simple representation)
        int iconSize = (int)(40 * Game.SCALE);
        int iconX = w / 2 - iconSize / 2;
        int iconY = msgY + 30;
        
        // Crown shape
        g2.setColor(new Color(255, 215, 0));
        int[] crownXPoints = {iconX, iconX + iconSize/4, iconX + iconSize/2, iconX + 3*iconSize/4, iconX + iconSize,
                              iconX + iconSize, iconX};
        int[] crownYPoints = {iconY + iconSize/2, iconY, iconY + iconSize/2, iconY, iconY + iconSize/2,
                              iconY + iconSize, iconY + iconSize};
        g2.fillPolygon(crownXPoints, crownYPoints, 7);
        
        // Gems on crown
        g2.setColor(Color.RED);
        g2.fillOval(iconX + iconSize/4 - 5, iconY + 5, 10, 10);
        g2.setColor(Color.BLUE);
        g2.fillOval(iconX + iconSize/2 - 5, iconY + iconSize/3, 10, 10);
        g2.setColor(Color.RED);
        g2.fillOval(iconX + 3*iconSize/4 - 5, iconY + 5, 10, 10);

        // Prompt text
        String hint = canContinue() ? "Press ENTER or click to return to menu" : "Victory!";
        g2.setFont(smallFont);
        FontMetrics fm3 = g2.getFontMetrics();
        int hintW = fm3.stringWidth(hint);
        int hintX = (w - hintW) / 2;
        int hintY = h - h / 4;

        // Blinking effect for prompt
        if (canContinue() && (animTick / 30) % 2 == 0) {
            g2.setColor(Color.WHITE);
        } else {
            g2.setColor(new Color(200, 200, 200));
        }
        g2.drawString(hint, hintX, hintY);

        g2.dispose();
    }
    
    /**
     * Handle continue action - returns to menu.
     */
    public void continueToMenu() {
        if (canContinue()) {
            deactivate();
            GameState.state = GameState.MENU;
        }
    }
}
