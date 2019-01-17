package bc19;

public class Castle implements BCRobot {
    private static final int MAX_INITIAL_PILGRIMS = 2;
    private static int initialPilgrimsBuilt = 0;

    MyRobot r;

    public Castle(MyRobot myRobot) {
        this.r = myRobot;
    }


    public Action act() {
        // Build our initial pilgrims
        if (initialPilgrimsBuilt < MAX_INITIAL_PILGRIMS) {
            BuildAction action = Utils.buildInRandomAdjacentSpace(r, r.SPECS.PILGRIM);
            if (action != null) {
                initialPilgrimsBuilt++;
                return action;
            }
        }

        // Build a pilgrim if there are none in vision radius
        int numPilgrims = Utils.getUnitsInRange(r, r.SPECS.PILGRIM, true, 0, Utils.mySpecs(r).VISION_RADIUS).size();
        if (numPilgrims < 1) {
            BuildAction action = Utils.buildInRandomAdjacentSpace(r, r.SPECS.PILGRIM);
            if (action != null) {
                return action;
            }
        }

        // TODO implement logic/heuristics to prevent existing units from starving Castle of building opportunities

        // Build a prophet otherwise
        BuildAction action = Utils.buildInRandomAdjacentSpace(r, r.SPECS.PROPHET);
        if (action != null) {
            return action;
        }
        return null;
    }

}

