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

    public static String MAIN_BG_0 = "/Mainbg_0.png";
    public static String MAIN_BG_1 = "/Mainbg_1.png";
    public static String GAME_BG_0 = "/Background_0.png";
    public static String GAME_BG_1 = "/Background_1.png";

    public static String COIN_SHEET = "/coin2_20x20.png";
    public static String SPIKES = "/spikes-1.png";

    // Enemy sprites (add /enemy1.png and /enemy2.png to your resources)
    public static String ENEMY_1 = "/enemy1.png";
    public static String ENEMY_2 = "/enemy2.png";

    // Hearts
    public static String HEART_EMPTY      = "/heart2-empty.png";     // 16x16
    public static String HEART_FULL_SHEET = "/heart2-shine.png";     // 96x16, 6 frames each 16x16

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