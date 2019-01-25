package bc19;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Pilgrim {

    private static Navigation targetMap;
    private static Navigation castleMap;

    private static HashMap<Point, Integer> knownTargets = new HashMap<>(); // Maps from point to bullshit

    private static State state = State.GATHERING;

    public enum State {
        GATHERING,
        MOVING_RESOURCE_HOME
    }

    public static void computeCastleMap(MyRobot r) {
        List<Point> targets = new ArrayList<>();
        for (Robot robot : r.getVisibleRobots()) {
            if (robot.unit == r.SPECS.CASTLE || robot.unit == r.SPECS.CHURCH) {
            	Point target = new Point(robot.x, robot.y);
                targets.add(target);
                knownTargets.put(target, 0);
            }
        }
        castleMap = new Navigation(r, r.getPassableMap(), targets);
    }

    private static void recalculateChurchAndCastleMapIfNecessary(MyRobot r) {
        boolean foundNewChurchOrCastle = false;
        List<Robot> nearbyCastles = Utils.getRobotsInRange(r, r.SPECS.CASTLE, true, 0, Utils.mySpecs(r).VISION_RADIUS);
        for(Robot rob: nearbyCastles) {
            if(!knownTargets.containsKey(Utils.getLocation(rob))) {
                knownTargets.put(Utils.getLocation(rob), 0);
                castleMap.addTarget(Utils.getLocation(rob));
                foundNewChurchOrCastle = true;
            }
        }

        List<Robot> nearbyChurches = Utils.getRobotsInRange(r, r.SPECS.CHURCH, true, 0, Utils.mySpecs(r).VISION_RADIUS);
        for(Robot rob: nearbyChurches) {
            if(!knownTargets.containsKey(Utils.getLocation(rob))) {
                knownTargets.put(Utils.getLocation(rob), 0);
                castleMap.addTarget(Utils.getLocation(rob));
                foundNewChurchOrCastle = true;
            }
        }

        if(foundNewChurchOrCastle) {
            r.log("Found new church or castle, so recalculating distance map.");
            castleMap.recalculateDistanceMap();
        }
    }

    private static void invalidateCastleOrChurchIfNecessary(MyRobot r) {
        if (castleMap.getPotential(Utils.myLocation(r)) == 0) {
            // This means we're standing on the square, which means it was destroyed. Remove it from the map
            castleMap.removeTarget(Utils.myLocation(r));
            r.log("Invalidated church or castle, so recalculating distance map.");
            castleMap.recalculateDistanceMap();
        }
    }

    public static Action act(MyRobot r) {
        // TODO add logic to regenerate Dijkstra map if null so the pilgrim isn't just fucked for all eternity
        if (r.turn == 1) {
            //TODO: what if there are multiple adj castles? (unlikely)
            List<Robot> adjacentCastles = Utils.getAdjacentRobots(r, r.SPECS.CASTLE, true);
            Point target = CommunicationUtils.getPilgrimTargetInfo(r, adjacentCastles.get(0));
            ArrayList<Point> targets = new ArrayList<>();
            targets.add(target);
            r.log("My target is " + target.x + " " + target.y);
            targetMap = new Navigation(r, r.getPassableMap(), targets);

            //TODO: Make sure we don't go out of range. That could cause a bug.
            CommunicationUtils.sendPilgrimInfoToCastle(r, target, 5);


            computeCastleMap(r);
            state = State.GATHERING;
        }

        recalculateChurchAndCastleMapIfNecessary(r);

        if (state == State.GATHERING) {
            // Check if we are at the destination
            if (targetMap.getPotential(Utils.myLocation(r)) == 0) {
            	if(Utils.canBuild(r,  r.SPECS.CHURCH) && castleMap.getPotential(Utils.myLocation(r)) > Constants.MIN_CHURCH_BUILD_DISTANCE) {
            	    Action buildAction = Utils.tryAndBuildChurch(r);
            	    if (buildAction != null) {
            	        return null;
                    }
            	}
                // Check if we haven't loaded up on resources
                // TODO don't bother mining if we only need a tiny amount to fill up (like 1 karb)
                if (r.me.karbonite < Utils.mySpecs(r).KARBONITE_CAPACITY && r.me.fuel < Utils.mySpecs(r).FUEL_CAPACITY) {
                    if (Utils.canMine(r)) {
                        // Harvest
                        return r.mine();
                    }
                } else {
                    // Change state and move
                    state = State.MOVING_RESOURCE_HOME;
                    return act(r);
                }
            } else {
                // Move towards Karbonite
                return Utils.moveDijkstraThenRandom(r, targetMap, 2);
            }
        }

        if (state == State.MOVING_RESOURCE_HOME) {
            invalidateCastleOrChurchIfNecessary(r);

        	// Drop off Karb or Fuel if necessary
            List<Point> adjacentPlacesToDeposit = Utils.getAdjacentUnitDeltas(r, r.SPECS.CASTLE, true);
            adjacentPlacesToDeposit.addAll(Utils.getAdjacentUnitDeltas(r, r.SPECS.CHURCH, true));
            if (adjacentPlacesToDeposit.size() > 0) {
                if (r.me.karbonite > 0 || r.me.fuel > 0) {
                    // Drop off at Castle/Church
                    Point adjacentDeposit = adjacentPlacesToDeposit.get(0);
                    return r.give(adjacentDeposit.x, adjacentDeposit.y, r.me.karbonite, r.me.fuel);
                } else {
                    state = State.GATHERING;
                    return act(r);
                }
            } else {
                // Move towards nearby Castle
                return Utils.moveDijkstraThenRandom(r, castleMap, 2);
            }
        }

        return null;
    }
}
