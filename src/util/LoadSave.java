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

    // NEW: additional levels
    public static String LEVEL_1_DATA = "/level_one_data.png";
    public static String LEVEL_2_DATA = "/level_two_data.png";
    public static String LEVEL_3_DATA = "/level_three_data.png";
    public static String LEVEL_4_DATA = "/level_four_data.png";
    public static String LEVEL_5_DATA = "/level_five_data.png";

    // UI/Button atlases
    public static String BUTTONS = "/button_atlas.png";
    public static String MENU_BACKGROUND = "/menu_background.png";
    public static String PAUSE_BACKGROUND = "/pause_background.png";
    public static String SOUND_BUTTONS = "/sound_button.png";
    public static String URM_BUTTONS = "/urm_buttons.png";
    public static String VOLUME_BUTTONS = "/volume_buttons.png";

    // NEW: layered backgrounds
    public static String MAIN_BG_0 = "/Mainbg_0.png";
    public static String MAIN_BG_1 = "/Mainbg_1.png";
    public static String GAME_BG_0 = "/Background_0.png";
    public static String GAME_BG_1 = "/Background_1.png";

    public static BufferedImage getAtlas(String fileName){
        BufferedImage img = null;
        InputStream is = LoadSave.class.getResourceAsStream(fileName);
        try {
            img = ImageIO.read(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return img;
    }

    // Existing single-level loader (kept for compatibility)
    public static int[][] getLevelData(){
        return getLevelData(LEVEL_ONE_DATA);
    }

    // NEW: generic level loader by resource path
    public static int[][] getLevelData(String resourcePath){
        int[][] levelData = new int[Game.TILES_HEIGHT][Game.TILES_WIDTH];
        BufferedImage img = getAtlas(resourcePath);
        for(int i =0; i< img.getHeight() && i < Game.TILES_HEIGHT; i++){
            for(int j=0; j<img.getWidth() && j < Game.TILES_WIDTH; j++){
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