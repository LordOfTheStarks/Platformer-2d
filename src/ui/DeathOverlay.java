package ui;

import Main.Game;
import java.awt.*;

public class DeathOverlay {
    private final Game game;

    private boolean active = false;
    private long activatedAt = 0L;
    private final long minShowMs = 1200L; // minimum time to show before allowing respawn

    private final Font bigFont;
    private final Font smallFont;

    public DeathOverlay(Game game) {
        this.game = game;
        bigFont = new Font("Serif", Font.BOLD, (int)(40 * Game.SCALE));
        smallFont = new Font("SansSerif", Font.PLAIN, (int)(18 * Game.SCALE));
    }

    public void activate() {
        active = true;
        activatedAt = System.currentTimeMillis();
    }

    public void deactivate() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    public boolean canRespawn() {
        return System.currentTimeMillis() - activatedAt >= minShowMs;
    }

    // Called each frame while overlay active
    public void update() {
        if (!active) return;
        // nothing dynamic yet, but could add anim/timer
    }

    public void draw(Graphics g) {
        if (!active) return;
        Graphics2D g2 = (Graphics2D) g.create();

        int w = Game.GAME_WIDTH;
        int h = Game.GAME_HEIGHT;

        // Dark translucent background
        g2.setColor(new Color(0, 0, 0, 190));
        g2.fillRect(0, 0, w, h);

        // YOU DIED text with a slight red glow
        String title = "YOU DIED";
        g2.setFont(bigFont);
        FontMetrics fm = g2.getFontMetrics();
        int titleW = fm.stringWidth(title);
        int titleX = (w - titleW) / 2;
        int titleY = h / 2 - fm.getHeight();

        // Glow / outline
        g2.setColor(new Color(120, 0, 0, 160));
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                if (dx == 0 && dy == 0) continue;
                g2.drawString(title, titleX + dx, titleY + dy);
            }
        }
        // main text (bright)
        g2.setColor(Color.RED);
        g2.drawString(title, titleX, titleY);

        // Prompt text
        String hint = canRespawn() ? "Press ENTER or click to respawn" : "Respawning soon...";
        g2.setFont(smallFont);
        FontMetrics fm2 = g2.getFontMetrics();
        int hintW = fm2.stringWidth(hint);
        int hintX = (w - hintW) / 2;
        int hintY = titleY + fm.getHeight() + fm2.getHeight() + 8;

        g2.setColor(Color.WHITE);
        g2.drawString(hint, hintX, hintY);

        g2.dispose();
    }
}
