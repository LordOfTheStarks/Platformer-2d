package util;


import Main.Game;

import static Main.Game.SCALE;

public class Constants {
    public static class UI{
        public static class Buttons{
            public static final int B_WIDTH_DEFAULT = 140;
            public static final int B_HEIGHT_DEFAULT = 56;
            public static final int B_WIDTH = (int)(B_WIDTH_DEFAULT* SCALE);
            public static final int B_HEIGHT = (int)(B_HEIGHT_DEFAULT* SCALE);
        }
        public static class PauseButton{
            public static final int SOUND_SIZE_DEFAULT = 42;
            public static final int SOUND_SIZE = (int) (42*SCALE);
        }

    }
    public static class Directions{
        public static final int LEFT = 0;
        public static final int UP = 1;
        public static final int RIGHT = 2;
        public static final int DOWN = 3;
    }
    public static class PlayerConstants{
        public static final int IDLE = 0;
        public static final int IDLE_MIRROR = 7;
        public static final int RUNNING = 1;
        public static final int RUNNING_MIRROR = 8;
        public static final int ATTACK = 2;
        public static final int ATTACK_MIRROR = 9;

        public static final int HURT = 3;
        public static final int HURT_MIRROR = 10;

        public static final int DYING = 4;
        public static final int DYING_MIRROR = 11;
        public static final int JUMP = 5;
        public static final int JUMP_MIRROR = 12;
        public static final int FALL = 6;
        public static final int FALL_MIRROR = 13;
    }
}
