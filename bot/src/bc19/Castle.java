package bc19;

import java.util.*;

public class Castle {
    private static final int CASTLE_ATTACK_RADIUS_SQ = 64;
    private static int initialPilgrimsBuilt = 0;
    
    private static int numFuelWorkers=0;
    private static int numKarbWorkers=0;
    private static HashMap<Integer, Point> pilgrimToTarget = new HashMap<>();
    private static ArrayList<Point> targets = new ArrayList<>();

    private static HashMap<Integer, Point> otherCastleLocations = new HashMap<>(); // Maps from unit ID to location
    private static ArrayList<Point> otherEnemyCastleLocations = new ArrayList<>(); // Doesn't include the enemy castle that mirrors ours

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
            // Send our X coordinate
            CastleTalkUtils.sendCastleCoord(r, r.me.x);
        } else if (r.turn == 2) {
            // Add X coordinates received from other castles
            for (Robot robot : r.getVisibleRobots()) {
                // TODO this assumes that only castles are broadcasting with castle talk on the first 3 turns. Something to keep in mind
                int castleCoordX = CastleTalkUtils.getCastleCoord(r, robot);
                if (castleCoordX != -1) {
                    // Put the X in our HashMap
                    otherCastleLocations.put(robot.id, new Point(castleCoordX, 0));
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
                    Point point = otherCastleLocations.get(robot.id);
                    otherCastleLocations.put(robot.id, new Point(point.x, castleCoordY));
                }
            }

            // Rebroadcast our Y coordinate (since it's expired as we intercepted it)
            CastleTalkUtils.sendCastleCoord(r, r.me.y);

            otherCastleLocations.remove(r.me.id);
            // Populate our enemy castle locations
            for (Integer id : otherCastleLocations.keySet()) {
                otherEnemyCastleLocations.add(Utils.getMirroredPosition(r, otherCastleLocations.get(id)));
            }
        } else if (r.turn == 5) {
            // Print enemy castle locations
            Point enemy = Utils.getMirroredPosition(r, new Point(r.me.x, r.me.y));
            r.log("[castle] Counterpart enemy castle location: " + enemy.x + " " + enemy.y);
            r.log("[castle] Other enemy castle locations: ");
            for (Point point : otherEnemyCastleLocations) {
                r.log(point.x + " " + point.y);
            }
        }
    }

    private static int enemyCastleLocationIndex = 0; // Used for broadcasting
    private static void broadcastEnemyCastleLocation(MyRobot r) {
        // 4. Broadcast enemy castle locations to nearby built prophets.
        if (enemyCastleLocationIndex < otherEnemyCastleLocations.size()) {
            Collections.sort(otherEnemyCastleLocations);
            CommunicationUtils.sendEnemyCastleLocation(r, otherEnemyCastleLocations.get(enemyCastleLocationIndex));
            enemyCastleLocationIndex++;
        }
    }

    public static Action act(MyRobot r) {
    	if(r.turn == 1) {
    		populateTargets(r);
    	}

    	getAllCastleLocations(r);

    	broadcastEnemyCastleLocation(r); // Finish up broadcasting if necessary
    	
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

        // TODO implement logic/heuristics to prevent existing units from starving Castle of building opportunities

        // 3. Build a prophet.
        if(r.turn > 50) {
            BuildAction action = Utils.tryAndBuildInRandomSpace(r, r.SPECS.PROPHET);
            if (action != null) {
                enemyCastleLocationIndex = 0;
                broadcastEnemyCastleLocation(r);
                return action;
            }
        }

        // 4. Finally, if we cannot build anything, attack if there are enemies in range.
        AttackAction attackAction = Utils.tryAndAttack(r, CASTLE_ATTACK_RADIUS_SQ);
        if (attackAction != null) {
            return attackAction;
        }

        return null;
    }

}

