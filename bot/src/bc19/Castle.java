package bc19;

public class Castle implements BCRobot {
    private static final int MAX_INITIAL_PILGRIMS = 2;
    private static final int CASTLE_ATTACK_RADIUS = 64;
    private static int initialPilgrimsBuilt = 0;

    MyRobot r;

    public Castle(MyRobot myRobot) {
        this.r = myRobot;
    }

    public Action act() {
        // 1. Build our initial pilgrims
        if (initialPilgrimsBuilt < MAX_INITIAL_PILGRIMS) {
            BuildAction action = Utils.tryAndBuildInRandomSpace(r, r.SPECS.PILGRIM);
            if (action != null) {
                initialPilgrimsBuilt++;
                return action;
            }
        }

        // 2. Build a pilgrim if there are none in vision radius
        int numPilgrims = Utils.getUnitsInRange(r, r.SPECS.PILGRIM, true, 0, Utils.mySpecs(r).VISION_RADIUS).size();
        if (numPilgrims < 1) {
            BuildAction action = Utils.tryAndBuildInRandomSpace(r, r.SPECS.PILGRIM);
            if (action != null) {
                return action;
            }
        }

        // TODO implement logic/heuristics to prevent existing units from starving Castle of building opportunities

        // 3. Build a prophet otherwise
        BuildAction action = Utils.tryAndBuildInRandomSpace(r, r.SPECS.PROPHET);
        if (action != null) {
            return action;
        }

        /*// 4. Finally, attack if there are enemies in range
        AttackAction attackAction = Utils.tryAndAttack(r, CASTLE_ATTACK_RADIUS);
        if (attackAction != null) {
            return attackAction;
        }*/

        return null;
    }

}

