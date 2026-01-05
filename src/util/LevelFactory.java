package util;

import Main.Game;

public class LevelFactory {
    private static final int AIR = 11; // passable
    private static final int GROUND = 1; // solid
    private static final int PLATFORM = 5; // solid

    public static int[][] level1() {
        int H = Game.TILES_HEIGHT, W = Game.TILES_WIDTH;
        int[][] l = fill(H, W, AIR);

        // Ground
        for (int x = 0; x < W; x++) {
            l[H-1][x] = GROUND;
            l[H-2][x] = GROUND;
        }

        // Platforms
        // mid-left
        for (int x = 3; x <= 8; x++) l[H-5][x] = PLATFORM;
        // mid-right
        for (int x = 15; x <= 22; x++) l[H-7][x] = PLATFORM;

        // small step near right border
        for (int x = W-6; x <= W-3; x++) l[H-4][x] = PLATFORM;

        return l;
    }

    public static int[][] level2() {
        int H = Game.TILES_HEIGHT, W = Game.TILES_WIDTH;
        int[][] l = fill(H, W, AIR);

        // Ground with a pit
        for (int x = 0; x < W; x++) {
            if (x < W/2 - 2 || x > W/2 + 2) {
                l[H-1][x] = GROUND;
                l[H-2][x] = GROUND;
            }
        }

        // Bridge platforms over the pit
        for (int x = W/2 - 4; x <= W/2 - 1; x++) l[H-6][x] = PLATFORM;
        for (int x = W/2 + 1; x <= W/2 + 4; x++) l[H-6][x] = PLATFORM;

        // Upper platform row
        for (int x = 4; x <= 10; x++) l[H-9][x] = PLATFORM;

        return l;
    }

    public static int[][] level3() {
        int H = Game.TILES_HEIGHT, W = Game.TILES_WIDTH;
        int[][] l = fill(H, W, AIR);

        // Ground
        for (int x = 0; x < W; x++) {
            l[H-1][x] = GROUND;
            l[H-2][x] = GROUND;
        }

        // Stairs/platforms rising to the right
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
                l[H-2][x] = GROUND;
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
            l[H-2][x] = GROUND;
        }

        // Tall pillars
        for (int y = H-3; y >= H-9; y--) l[y][4] = GROUND;
        for (int y = H-3; y >= H-9; y--) l[y][12] = GROUND;
        for (int y = H-3; y >= H-9; y--) l[y][20] = GROUND;

        // Top platforms connecting pillars
        for (int x = 4; x <= 12; x++) l[H-9][x] = PLATFORM;
        for (int x = 12; x <= 20; x++) l[H-9][x] = PLATFORM;

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