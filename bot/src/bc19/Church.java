package bc19;

import java.util.ArrayList;

public class Church implements BCRobot {

    private static final int MAX_PILGRIMS = 5;
    MyRobot r;
    static int pilgrimsBuilt;

    public Church(MyRobot myRobot) {
        this.r = myRobot;
    }

    public Action act() {
        if (r.karbonite > Utils.getSpecs(r, r.SPECS.PILGRIM).CONSTRUCTION_KARBONITE && r.fuel > Utils.getSpecs(r, r.SPECS.PILGRIM).CONSTRUCTION_FUEL && pilgrimsBuilt < MAX_PILGRIMS) {
            ArrayList<Point> freeSpaces = Utils.getAdjacentFreeSpaces(r);
            Point move = freeSpaces.get((int) (Math.random() * freeSpaces.size()));
            pilgrimsBuilt += 1;
            return r.buildUnit(r.SPECS.PILGRIM, move.x, move.y);
        }
        return null;
    }

}
