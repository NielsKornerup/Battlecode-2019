package bc19;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Pilgrim {

    static Navigation targetMap;
    static Point target;
    static Navigation castleMap;
    static State state = State.GATHERING;
    static Set<Point> knownTargets = new HashSet<>();
    private static PriorityQueue targets = new PriorityQueue();
    private static HashMap<Integer, Boolean> pilgrimsTalkedTo = new HashMap<>();

    private static void populateTargets(MyRobot r) {
    	ArrayList<Point> mySpot = new ArrayList<>();
    	mySpot.add(Utils.getLocation(r.me));
    	Navigation myMap = new Navigation(r, r.getPassableMap(), mySpot);
    	List<Point> karbPoints = computeKarbPoints(r);
    	List<Point> fuelPoints = computeFuelPoints(r);
    	
    	for(Point p : karbPoints) {
    		targets.enqueue(new Node(myMap.getPotential(p), p));
    	}
    	for(Point p : fuelPoints) {
    		targets.enqueue(new Node(myMap.getPotential(p), p));
    	}
    }
    
    private static List<Point> computeKarbPoints(MyRobot r) {
        boolean[][] karboniteMap = r.getKarboniteMap();

        List<Point> targets = new ArrayList<>();
        for (int y = 0; y < karboniteMap.length; y++) {
            for (int x = 0; x < karboniteMap[y].length; x++) {
                if (karboniteMap[y][x]) {
                    targets.add(new Point(x, y));
                }
            }
        }

        return targets;
    }

    private static List<Point> computeFuelPoints(MyRobot r) {
        boolean[][] fuelMaps = r.getFuelMap();

        List<Point> targets = new ArrayList<>();
        for (int y = 0; y < fuelMaps.length; y++) {
            for (int x = 0; x < fuelMaps[y].length; x++) {
                if (fuelMaps[y][x]) {
                    targets.add(new Point(x, y));
                }
            }
        }

        return targets;
    }


    
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
        	populateTargets(r);
        	ArrayList<Robot> adjacentCastles = Utils.getAdjacentRobots(r, r.SPECS.CASTLE, true);
        	target = targets.dequeue().p;
        	computeMaps(r, target);
            state = State.GATHERING;
        }
        
        
        
        List<Robot> nearbyCastles = Utils.getRobotsInRange(r, r.SPECS.CASTLE, true, 0, Utils.mySpecs(r).VISION_RADIUS);
        List<Robot> nearbyChurches = Utils.getRobotsInRange(r, r.SPECS.CHURCH, true, 0, Utils.mySpecs(r).VISION_RADIUS);
        //TODO: make this a constant
        List<Robot> nearbyPilgrims = Utils.getRobotsInRange(r, r.SPECS.PILGRIM, true, 0, 2);

        boolean needNewTarget = false;
        for(Robot rob: nearbyPilgrims) {
        	Point robTarget = CommunicationUtils.getPilgrimTargetInfo(r, rob);
        	if(target.equals(robTarget)) {
        		needNewTarget = true;
        	}
        	targets.delete(Utils.getLocation(rob));
        }
        
        //TODO: what if we run out of targets?
        if(needNewTarget) {
        	pilgrimsTalkedTo = new HashMap<>();
        	Point oldTarget = target;
        	target = targets.dequeue().p;
        	Pilgrim.computeTargetMap(r, target);
        	if(r.me.karbonite > Utils.mySpecs(r).KARBONITE_CAPACITY/3 || r.me.karbonite > Utils.mySpecs(r).FUEL_CAPACITY/3) {
        		state = State.MOVING_RESOURCE_HOME;
        	}
        	r.log("old target " + oldTarget + " my new target is " + target);
        }
        
        boolean foundMessageTarget = false;
        for(Robot rob: nearbyPilgrims) {
        	if(!pilgrimsTalkedTo.containsKey(rob.id)) {
        		pilgrimsTalkedTo.put(rob.id, true);
        		foundMessageTarget = true;
        	}
        }
        
        //TODO: no hard constants
        if(foundMessageTarget) {
        	r.log("found someone to com with");
        	CommunicationUtils.sendPilgrimTargetMessage(r, target, 2);
        }
        
        boolean foundNewChurchOrCastle = false;
        for(Robot rob: nearbyCastles) {
        	if(!knownTargets.contains(Utils.getLocation(rob))) {
        		knownTargets.add(Utils.getLocation(rob));
        		castleMap.addTarget(Utils.getLocation(rob));
        		foundNewChurchOrCastle = true;
        	}
        }

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
            				r.log("loc is " + loc + " and it has resources? " + Utils.hasResource(r, loc)+ " adj tiles with resources is " + Utils.getAdjacentResourceCount(r, loc));
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
        	if(castleMap.getPotential(Utils.getLocation(r.me)) == 0) {
        		castleMap.removeTarget(Utils.getLocation(r.me));
        		castleMap.recalculateDistanceMap();
        	}
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
