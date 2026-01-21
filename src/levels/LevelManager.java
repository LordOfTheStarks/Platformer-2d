package levels;

import Main.Game;
import util.LevelFactory;
import util.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static Main.Game.*;

public class LevelManager {
    private Game game;
    private BufferedImage[] levelSprite;

    // Multi-level using arrays built in LevelFactory
    private final List<Level> levels = new ArrayList<>();
    private int currentLevelIdx = 0;

    // Layered backgrounds
    private BufferedImage gameBg0, gameBg1;

    public LevelManager(Game game){
        this.game = game;
        importSprites();
        loadBackgrounds();
        loadLevels();
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

    private void loadBackgrounds() {
        gameBg0 = LoadSave.getAtlas(LoadSave.GAME_BG_0);
        gameBg1 = LoadSave.getAtlas(LoadSave.GAME_BG_1);
    }

    private void loadLevels() {
        // Build 5 levels programmatically (no PNG authoring needed)
        levels.add(new Level(LevelFactory.level1()));
        levels.add(new Level(LevelFactory.level2()));
        levels.add(new Level(LevelFactory.level3()));
        levels.add(new Level(LevelFactory.level4()));
        levels.add(new Level(LevelFactory.level5()));
    }

    public void draw(Graphics g, int cameraOffsetX){
        // Layered game backgrounds fullscreen
        drawFullscreen(g, gameBg0);
        drawFullscreen(g, gameBg1);

        // Draw current level tiles with camera offset
        Level current = getCurrentLevel();
        int levelWidth = current.getLevelWidth();
        
        // Calculate visible tile range based on camera
        int startTileX = Math.max(0, cameraOffsetX / TILES_SIZE);
        int endTileX = Math.min(levelWidth, (cameraOffsetX + GAME_WIDTH) / TILES_SIZE + 2);
        
        for(int i = 0; i < Game.TILES_HEIGHT; i++){
            for(int j = startTileX; j < endTileX && j < levelWidth; j++){
                int index = current.getSpriteIndex(i, j);
                int drawX = TILES_SIZE * j - cameraOffsetX;
                g.drawImage(levelSprite[index], drawX, TILES_SIZE * i, TILES_SIZE, TILES_SIZE, null);
            }
        }
    }

    private void drawFullscreen(Graphics g, BufferedImage img){
        if (img != null) {
            g.drawImage(img, 0, 0, GAME_WIDTH, GAME_HEIGHT, null);
        }
    }

    public void update(){ }

    public Level getCurrentLevel() {
        if (levels.isEmpty()) return new Level(LevelFactory.level1());
        if (currentLevelIdx < 0) currentLevelIdx = 0;
        if (currentLevelIdx >= levels.size()) currentLevelIdx = levels.size()-1;
        return levels.get(currentLevelIdx);
    }

    public int getCurrentLevelIndex() {
        return currentLevelIdx;
    }

    public void nextLevel() {
        if (currentLevelIdx < levels.size() - 1) {
            currentLevelIdx++;
        }
    }

    public boolean isLastLevel() {
        return currentLevelIdx >= levels.size() - 1;
    }

    public void resetToFirstLevel() {
        currentLevelIdx = 0;
    }
}