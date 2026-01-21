package Entities;

import Main.Game;
import util.LoadSave;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import static util.Constants.PlayerConstants.*;
import static util.Helpmethods.*;

public class Player extends Entity{
    private static ArrayList<BufferedImage[]> animations = new ArrayList<>();

    private int tick,index,speed= 30;
    private int currentAction = RUNNING;
    private boolean moving = false,attacking = false, mirror = false;
    private boolean left,right,jump,inAir = false;

    // Physics
    private float playerSpeed = Game.SCALE;
    private int[][] levelData;
    private float offsetX = 21* Game.SCALE , offsetY = 4*Game.SCALE;
    private float airSpeed = 0;
    private float gravity = 0.04f * Game.SCALE;
    private float jumpSpeed = -2.8f * Game.SCALE; // Increased for better platforming
    private float xSpeed;

    // Double jump
    private int jumpsDone = 0;
    private int maxJumps = 2;

    // NEW: Hearts-based health
    private int maxHearts = 3;
    private int hearts    = 3;

    public Player(float x, float y, int w,int h) {
        super(x, y, w, h);
        loadAnimations();
        initHitBox(x,y,(int)(20*Game.SCALE),(int)(40*Game.SCALE));
    }

    public void update(){
        updatePos();
        updateAnimationTick();
        setAnimation();
    }

    public void render(Graphics g, int cameraOffsetX){
        int drawX = (int)hitBox.x - (int)offsetX - cameraOffsetX;
        int drawY = (int)hitBox.y - (int)offsetY;
        g.drawImage(animations.get(currentAction)[index], drawX, drawY, width, height, null);
        // drawHitBox(g, cameraOffsetX);
    }

    public boolean isInAir() {
        return inAir;
    }

    private void loadAnimations() {
        BufferedImage img1 = LoadSave.getAtlas(LoadSave.PLAYER_ATLAS1);
        BufferedImage img2 = LoadSave.getAtlas(LoadSave.PLAYER_ATLAS2);

        // idle
        BufferedImage[] idle = new BufferedImage[4];
        for(int i =0;i<idle.length;i++){
            idle[i] = img1.getSubimage(i*50,0,50,37);
        }
        animations.add(0,idle);

        // running
        BufferedImage[] running = new BufferedImage[8];
        int runIndex = 0;
        for(int i =0;i<5;i++){
            running[runIndex] = img2.getSubimage((i+2)*50,8*37,50,37);
            runIndex++;
        }
        for(int i=3;i>0;i--){
            running[runIndex] = img2.getSubimage((i+2)*50,8*37,50,37);
            runIndex++;
        }
        animations.add(1,running);

        // attack
        BufferedImage[] attack = new BufferedImage[7];
        for(int i =0;i<7;i++){
            attack[i] = img2.getSubimage(i*50,0,50,37);
        }
        animations.add(2,attack);

        // hurt
        BufferedImage[] hurt = new BufferedImage[12];
        hurt[0] = img2.getSubimage(4*50,4*37,50,37);
        hurt[1] = img2.getSubimage(5*50,4*37,50,37);
        hurt[2] = img2.getSubimage(6*50,4*37,50,37);
        hurt[3] = img2.getSubimage(1*50,5*37,50,37);
        hurt[4] = img2.getSubimage(2*50,5*37,50,37);
        hurt[5] = img2.getSubimage(3*50,5*37,50,37);
        hurt[6] = img2.getSubimage(4*50,5*37,50,37);
        hurt[7] = img2.getSubimage(5*50,5*37,50,37);
        hurt[8] = img2.getSubimage(6*50,5*37,50,37);
        hurt[9] = img2.getSubimage(1*50,6*37,50,37);
        hurt[10] = img2.getSubimage(2*50,6*37,50,37);
        hurt[11] = img2.getSubimage(3*50,6*37,50,37);
        animations.add(3,hurt);

        // dying
        BufferedImage[] dying = new BufferedImage[8];
        dying[0] = img2.getSubimage(4*50,4*37,50,37);
        dying[1] = img2.getSubimage(5*50,4*37,50,37);
        dying[2] = img2.getSubimage(6*50,4*37,50,37);
        dying[3] = img2.getSubimage(1*50,5*37,50,37);
        dying[4] = img2.getSubimage(2*50,5*37,50,37);
        dying[5] = img2.getSubimage(3*50,5*37,50,37);
        dying[6] = img2.getSubimage(4*50,5*37,50,37);
        dying[7] = img2.getSubimage(5*50,5*37,50,37);
        animations.add(4,dying);

        // jump
        BufferedImage[] jump = new BufferedImage[4];
        jump[0] = img1.getSubimage(3*50,1*37,50,37);
        jump[1] = img1.getSubimage(4*50,1*37,50,37);
        jump[2] = img1.getSubimage(2*50,2*37,50,37);
        jump[3] = img1.getSubimage(3*50,2*37,50,37);
        animations.add(5,jump);

        // fall
        BufferedImage[] fall = new BufferedImage[2];
        fall[0] = img1.getSubimage(1*50,3*37,50,37);
        fall[1] = img1.getSubimage(2*50,3*37,50,37);
        animations.add(6,fall);

        // mirrors
        ArrayList<BufferedImage[]> mirroredAnimations = new ArrayList<>();
        for (BufferedImage[] animation : animations) {
            BufferedImage[] mirroredFrames = new BufferedImage[animation.length];
            for (int i = 0; i < animation.length; i++) {
                mirroredFrames[i] = flipImage(animation[i]);
            }
            mirroredAnimations.add(mirroredFrames);
        }
        animations.addAll(mirroredAnimations);
    }

    private BufferedImage flipImage(BufferedImage image) {
        BufferedImage flippedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        AffineTransform transform = new AffineTransform();
        transform.setToScale(-1, 1);
        transform.translate(-image.getWidth(), 0);
        AffineTransformOp operation = new AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        operation.filter(image, flippedImage);
        return flippedImage;
    }

    public void loadLevelData(int[][] levelData){
        this.levelData = levelData;
        if(!IsOnFloor(hitBox,levelData)){
            inAir = true;
        } else {
            jumpsDone = 0;
        }
    }

    private void updateAnimationTick() {
        tick++;
        if (tick >= speed) {
            tick = 0;
            index++;
            if (index >= animations.get(currentAction).length) {
                index = 0;
                attacking = false;
            }
        }
    }

    private void setAnimation(){
        int start = currentAction;
        if(moving){
            if(mirror) currentAction = RUNNING_MIRROR;
            else currentAction = RUNNING;
        } else{
            if(mirror) currentAction = IDLE_MIRROR;
            else currentAction = IDLE;
        }
        if(inAir){
            if(airSpeed > 0){
                if(mirror) currentAction = FALL_MIRROR;
                else currentAction = FALL;
            } else{
                if(mirror) currentAction = JUMP_MIRROR;
                else currentAction = JUMP;
            }
        }
        if(attacking){
            if(mirror) currentAction = ATTACK_MIRROR;
            else currentAction = ATTACK;
        }
        if(start != currentAction) {
            tick = 0;
            index = 0;
        }
    }

    private void updatePos(){
        moving = false;

        if(jump) {
            tryJump();
        }

        if(!left && !right && !inAir)
            return;

        xSpeed = 0;

        if(left) {
            mirror = true;
            xSpeed -= playerSpeed;
        }
        if(right) {
            mirror = false;
            xSpeed += playerSpeed;
        }

        if(!inAir){
            if(!IsOnFloor(hitBox,levelData)){
                inAir = true;
            } else {
                jumpsDone = 0;
            }
        }

        if(inAir) {
            airSpeed += gravity;
        }

        updatePosition(xSpeed);

        // Only mark moving when there's horizontal speed or when in-air (so animations/states match reality)
        moving = (xSpeed != 0) || inAir;
    }

    private void tryJump() {
        if (jumpsDone < maxJumps) {
            inAir = true;
            airSpeed = jumpSpeed;
            jumpsDone++;
            // Play jump sound
            util.SoundManager.play(util.SoundManager.SoundEffect.PLAYER_JUMP);
        }
        jump = false;
    }

    private void updatePosition(float xSpeed){
        if(CanMoveHere(hitBox.x+xSpeed, hitBox.y-1, hitBox.width, hitBox.height,levelData)) {
            hitBox.x += xSpeed;
        }else{
            hitBox.x = XPosNextToWall(hitBox,xSpeed);
        }
        if(CanMoveHere(hitBox.x, hitBox.y+airSpeed, hitBox.width, hitBox.height,levelData)) {
            hitBox.y += airSpeed;
        }else{
            if(airSpeed<0){
                airSpeed=0;
            }else if(airSpeed > 0){
                inAir = false;
                moving = false;
                jumpsDone = 0;
            }
        }
    }

    // Hearts API
    public int getHearts() { return hearts; }
    public int getMaxHearts() { return maxHearts; }
    public void takeHeartDamage(int heartsToLose) {
        if (heartsToLose <= 0) return;
        hearts = Math.max(0, hearts - heartsToLose);
        // Play damage sound
        util.SoundManager.play(util.SoundManager.SoundEffect.PLAYER_DAMAGE);
    }
    public void healHearts(int heartsToAdd) {
        if (heartsToAdd <= 0) return;
        hearts = Math.min(maxHearts, hearts + heartsToAdd);
    }
    public void resetHeartsToFull() {
        hearts = maxHearts;
    }

    // boolean setters
    public void setAttacking(boolean attacking){
        this.attacking = attacking;
        // Play attack sound when starting attack
        if (attacking) {
            util.SoundManager.play(util.SoundManager.SoundEffect.PLAYER_ATTACK);
        }
    }
    public void setLeft(boolean left) {
        this.left = left;
    }
    public void setRight(boolean right) {
        this.right = right;
    }
    public void setJump(boolean jump) {
        this.jump = jump;
    }

    public void resetBooleans() {
        setRight(false);
        setLeft(false);
        setJump(false);
    }
    
    // Attack system
    public boolean isAttacking() {
        return attacking;
    }
    
    /**
     * Returns the attack hitbox when the player is attacking, null otherwise.
     * The hitbox is positioned in front of the player based on facing direction.
     * Size: approximately 30x40 pixels scaled.
     */
    public Rectangle2D.Float getAttackHitbox() {
        if (!attacking) return null;
        
        int attackW = (int)(30 * Game.SCALE);
        int attackH = (int)(40 * Game.SCALE);
        
        float attackX;
        if (mirror) {
            // Facing left - attack hitbox to the left of player
            attackX = hitBox.x - attackW;
        } else {
            // Facing right - attack hitbox to the right of player
            attackX = hitBox.x + hitBox.width;
        }
        
        // Vertically centered on player hitbox
        float attackY = hitBox.y + (hitBox.height - attackH) / 2;
        
        return new Rectangle2D.Float(attackX, attackY, attackW, attackH);
    }
}