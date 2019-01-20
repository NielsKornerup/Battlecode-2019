package bc19;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Castle {
    private static int initialPilgrimsBuilt = 0;
    
    private static int numFuelWorkers=0;
    private static int numKarbWorkers=0;
    private static HashMap<Integer, Point> pilgrimToTarget = new HashMap<>();
    private static ArrayList<Point> targets = new ArrayList<>();

    private static HashMap<Integer, Point> castleLocations = new HashMap<>(); // Maps from unit ID to location
    private static ArrayList<Point> enemyCastleLocations = new ArrayList<>();
    
    private static void populateTargets(MyRobot r) {
    	ArrayList<Point> mySpot = new ArrayList<>();
    	mySpot.add(Utils.getLocation(r.me));
    	Navigation myMap = new Navigation(r, r.getPassableMap(), mySpot);
    	List<Point> karbPoints = computeKarbPoints(r);
    	List<Point> fuelPoints = computeFuelPoints(r);
    	
    	//TODO: make this fast with sorting
    	while(karbPoints.size() > 0 || fuelPoints.size() > 0) {
    		if(karbPoints.size()> 0 && (fuelPoints.size()==0 || targets.size()%2 ==0)) {
    			int bestIndex = 0;
    			for(int index = 1; index < karbPoints.size(); index++) {
    				if(myMap.getPotential(karbPoints.get(index)) < myMap.getPotential(karbPoints.get(bestIndex))) {
    					bestIndex = index;
    				}
    			}
    			targets.add(karbPoints.get(bestIndex));
    			karbPoints.remove(bestIndex);
    		} else {
    			int bestIndex = 0;
    			for(int index = 1; index < fuelPoints.size(); index++) {
    				if(myMap.getPotential(fuelPoints.get(index)) < myMap.getPotential(fuelPoints.get(bestIndex))) {
    					bestIndex = index;
    				}
    			}
    			targets.add(fuelPoints.get(bestIndex));
    			fuelPoints.remove(bestIndex);
    		}
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

    public static void getAllCastleLocations(MyRobot r) {
        if (r.turn == 1) {
            // Add our own location to the HashMap
            castleLocations.put(r.me.id, new Point(r.me.x, r.me.y));

            // Send our X coordinate
            CastleTalkUtils.sendCastleCoord(r, r.me.x);
        } else if (r.turn == 2) {
            // Add X coordinates received from other castles
            for (Robot robot : r.getVisibleRobots()) {
                // TODO this assumes that only castles are broadcasting with castle talk on the first 3 turns. Something to keep in mind
                int castleCoordX = CastleTalkUtils.getCastleCoord(r, robot);
                if (castleCoordX != -1) {
                    // Put the X in our HashMap
                    castleLocations.put(robot.id, new Point(castleCoordX, 0));
                }
            }

            // Rebroadcast our X coordinate (since it's expired as we intercepted it)
            CastleTalkUtils.sendCastleCoord(r, r.me.x);
        } else if (r.turn == 3) {
            // Send our Y coordinate
            CastleTalkUtils.sendCastleCoord(r, r.me.y);
        } else if (r.turn == 4) {
            // Add Y coordinates received from other castles
            for (Robot robot : r.getVisibleRobots()) {
                int castleCoordY = CastleTalkUtils.getCastleCoord(r, robot);
                if (castleCoordY != -1) {
                    // Put the Y in our HashMap
                    Point point = castleLocations.get(robot.id);
                    castleLocations.put(robot.id, new Point(point.x, castleCoordY));
                }
            }

            // Rebroadcast our Y coordinate (since it's expired as we intercepted it)
            CastleTalkUtils.sendCastleCoord(r, r.me.y);

            // Populate our enemy castle locations
            for (Integer id : castleLocations.keySet()) {
                enemyCastleLocations.add(Utils.getMirroredPosition(r, castleLocations.get(id)));
            }
        }
    }

    public static Action act(MyRobot r) {
    	if(r.turn == 1) {
    		populateTargets(r);
    	}

    	getAllCastleLocations(r);
    	
    	//TODO: code this constant for the max range
    	List<Robot> robots = Utils.getRobotsInRange(r, r.SPECS.PILGRIM, true, 0, 5);
    	for(Robot rob: robots) {
    		Point target = CommunicationUtils.getPilgrimTargetForCastle(r, rob);
    		if(target!=null) {
    			pilgrimToTarget.put(rob.id, target);
    		}
    	}
    	
		Set<Integer> allRobots = new HashSet<>();
		for(Robot rob: r.getVisibleRobots()) {
			if(pilgrimToTarget.containsKey(rob.id)) {
				allRobots.add(rob.id);
			}
		}
    	
    	for(Integer id: pilgrimToTarget.keySet()) {
    		if(!allRobots.contains(id)) {
    			targets.add(0, pilgrimToTarget.get(id));
    			//TODO: get rid of this it will cause bugs
    			initialPilgrimsBuilt--;
    			pilgrimToTarget.remove(id);
    		}
    	}
    	
        // 1. Build our initial pilgrims if we haven't built them yet.
        if (initialPilgrimsBuilt < Constants.CASTLE_MAX_INITIAL_PILGRIMS) {
            CommunicationUtils.sendPilgrimInfoMessage(r, targets.get(0), 3);
        	BuildAction action = Utils.tryAndBuildInRandomSpace(r, r.SPECS.PILGRIM);
            if (action != null) {
                initialPilgrimsBuilt++;
                //TODO: is the 3 right?
                //TODO: also track the number of pilgrims of each type
                targets.remove(0);
                return action;
            }
        }

        // 2. Build a pilgrim if there are none in vision radius.
        int numPilgrims = Utils.getUnitsInRange(r, r.SPECS.PILGRIM, true, 0, Utils.mySpecs(r).VISION_RADIUS).size();
		// This operates off of the assumption that if there are no pilgrims in the vision radius, then they are dead.
		// This assumption is OK because the current map generation algorithm always puts resource deposits next to castles.
		// However, it's not good anymore if maps are handmade, or we modify pilgrims to go for resource deposits farther than that.
        if (numPilgrims < 1) {
            BuildAction action = Utils.tryAndBuildInRandomSpace(r, r.SPECS.PILGRIM);
            if (action != null) {
                return action;
            }
        }

        // TODO implement logic/heuristics to prevent existing units from starving Castle of building opportunities

        // 3. Build a prophet.
        BuildAction action = Utils.tryAndBuildInRandomSpace(r, r.SPECS.PROPHET);
        if (action != null) {
            return action;
        }

        // 4. Finally, if we cannot build anything, attack if there are enemies in range.
        /*AttackAction attackAction = Utils.tryAndAttack(r, CASTLE_ATTACK_RADIUS);
        if (attackAction != null) {
            return attackAction;
        }*/

        return null;
    }

}

