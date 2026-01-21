package util;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple sound manager for playing game sound effects.
 * Currently configured to handle missing sound files gracefully.
 */
public class SoundManager {
    private static final Map<SoundEffect, Clip> soundClips = new HashMap<>();
    private static boolean soundEnabled = true;
    private static boolean initialized = false;
    
    public enum SoundEffect {
        PLAYER_JUMP("/sounds/jump.wav"),
        PLAYER_ATTACK("/sounds/attack.wav"),
        PLAYER_DAMAGE("/sounds/player_damage.wav"),
        PLAYER_DEATH("/sounds/player_death.wav"),
        ENEMY_DAMAGE("/sounds/enemy_damage.wav"),
        ENEMY_DEATH("/sounds/enemy_death.wav"),
        COIN_COLLECT("/sounds/coin.wav");
        
        private final String path;
        
        SoundEffect(String path) {
            this.path = path;
        }
        
        public String getPath() {
            return path;
        }
    }
    
    /**
     * Initialize the sound system. Call this once at game start.
     */
    public static void init() {
        if (initialized) return;
        initialized = true;
        
        // Try to load all sound effects
        for (SoundEffect effect : SoundEffect.values()) {
            try {
                loadSound(effect);
            } catch (Exception e) {
                // Sound file not found - this is okay, we'll just skip playing it
                System.out.println("[SoundManager] Sound file not found: " + effect.getPath() + " (optional)");
            }
        }
    }
    
    /**
     * Load a sound effect into memory.
     */
    private static void loadSound(SoundEffect effect) {
        try {
            InputStream is = SoundManager.class.getResourceAsStream(effect.getPath());
            if (is == null) {
                // Sound file doesn't exist yet - that's okay
                return;
            }
            
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(is);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            soundClips.put(effect, clip);
            
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            // Sound file issue - just skip it
            System.out.println("[SoundManager] Could not load sound: " + effect.getPath());
        }
    }
    
    /**
     * Play a sound effect. Safe to call even if sound doesn't exist.
     */
    public static void play(SoundEffect effect) {
        if (!soundEnabled || !initialized) return;
        
        Clip clip = soundClips.get(effect);
        if (clip != null) {
            // Stop and rewind if already playing
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.setFramePosition(0);
            clip.start();
        }
    }
    
    /**
     * Enable or disable all sound effects.
     */
    public static void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }
    
    /**
     * Check if sound is enabled.
     */
    public static boolean isSoundEnabled() {
        return soundEnabled;
    }
    
    /**
     * Clean up resources when game closes.
     */
    public static void cleanup() {
        for (Clip clip : soundClips.values()) {
            if (clip != null) {
                clip.close();
            }
        }
        soundClips.clear();
    }
}
