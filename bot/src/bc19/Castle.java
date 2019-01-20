package bc19;

import java.util.ArrayList;
import java.util.HashMap;

public class Castle {
    private static int initialPilgrimsBuilt = 0;
    private static int initialAggressiveScoutUnitsBuilt = 0;

    public static int maxInitialPilgrimsToBuild = 2;
    private static int enemyCastleLocationIndex = Constants.MAX_NUM_CASTLES; // Starts greater than # castles index

    private static HashMap<Integer, Point> otherCastleLocations = new HashMap<>(); // Maps from unit ID to location
    private static ArrayList<Point> enemyCastleLocations = new ArrayList<>(); // Doesn't include the enemy castle that mirrors ours

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
            // Compute the new max number of pilgrims to build
            int depositCount = Utils.getFuelPoints(r).size() + Utils.getKarbonitePoints(r).size();
            maxInitialPilgrimsToBuild = depositCount / (2 * enemyCastleLocations.size());
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

    public static Action act(MyRobot r) {

        handleCastleTalk(r);

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
            BuildAction action = Utils.tryAndBuildInRandomSpace(r, r.SPECS.CRUSADER);
            if (action != null) {
                enemyCastleLocationIndex = 1;
                if (!alreadyBroadcastedLocation) {
                    broadcastEnemyCastleLocationIfNeeded(r);
                }
                return action;
            }
        }

        // 4. Build a prophet.
        if (r.turn <= Constants.CASTLE_CREATE_COMBAT_PROPHETS_TURN_THRESHOLD) {
            // Only build a prophet if we've detected enemies nearby and there are no prophets
            int numEnemyUnits = Utils.getRobotsInRange(r, -1, false, 0, 1000).size();
            int numFriendlyProphets = Utils.getRobotsInRange(r, r.SPECS.PROPHET, true, 0, 1000).size();
            if (numFriendlyProphets < numEnemyUnits) {
                return Utils.tryAndBuildInOptimalSpace(r, r.SPECS.PROPHET);
            }
        } else {
            BuildAction action = Utils.tryAndBuildInOptimalSpace(r, r.SPECS.PROPHET);
            if (action != null) {
                enemyCastleLocationIndex = 1;
                if (!alreadyBroadcastedLocation) {
                    broadcastEnemyCastleLocationIfNeeded(r);
                }
                return action;
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

