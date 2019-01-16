package bc19;

import java.util.ArrayList;

public class Castle implements BCRobot {

    MyRobot r;

    public Castle(MyRobot myRobot) {
        this.r = myRobot;
    }


    public Action act() {
        if (r.karbonite > Utils.getSpecs(r, r.SPECS.PILGRIM).CONSTRUCTION_KARBONITE && r.fuel > Utils.getSpecs(r, r.SPECS.PILGRIM).CONSTRUCTION_FUEL) {
            ArrayList<Point> freeSpaces = Utils.getAdjacentFreeSpaces(r);
            Point move = freeSpaces.get((int) (Math.random() * freeSpaces.size()));
            return r.buildUnit(r.SPECS.PILGRIM, move.x, move.y);
        }
        return null;
    }

}

