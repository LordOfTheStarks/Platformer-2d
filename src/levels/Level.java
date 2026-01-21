package levels;


public class Level {
    private int[][] levelData;
    public Level(int[][] levelData){
        this.levelData = levelData;
    }
    public int getSpriteIndex(int x,int y){
        return levelData[x][y];
    }
    public int[][] getLevelData(){
           return levelData;
    }
    
    public int getLevelWidth() {
        if (levelData == null || levelData.length == 0) return 0;
        return levelData[0].length;
    }
}
