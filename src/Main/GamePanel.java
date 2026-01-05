package Main;

import Entities.*;
import Inputs.KeyboardInputs;
import Inputs.MouseInputs;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static Main.Game.*;


public class GamePanel extends JPanel {
    private Game game;
    public GamePanel(Game game){
        this.game = game;
        MouseInputs mouseInputs = new MouseInputs(this);
        KeyboardInputs keyboardInputs = new KeyboardInputs(this);
        addMouseListener(mouseInputs);
        addMouseMotionListener(mouseInputs);
        addKeyListener(keyboardInputs);
        requestFocus();
        setPanelSize();
    }
    public void updateGame() {

    }
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        game.render(g);

    }
    private void setPanelSize() {
        Dimension size = new Dimension(GAME_WIDTH,GAME_HEIGHT);
        setPreferredSize(size);
        System.out.println("SIZE: "+GAME_WIDTH+" "+GAME_HEIGHT);
    }
    public Game getGame(){
        return game;
    }




}
