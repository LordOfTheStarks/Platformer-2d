package Entities;

import Main.Game;
import util.LoadSave;
import java.awt.*;
import java.awt.geom.AffineTransform;
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
    private float playerSpeed = Game.SCALE;
    private int[][] levelData;
    //variables used for jumping and gravity
    private float offsetX = 21* Game.SCALE , offsetY = 4*Game.SCALE;
    private float airSpeed = 0;
    private float gravity = 0.04f * Game.SCALE;
    private float jumpSpeed = -2.25f * Game.SCALE;
    private float xSpeed;

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
    public void render(Graphics g){
            g.drawImage(animations.get(currentAction)[index], (int)hitBox.x - (int)offsetX, (int)(hitBox.y) - (int)offsetY, width, height, null);
            drawHitBox(g);
    }
    private void loadAnimations() {
            BufferedImage img1 = LoadSave.getAtlas(LoadSave.PLAYER_ATLAS1);
            BufferedImage img2 = LoadSave.getAtlas(LoadSave.PLAYER_ATLAS2);

        //code for idle images
        BufferedImage[] idle = new BufferedImage[4];
        for(int i =0;i<idle.length;i++){
            idle[i] = img1.getSubimage(i*50,0,50,37);
        }

        animations.add(0,idle);

        //running
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

        //code for attack
        BufferedImage[] attack = new BufferedImage[7];
        for(int i =0;i<7;i++){
            attack[i] = img2.getSubimage(i*50,0,50,37);
        }
        animations.add(2,attack);

        //code for hurt images
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

        //animation for dying
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

        //animation for jumping
        BufferedImage[] jump = new BufferedImage[4];
        jump[0] = img1.getSubimage(3*50,1*37,50,37);
        jump[1] = img1.getSubimage(4*50,1*37,50,37);
        jump[2] = img1.getSubimage(2*50,2*37,50,37);
        jump[3] = img1.getSubimage(3*50,2*37,50,37);
        animations.add(5,jump);

        //animation for dying
        BufferedImage[] fall = new BufferedImage[2];
        fall[0] = img1.getSubimage(1*50,3*37,50,37);
        fall[1] = img1.getSubimage(2*50,3*37,50,37);
        animations.add(6,fall);

        //code for mirror animations
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
    //mirror image method
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
            if(mirror) {
                currentAction = RUNNING_MIRROR;
            }else{
                currentAction = RUNNING;
            }
        }
        else{
            if(mirror) {
                currentAction = IDLE_MIRROR;
            }else{
                currentAction = IDLE;
            }
        }
        if(inAir){
            if(airSpeed > 0){
                if(mirror) {
                    currentAction = FALL_MIRROR;
                }else{
                    currentAction = FALL;
                }
            }
            else{
                if(mirror) {
                    currentAction = JUMP_MIRROR;
                }else{
                    currentAction = JUMP;
                }
            }
        }

        if(attacking){
            if(mirror) {
                currentAction = ATTACK_MIRROR;
            }else{
                currentAction = ATTACK;
            }
        }
        if(start != currentAction) {
            tick = 0;
            index = 0;
        }
    }

    private void updatePos(){
          moving = false;
          if(jump)
              jump();

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
              }
          }
          if(inAir) {
                  airSpeed += gravity;
          }
          updatePosition(xSpeed);
          moving = true;
    }

    private void jump() {
        if(inAir)
            return;

        inAir = true;
        airSpeed = jumpSpeed;
    }

    private void updatePosition(float xSpeed){
        //for x position
        if(CanMoveHere(hitBox.x+xSpeed, hitBox.y-1, hitBox.width, hitBox.height,levelData)) {
            hitBox.x += xSpeed;
        }else{
            hitBox.x = XPosNextToWall(hitBox,xSpeed);
        }
        //for y position
        if(CanMoveHere(hitBox.x, hitBox.y+airSpeed, hitBox.width, hitBox.height,levelData)) {
            hitBox.y += airSpeed;
        }else{
            if(airSpeed<0){
                airSpeed=0;
            }else if(airSpeed > 0){
                inAir = false;
                moving = false;
            }

        }

    }


// boolean setters
    public void setAttacking(boolean attacking){
        this.attacking = attacking;
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
}
