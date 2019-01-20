package bc19;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Pilgrim {

    static Navigation targetMap;
    private static Navigation castleMap;
    static Point target;

    private static PriorityQueue targets = new PriorityQueue();
    private static Set<Point> knownTargets = new HashSet<>();
    private static HashMap<Integer, Boolean> pilgrimsTalkedTo = new HashMap<>();

    private static State state = State.GATHERING;

    public enum State {
        GATHERING,
        MOVING_RESOURCE_HOME;

    }

    private static void populateTargets(MyRobot r) {
        ArrayList<Point> mySpot = new ArrayList<>();
        mySpot.add(Utils.myLocation(r));
        Navigation myMap = new Navigation(r, r.getPassableMap(), mySpot);
        for(Point p : Utils.getKarbonitePoints(r)) {
            targets.enqueue(new Node(myMap.getPotential(p), p));
        }
        for(Point p : Utils.getFuelPoints(r)) {
            targets.enqueue(new Node(myMap.getPotential(p), p));
        }
    }

    public static void computeCastleMap(MyRobot r) {
        List<Point> targets = new ArrayList<>();
        for (Robot robot : r.getVisibleRobots()) {
            if (robot.unit == r.SPECS.CASTLE || robot.unit == r.SPECS.CHURCH) {
            	Point target = new Point(robot.x, robot.y);
                targets.add(target);
                knownTargets.add(target);
            }
        }
        castleMap = new Navigation(r, r.getPassableMap(), targets);
    }
    
    public static void computeTargetMap(MyRobot r, Point target) {
    	List<Point> targetList = new ArrayList<>();
    	if (target != null) {
            targetList.add(target);
        }
    	targetMap = new Navigation(r, r.getPassableMap(), targetList);
    }

    public static void computeMaps(MyRobot r, Point target) {
    	computeTargetMap(r, target);
        computeCastleMap(r);
    }

    private static void recalculateChurchAndCastleMapIfNecessary(MyRobot r) {
        boolean foundNewChurchOrCastle = false;
        List<Robot> nearbyCastles = Utils.getRobotsInRange(r, r.SPECS.CASTLE, true, 0, Utils.mySpecs(r).VISION_RADIUS);
        for(Robot rob: nearbyCastles) {
            if(!knownTargets.contains(Utils.getLocation(rob))) {
                knownTargets.add(Utils.getLocation(rob));
                castleMap.addTarget(Utils.getLocation(rob));
                foundNewChurchOrCastle = true;
            }
        }

        List<Robot> nearbyChurches = Utils.getRobotsInRange(r, r.SPECS.CHURCH, true, 0, Utils.mySpecs(r).VISION_RADIUS);
        for(Robot rob: nearbyChurches) {
            if(!knownTargets.contains(Utils.getLocation(rob))) {
                knownTargets.add(Utils.getLocation(rob));
                castleMap.addTarget(Utils.getLocation(rob));
                foundNewChurchOrCastle = true;
            }
        }

        if(foundNewChurchOrCastle) {
            castleMap.recalculateDistanceMap();
        }
    }

    private static void recalculateTargetMapIfNecessary(MyRobot r) {
        List<Robot> nearbyPilgrims = Utils.getRobotsInRange(r, r.SPECS.PILGRIM, true, 0, CommunicationUtils.PILGRIM_TARGET_RADIUS_SQ);

        boolean needNewTarget = false;
        for(Robot rob: nearbyPilgrims) {
            Point robTarget = CommunicationUtils.getPilgrimTargetInfo(r, rob);
            if(target.equals(robTarget)) {
                needNewTarget = true;
            }
            targets.delete(Utils.getLocation(rob));
        }

        //TODO: what if we run out of targets?
        if (needNewTarget) {
            pilgrimsTalkedTo = new HashMap<>();
            target = targets.dequeue().p;
            Pilgrim.computeTargetMap(r, target);
            // Drop off Karbonite first if necessary
            if(r.me.karbonite > Utils.mySpecs(r).KARBONITE_CAPACITY * Constants.PILGRIM_PERCENTAGE_THRESHOLD_TO_DROP_OFF_FIRST
                    || r.me.karbonite > Utils.mySpecs(r).FUEL_CAPACITY * Constants.PILGRIM_PERCENTAGE_THRESHOLD_TO_DROP_OFF_FIRST) {
                state = State.MOVING_RESOURCE_HOME;
            }
        }

        boolean foundMessageTarget = false;
        for(Robot rob: nearbyPilgrims) {
            if(!pilgrimsTalkedTo.containsKey(rob.id)) {
                pilgrimsTalkedTo.put(rob.id, true);
                foundMessageTarget = true;
            }
        }

        if(foundMessageTarget) {
            CommunicationUtils.sendPilgrimTargetMessage(r, target);
        }
    }

    private static void invalidateCastleOrChurchIfNecessary(MyRobot r) {
        if (castleMap.getPotential(Utils.myLocation(r)) == 0) {
            // This means we're standing on the square, which means it was destroyed. Remove it from the map
            castleMap.removeTarget(Utils.myLocation(r));
            castleMap.recalculateDistanceMap();
        }
    }

    public static Action act(MyRobot r) {
        if (r.turn == 1) {
        	populateTargets(r);
        	target = targets.dequeue().p;
        	computeMaps(r, target);
            state = State.GATHERING;
        }

        recalculateTargetMapIfNecessary(r);
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
