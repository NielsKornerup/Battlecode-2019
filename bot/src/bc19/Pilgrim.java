package bc19;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Pilgrim {

    static Navigation targetMap;
    static Navigation castleMap;
    static State state = State.GATHERING;
    static Set<Point> knownTargets = new HashSet<>();
    
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
                knownTargets.add(target);
            }
        }
        castleMap = new Navigation(r, r.getPassableMap(), targets);
    }
    
    public static void computeTargetMap(MyRobot r, Point target) {
    	List<Point> targetList = new ArrayList<>();
    	targetList.add(target);
    	
    	//TODO: fix cannot read property 'y' of null caused by this
    	if(target == null) {
    		r.log("cannot get target from castle because castle is dead");
    	}
    	targetMap = new Navigation(r, r.getPassableMap(), targetList);
    }

    public static void computeMaps(MyRobot r, Point target) {
    	computeTargetMap(r, target);
        computeCastleMap(r);
    }

    public static Action act(MyRobot r) {
        if (r.turn == 1) {
        	ArrayList<Robot> adjacentCastles = Utils.getAdjacentRobots(r, r.SPECS.CASTLE, true);
        	//TODO: what if there are multiple adj castles? (unlikely)
        	Point target = CommunicationUtils.getPilgrimTargetInfo(r, adjacentCastles.get(0));
            computeMaps(r, target);
            state = State.GATHERING;
            
            //TODO: Make sure we don't go out of range. That could cause a bug.
            CommunicationUtils.sendPilgrimInfoToCastle(r, target, 5);
        }
        
        List<Robot> nearbyCastles = Utils.getRobotsInRange(r, r.SPECS.CASTLE, true, 0, Utils.mySpecs(r).VISION_RADIUS);
        List<Robot> nearbyChurches = Utils.getRobotsInRange(r, r.SPECS.CHURCH, true, 0, Utils.mySpecs(r).VISION_RADIUS);
        boolean foundNewTarget = false;
        for(Robot rob: nearbyCastles) {
        	if(!knownTargets.contains(Utils.getLocation(rob))) {
        		knownTargets.add(Utils.getLocation(rob));
        		castleMap.addTarget(Utils.getLocation(rob));
        		foundNewTarget = true;
        	}
        }

        for(Robot rob: nearbyChurches) {
        	if(!knownTargets.contains(Utils.getLocation(rob))) {
        		knownTargets.add(Utils.getLocation(rob));
        		castleMap.addTarget(Utils.getLocation(rob));
        		foundNewTarget = true;
        	}
        }
        
        if(foundNewTarget) {
        	castleMap.recalculateDistanceMap();
        }
        
        // TODO LOAD UP ON BOTH KARB AND FUEL BEFORE MOVING BACK
        // TODO ADJUST MOVEMENT SPEEDS TO BE MORE THAN RADIUS 1
        if (state == State.GATHERING) {
            // Check if we are at the destination
            if (targetMap.getPotential(Utils.getLocation(r.me)) == 0) {
            	//TODO: make this a constant
            	if(Utils.canBuild(r,  r.SPECS.CHURCH) && castleMap.getPotential(Utils.getLocation(r.me)) > 4) {
            		List<Point> locations = Utils.getAdjacentFreeSpaces(r);
            		if(locations.size()>0) {
            			int maxIndex = -1;
            			for(int index = 0; index < locations.size(); index++) {
            				Point loc = locations.get(index);
            				loc = new Point(r.me.x + loc.x, r.me.y + loc.y);
            				if(Utils.hasResource(r, loc)) {
            					continue;
            				}
            				if(maxIndex == -1) {
            					maxIndex = index;
            					continue;
            				}
            				Point best = locations.get(maxIndex);
            				best = new Point(r.me.x + best.x, r.me.y + best.y);
            				if(Utils.getAdjacentResourceCount(r, loc) > Utils.getAdjacentResourceCount(r, best)) {
            					maxIndex = index;
            				}
            			}
            			if(maxIndex != -1) {
            				return r.buildUnit(r.SPECS.CHURCH, locations.get(maxIndex).x, locations.get(maxIndex).y);
            			}
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
            // Check if next to Castles or churches
            ArrayList<Point> adjacentPlacesToDeposit = Utils.getAdjacentUnits(r, r.SPECS.CASTLE, true);
            adjacentPlacesToDeposit.addAll(Utils.getAdjacentUnits(r, r.SPECS.CHURCH, true));
            if (adjacentPlacesToDeposit.size() > 0) {
                // Check if Karbonite or Fuel left to drop off
                // TODO don't bother dropping off if only have trivial amounts to give
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
