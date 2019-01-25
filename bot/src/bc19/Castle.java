package bc19;

import java.util.ArrayList;
import java.util.HashMap;
import java.lang.Math;

public class Castle {
    private static int initialPilgrimsBuilt = 0;
    private static int initialAggressiveScoutUnitsBuilt = 0;
    private static int turtleUnitsBuilt = 6;

    public static int maxInitialPilgrimsToBuild = 2;
    private static int enemyCastleLocationIndex = Constants.MAX_NUM_CASTLES; // Starts greater than # castles index

    private static HashMap<Integer, Point> otherCastleLocations = new HashMap<>(); // Maps from unit ID to location
    private static ArrayList<Point> enemyCastleLocations = new ArrayList<>(); // Doesn't include the enemy castle that mirrors ours
    private static ArrayList<Point> latticeLocations = new ArrayList<Point>();
    public static void handleCastleLocationMessages(MyRobot r) {
        if (r.turn == 1) {
            // Send our X coordinate
            CastleTalkUtils.sendCastleCoord(r, r.me.x);
            initializeLattice(r);
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
            enemyCastleLocations.add(Utils.getMirroredPosition(r, Utils.myLocation(r))); // Add counterpart
            for (Integer id : otherCastleLocations.keySet()) {
                enemyCastleLocations.add(Utils.getMirroredPosition(r, otherCastleLocations.get(id)));
            }
        } else if (r.turn == 5) {
            // Compute the new max number of pilgrims to build
            int depositCount = Utils.getFuelPoints(r).size() + Utils.getKarbonitePoints(r).size();
            maxInitialPilgrimsToBuild = depositCount / (2 * enemyCastleLocations.size());
        }
    }

    private static void handleEnemyCastleKilledMessages(MyRobot r) {
        // Check for enemy castle killed messages
        for (Robot robot : r.getVisibleRobots()) {
            if (CastleTalkUtils.enemyCastleKilled(r, robot)) {
                // Figure out which one was killed
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

    private static boolean broadcastEnemyCastleLocationIfNeeded(MyRobot r) {
        if (enemyCastleLocationIndex < enemyCastleLocations.size()) {
            if (CommunicationUtils.sendEnemyCastleLocation(r, enemyCastleLocations.get(enemyCastleLocationIndex))) {
                enemyCastleLocationIndex++;
                return true;
            }
        }
        return false;
    }

    private static void handleCastleTalk(MyRobot r) {
        handleCastleLocationMessages(r);
        handleEnemyCastleKilledMessages(r);
    }

    private static void removeDeadFriendlyCastles(MyRobot r) {
        Robot[] robots = r.getVisibleRobots();
        int deadCastle = -1;
        for (Integer castleId : otherCastleLocations.keySet()) {
            boolean foundId = false;
            for (Robot robot : robots) {
                if (robot.id == castleId) {
                    foundId = true;
                }
            }
            if (!foundId) {
                deadCastle = castleId;
                break;
            }
        }
        if (deadCastle != -1) {
            r.log("Castle " + deadCastle + " has died.");
            otherCastleLocations.remove(deadCastle);
        }
    }
    //https://www.programcreek.com/2013/01/leetcode-spiral-matrix-java/
    
    public static void initializeLattice(MyRobot r) {
        ArrayList<Point> result = new ArrayList<Point>();
     
        int m = 63; //row
        int n = 63; //col
     
        int left=0;
        int right=n-1;
        int top = 0;
        int bottom = m-1;
     
        while(result.size()<m*n){
            for(int j=left; j<=right; j++){
            	result.add(0,new Point(top,j));
            }
            top++;
     
            for(int i=top; i<=bottom; i++){
                result.add(0,new Point(i, right));
            }
            right--;
     
            //prevent duplicate row
            if(bottom<top)
                break;
     
            for(int j=right; j>=left; j--){
                result.add(0,new Point(bottom, j));
            }
            bottom--;
     
            // prevent duplicate column
            if(right<left)
                break;
     
            for(int i=bottom; i>=top; i--){
                result.add(0,new Point(i, left));
            }
            left++;
        }
        
        Point myLoc = new Point(r.me.x, r.me.y);
        boolean[][] passableMap = r.getPassableMap();
        boolean[][] karbMap = r.getKarboniteMap();
        boolean[][] fuelMap = r.getFuelMap();
     
        for (Point offset : result){
        	int dx = offset.x-((n-1)/2);
        	int dy = offset.y-((m-1)/2);
        	//checkerboard pattern
        	if ((dx+dy+200)%2!=(myLoc.x+myLoc.y)%2){
        		continue;
        	}
        	/*
        	if (Math.abs(dx) < 2 || Math.abs(dy) < 2){
        		continue;
        	}
        	*/
        	Point mapLoc = new Point(myLoc.x+dx, myLoc.y+dy);
        	r.log("dx: "+dx+" dy: "+dy);
        	
        	if (mapLoc.x>=0 && mapLoc.x < passableMap[0].length && mapLoc.y>=0 && mapLoc.y < passableMap.length 
        			&& passableMap[mapLoc.y][mapLoc.x] && !karbMap[mapLoc.y][mapLoc.x] && !fuelMap[mapLoc.y][mapLoc.x]){
        		latticeLocations.add(mapLoc);
        		//r.log("X: "+mapLoc.x+" Y: "+mapLoc.y);
        	}
        }
        
    }

    public static Action act(MyRobot r) {

        handleCastleTalk(r);
        removeDeadFriendlyCastles(r);

        // If it's close to the end of the game, and we're down Castles, send attack message!
        if (r.turn >= Constants.ATTACK_TURN && r.turn % 50 == 0 && otherCastleLocations.size() + 1 < enemyCastleLocations.size()) {
            r.log("It's late game... sending attack message.");
            // TODO this propogates like a virus, which can waste fuel. Modify it to just send one global message
            // TODO This involves making it save enough fuel late in the game which is why it hasn't been done yet
            // Try to maximize our range
            for (int i = 4; i > 0; i--) {
                boolean sentSuccessfully = CommunicationUtils.sendAttackMessage(r, i * i);
                if (sentSuccessfully) {
                    break;
                }
            }
        }

        // Finish up broadcasting enemy castle location if needed.
        boolean alreadyBroadcastedLocation = broadcastEnemyCastleLocationIfNeeded(r);
    	
    	// 1. If we haven't built any aggressive scout units yet, build them.
        if (initialAggressiveScoutUnitsBuilt < Constants.NUM_AGGRESSIVE_SCOUT_UNITS_TO_BUILD) {
            BuildAction action = Utils.tryAndBuildInOptimalSpace(r, r.SPECS.PROPHET);
            if (action != null) {
                CommunicationUtils.sendAggressiveScoutLocation(r, Utils.getContestedKarboniteGuardPoint(r));
                initialAggressiveScoutUnitsBuilt++;
                return action;
            }
        }
    	
        // 2. Build our initial pilgrims if we haven't built them yet.
        if (initialPilgrimsBuilt < maxInitialPilgrimsToBuild) {
        	BuildAction action = Utils.tryAndBuildInOptimalSpace(r, r.SPECS.PILGRIM);
            if (action != null) {
                initialPilgrimsBuilt++;
                return action;
            }
        }

        // 3. Spam crusaders at end of game
        if (r.turn > Constants.CASTLE_SPAM_CRUSADERS_TURN_THRESHOLD) {
            if (r.turn < Constants.FUEL_CAP_TURN_THRESHOLD || r.fuel > Constants.FUEL_CAP) {
                BuildAction action = Utils.tryAndBuildInRandomSpace(r, r.SPECS.CRUSADER);
                if (action != null) {
                    enemyCastleLocationIndex = 1;
                    if (!alreadyBroadcastedLocation) {
                        broadcastEnemyCastleLocationIfNeeded(r);
                    }
                    return action;
                }
            }
        }

        // 4. Build a prophet.
        if (r.turn <= Constants.CASTLE_CREATE_COMBAT_PROPHETS_TURN_THRESHOLD) {
            // Only build a prophet if we've detected enemies nearby and there are no prophets
            int numEnemyUnits = Utils.getRobotsInRange(r, -1, false, 0, 1000).size();
            int numFriendlyProphets = Utils.getRobotsInRange(r, r.SPECS.PROPHET, true, 0, 1000).size();
            if (numFriendlyProphets < numEnemyUnits) {
                BuildAction action = Utils.tryAndBuildInOptimalSpace(r, r.SPECS.PROPHET);
            
                if (action != null) {
	                CommunicationUtils.sendTurtleLocation(r, latticeLocations.get(turtleUnitsBuilt));
	                turtleUnitsBuilt++;
	                return action;
	            }
            }
        } else {
            if (r.turn < Constants.FUEL_CAP_TURN_THRESHOLD || r.fuel > Constants.FUEL_CAP) {
                BuildAction action = Utils.tryAndBuildInOptimalSpace(r, r.SPECS.PROPHET);
                /* Commented out for now in favor of telling it where to go on the lattice
                if (action != null) {
                    enemyCastleLocationIndex = 1;
                    if (!alreadyBroadcastedLocation) {
                        broadcastEnemyCastleLocationIfNeeded(r);
                    }
                    return action;
                }
                */
                if (action != null) {
                    CommunicationUtils.sendTurtleLocation(r, latticeLocations.get(turtleUnitsBuilt));
                    turtleUnitsBuilt++;
                    return action;
                }
            }
        }

        // 6. Finally, if we cannot build anything, attack if there are enemies in range.
        AttackAction attackAction = Utils.tryAndAttack(r, Constants.CASTLE_ATTACK_RADIUS_SQ);
        if (attackAction != null) {
            return attackAction;
        }

        return null;
    }

}

