package Main;
import GameStates.GameState;
import GameStates.Menu;
import GameStates.Playing;
import GameStates.Options;

import java.awt.*;

public class Game implements Runnable{
    private GameWindow gameWindow;
    private GamePanel gamePanel;
    private Thread gameThread;
    private final int FPS = 120;
    private final int UPS = 200;
    private Playing playing;
    private Menu menu;
    private Options options;

    public final static int TILES_DEFAULT = 32;
    public final static float SCALE = 1.5f;
    public final static int TILES_WIDTH = 26;
    public final static int TILES_HEIGHT = 14;
    public final static int TILES_SIZE = (int)(TILES_DEFAULT*SCALE);
    public final static int GAME_WIDTH = TILES_SIZE*TILES_WIDTH;
    public final static int GAME_HEIGHT = TILES_SIZE*TILES_HEIGHT;
    public Game(){
        init();

        gamePanel = new GamePanel(this);
        gameWindow = new GameWindow(gamePanel);
        gamePanel.requestFocus();

        startGameLoop();

    }

    private void init() {
        // Initialize sound system and start background music
        util.SoundManager.init();
        util.SoundManager.startBackgroundMusic();
        
        menu = new Menu(this);
        playing = new Playing(this);
        options = new Options(this);
    }

    private void update(){
        switch(GameState.state){
            case MENU:
                menu.update();
                break;
            case PLAYING:
                playing.update();
                break;
            case OPTIONS:
                options.update();
                break;
            default:
                break;
        }
    }
    public void render(Graphics g){
        switch(GameState.state){
            case MENU:
                menu.draw(g);
                break;
            case PLAYING:
                playing.draw(g);
                break;
            case OPTIONS:
                options.draw(g);
                break;
            case QUIT:
                System.exit(0);
            default:
                break;
        }
    }
    public void startGameLoop(){
        gameThread = new Thread(this);
        gameThread.start();
    }
    @Override
    public void run() {
        double timePerFrame = 1000000000.0/FPS;
        double timePerUpdate = 1000000000.0/UPS;

        long previousTime = System.nanoTime();

        int framesPerSecond = 0;
        int updatesPerSecond = 0;
        long lastCheck = System.currentTimeMillis();

        double deltaU = 0;
        double deltaF = 0;
        while(true){
            long currentTime = System.nanoTime();

            deltaU += (currentTime-previousTime)/timePerUpdate;
            deltaF += (currentTime-previousTime)/timePerFrame;
            previousTime = currentTime;
            if(deltaU>=1){
                update();
                updatesPerSecond++;
                deltaU--;
            }
            if(deltaF>=1){
                gamePanel.repaint();
                framesPerSecond++;
                deltaF--;
            }
            if(System.currentTimeMillis()-lastCheck >= 1000){
                lastCheck = System.currentTimeMillis();
                System.out.println("FPS: "+framesPerSecond+ " | UPS: "+updatesPerSecond);
                framesPerSecond = 0;
                updatesPerSecond = 0;
            }
        }
    }
    public void windowFocusLost() {
        if(GameState.state == GameState.PLAYING){
            playing.getPlayer().resetBooleans();
        }
    }
    public Menu getMenu(){
        return menu;
    }

    public Playing getPlaying() {
        return playing;
    }

    public Options getOptions() { return options; }
}