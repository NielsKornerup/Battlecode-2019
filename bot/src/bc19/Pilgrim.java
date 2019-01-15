package bc19;

import java.util.ArrayList;
import java.util.List;

public class Pilgrim implements BCRobot {

    MyRobot r;

    public Pilgrim(MyRobot myRobot){
        this.r = myRobot;
    }

    public Action act() {
        if (r.turn == 1) {
            boolean[][] karboniteMap = r.karboniteMap;

            List<Point> targets = new ArrayList<>();

            for (int y = 0; y < karboniteMap.length; y++) {
                for (int x = 0; x < karboniteMap[y].length; x++) {
                    if (karboniteMap[y][x]) {
                        targets.add(new Point(x, y));
                    }
                }
            }

            Navigation navigation = new Navigation(r, r.getPassableMap(), targets);
            //navigation.printDistances();

        }
        return Utils.moveRandom(r);
    }
}
