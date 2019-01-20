package bc19;

public class Church {

    public static Action act(MyRobot r) {
        // If we see an enemy econ unit, spawn a prophet to kill that boi.
        int numEnemyPilgrims = Utils.getRobotsInRange(r, r.SPECS.PILGRIM, false, 0, 1000).size();
        int numFriendlyProphets = Utils.getRobotsInRange(r, r.SPECS.PROPHET, true, 0, 1000).size();
        if (numEnemyPilgrims > 0 && numFriendlyProphets < 1) {
            return Utils.tryAndBuildInOptimalSpace(r, r.SPECS.PROPHET);
        }

        return null;

    }
}
