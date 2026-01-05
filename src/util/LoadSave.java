package util;

import Main.Game;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class LoadSave {
    public static String PLAYER_ATLAS1 = "/adventurer_sheet.png";
    public static String PLAYER_ATLAS2 = "/adventurer_sheet2.png";
    public static String LEVEL_ATLAS = "/outside_sprites.png";
    public static String LEVEL_ONE_DATA = "/level_one_data.png";

    public static String BUTTONS = "/button_atlas.png";
    public static String MENU_BACKGROUND = "/menu_background.png";
    public static String PAUSE_BACKGROUND = "/pause_background.png";
    public static String SOUND_BUTTONS = "/sound_button.png";
    public static String URM_BUTTONS = "/urm_buttons.png";
    public static String VOLUME_BUTTONS = "/volume_buttons.png";

    public static BufferedImage getAtlas(String fileName){
        BufferedImage img = null;
        InputStream is = LoadSave.class.getResourceAsStream(fileName);
        try {
            img = ImageIO.read(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return img;
    }
    public static int[][] getLevelData(){
        int[][] levelData = new int[Game.TILES_HEIGHT][Game.TILES_WIDTH];
        BufferedImage img = getAtlas(LEVEL_ONE_DATA);
        for(int i =0; i< img.getHeight(); i++){
            for(int j=0; j<img.getWidth(); j++){
                 Color color = new Color(img.getRGB(j,i));
                 int value = color.getRed();
                 if(value>=48) {
                     value = 0;
                 }
                 levelData[i][j] = value;
            }
        }
        return levelData;
    }

}
