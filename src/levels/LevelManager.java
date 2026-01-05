package levels;

import Main.Game;
import util.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;

import static Main.Game.*;

public class LevelManager {
    private Game game;
    private BufferedImage[] levelSprite;
    private Level levelOne;
    public LevelManager(Game game){
        this.game = game;
        importSprites();
        levelOne = new Level(LoadSave.getLevelData());
    }

    private void importSprites() {
        BufferedImage img = LoadSave.getAtlas(LoadSave.LEVEL_ATLAS);
        levelSprite = new BufferedImage[48];
        for(int i =0; i<4; i++){
            for(int j = 0; j<12; j++){
                int index = (i*12) + j;
                levelSprite[index] = img.getSubimage(j*32,i*32,32,32);
            }
        }
    }

    public void draw(Graphics g){
        for(int i = 0; i<Game.TILES_HEIGHT;i++){
            for(int j = 0; j<Game.TILES_WIDTH; j++){
                int index = levelOne.getSpriteIndex(i,j);
                g.drawImage(levelSprite[index],TILES_SIZE*j,TILES_SIZE*i,TILES_SIZE,TILES_SIZE,null);
            }
        }

    }
    public void update(){

    }

    public Level getCurrentLevel() {
        return levelOne;
    }
}
