package bc19;

import java.util.*;
import java.lang.Math;

public class Castle {
    private static final int CASTLE_ATTACK_RADIUS_SQ = 64;
    private static int initialPilgrimsBuilt = 0;

    public static int CASTLE_MAX_INITIAL_PILGRIMS = 2;

    private static int numAggressiveScoutUnitsBuilt = 0;

    private static HashMap<Integer, Point> otherCastleLocations = new HashMap<>(); // Maps from unit ID to location
    private static ArrayList<Point> enemyCastleLocations = new ArrayList<>(); // Doesn't include the enemy castle that mirrors ours
    
    /*
     * must get enemy castle locations before calling this
     */
    private static void computeNumPilgrimsToBuild(MyRobot r) {
        boolean[][] karbMap = r.karboniteMap;
        boolean[][] fuelMap = r.fuelMap;
        int count = 0;
        for(int y = 0; y < karbMap.length; y++) {
            for(int x = 0; x < karbMap[y].length; x++) {
                if(karbMap[y][x] || fuelMap[y][x]) {
                    count++;
                }
            }
        }
        CASTLE_MAX_INITIAL_PILGRIMS = count/(2*(enemyCastleLocations.size()));
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
    
    private static Point getContestedKarboniteGuardPoint(MyRobot r){
    	List<Point> karb = computeKarbPoints(r);
    	Point myLoc = new Point(r.me.x, r.me.y);
    	Point enemyLoc = Utils.getMirroredPosition(r, myLoc);
    	int smallestDiff = 100000;
    	Point bestPoint = null;
    	for (Point loc : karb){
    		int dist1 = Utils.computeManhattanDistance(myLoc, loc);
    		int dist2 = Utils.computeManhattanDistance(enemyLoc,loc);
    		int diff = Math.abs(dist1-dist2)*100+dist1;
    		if (diff<smallestDiff){
    			smallestDiff = diff;
    			bestPoint = loc;
    		}
    	}
    	boolean[][] passableMap = r.getPassableMap();
    	Point finalPoint = new Point(bestPoint.x, bestPoint.y);
    	for (int dx = -1; dx <=1; dx++){
    		for (int dy = -1; dy <=1; dy++){
    			if (!(dx==0&&dy==0)){
    				if (passableMap[bestPoint.x+dx][bestPoint.y+dy]){
    					finalPoint.x = bestPoint.x+dx;
    					finalPoint.y = bestPoint.y+dy;
    				}
    			}
    		}
    	}
    	r.log("Contested karb location is " + finalPoint.x + " " + finalPoint.y);
    	return finalPoint;
    }

    public static void handleCastleLocationMessages(MyRobot r) {
        if (r.turn == 1) {
            // Send our X coordinate
            CastleTalkUtils.sendCastleCoord(r, r.me.x);
        } else if (r.turn == 2) {
            // Add X coordinates received from other castles
            for (Robot robot : r.getVisibleRobots()) {
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

            // Remove our entry (in case we read our own broadcast)
            otherCastleLocations.remove(r.me.id);

            // Populate our enemy castle locations
            enemyCastleLocations.add(Utils.getMirroredPosition(r, new Point(r.me.x, r.me.y))); // Add counterpart
            for (Integer id : otherCastleLocations.keySet()) {
                enemyCastleLocations.add(Utils.getMirroredPosition(r, otherCastleLocations.get(id)));
            }
        } else if (r.turn == 5) {
            Castle.computeNumPilgrimsToBuild(r);
        }
    }

    private static void handleEnemyCastleKilledMessages(MyRobot r) {
        // Check for enemy castle killed messages
        for (Robot robot : r.getVisibleRobots()) {
            if (CastleTalkUtils.enemyCastleKilled(r, robot)) {
                r.log("Received enemy castle killed message");
                // Figure out which one was killed
                // TODO this removes the other two castles, but not the one we spawned at... this is problematic
                Point pointToRemove = null;
                for (Point point : enemyCastleLocations) {
                    if (CastleTalkUtils.enemyCastleKilledLocationMatches(r, robot, point)) {
                        pointToRemove = point;
                    }
                }
                if (pointToRemove != null) {
                    enemyCastleLocations.remove(pointToRemove);
                }
            }
        }
    }

    private static int enemyCastleLocationIndex = Constants.MAX_NUM_CASTLES; // Used for broadcasting, starts greater than number of castles
    private static void broadcastEnemyCastleLocation(MyRobot r) {
        if (enemyCastleLocationIndex < enemyCastleLocations.size()) {
            CommunicationUtils.sendEnemyCastleLocation(r, enemyCastleLocations.get(enemyCastleLocationIndex));
            enemyCastleLocationIndex++;
        }
    }

    private static void handleCastleTalk(MyRobot r) {
        handleCastleLocationMessages(r);
        handleEnemyCastleKilledMessages(r);
    }

    public static Action act(MyRobot r) {

        handleCastleTalk(r);

        // Finish up broadcasting if necessary
        boolean alreadyBroadcastedEnemyCastleLocation = false;
        if (enemyCastleLocationIndex < enemyCastleLocations.size()) {
            broadcastEnemyCastleLocation(r);
            alreadyBroadcastedEnemyCastleLocation = true;
        }
    	
    	// 1. If we haven't built any aggressive scout units yet, build them.
        if (numAggressiveScoutUnitsBuilt < Constants.NUM_AGGRESSIVE_SCOUT_UNITS_TO_BUILD) {
            BuildAction action = Utils.tryAndBuildInOptimalSpace(r, r.SPECS.PROPHET);
            if (action != null) {
                CommunicationUtils.sendAggressiveScoutLocation(r, getContestedKarboniteGuardPoint(r));
                numAggressiveScoutUnitsBuilt++;
                return action;
            }
        }
    	
        // 2. Build our initial pilgrims if we haven't built them yet.
        if (initialPilgrimsBuilt < CASTLE_MAX_INITIAL_PILGRIMS) {
        	BuildAction action = Utils.tryAndBuildInOptimalSpace(r, r.SPECS.PILGRIM);
            if (action != null) {
                initialPilgrimsBuilt++;
                //TODO: is the 3 right?
                //TODO: also track the number of pilgrims of each type
                return action;
            }
        }

        // TODO implement logic/heuristics to prevent existing units from starving Castle of building opportunities

        // 3. Spam crusaders at end of game
        if (r.turn > Constants.CASTLE_SPAM_CRUSADERS_TURN) {
            BuildAction action = Utils.tryAndBuildInRandomSpace(r, r.SPECS.CRUSADER);
            if (action != null) {
                return action;
            }
        }

        // 4. If we haven't built any aggressive scout units yet, build them.
        if (numAggressiveScoutUnitsBuilt < Constants.NUM_AGGRESSIVE_SCOUT_UNITS_TO_BUILD) {
            BuildAction action = Utils.tryAndBuildInRandomSpace(r, r.SPECS.PROPHET);
            if (action != null) {
                CommunicationUtils.sendAggressiveScoutLocation(r, getContestedKarboniteGuardPoint(r));
                numAggressiveScoutUnitsBuilt++;
                return action;
            }
        }

        // 5. Build a prophet.
        if (r.turn <= 50) {
            // Only build a prophet if we've detected enemies nearby and there are no prophets
            int numEnemyPilgrims = Utils.getRobotsInRange(r, -1, false, 0, 1000).size();
            int numFriendlyProphets = Utils.getRobotsInRange(r, r.SPECS.PROPHET, true, 0, 1000).size();
            if (numEnemyPilgrims > 0 && numFriendlyProphets < 1) {
                return Utils.tryAndBuildInRandomSpace(r, r.SPECS.PROPHET); // TODO spawn on side of castle closer to enemy
            }
        } else if(r.turn > 50) {
            BuildAction action = Utils.tryAndBuildInRandomSpace(r, r.SPECS.PROPHET);
            if (action != null) {
                enemyCastleLocationIndex = 1;
                if (!alreadyBroadcastedEnemyCastleLocation) {
                    broadcastEnemyCastleLocation(r);
                }
                return action;
            }
        }

        // 6. Finally, if we cannot build anything, attack if there are enemies in range.
        AttackAction attackAction = Utils.tryAndAttack(r, CASTLE_ATTACK_RADIUS_SQ);
        if (attackAction != null) {
            return attackAction;
        }

        return null;
    }

}

