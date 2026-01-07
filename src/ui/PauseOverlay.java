package ui;
import Main.Game;
import GameStates.GameState;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static Main.Game.*;
import static util.Constants.UI.PauseButton.*;
import static util.Constants.UI.URMButton.*;
import static util.Constants.UI.VolumeButton.*;
import static util.LoadSave.*;

public class PauseOverlay {
    private final Game game;

    private BufferedImage pauseBackground;
    private SoundButton musicButton,sfxButton;
    private URMButton playButton, menuButton, quitButton;
    private VolumeButton volumeControl; // Single slider controlling overall volume

    private int pbgX,pbgY,pbgW,pbgH;

    // Layout helpers (content area inside overlay)
    private int contentLeftX;
    private int contentRightX;
    private int rowY_SoundTop;
    private int rowY_VolumeTop;
    private int rowY_URM;

    public PauseOverlay(Game game){
        this.game = game;
        loadBackGround();
        computeLayout();
        createSoundButtons();
        createVolumeButton();
        createUrmButtons();
    }

    private void loadBackGround() {
        pauseBackground = getAtlas(PAUSE_BACKGROUND);
        pbgW = (int) (pauseBackground.getWidth()* SCALE);
        pbgH = (int) (pauseBackground.getHeight()*SCALE);
        pbgX = GAME_WIDTH/2-pbgW/2;
        pbgY = (int)(20*SCALE);
    }

    private void computeLayout() {
        // Define an inner content area with margins inside the pause panel
        int marginH = (int)(60 * SCALE);
        contentLeftX = pbgX + marginH;
        contentRightX = pbgX + pbgW - marginH;

        // Sound toggles row
        rowY_SoundTop = pbgY + (int)(115 * SCALE);

        // Volume slider row beneath the "VOLUME" section
        rowY_VolumeTop = rowY_SoundTop + (int)(150 * SCALE);

        // URM buttons row: centered at bottom inside the overlay
        rowY_URM = pbgY + pbgH - URM_SIZE - (int)(35 * SCALE);
    }

    private void createSoundButtons() {
        // Place sound buttons (mute toggles) at the right side of the content area
        int soundX = contentRightX - SOUND_SIZE;
        int musicY = rowY_SoundTop;
        int sfxY = rowY_SoundTop + (int)(46 * SCALE);

        musicButton = new SoundButton(soundX, musicY, SOUND_SIZE, SOUND_SIZE);
        sfxButton = new SoundButton(soundX, sfxY, SOUND_SIZE, SOUND_SIZE);
    }

    private void createVolumeButton() {
        // Center the single volume slider beneath the VOLUME label area
        int volX = pbgX + (pbgW / 2) - (TRACK_W / 2);
        volumeControl = new VolumeButton(volX, rowY_VolumeTop);
    }

    private void createUrmButtons() {
        // Center URM buttons horizontally near the bottom, aligned to slider center
        int sliderCenterX = (int)((pbgX/1.1) + (pbgW / 2));
        int spacing = (int)(URM_SIZE * 1.2);

        playButton = new URMButton(sliderCenterX - spacing, rowY_URM, 0); // play/continue
        menuButton = new URMButton(sliderCenterX, rowY_URM, 1);          // main menu
        quitButton = new URMButton(sliderCenterX + spacing, rowY_URM, 2); // quit
    }

    public void update(){
        musicButton.update();
        sfxButton.update();

        playButton.update();
        menuButton.update();
        quitButton.update();

        volumeControl.update();

        // TODO: Connect master volume to audio system:
        // Audio.setMasterVolume(volumeControl.getValue());
    }

    public void draw(Graphics g){
        g.drawImage(pauseBackground,pbgX,pbgY,pbgW,pbgH,null);

        // Sound mute buttons
        musicButton.draw(g);
        sfxButton.draw(g);

        // Single volume slider (track + knob)
        volumeControl.draw(g);

        // URM controls
        playButton.draw(g);
        menuButton.draw(g);
        quitButton.draw(g);
    }

    public void mouseDragged(MouseEvent e){
        volumeControl.mouseDragged(e);
    }

    public void mouseReleased(MouseEvent e) {
        // Sound toggles
        if(isIn(e,musicButton))
            if(musicButton.isMousePressed())
                musicButton.setMuted(!musicButton.isMuted());

        if(isIn(e,sfxButton))
            if (sfxButton.isMousePressed())
                sfxButton.setMuted(!sfxButton.isMuted());

        musicButton.setMousePressed(false);
        sfxButton.setMousePressed(false);

        // URM actions
        if (isIn(e, playButton) && playButton.isMousePressed()) {
            if (GameState.state == GameState.PLAYING) {
                game.getPlaying().setPaused(false);
            } else {
                GameState.state = GameState.PLAYING;
            }
        }

        if (isIn(e, menuButton) && menuButton.isMousePressed()) {
            GameState.state = GameState.MENU;
        }

        if (isIn(e, quitButton) && quitButton.isMousePressed()) {
            GameState.state = GameState.QUIT;
        }

        // Volume release
        volumeControl.mouseReleased(e);

        // Reset URM hover/press
        playButton.reset();
        menuButton.reset();
        quitButton.reset();
    }

    public void mousePressed(MouseEvent e) {
        // Sound
        if(isIn(e,musicButton))
            musicButton.setMousePressed(true);

        if(isIn(e,sfxButton))
            sfxButton.setMousePressed(true);

        // URM
        if (isIn(e, playButton)) playButton.setMousePressed(true);
        if (isIn(e, menuButton)) menuButton.setMousePressed(true);
        if (isIn(e, quitButton)) quitButton.setMousePressed(true);

        // Volume
        volumeControl.mousePressed(e);
    }

    public void mouseMoved(MouseEvent e) {
        // Sound
        musicButton.setMouseOver(false);
        sfxButton.setMouseOver(false);
        if(isIn(e,musicButton)) musicButton.setMouseOver(true);
        if(isIn(e,sfxButton)) sfxButton.setMouseOver(true);

        // URM
        playButton.setMouseOver(false);
        menuButton.setMouseOver(false);
        quitButton.setMouseOver(false);
        if (isIn(e, playButton)) playButton.setMouseOver(true);
        if (isIn(e, menuButton)) menuButton.setMouseOver(true);
        if (isIn(e, quitButton)) quitButton.setMouseOver(true);

        // Volume
        volumeControl.mouseMoved(e);
    }

    private boolean isIn(MouseEvent e, PauseButtons b){
        return b.getBounds().contains(e.getX(),e.getY());
    }
}