package bc19;

import java.util.ArrayList;
import java.util.HashMap;

public class Church {

    private static int tick = 0;
    private static final int TICK_MAX = 8; // Threshold for building units again

    private static Lattice lattice;

    private static int numUnitsSpawned = 0;

    public static Action act(MyRobot r) {
        if (r.turn == 1) {
            lattice = new Lattice(r, new HashMap<>(), new ArrayList<>());
        }

        // If we see an enemy econ unit, spawn a prophet to kill that boi.
        int numEnemyPilgrims = Utils.getRobotsInRange(r, r.SPECS.PILGRIM, false, 0, 1000).size();
        int numFriendlyProphets = Utils.getRobotsInRange(r, r.SPECS.PROPHET, true, 0, 1000).size();
        if (numEnemyPilgrims > 0 && numFriendlyProphets < 1) {
            return Utils.tryAndBuildInOptimalSpace(r, r.SPECS.PROPHET);
        }

        // Spam crusaders if endgame
        if (r.turn > Constants.CASTLE_SPAM_CRUSADERS_TURN_THRESHOLD) {
            if (r.turn < Constants.FUEL_CAP_TURN_THRESHOLD || r.fuel > Constants.FUEL_CAP) {
                BuildAction action = Utils.tryAndBuildInOptimalSpace(r, r.SPECS.CRUSADER);
                if (action != null) {
                    Point crusaderLocation = lattice.popCrusaderLatticeLocation();
                    if (crusaderLocation != null) {
                        CommunicationUtils.sendTurtleLocation(r, crusaderLocation);
                        return action;
                    } else {
                        //r.log("Not spawning crusader because nowhere to send it.");
                    }
                }
            }
        }

        // Alternate prophets and crusader spamming
        int unitToBuild = numUnitsSpawned % 2 == 0 ? r.SPECS.CRUSADER : r.SPECS.PROPHET;
        if (Utils.canBuild(r, unitToBuild) && tick < TICK_MAX) {
            tick++;
        }
        if (tick >= TICK_MAX) {
            BuildAction action = Utils.tryAndBuildInOptimalSpace(r, unitToBuild);
            if (action != null) {
                Point latticeLocation = lattice.popProphetLatticeLocation();
                if (latticeLocation != null) {
                    tick = 0;
                    numUnitsSpawned++;
                    CommunicationUtils.sendTurtleLocation(r, latticeLocation);
                    return action;
                } else {
                    //r.log("Not spawning prophet because nowhere to send it.");
                }
            }
        }


        return null;

    }
}
