package Entities;

import Main.Game;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import static util.Helpmethods.*;

public class Enemy extends Entity {
    private int[][] levelData;
    private float speed = 0.5f * Game.SCALE;
    private boolean movingRight = true;

    public Enemy(float x, float y, int w, int h, int[][] levelData) {
        super(x, y, w, h);
        this.levelData = levelData;
        // Slightly smaller hitbox for more forgiving collisions
        initHitBox(x, y, w - (int)(10*Game.SCALE), h - (int)(10*Game.SCALE));
    }

    public void update() {
        // Simple patrol: move horizontally, turn on wall/edge, apply gravity
        float xSpeed = movingRight ? speed : -speed;

        // Horizontal collision
        if (CanMoveHere(hitBox.x + xSpeed, hitBox.y, hitBox.width, hitBox.height, levelData)) {
            hitBox.x += xSpeed;
        } else {
            movingRight = !movingRight;
        }

        // Edge ahead check: build a thin probe hitbox one pixel ahead
        float probeX = movingRight ? (hitBox.x + hitBox.width + 1) : (hitBox.x - 1);
        Rectangle2D.Float probeHB = new Rectangle2D.Float(probeX, hitBox.y, 1, hitBox.height);

        // If there is no floor directly under the probe, turn around
        if (!IsOnFloor(probeHB, levelData)) {
            movingRight = !movingRight;
        }

        // Gravity: move down if there is space
        if (CanMoveHere(hitBox.x, hitBox.y + 1, hitBox.width, hitBox.height, levelData)) {
            hitBox.y += 1;
        }
    }

    public void render(Graphics g) {
        // Placeholder: draw a red rectangle. Replace with enemy sprites if you have them.
        g.setColor(Color.RED);
        g.fillRect((int)hitBox.x, (int)hitBox.y, (int)hitBox.width, (int)hitBox.height);
    }
}