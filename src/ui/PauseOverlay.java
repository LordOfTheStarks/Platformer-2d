package ui;
import Main.Game;
import util.Constants;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static Main.Game.*;
import static util.Constants.UI.PauseButton.*;
import static util.LoadSave.*;

public class PauseOverlay {
    private BufferedImage pauseBackground;
    private SoundButton musicButton,sfxButton;
    private int pbgX,pbgY,pbgW,pbgH;
    public PauseOverlay(){
        loadBackGround();
        createSoundButtons();
    }

    private void createSoundButtons() {
        int soundX = (int)(450*SCALE),musicY = (int)(140*SCALE),sfxY = (int)(186*SCALE);
        musicButton = new SoundButton(soundX,musicY,SOUND_SIZE,SOUND_SIZE);
        sfxButton = new SoundButton(soundX,sfxY,SOUND_SIZE,SOUND_SIZE);
    }

    private void loadBackGround() {
        pauseBackground = getAtlas(PAUSE_BACKGROUND);
        pbgW = (int) (pauseBackground.getWidth()* SCALE);
        pbgH = (int) (pauseBackground.getHeight()*SCALE);
        pbgX = GAME_WIDTH/2-pbgW/2;
        pbgY = (int)(20*SCALE);
    }

    public void update(){
         musicButton.update();
         sfxButton.update();
    }
    public void draw(Graphics g){

        g.drawImage(pauseBackground,pbgX,pbgY,pbgW,pbgH,null);

        musicButton.draw(g);
        sfxButton.draw(g);
    }
    public void mouseDragged(MouseEvent e){

    }

    public void mouseReleased(MouseEvent e) {
        if(isIn(e,musicButton))
            if(musicButton.isMousePressed())
                musicButton.setMuted(!musicButton.isMuted());

        if(isIn(e,sfxButton))
            if (sfxButton.isMousePressed())
                sfxButton.setMuted(!musicButton.isMuted());
    }

    public void mousePressed(MouseEvent e) {
         if(isIn(e,musicButton))
             musicButton.setMousePressed(true);

         if(isIn(e,sfxButton))
             sfxButton.setMousePressed(true);
    }

    public void mouseMoved(MouseEvent e) {
       musicButton.setMouseOver(false);
       sfxButton.setMouseOver(false);

        if(isIn(e,musicButton))
            musicButton.setMouseOver(true);

        if(isIn(e,sfxButton))
            sfxButton.setMouseOver(true);
    }
    private boolean isIn(MouseEvent e, PauseButtons b){
         return b.getBounds().contains(e.getX(),e.getY());
    }

}
