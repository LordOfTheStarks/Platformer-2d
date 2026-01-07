package util;

import Main.Game;

public class LevelFactory {
    // Choose tile indices that fit your visuals from outside_sprites.png (4x12 grid).
    // AIR must be a passable tile; currently your collision treats only index 11 as air.
    public static final int AIR = 11;        // passable
    public static final int GROUND = 1;      // solid ground top/fill – set to your preferred tile index
    public static final int PLATFORM = 3;    // solid platform – set to your preferred tile index

    public static int[][] level1() {
        int H = Game.TILES_HEIGHT, W = Game.TILES_WIDTH;
        int[][] l = fill(H, W, AIR);

        // Ground
        for (int x = 0; x < W; x++) {
            l[H-1][x] = GROUND;
        }

        // Platforms
        for (int x = 3; x <= 8; x++) l[H-5][x] = PLATFORM;
        for (int x = 15; x <= 22; x++) l[H-7][x] = PLATFORM;
        for (int x = W-6; x <= W-3; x++) l[H-4][x] = PLATFORM;

        return l;
    }

    public static int[][] level2() {
        int H = Game.TILES_HEIGHT, W = Game.TILES_WIDTH;
        int[][] l = fill(H, W, AIR);

        // Ground with a pit at center
        for (int x = 0; x < W; x++) {
            if (x < W/2 - 2 || x > W/2 + 2) {
                l[H-1][x] = GROUND;
            }
        }

        // Platforms bridging the pit
        for (int x = W/2 - 4; x <= W/2 - 1; x++) l[H-6][x] = PLATFORM;
        for (int x = W/2 + 1; x <= W/2 + 4; x++) l[H-6][x] = PLATFORM;

        // Upper platform
        for (int x = 4; x <= 10; x++) l[H-9][x] = PLATFORM;

        return l;
    }

    public static int[][] level3() {
        int H = Game.TILES_HEIGHT, W = Game.TILES_WIDTH;
        int[][] l = fill(H, W, AIR);

        // Ground
        for (int x = 0; x < W; x++) {
            l[H-1][x] = GROUND;
        }

        // Staircase upwards to the right
        for (int i = 0; i < 6; i++) {
            int y = H - 4 - i;
            int startX = 2 + i*3;
            int endX = startX + 3;
            for (int x = startX; x < endX && x < W; x++) l[y][x] = PLATFORM;
        }

        return l;
    }

    public static int[][] level4() {
        int H = Game.TILES_HEIGHT, W = Game.TILES_WIDTH;
        int[][] l = fill(H, W, AIR);

        // Ground with small gaps
        for (int x = 0; x < W; x++) {
            if (x % 7 != 0) {
                l[H-1][x] = GROUND;
            }
        }

        // Floating platforms
        for (int x = 5; x <= 10; x++) l[H-8][x] = PLATFORM;
        for (int x = 14; x <= 19; x++) l[H-6][x] = PLATFORM;

        return l;
    }

    public static int[][] level5() {
        int H = Game.TILES_HEIGHT, W = Game.TILES_WIDTH;
        int[][] l = fill(H, W, AIR);

        // Ground
        for (int x = 0; x < W; x++) {
            l[H-1][x] = GROUND;
        }


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