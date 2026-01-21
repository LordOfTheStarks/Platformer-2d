package util;

import Main.Game;

public class LevelFactory {
    // Choose tile indices that fit your visuals from outside_sprites.png (4x12 grid).
    // AIR must be a passable tile; currently your collision treats only index 11 as air.
    public static final int AIR = 11;        // passable
    public static final int GROUND = 1;      // solid ground top/fill – set to your preferred tile index
    public static final int PLATFORM = 3;    // solid platform – set to your preferred tile index

    /**
     * Level 1 - Tutorial/Easy
     * Width: ~50 tiles, simple platforms introducing basic jumping, 2-3 enemies spread out
     */
    public static int[][] level1() {
        int H = Game.TILES_HEIGHT;
        int W = 50; // Extended width for scrolling
        int[][] l = fill(H, W, AIR);

        // Ground with small gap to teach jumping
        for (int x = 0; x < W; x++) {
            if (x >= 18 && x <= 20) {
                // Small gap
            } else {
                l[H-1][x] = GROUND;
            }
        }

        // Simple platforms for learning to jump - varying heights
        for (int x = 5; x <= 10; x++) l[H-4][x] = PLATFORM;
        for (int x = 13; x <= 16; x++) l[H-3][x] = PLATFORM; // Easy step over gap
        for (int x = 22; x <= 25; x++) l[H-3][x] = PLATFORM; // Landing after gap
        for (int x = 28; x <= 34; x++) l[H-5][x] = PLATFORM; // Higher platform
        for (int x = 38; x <= 42; x++) l[H-6][x] = PLATFORM; // Even higher
        for (int x = 45; x <= 48; x++) l[H-4][x] = PLATFORM; // Descending

        return l;
    }

    /**
     * Level 2 - Platform Challenge
     * Width: ~60 tiles, multiple floating platforms at varying heights, a pit section, 4-5 enemies
     */
    public static int[][] level2() {
        int H = Game.TILES_HEIGHT;
        int W = 60;
        int[][] l = fill(H, W, AIR);

        // Ground with multiple gaps for challenge
        for (int x = 0; x < W; x++) {
            if ((x >= 18 && x <= 22) || (x >= 30 && x <= 36) || (x >= 48 && x <= 51)) {
                // Gaps - no ground
            } else {
                l[H-1][x] = GROUND;
            }
        }

        // Lower tier platforms for initial section
        for (int x = 8; x <= 14; x++) l[H-4][x] = PLATFORM;
        
        // Platforms bridging first gap (choices: high or low route)
        for (int x = 18; x <= 22; x++) l[H-5][x] = PLATFORM; // Main route
        for (int x = 19; x <= 21; x++) l[H-8][x] = PLATFORM; // High risk route
        
        // Main pit section - zigzag pattern
        for (int x = 26; x <= 28; x++) l[H-4][x] = PLATFORM; // Before pit
        for (int x = 30; x <= 32; x++) l[H-6][x] = PLATFORM; // High platform
        for (int x = 34; x <= 36; x++) l[H-4][x] = PLATFORM; // After pit
        
        // Upper tier combat platforms
        for (int x = 40; x <= 45; x++) l[H-7][x] = PLATFORM;
        for (int x = 42; x <= 43; x++) l[H-9][x] = PLATFORM; // Narrow high platform
        
        // Final section with gap
        for (int x = 48; x <= 51; x++) l[H-6][x] = PLATFORM;
        for (int x = 54; x <= 58; x++) l[H-4][x] = PLATFORM;

        return l;
    }

    /**
     * Level 3 - Vertical Climb
     * Width: ~55 tiles, staircase-style ascending platforms, narrow platforms, enemies guarding
     */
    public static int[][] level3() {
        int H = Game.TILES_HEIGHT;
        int W = 55;
        int[][] l = fill(H, W, AIR);

        // Ground with gaps
        for (int x = 0; x < W; x++) {
            if (x >= 12 && x <= 14) {
                // Gap
            } else {
                l[H-1][x] = GROUND;
            }
        }

        // Initial warm-up platforms
        for (int x = 5; x <= 9; x++) l[H-3][x] = PLATFORM;
        for (int x = 12; x <= 14; x++) l[H-4][x] = PLATFORM; // Bridge gap
        
        // Ascending staircase platforms (tighter and more challenging)
        for (int i = 0; i < 7; i++) {
            int y = H - 5 - i;
            int startX = 16 + i * 4;
            int endX = startX + 3; // Narrower platforms
            if (y >= 0 && y < H) {
                for (int x = startX; x < endX && x < W; x++) {
                    l[y][x] = PLATFORM;
                }
            }
        }

        // Peak section with narrow precision platforms (adjusted to reachable height)
        for (int x = 42; x <= 45; x++) l[H-10][x] = PLATFORM; // Challenging but reachable
        for (int x = 46; x <= 48; x++) l[H-9][x] = PLATFORM;
        
        // Descent with wider platforms (reward for reaching top)
        for (int x = 50; x <= 54; x++) l[H-7][x] = PLATFORM;

        return l;
    }

    /**
     * Level 4 - Gauntlet
     * Width: ~65 tiles, mix of ground sections with gaps, floating platforms over pits, 5-6 enemies
     */
    public static int[][] level4() {
        int H = Game.TILES_HEIGHT;
        int W = 65;
        int[][] l = fill(H, W, AIR);

        // Ground with multiple dangerous gaps
        for (int x = 0; x < W; x++) {
            if ((x >= 8 && x <= 11) || (x >= 20 && x <= 25) || 
                (x >= 35 && x <= 40) || (x >= 50 && x <= 54)) {
                // Dangerous gaps
            } else {
                l[H-1][x] = GROUND;
            }
        }

        // Multi-level platforms over first gap
        for (int x = 8; x <= 11; x++) l[H-5][x] = PLATFORM;
        for (int x = 9; x <= 10; x++) l[H-8][x] = PLATFORM; // High risky route
        
        // Combat arena before big gap
        for (int x = 15; x <= 18; x++) l[H-4][x] = PLATFORM;
        for (int x = 16; x <= 17; x++) l[H-7][x] = PLATFORM; // Elevated combat
        
        // Challenging gap crossing - multiple small platforms
        for (int x = 20; x <= 22; x++) l[H-6][x] = PLATFORM;
        for (int x = 23; x <= 25; x++) l[H-4][x] = PLATFORM;
        
        // Mid-section with vertical spacing
        for (int x = 28; x <= 32; x++) l[H-5][x] = PLATFORM;
        for (int x = 30; x <= 31; x++) l[H-9][x] = PLATFORM; // High platform for double jump
        
        // Wide pit with difficult crossing
        for (int x = 35; x <= 37; x++) l[H-7][x] = PLATFORM; // High entry
        for (int x = 38; x <= 40; x++) l[H-5][x] = PLATFORM; // Lower exit
        
        // Final gauntlet section
        for (int x = 44; x <= 47; x++) l[H-6][x] = PLATFORM;
        for (int x = 50; x <= 52; x++) l[H-8][x] = PLATFORM; // Very high
        for (int x = 53; x <= 54; x++) l[H-5][x] = PLATFORM;
        for (int x = 58; x <= 63; x++) l[H-4][x] = PLATFORM; // Final platform

        return l;
    }

    /**
     * Level 5 - Final Challenge
     * Width: ~70 tiles, combines all mechanics: jumping, combat, precision platforming
     * Multiple enemy encounters, satisfying finale before returning to menu
     */
    public static int[][] level5() {
        int H = Game.TILES_HEIGHT;
        int W = 70;
        int[][] l = fill(H, W, AIR);

        // Ground with major gaps - ultimate challenge
        for (int x = 0; x < W; x++) {
            if ((x >= 12 && x <= 17) || (x >= 28 && x <= 35) || (x >= 48 && x <= 54)) {
                // Major gaps
            } else {
                l[H-1][x] = GROUND;
            }
        }

        // Section 1: Ascending challenge with combat
        for (int x = 5; x <= 8; x++) l[H-4][x] = PLATFORM;
        for (int x = 9; x <= 11; x++) l[H-6][x] = PLATFORM;
        
        // First gap - multiple routes
        for (int x = 12; x <= 14; x++) l[H-5][x] = PLATFORM; // Safe route
        for (int x = 15; x <= 17; x++) l[H-7][x] = PLATFORM; // Medium route
        for (int x = 13; x <= 15; x++) l[H-9][x] = PLATFORM; // High risk route
        
        // Section 2: Combat arena with elevation
        for (int x = 20; x <= 24; x++) l[H-4][x] = PLATFORM; // Lower level
        for (int x = 22; x <= 26; x++) l[H-7][x] = PLATFORM; // Upper level
        for (int x = 24; x <= 25; x++) l[H-10][x] = PLATFORM; // Peak
        
        // Section 3: Major pit with difficult crossing
        for (int x = 28; x <= 30; x++) l[H-6][x] = PLATFORM;
        for (int x = 31; x <= 32; x++) l[H-8][x] = PLATFORM; // Narrow high platform
        for (int x = 33; x <= 35; x++) l[H-6][x] = PLATFORM;
        
        // Section 4: Gauntlet with narrow platforms
        for (int x = 38; x <= 40; x++) l[H-5][x] = PLATFORM;
        for (int x = 42; x <= 44; x++) l[H-7][x] = PLATFORM;
        for (int x = 45; x <= 47; x++) l[H-9][x] = PLATFORM;
        
        // Section 5: Final pit challenge
        for (int x = 48; x <= 50; x++) l[H-8][x] = PLATFORM; // High entry
        for (int x = 51; x <= 53; x++) l[H-10][x] = PLATFORM; // Peak platform (difficult but fair)
        for (int x = 54; x <= 55; x++) l[H-7][x] = PLATFORM; // Exit
        
        // Section 6: Victory stretch with elevation
        for (int x = 57; x <= 61; x++) l[H-5][x] = PLATFORM;
        for (int x = 63; x <= 67; x++) l[H-3][x] = PLATFORM; // Triumphant finale
        
        return l;
    }

    private static int[][] fill(int H, int W, int value) {
        int[][] a = new int[H][W];
        for (int i = 0; i < H; i++) {
            for (int j = 0; j < W; j++) a[i][j] = value;
        }
        return a;
    }
}