package bc19;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Prophet {
    private static HashMap<Integer, ArrayList<Point>> ringLocations = new HashMap<>();
    private static Point ringTarget = null;
    private static final int RING_START = 3;
    private static final int MAX_RING_LEVEL = 8;
    private static int ring = RING_START;

    public enum State {
        TURTLING,
        ATTACKING
    }

    private static State state = State.TURTLING;

    private static Navigation ringMap;
    private static Navigation enemyCastleMap;

    private static Point initialCastleLocation;
    private static Point enemyCastleLocation;
    private static HashMap<Point, Integer> otherEnemyCastleLocations = new HashMap<>(); // Maps from Point to bullshit

    private static Point pickRingTarget(MyRobot r) {
        ArrayList<Point> pointsInRing = ringLocations.get(ring);
        if (ring > RING_START) {
            return Utils.findClosestPoint(r, pointsInRing);
        } else {
            return pointsInRing.get((int) (Math.random() * pointsInRing.size()));
        }
    }

    private static Action beginAttack(MyRobot r) {
        if (Utils.canSignal(r, 2)) { // TODO hardcoded
            CommunicationUtils.sendAttackMessage(r);
            state = State.ATTACKING;
            return act(r);
        }
        return null;
    }

    private static Action ringFormation(MyRobot r) {
        if (CommunicationUtils.receivedBumpMessage(r) && Utils.isOn(r, ringTarget)) {
            if (ring >= MAX_RING_LEVEL) {
                return beginAttack(r);
            }
            ring++;
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


    private static void computeMaps(MyRobot r) {
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

    private static void doFirstTurnActions(MyRobot r) {
        computeMaps(r);
        initialCastleLocation = Utils.getCastleLocation(r);
        enemyCastleLocation = Utils.getMirroredPosition(r, initialCastleLocation);
        ringLocations = Utils.generateRingLocations(r, initialCastleLocation, enemyCastleLocation);
    }

    private static void getEnemyCastleLocations(MyRobot r) {
        for (Robot robot : Utils.getAdjacentRobots(r, r.SPECS.CASTLE, true)) {
            if (CommunicationUtils.receivedEnemyCastleLocation(r, robot)) {
                Point location = CommunicationUtils.getEnemyCastleLocation(r, robot);
                otherEnemyCastleLocations.put(location, 0);
            }
        }
    }

    public static Action act(MyRobot r) {
        if (r.turn == 1) {
            doFirstTurnActions(r);
        }

        getEnemyCastleLocations(r);

        if (r.turn < 3) { // TODO should this be 3 instead?
            // Wait for castle to finish broadcasting
            return null;
        }

        if (r.turn == 3) {
            r.log("[combat] Counterpart enemy castle location: " + enemyCastleLocation.x + " " + enemyCastleLocation.y);
            r.log("[combat] Other enemy castle locations:");
            for (Point point : otherEnemyCastleLocations.keySet()) {
                r.log(point.x + " " + point.y);
            }
        }

        // 1. Attack enemies if nearby
        AttackAction attackAction = Utils.tryAndAttack(r, Utils.mySpecs(r).ATTACK_RADIUS[1]);
        if (attackAction != null) {
            return attackAction;
        }

        // 2. Do either turtling or attacking actions
        if (state == State.TURTLING) {
            if (CommunicationUtils.receivedAttackMessage(r)) {
                r.log("Received attack message");
                return beginAttack(r);
            }
            return ringFormation(r);
        } else if (state == State.ATTACKING) {
            // TODO: add movement logic invalidating castles as targets if they have been destroyed
            // Approach the enemy.
            // TODO: add movement logic for exploring
            return Utils.moveDijkstraThenRandom(r, enemyCastleMap, 1);
        }

        return null;
    }

}