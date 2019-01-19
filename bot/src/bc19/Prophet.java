package bc19;

import java.util.ArrayList;
import java.util.HashMap;

public class Prophet {
    private static HashMap<Integer, ArrayList<Point>> ringLocations = new HashMap<>();
    private static Point ringTarget = null;
    private static final int RING_START = 3;
    private static final int MAX_RING_LEVEL = 8;
    private static final int ATTACK_SIGNAL = 65532;
    private static final int ATTACK_SIGNAL_RADIUS_SQ = 5;
    private static int ring = RING_START;

    public enum State {
        TURTLING,
        ATTACKING
    }

    private static State state = State.TURTLING;


    private static Navigation ringMap;
    private static Navigation enemyCastleMap;

    public static HashMap<Integer, ArrayList<Point>> generateRingLocations(MyRobot r, Point center) {
        // Get opposite castle location
        Point opposite = Utils.getMirroredPosition(r, center);

        HashMap<Integer, ArrayList<Point>> ringLocations = new HashMap<>();
        boolean[][] passableMap = r.getPassableMap();

        for (int y = 0; y < passableMap.length; y++) {
            for (int x = 0; x < passableMap[y].length; x++) {
                if (!passableMap[y][x] || !Utils.isBetween(center, opposite, new Point(x, y))) {
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
        if (ring > RING_START) {
            return Utils.findClosestPoint(r, pointsInRing);
        } else {
            return pointsInRing.get((int) (Math.random() * pointsInRing.size()));
        }
    }

    private static boolean receivedBumpSignal(MyRobot r) {
        for (Robot robot : r.getVisibleRobots()) {
            if (robot.signal == r.id) {
                return true;
            }
        }
        return false;
    }

    private static Action beginAttack(MyRobot r) {
        r.signal(ATTACK_SIGNAL, ATTACK_SIGNAL_RADIUS_SQ);
        state = State.ATTACKING;
        return act(r);
    }

    private static Action ringFormation(MyRobot r) {
        if (receivedBumpSignal(r) && Utils.isOn(r, ringTarget)) {
            if (ring >= MAX_RING_LEVEL) {
                return beginAttack(r);
            }
            ring++;
            r.log("Bumping up ring level to " + ring);
            ringTarget = null;
        }

        if (ringTarget == null) {
            ringTarget = pickRingTarget(r);

            if (ringTarget == null) {
                return null;
            }
        }

        ArrayList<Point> targets = new ArrayList<>();
        targets.add(ringTarget);
        ringMap = new Navigation(r, r.getPassableMap(), targets);

        int[][] visibleMap = r.getVisibleRobotMap();
        if (Utils.isAdjacentOrOn(r, ringTarget) && !Utils.isOn(r, ringTarget) && visibleMap[ringTarget.y][ringTarget.x] > 0) {
            // Emit bump request TODO ignore enemies
            // Send out the ID of the robot that's sitting on our square
            int id = r.getVisibleRobotMap()[ringTarget.y][ringTarget.x];
            r.signal(id, 2);
            return null;
        }

        if (!Utils.isOn(r, ringTarget)) {
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

    private static boolean receivedAttackSignal(MyRobot r) {
        for (Robot robot : r.getVisibleRobots()) {
            if (robot.signal == ATTACK_SIGNAL) {
                return true;
            }
        }
        return false;
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

        // 1. Attack enemies if nearby
        AttackAction attackAction = Utils.tryAndAttack(r, Utils.mySpecs(r).ATTACK_RADIUS[1]);
        if (attackAction != null) {
            return attackAction;
        }

        if (state == State.TURTLING) {
            if (receivedAttackSignal(r)) {
                return beginAttack(r);
            }
            return ringFormation(r);
        } else if (state == State.ATTACKING) {
            // TODO: add movement logic invalidating castles as targets if they have been destroyed
            // Approach the enemy.
            return Utils.moveDijkstraThenRandom(r, enemyCastleMap, 1);
            // TODO: add movement logic for exploring
        }

        return null;
    }

}