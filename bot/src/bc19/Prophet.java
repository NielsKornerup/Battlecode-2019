package bc19;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Prophet {

    public enum State {
        TURTLING,
        ATTACKING
    }

    private static State state = State.TURTLING;

    private static Navigation enemyCastleMap;

    private static Point initialCastleLocation;
    private static Point enemyCastleLocation;
    private static HashMap<Point, Integer> otherEnemyCastleLocations = new HashMap<>(); // Maps from Point to bullshit

    private static boolean isAggressiveScout = false;
    private static boolean isTurtle = false;

    private static Action beginAttack(MyRobot r) {
        if (Math.random() < .5) {
            CommunicationUtils.sendAttackMessage(r);
        }
        state = State.ATTACKING;
        return act(r);
    }

    private static void computeEnemyCastlesMap(MyRobot r) {
        ArrayList<Point> targets = new ArrayList<>();
        targets.add(enemyCastleLocation);
        for (Point location : otherEnemyCastleLocations.keySet()) {
            targets.add(location);
        }
        enemyCastleMap = new Navigation(r, r.getPassableMap(), targets);
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

        Point myLoc = Utils.myLocation(r);
        for (Point target : targets) {
            if (Utils.computeSquareDistance(target, myLoc) > Utils.mySpecs(r).VISION_RADIUS) {
                continue;
            }
            boolean foundCastle = false;
            for (Robot robot : Utils.getRobotsInRange(r, r.SPECS.CASTLE, false, 0, 10000)) {
                if (Utils.getLocation(robot).equals(target)) {
                    foundCastle = true;
                }
            }
            if (!foundCastle) {
                enemyCastleMap.removeTarget(target);
                r.log("Enemy castle is dead, so recalculating distance map.");
                enemyCastleMap.recalculateDistanceMap();
                CastleTalkUtils.sendEnemyCastleKilled(r, target);
            }
        }
    }

    private static void doAggressiveScoutInitialization(MyRobot r) {
        // Check if we are an aggressive scout
        for (Robot robot : r.getVisibleRobots()) {
            if (CommunicationUtils.receivedAggressiveScoutLocation(r, robot)) {
                isAggressiveScout = true;
                // Set Dijkstra map to just go to that area TODO we probably shouldn't overload what the map does here
                ArrayList<Point> targets = new ArrayList<>();
                targets.add(CommunicationUtils.getAggressiveScoutLocation(r, robot));
                enemyCastleMap = new Navigation(r, r.getPassableMap(), targets);
                break;
            }
        }
    }
    
    private static void doTurtleInitialization(MyRobot r) {
        // Check if we are a turtle
        for (Robot robot : r.getVisibleRobots()) {
            if (CommunicationUtils.receivedTurtleLocation(r, robot)) {
                isTurtle = true;
                // Set Dijkstra map to just go to that area TODO we probably shouldn't overload what the map does here
                ArrayList<Point> targets = new ArrayList<>();
                targets.add(CommunicationUtils.getTurtleLocation(r, robot));
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
        return Utils.moveDijkstraThenRandom(r, enemyCastleMap, 2);
    }
    
    private static Action doTurtleActions(MyRobot r) {
        // 1. Attack enemies if nearby
        AttackAction attackAction = Utils.tryAndAttack(r, Utils.mySpecs(r).ATTACK_RADIUS[1]);
        if (attackAction != null) {
            return attackAction;
        }

        // 2. Move towards aggression point
        return Utils.moveDijkstraThenRandom(r, enemyCastleMap, 2);
    }

    public static Action act(MyRobot r) {
        if (r.turn == 1) {
            initialCastleLocation = Utils.getSpawningCastleOrChurchLocation(r);
            enemyCastleLocation = Utils.getMirroredPosition(r, initialCastleLocation);

            doAggressiveScoutInitialization(r);
            doTurtleInitialization(r);
        }

        if (isAggressiveScout) {
            return doAggressiveScoutActions(r);
        }
        if(isTurtle){
        	return doTurtleActions(r);
        }

        if (r.turn < Constants.TURNS_BEFORE_DONE_RECEIVING_ENEMY_CASTLE_LOCATIONS) {
            getEnemyCastleLocations(r);
        } else if (r.turn == Constants.TURNS_BEFORE_DONE_RECEIVING_ENEMY_CASTLE_LOCATIONS) {
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

        // 1.5. Don't move if we're waiting for enemy castle locations
        if (r.turn < Constants.TURNS_BEFORE_DONE_RECEIVING_ENEMY_CASTLE_LOCATIONS) {
            // Wait for castle to finish broadcasting enemy castle locations before moving
            return null;
        }

        // 2. Do either turtling or attacking actions
        if (state == State.TURTLING) {
            if (CommunicationUtils.receivedAttackMessage(r)) {
                return beginAttack(r);
            }
            Action ringAction = Ring.ringFormation(r, initialCastleLocation, enemyCastleLocation);
            if (ringAction != null) {
                return ringAction;
            }
            if (Ring.exceededMaxRingLevel()) {
                return beginAttack(r);
            }
        } else if (state == State.ATTACKING) {
            // Approach the enemy
            return Utils.moveDijkstraThenRandom(r, enemyCastleMap, 2);
        }

        return null;
    }

}