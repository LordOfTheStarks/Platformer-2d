package util;

import Main.Game;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class Helpmethods {
    public static boolean CanMoveHere(float x,float y, float width, float height, int[][] levelData){
     if(!isSolid(x,y,levelData))
        if(!isSolid(x+width,y+height,levelData))
            if(!isSolid(x+width,y,levelData))
                if(!isSolid(x,y+height,levelData))
                    if(!isSolid(x,(y+(height/2)),levelData))
                        if(!isSolid(x+width,(y+(height/2)),levelData))
                                 return true;

 return false;

    }
    private static boolean isSolid(float x, float y, int[][] levelData){
        if(x < 0)
            return true;
        if (y < 0)
            return true;
        if (y >= Game.GAME_HEIGHT)
            return false;

        float xIndex = x / Game.TILES_SIZE;
        float yIndex = y / Game.TILES_SIZE;
        
        // Check array bounds using actual level data dimensions
        if (yIndex >= levelData.length)
            return false;
        if (xIndex >= levelData[0].length)
            return true;
        
        int value = levelData[(int) yIndex][(int) xIndex];

        return value != 11;
    }

    public static float XPosNextToWall(Rectangle2D.Float hitBox, float xSpeed){
        int currentTile = (int)(hitBox.x/Game.TILES_SIZE);
        if(xSpeed > 0){
            int tileXPos = currentTile * Game.TILES_SIZE;
            int offsetX = (int)(Game.TILES_SIZE - hitBox.width);
            return tileXPos + (offsetX-1);
        }else{
            return currentTile *Game.TILES_SIZE;
        }
    }

    public static boolean IsOnFloor(Rectangle2D.Float hitBox, int[][] levelData){
          if(!isSolid(hitBox.x, hitBox.y+hitBox.height+1, levelData )){
              if(!isSolid(hitBox.x+hitBox.width, hitBox.y+hitBox.height+1, levelData)){
                  return false;
              }
          }
          return true;
    }
}
