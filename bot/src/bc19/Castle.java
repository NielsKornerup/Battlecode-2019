package bc19;

import java.util.*;

public class Castle {
    private static final int CASTLE_ATTACK_RADIUS_SQ = 64;
    private static int initialPilgrimsBuilt = 0;
	
    public static int CASTLE_MAX_INITIAL_PILGRIMS = 5;

    private static HashMap<Integer, Point> castleLocations = new HashMap<>(); // Maps from unit ID to location
    private static ArrayList<Point> enemyCastleLocations = new ArrayList<>();

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
    	CASTLE_MAX_INITIAL_PILGRIMS = count/(2*(1+enemyCastleLocations.size()));
    }
    
    private static HashMap<Integer, Point> otherCastleLocations = new HashMap<>(); // Maps from unit ID to location
    private static ArrayList<Point> otherEnemyCastleLocations = new ArrayList<>(); // Doesn't include the enemy castle that mirrors ours

    
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
            // Point enemy = Utils.getMirroredPosition(r, new Point(r.me.x, r.me.y));
            // r.log("[castle] Counterpart enemy castle location: " + enemy.x + " " + enemy.y);
            // r.log("[castle] Other enemy castle locations: ");
            for (Point point : otherEnemyCastleLocations) {
                r.log(point.x + " " + point.y);
            }
            Castle.computeNumPilgrimsToBuild(r);
        }
    }

    private static int enemyCastleLocationIndex = 3; // Used for broadcasting, starts greater than number of castles
    private static void broadcastEnemyCastleLocation(MyRobot r) {
        if (enemyCastleLocationIndex < otherEnemyCastleLocations.size()) {
            CommunicationUtils.sendEnemyCastleLocation(r, otherEnemyCastleLocations.get(enemyCastleLocationIndex));
            enemyCastleLocationIndex++;
        }
    }

    public static Action act(MyRobot r) {

    	getAllCastleLocations(r);

        // Finish up broadcasting if necessary
    	boolean alreadyBroadcastedEnemyCastleLocation = false;
    	if (enemyCastleLocationIndex < otherEnemyCastleLocations.size()) {
            broadcastEnemyCastleLocation(r);
            alreadyBroadcastedEnemyCastleLocation = true;
        }
    	    	
        // 1. Build our initial pilgrims if we haven't built them yet.
        if (initialPilgrimsBuilt < CASTLE_MAX_INITIAL_PILGRIMS) {
        	BuildAction action = Utils.tryAndBuildInRandomSpace(r, r.SPECS.PILGRIM);
            if (action != null) {
                initialPilgrimsBuilt++;
                //TODO: is the 3 right?
                //TODO: also track the number of pilgrims of each type
                return action;
            }
        }

        // TODO implement logic/heuristics to prevent existing units from starving Castle of building opportunities

        // 3. Build a prophet.
        if(r.turn > 50) {
            BuildAction action = Utils.tryAndBuildInRandomSpace(r, r.SPECS.PROPHET);
            if (action != null) {
                enemyCastleLocationIndex = 0;
                if (!alreadyBroadcastedEnemyCastleLocation) {
                    broadcastEnemyCastleLocation(r);
                }
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

