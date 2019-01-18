package bc19;

import java.util.ArrayList;
import java.util.HashMap;

public class Prophet {
    private static HashMap<Integer, ArrayList<Point>> ringLocations = new HashMap<>();
    private static Point ringTarget = null;
    private static Navigation ringMap = null;
    private static final int RING_START = 3;
    private static int ring = RING_START;


    private static Navigation enemyCastleMap;

    public static boolean isBetween(Point a, Point b, Point test) {
        if (test.x < a.x - 2 && test.x < b.x) {
            return false;
        }
        if (test.x > a.x + 2 && test.x > b.x) {
            return false;
        }
        if (test.y < a.y - 2 && test.y < b.y) {
            return false;
        }
        if (test.y > a.y + 2 && test.y > b.y) {
            return false;
        }
        return true;
    }

    public static HashMap<Integer, ArrayList<Point>> generateRingLocations(MyRobot r, Point center) {

        // Get opposite castle location
        Point opposite = Utils.getMirroredPosition(r, center);

        HashMap<Integer, ArrayList<Point>> ringLocations = new HashMap<>();
        boolean[][] passableMap = r.getPassableMap();

        for (int y = 0; y < passableMap.length; y++) {
            for (int x = 0; x < passableMap[y].length; x++) {
                if (!passableMap[y][x]) {
                    continue;
                }
                // Only include points between the two plus or minus a bit
                if (!isBetween(center, opposite, new Point(x, y))) {
                    continue;
                }
                double dx = x - center.x;
                double dy = y - center.y;
                int distance = (int) Math.sqrt(dx * dx + dy * dy);
                if (!ringLocations.containsKey(distance)) {
                    ringLocations.put(distance, new ArrayList<>());
                }
                ringLocations.get(distance).add(new Point(x, y));
            }
        }
        return ringLocations;
    }

    private static Point pickRingTarget(MyRobot r) {
        ArrayList<Point> pointsInRing = ringLocations.get(ring);

        int[][] visibleMap = r.getVisibleRobotMap();
        ArrayList<Point> unoccupiedPoints = new ArrayList<>();
        for (Point point : pointsInRing) {
            if (ring > RING_START && !Utils.isAdjacentOrOn(r, point)) {
                pointsInRing.remove(point);
            }

            if (visibleMap[point.y][point.x] <= 0) {
                unoccupiedPoints.add(point);
            }
        }

        if (unoccupiedPoints.size() > 0) {
            return Utils.findClosestPoint(r, unoccupiedPoints);
        } else if (pointsInRing.size() > 0) {
            return Utils.findClosestPoint(r, pointsInRing);
        } else {
            r.log("No point to move to! This is rare but should be handled.");
            return null;
        }
    }

    private static Action ringFormation(MyRobot r) {

        // Check for bump requests.
        for (Robot robot : r.getVisibleRobots()) {
            if (robot.signal == r.id && Utils.isOn(r, ringTarget)) {
                ring++;
                r.log("Bumping up ring level to " + ring);
                ringTarget = null;
                break;
            }
        }

        if (ringTarget == null) {
            ringTarget = pickRingTarget(r);
            ArrayList<Point> targets = new ArrayList<>();
            targets.add(ringTarget);
            r.log("Generating target map");
            ringMap = new Navigation(r, r.getPassableMap(), targets);
        }

        int[][] visibleMap = r.getVisibleRobotMap();
        if (Utils.isAdjacentOrOn(r, ringTarget) && !Utils.isOn(r, ringTarget) && visibleMap[ringTarget.y][ringTarget.x] > 0) {
            // Emit bump request TODO ignore enemies

            // Get the ID of the robot that's sitting on our square
            int id = r.getVisibleRobotMap()[ringTarget.y][ringTarget.x];

            // Signal that ID out
            r.signal(id, 2);

            //r.log("Bump");
            return null;
        }

        if (!Utils.isOn(r, ringTarget)) {
            //r.log("Moving Dijkstra");
            return Utils.moveDijkstra(r, ringMap, 1);
        }

        return null;
    }


    public static void computeMaps(MyRobot r) {
        // We use our current castle location to infer the enemy's castle location. This is then used for Dijkstra map.
        // TODO: this means that a unit is not aware of all castles, which can be problematic.
        // we may need to remove this assumption at some point, especially if we're using
        //  Churches (since there are no guaranteed enemy counterparts to those)
        ArrayList<Point> targets = new ArrayList<>();
        targets.add(Utils.getMirroredPosition(r, new Point(r.me.x, r.me.y)));
        enemyCastleMap = new Navigation(r, r.getPassableMap(), targets);
    }


    private static boolean shouldMoveTowardsCastles(MyRobot r) {
        // If there are a few friendly units nearby, then move a step towards the enemy.
        int numFriendliesNearby = Utils.getUnitsInRange(r, -1, true, 0, Integer.MAX_VALUE).size();
        // TODO: adjust this movement function to balance well between clumping and trickling.
        double probabilityMoving = 1.0 / (1.0 + Math.exp(-(numFriendliesNearby - 4))); // Modified sigmoid function
        return Math.random() < probabilityMoving;
    }

    public static Action act(MyRobot r) {
        if (r.turn == 1) {
            computeMaps(r);

            // Store initial Castle location
            // TODO make work with Churches too
            Point initialCastleDelta = Utils.getAdjacentUnits(r, r.SPECS.CASTLE, true).get(0);
            Point initialCastleLocation = new Point(r.me.x + initialCastleDelta.x, r.me.y + initialCastleDelta.y);
            ringLocations = generateRingLocations(r, initialCastleLocation);
        }

        /*// 1. Attack enemies if nearby
        AttackAction attackAction = Utils.tryAndAttack(r, Utils.mySpecs(r).ATTACK_RADIUS[1]);
        if (attackAction != null) {
            return attackAction;
        }*/

        return ringFormation(r);


        /*
        // TODO: add movement logic invalidating castles as targets if they have been destroyed
        // 2. Do movement stuff - approach the enemy.
        if (shouldMoveTowardsCastles(r)) {
            return Utils.moveDijkstraThenRandom(r, enemyCastleMap, 1);
        }

        // TODO: add movement logic for exploring

        return null;
        */

        //return null;
    }

}