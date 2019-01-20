package bc19;

import java.util.ArrayList;
import java.util.HashMap;

public class Ring {
    private static final int RING_START = 3;
    private static final int MAX_RING_LEVEL = 20;

    private static Navigation ringMap;
    private static int ring = RING_START;

    private static HashMap<Integer, ArrayList<Point>> ringLocations = null;
    private static Point ringTarget = null;

    private static Point pickRingTarget(MyRobot r) {
        if (!ringLocations.containsKey(ring)) {
            return null;
        }
        ArrayList<Point> pointsInRing = ringLocations.get(ring);
        if (ring > RING_START) {
            return Utils.findClosestPointManhattan(r, pointsInRing);
        } else {
            return pointsInRing.get((int) (Math.random() * pointsInRing.size()));
        }
    }

    public static boolean exceededMaxRingLevel() {
        return ring >= MAX_RING_LEVEL;
    }

    public static Action ringFormation(MyRobot r, Point initialCastleLocation, Point enemyCastleLocation) {

        if (ringLocations == null) {
            ringLocations = Utils.generateRingLocations(r, initialCastleLocation, enemyCastleLocation);
        }

        if (CommunicationUtils.receivedBumpMessage(r) && Utils.isOn(r, ringTarget)) {
            ring++;
            if (exceededMaxRingLevel()) {
                return null;
            }
            r.log("Received bump request to level " + ring);
            ringTarget = null;
        }

        if (ringTarget == null) {
            ringTarget = pickRingTarget(r);

            if (ringTarget == null) {
                return null;
            }

            ArrayList<Point> targets = new ArrayList<>();
            targets.add(ringTarget);
            ringMap = new Navigation(r, r.getPassableMap(), targets);
        }

        int[][] visibleMap = r.getVisibleRobotMap();
        if (Utils.isAdjacentOrOn(r, ringTarget) && !Utils.isOn(r, ringTarget) && visibleMap[ringTarget.y][ringTarget.x] > 0) {
            // Emit bump request TODO ignore enemies
            // Send out the ID of the robot that's sitting on our square
            CommunicationUtils.sendBumpMessage(r, r.getVisibleRobotMap()[ringTarget.y][ringTarget.x]);
            return null;
        }

        if (!Utils.isOn(r, ringTarget) && ringMap != null) {
            return Utils.moveDijkstra(r, ringMap, 1);
        }

        return null;
    }
}
