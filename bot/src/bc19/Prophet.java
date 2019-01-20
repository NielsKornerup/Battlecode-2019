package bc19;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Prophet {
    private static HashMap<Integer, ArrayList<Point>> ringLocations = new HashMap<>();
    private static Point ringTarget = null;
    private static final int RING_START = 3;
    private static final int MAX_RING_LEVEL = 20;
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

    private static final int TURNS_BEFORE_DONE_RECEIVING_ENEMY_CASTLE_LOCATIONS = 2;

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


    private static void computeEnemyCastlesMap(MyRobot r) {
        ArrayList<Point> targets = new ArrayList<>();
        targets.add(enemyCastleLocation);
        for (Point location : otherEnemyCastleLocations.keySet()) {
            targets.add(location);
        }
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

    private static void invalidateEnemyCastleTargetsIfNecessary(MyRobot r) {
        if (enemyCastleMap == null) {
            return;
        }

        List<Point> targets = enemyCastleMap.getTargets();

        Point myLoc = new Point(r.me.x, r.me.y);
        for (Point target : targets) {
            if (Utils.computeSquareDistance(target, myLoc) > Utils.mySpecs(r).VISION_RADIUS) {
                continue;
            }
            boolean foundCastle = false;
            for (Robot robot : Utils.getRobotsInRange(r, r.SPECS.CASTLE, false, 0, 10000)) {
                if (new Point(robot.x, robot.y).equals(target)) {
                    foundCastle = true;
                }
            }
            if (!foundCastle) {
                r.log(".......... Enemy castle is dead. Removing from map.");
                enemyCastleMap.removeTarget(target);
                enemyCastleMap.recalculateDistanceMap();
            }
        }
    }

    private static void doAggressiveScoutInitialization(MyRobot r) {
        // Check if we are an aggressive scout
        for (Robot robot : r.getVisibleRobots()) {
            if (CommunicationUtils.receivedAggressiveScoutLocation(r, robot)) {
                aggressiveScout = true;
                r.log("I am an aggressive scout.");
                // Set Dijkstra map to just go to that area
                ArrayList<Point> targets = new ArrayList<>();
                targets.add(CommunicationUtils.getAggressiveScoutLocation(r, robot));
                // TODO we overload what the map does here which is kinda bad
                enemyCastleMap = new Navigation(r, r.getPassableMap(), targets);
                break;
            }
        }
    }

    private static Action doAggressiveScoutActions(MyRobot r) {
        // 1. Attack enemies if nearby
        AttackAction attackAction = Utils.tryAndAttack(r, Utils.mySpecs(r).ATTACK_RADIUS[1]);
        if (attackAction != null) {
            return attackAction;
        }

        // 2. Move towards aggression point
        return Utils.moveDijkstraThenRandom(r, enemyCastleMap, 1);
    }

    static boolean aggressiveScout = false;
    public static Action act(MyRobot r) {
        if (r.turn == 1) {
            doFirstTurnActions(r);
            doAggressiveScoutInitialization(r);
        }

        if (aggressiveScout) {
            return doAggressiveScoutActions(r);
        }

        if (r.turn < TURNS_BEFORE_DONE_RECEIVING_ENEMY_CASTLE_LOCATIONS) {
            getEnemyCastleLocations(r);
        } else if (r.turn == TURNS_BEFORE_DONE_RECEIVING_ENEMY_CASTLE_LOCATIONS) {
            computeEnemyCastlesMap(r);
        } else {
            // Invalidate any Dijkstra map targets that are stale
            invalidateEnemyCastleTargetsIfNecessary(r);
        }

        // 1. Attack enemies if nearby
        AttackAction attackAction = Utils.tryAndAttack(r, Utils.mySpecs(r).ATTACK_RADIUS[1]);
        if (attackAction != null) {
            return attackAction;
        }

        if (r.turn < TURNS_BEFORE_DONE_RECEIVING_ENEMY_CASTLE_LOCATIONS) {
            // Wait for castle to finish broadcasting enemy castle locations before moving
            return null;
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