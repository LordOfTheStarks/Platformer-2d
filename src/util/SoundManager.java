package util;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple sound manager for playing game sound effects and background music.
 * Currently configured to handle missing sound files gracefully.
 */
public class SoundManager {
    private static final Map<SoundEffect, Clip> soundClips = new HashMap<>();
    private static Clip backgroundMusicClip = null;
    private static boolean soundEnabled = true;
    private static boolean musicEnabled = true;
    private static boolean initialized = false;
    
    /** Default volume for background music (0.0 to 1.0) */
    private static final float DEFAULT_MUSIC_VOLUME = 0.5f;
    private static float musicVolume = DEFAULT_MUSIC_VOLUME;
    
    public enum SoundEffect {
        PLAYER_JUMP("/sounds/jump.wav"),
        PLAYER_ATTACK("/sounds/attack.wav"),
        PLAYER_DAMAGE("/sounds/player_damage.wav"),
        PLAYER_DEATH("/sounds/player_death.wav"),
        ENEMY_DAMAGE("/sounds/enemy_damage.wav"),
        ENEMY_DEATH("/sounds/enemy_death.wav"),
        COIN_COLLECT("/sounds/coin.wav"),
        BOSS_ATTACK("/sounds/attack.wav"),      // Reuse attack sound for boss
        BOSS_DAMAGE("/sounds/enemy_damage.wav"), // Reuse enemy damage for boss
        BOSS_DEATH("/sounds/enemy_death.wav");   // Reuse enemy death for boss
        
        private final String path;
        
        SoundEffect(String path) {
            this.path = path;
        }
        
        public String getPath() {
            return path;
        }
    }
    
    private static final String BACKGROUND_MUSIC_PATH = "/sounds/background_music.wav";
    
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
        
        // Load background music
        loadBackgroundMusic();
    }
    
    /**
     * Load a sound effect into memory.
     */
    private static void loadSound(SoundEffect effect) {
        AudioInputStream audioStream = null;
        try {
            InputStream is = SoundManager.class.getResourceAsStream(effect.getPath());
            if (is == null) {
                // Sound file doesn't exist yet - that's okay
                return;
            }
            
            audioStream = AudioSystem.getAudioInputStream(is);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            soundClips.put(effect, clip);
            
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            // Sound file issue - just skip it
            System.out.println("[SoundManager] Could not load sound: " + effect.getPath());
        } finally {
            // Close the audio stream
            if (audioStream != null) {
                try {
                    audioStream.close();
                } catch (IOException e) {
                    // Ignore close errors
                }
            }
        }
    }
    
    /**
     * Load background music for looping playback.
     */
    private static void loadBackgroundMusic() {
        AudioInputStream audioStream = null;
        try {
            InputStream is = SoundManager.class.getResourceAsStream(BACKGROUND_MUSIC_PATH);
            if (is == null) {
                System.out.println("[SoundManager] Background music not found: " + BACKGROUND_MUSIC_PATH + " (optional)");
                return;
            }
            
            audioStream = AudioSystem.getAudioInputStream(is);
            backgroundMusicClip = AudioSystem.getClip();
            backgroundMusicClip.open(audioStream);
            
            // Set volume
            setMusicVolume(musicVolume);
            
            System.out.println("[SoundManager] Background music loaded successfully.");
            
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.out.println("[SoundManager] Could not load background music: " + BACKGROUND_MUSIC_PATH);
            backgroundMusicClip = null;
        } finally {
            if (audioStream != null) {
                try {
                    audioStream.close();
                } catch (IOException e) {
                    // Ignore close errors
                }
            }
        }
    }
    
    /**
     * Start playing background music on loop.
     */
    public static void startBackgroundMusic() {
        if (!musicEnabled || backgroundMusicClip == null) return;
        
        if (!backgroundMusicClip.isRunning()) {
            backgroundMusicClip.setFramePosition(0);
            backgroundMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }
    
    /**
     * Stop the background music.
     */
    public static void stopBackgroundMusic() {
        if (backgroundMusicClip != null && backgroundMusicClip.isRunning()) {
            backgroundMusicClip.stop();
        }
    }
    
    /**
     * Set the volume for background music (0.0 to 1.0).
     */
    public static void setMusicVolume(float volume) {
        musicVolume = Math.max(0f, Math.min(1f, volume));
        if (backgroundMusicClip != null) {
            try {
                FloatControl volumeControl = (FloatControl) backgroundMusicClip.getControl(FloatControl.Type.MASTER_GAIN);
                // Convert 0-1 to decibels (logarithmic scale)
                float dB = (float) (Math.log10(Math.max(0.0001, musicVolume)) * 20);
                volumeControl.setValue(Math.max(volumeControl.getMinimum(), Math.min(volumeControl.getMaximum(), dB)));
            } catch (IllegalArgumentException e) {
                // Volume control not supported - ignore
            }
        }
    }
    
    /**
     * Enable or disable background music.
     */
    public static void setMusicEnabled(boolean enabled) {
        musicEnabled = enabled;
        if (!enabled) {
            stopBackgroundMusic();
        }
    }
    
    /**
     * Check if music is enabled.
     */
    public static boolean isMusicEnabled() {
        return musicEnabled;
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
        stopBackgroundMusic();
        if (backgroundMusicClip != null) {
            backgroundMusicClip.close();
            backgroundMusicClip = null;
        }
        for (Clip clip : soundClips.values()) {
            if (clip != null) {
                clip.close();
            }
        }
        soundClips.clear();
    }
}
