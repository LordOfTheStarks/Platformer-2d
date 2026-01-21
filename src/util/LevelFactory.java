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

        // Ground
        for (int x = 0; x < W; x++) {
            l[H-1][x] = GROUND;
        }

        // Simple platforms for learning to jump
        for (int x = 5; x <= 10; x++) l[H-4][x] = PLATFORM;
        for (int x = 15; x <= 20; x++) l[H-5][x] = PLATFORM;
        for (int x = 25; x <= 32; x++) l[H-6][x] = PLATFORM;
        for (int x = 38; x <= 45; x++) l[H-4][x] = PLATFORM;

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

        // Ground with pit in the middle
        int pitCenter = 30;
        for (int x = 0; x < W; x++) {
            if (x < pitCenter - 3 || x > pitCenter + 3) {
                l[H-1][x] = GROUND;
            }
        }

        // Platforms bridging the pit at different heights
        for (int x = pitCenter - 5; x <= pitCenter - 1; x++) l[H-6][x] = PLATFORM;
        for (int x = pitCenter + 1; x <= pitCenter + 5; x++) l[H-6][x] = PLATFORM;

        // Additional floating platforms
        for (int x = 8; x <= 14; x++) l[H-5][x] = PLATFORM;
        for (int x = 18; x <= 22; x++) l[H-8][x] = PLATFORM;
        for (int x = 38; x <= 45; x++) l[H-7][x] = PLATFORM;
        for (int x = 50; x <= 56; x++) l[H-4][x] = PLATFORM;

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

        // Ground
        for (int x = 0; x < W; x++) {
            l[H-1][x] = GROUND;
        }

        // Ascending staircase platforms
        for (int i = 0; i < 8; i++) {
            int y = H - 4 - i;
            int startX = 3 + i * 5;
            int endX = startX + 4;
            if (y >= 0 && y < H) {
                for (int x = startX; x < endX && x < W; x++) {
                    l[y][x] = PLATFORM;
                }
            }
        }

        // Narrow precision platforms at top
        for (int x = 45; x <= 48; x++) l[H-10][x] = PLATFORM;
        for (int x = 50; x <= 53; x++) l[H-8][x] = PLATFORM;

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

        // Ground with gaps creating a gauntlet
        for (int x = 0; x < W; x++) {
            // Create gaps every 8-12 tiles
            if ((x >= 10 && x <= 13) || (x >= 25 && x <= 28) || 
                (x >= 42 && x <= 46) || (x >= 58 && x <= 61)) {
                // gap - no ground
            } else {
                l[H-1][x] = GROUND;
            }
        }

        // Platforms over the gaps
        for (int x = 10; x <= 13; x++) l[H-5][x] = PLATFORM;
        for (int x = 25; x <= 28; x++) l[H-6][x] = PLATFORM;
        for (int x = 42; x <= 46; x++) l[H-7][x] = PLATFORM;
        for (int x = 58; x <= 61; x++) l[H-5][x] = PLATFORM;

        // Additional combat platforms
        for (int x = 18; x <= 22; x++) l[H-8][x] = PLATFORM;
        for (int x = 35; x <= 40; x++) l[H-9][x] = PLATFORM;
        for (int x = 52; x <= 56; x++) l[H-8][x] = PLATFORM;

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

        // Ground with multiple challenging sections
        for (int x = 0; x < W; x++) {
            l[H-1][x] = GROUND;
        }

        // Section 1: Jump challenge (tiles 8-25)
        for (int x = 8; x <= 12; x++) l[H-5][x] = PLATFORM;
        for (int x = 15; x <= 19; x++) l[H-7][x] = PLATFORM;
        for (int x = 22; x <= 25; x++) l[H-9][x] = PLATFORM;

        // Section 2: Combat arena (tiles 28-42)
        for (int x = 28; x <= 32; x++) l[H-4][x] = PLATFORM;
        for (int x = 36; x <= 40; x++) l[H-4][x] = PLATFORM;
        for (int x = 32; x <= 36; x++) l[H-7][x] = PLATFORM;

        // Section 3: Precision platforming (tiles 45-58)
        for (int x = 45; x <= 47; x++) l[H-6][x] = PLATFORM;
        for (int x = 50; x <= 52; x++) l[H-8][x] = PLATFORM;
        for (int x = 54; x <= 56; x++) l[H-10][x] = PLATFORM;

        // Section 4: Final stretch (tiles 60-68)
        for (int x = 60; x <= 63; x++) l[H-5][x] = PLATFORM;
        for (int x = 65; x <= 68; x++) l[H-3][x] = PLATFORM;

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