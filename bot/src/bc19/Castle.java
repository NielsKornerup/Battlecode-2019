package bc19;

import java.util.*;

public class Castle {

    private static int initialAggressiveScoutUnitsBuilt = 0;

    private static HashMap<Integer, Point> pilgrimToTarget = new HashMap<>();

    private static int enemyCastleLocationIndex = Constants.MAX_NUM_CASTLES; // Starts greater than # castles index

    private static HashMap<Integer, Point> otherCastleLocations = new HashMap<>(); // Maps from unit ID to location
    private static ArrayList<Point> enemyCastleLocations = new ArrayList<>(); // Doesn't include the enemy castle that mirrors ours

    private static PriorityQueue pilgrimLocationQueue = null;

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
            enemyCastleLocations.add(Utils.getMirroredPosition(r, Utils.myLocation(r))); // Add counterpart
            for (Integer id : otherCastleLocations.keySet()) {
                enemyCastleLocations.add(Utils.getMirroredPosition(r, otherCastleLocations.get(id)));
            }
        } else if (r.turn == 5) {
            pilgrimLocationQueue = generatePilgrimLocationQueue(r, otherCastleLocations);
        }
    }

    private static PriorityQueue generatePilgrimLocationQueue(MyRobot r, HashMap<Integer, Point> otherCastleLocations) {
        HashMap<Integer, Navigation> castleIdToResourceMap = new HashMap<>();
        for (Integer id : otherCastleLocations.keySet()) {
            ArrayList<Point> targets = new ArrayList<>();
            targets.add(otherCastleLocations.get(id));
            Navigation map = new Navigation(r, r.getPassableMap(), targets);
            castleIdToResourceMap.put(id, map);
        }

        ArrayList<Point> myPosition = new ArrayList<>();
        myPosition.add(Utils.myLocation(r));
        Navigation myMap = new Navigation(r, r.getPassableMap(), myPosition);
        castleIdToResourceMap.put(r.me.id, myMap);

        List<Point> resourceLocationsToConsider = Utils.getKarbonitePoints(r);
        resourceLocationsToConsider.addAll(Utils.getFuelPoints(r));

        PriorityQueue pilgrimLocationQueue = new PriorityQueue();
        for (Point point : resourceLocationsToConsider) {
            // Find the Castle ID with smallest potential
            int smallestId = -1;
            int smallestValue = 1000000;
            for (Integer id : castleIdToResourceMap.keySet()) {
                Navigation map = castleIdToResourceMap.get(id);
                int value = map.getPotential(point) * 5432 + (id % 5432);
                if (value < smallestValue) {
                    smallestId = id;
                    smallestValue = value;
                }
            }

            // TODO only enqueue locations that are closer to us than the enemy

            if (smallestId == r.me.id) {
                pilgrimLocationQueue.enqueue(new Node(smallestValue, point));
            } else {
                pilgrimLocationQueue.enqueue(new Node(smallestValue, new Point(-1, smallestId)));
            }
        }
        return pilgrimLocationQueue;
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

    private static void handleFriendlyPilgrimSpawnedMessages(MyRobot r) {
        for (Robot robot : r.getVisibleRobots()) {
            if (robot.id == r.me.id) {
                continue;
            }
            if (CastleTalkUtils.friendlyPilgrimSpawned(r, robot)) {
                Node removed = pilgrimLocationQueue.dequeue();
                // Sanity check
                if (removed.p.x != -1) {
                    r.log("Popped one of our own locations! This should not happen.");
                }
            }
        }
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

    private static void cleanupPilgrimQueue(MyRobot r) {
        Node toBuild = pilgrimLocationQueue.peek();
        if (toBuild == null) {
            r.log("No more pilgrims to build.");
            return;
        }

        if (toBuild.p.x == -1) {
            // Not our responsibility, but check if castle is dead and remove
            if (!otherCastleLocations.containsKey(toBuild.p.y)) {
                r.log("This boi is dead!");
                pilgrimLocationQueue.dequeue();
                cleanupPilgrimQueue(r);
                return;
            }
        }

        handleFriendlyPilgrimSpawnedMessages(r);
    }

    private static BuildAction buildPilgrimIfNeeded(MyRobot r) {
        // Check if its our turn
        Node pilgrimTarget = pilgrimLocationQueue.peek();
        if (pilgrimTarget == null) {
            r.log("No more pilgrims to build");
            return null;
        }
        // Invariant: pilgrimTarget will never be the Node for a dead castle

        if (pilgrimTarget.p.x != -1) {
            // Our turn to build
            // TODO keep a counter if you're unable to build so you don't stall indefinitely
            CastleTalkUtils.sendFriendlyPilgrimSpawned(r);
            // TODO check to make sure we can both produce a unit and send a message to it
            //CommunicationUtils.sendPilgrimTargetMessage(r, pilgrimTarget.p, CommunicationUtils.PILGRIM_TARGET_RADIUS_SQ);
            // TODO build pilgrims in location closest to desired karbonite spot
            BuildAction action = Utils.tryAndBuildInOptimalSpace(r, r.SPECS.PILGRIM);
            if (action != null) {
                pilgrimLocationQueue.dequeue();
                return action;
            } else {
                CastleTalkUtils.invalidate(r);
                // TODO maybe invalidate with CommunicationUtils
            }
        }
        return null;
    }

    private static void updatePilgrimLocations(MyRobot r) {
        //TODO: code this constant for the max range
        List<Robot> robots = Utils.getRobotsInRange(r, r.SPECS.PILGRIM, true, 0, 5);
        for(Robot rob: robots) {
            Point target = CommunicationUtils.getPilgrimTargetForCastle(r, rob);
            if(target != null) {
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
                pilgrimToTarget.remove(id);
                // TODO: write logic to handle a dead pilgrim here
            }
        }
    }

    public static Action act(MyRobot r) {
        removeDeadFriendlyCastles(r);
        if (r.turn > 5) { // TODO hardcoded constant
            cleanupPilgrimQueue(r);
        }
        updatePilgrimLocations(r);
        handleCastleTalk(r);

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
    	
    	/*// 1. If we haven't built any aggressive scout units yet, build them.
        if (initialAggressiveScoutUnitsBuilt < Constants.NUM_AGGRESSIVE_SCOUT_UNITS_TO_BUILD) {
            BuildAction action = Utils.tryAndBuildInOptimalSpace(r, r.SPECS.PROPHET);
            if (action != null) {
                CommunicationUtils.sendAggressiveScoutLocation(r, Utils.getContestedKarboniteGuardPoint(r));
                initialAggressiveScoutUnitsBuilt++;
                return action;
            }
        }*/

        // TODO figure out when to intersperse building combat units
        if (r.turn > 5) { // TODO hardcoded constant
            // 2. Build pilgrims if its our turn
            BuildAction action = buildPilgrimIfNeeded(r);
            if (action != null) {
                return action;
            }
        }

        /*// 3. Spam crusaders at end of game
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
                return Utils.tryAndBuildInOptimalSpace(r, r.SPECS.PROPHET);
            }
        } else {
            if (r.turn < Constants.FUEL_CAP_TURN_THRESHOLD || r.fuel > Constants.FUEL_CAP) {
                BuildAction action = Utils.tryAndBuildInOptimalSpace(r, r.SPECS.PROPHET);
                if (action != null) {
                    enemyCastleLocationIndex = 1;
                    if (!alreadyBroadcastedLocation) {
                        broadcastEnemyCastleLocationIfNeeded(r);
                    }
                    return action;
                }
            }
        }*/

        // 6. Finally, if we cannot build anything, attack if there are enemies in range.
        AttackAction attackAction = Utils.tryAndAttack(r, Constants.CASTLE_ATTACK_RADIUS_SQ);
        if (attackAction != null) {
            return attackAction;
        }

        return null;
    }

}

