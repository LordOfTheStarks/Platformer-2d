# Adding Sound Effects to the Game

The sound system is now implemented and ready to use! To add sound effects to your game, follow these steps:

## Sound Files Needed

Place the following `.wav` audio files in the `resources/sounds/` directory:

1. **jump.wav** - Played when the player jumps
2. **attack.wav** - Played when the player attacks
3. **player_damage.wav** - Played when the player takes damage
4. **player_death.wav** - Played when the player dies
5. **enemy_damage.wav** - Played when an enemy is hit
6. **enemy_death.wav** - Played when an enemy dies
7. **coin.wav** - Played when collecting a coin

## Directory Structure

```
resources/
  └── sounds/
      ├── jump.wav
      ├── attack.wav
      ├── player_damage.wav
      ├── player_death.wav
      ├── enemy_damage.wav
      ├── enemy_death.wav
      └── coin.wav
```

## Finding Free Sound Effects

You can find free game sound effects at:
- **Freesound.org** - https://freesound.org/
- **OpenGameArt.org** - https://opengameart.org/
- **Kenney.nl** - https://kenney.nl/assets?q=audio

## Notes

- The sound system works without sound files - it will gracefully skip playing sounds if files are missing
- Sounds should be short (under 1 second) for best results
- WAV format is recommended for compatibility
- The system initializes automatically when the game starts
- All sound triggers are already implemented in the code

## Testing

Once you add the sound files, simply run the game. You should hear:
- Jump sound when pressing SPACE
- Attack sound when left-clicking
- Damage sounds when hit by enemies or spikes
- Death sound when losing all hearts
- Coin collection sound when picking up coins
- Enemy sounds when hitting/defeating enemies
