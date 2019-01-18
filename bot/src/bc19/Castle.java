package bc19;

public class Castle {
    private static int initialPilgrimsBuilt = 0;

    public static Action act(MyRobot r) {
        // 1. Build our initial pilgrims if we haven't built them yet.
        if (initialPilgrimsBuilt < Constants.CASTLE_MAX_INITIAL_PILGRIMS) {
            BuildAction action = Utils.tryAndBuildInRandomSpace(r, r.SPECS.PILGRIM);
            if (action != null) {
                initialPilgrimsBuilt++;
                return action;
            }
        }

        // 2. Build a pilgrim if there are none in vision radius.
        int numPilgrims = Utils.getUnitsInRange(r, r.SPECS.PILGRIM, true, 0, Utils.mySpecs(r).VISION_RADIUS).size();
		// This operates off of the assumption that if there are no pilgrims in the vision radius, then they are dead.
		// This assumption is OK because the current map generation algorithm always puts resource deposits next to castles.
		// However, it's not good anymore if maps are handmade, or we modify pilgrims to go for resource deposits farther than that.
        if (numPilgrims < 1) {
            BuildAction action = Utils.tryAndBuildInRandomSpace(r, r.SPECS.PILGRIM);
            if (action != null) {
                return action;
            }
        }

        // TODO implement logic/heuristics to prevent existing units from starving Castle of building opportunities

        // 3. Build a prophet.
        BuildAction action = Utils.tryAndBuildInRandomSpace(r, r.SPECS.PROPHET);
        if (action != null) {
            return action;
        }

        // 4. Finally, if we cannot build anything, attack if there are enemies in range.
        /*AttackAction attackAction = Utils.tryAndAttack(r, CASTLE_ATTACK_RADIUS);
        if (attackAction != null) {
            return attackAction;
        }*/

        return null;
    }

}

