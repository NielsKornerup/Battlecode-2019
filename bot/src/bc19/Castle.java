package bc19;
import java.util.*;

public class Castle {

    private static int initialAggressiveScoutUnitsBuilt = 0;

    private static HashMap<Integer, Point> pilgrimToTarget = new HashMap<>();

    private static int enemyCastleLocationIndex = Constants.MAX_NUM_CASTLES; // Starts greater than # castles index

    private static HashMap<Integer, Point> otherCastleLocations = new HashMap<>(); // Maps from unit ID to location
    private static ArrayList<Point> enemyCastleLocations = new ArrayList<>(); // Doesn't include the enemy castle that mirrors ours

    private static int tick = 0; // Used to prevent one castle from building more than other castles
    private static int tickMax = 1; // Threshold for building again

    private static int buildTypeTick = 0;
    private static final int UNIT_TYPE_MODULUS = 10;

    private static int churchesToAllowBuilding = 0;
    private static KarbFuelTargetQueue pilgrimLocationQueue = null;
    private static Point mostContestedPoint = null; // Tracks the point closest to enemy that aggressive unit should go for

    private static Lattice lattice;

    private static int numFirstTwoPilgrimsBuilt = 0;

    public static int pickUnitToBuild(MyRobot r) {
        // NOTE: THIS METHOD MUST OPERATE COMPLETELY DETERMINISTICALLY AND NOT BE BASED ON TURN #
        if (Utils.getRobotsInRange(r, r.SPECS.PILGRIM, true, 0, 1000).size() >= 2
                && Utils.getRobotsInRange(r, r.SPECS.PROPHET, true, 0, 1000).size() < 4) {
            return r.SPECS.PROPHET;
        }

        int value = buildTypeTick % UNIT_TYPE_MODULUS;

        if (value == 9 && churchesToAllowBuilding > 0) {
            return r.SPECS.CHURCH;
        }

        if (pilgrimLocationQueue != null && pilgrimLocationQueue.isEmpty()) {
            return r.SPECS.PROPHET;
        }

        if (value < 4) {
            return r.SPECS.PROPHET;
        } else if (value < 6) {
            return r.SPECS.PILGRIM;
        } else {
            return r.SPECS.PROPHET;
        }
    }

    public static void handleCastleLocationMessages(MyRobot r) {
        if (r.turn == 1) {
            // Send our X coordinate
            CastleTalkUtils.sendCastleCoord(r, r.me.x);

            //enemyCastleLocations
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
        } else if (r.turn == 5) {
            // Remove our entry (in case we read our own broadcast)
            otherCastleLocations.remove(r.me.id);

            // Populate our enemy castle locations
            enemyCastleLocations.add(Utils.getMirroredPosition(r, Utils.myLocation(r))); // Add counterpart
            for (Integer id : otherCastleLocations.keySet()) {
                enemyCastleLocations.add(Utils.getMirroredPosition(r, otherCastleLocations.get(id)));
            }

            // Initialize variables that depend on castle locations
            lattice = new Lattice(r, otherCastleLocations, enemyCastleLocations);
            pilgrimLocationQueue = new KarbFuelTargetQueue(r, otherCastleLocations, enemyCastleLocations);
            churchesToAllowBuilding = computeNumChurchesToAllowBuilding(r, pilgrimLocationQueue.getAllCastlePilgrimBuildLocations());
            mostContestedPoint = pilgrimLocationQueue.getMostContestedPoint();
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

    private static void handleFriendlyPilgrimSpawnedMessages(MyRobot r) {
        for (Robot robot : r.getVisibleRobots()) {
            if (robot.id == r.me.id) {
                continue;
            }
            if (CastleTalkUtils.friendlyPilgrimSpawned(r, robot)) {
                Point removed = pilgrimLocationQueue.dequeue();
                // Sanity check
                if (removed.x != -1) {
                    r.log("Popped one of our own locations! This should not happen.");
                }
            }
        }
    }

    private static void handleCastleTalk(MyRobot r) {
        handleCastleLocationMessages(r);
        handleEnemyCastleKilledMessages(r);
    }
    
    private static boolean closestToEnemyCastle(MyRobot r) {
    	int closestSquareDistanceForOtherCastles = Constants.MAX_INT;
    	for(Point mine: Castle.otherCastleLocations.values()) {
    		for(Point p: enemyCastleLocations) {
    			closestSquareDistanceForOtherCastles = Math.min(closestSquareDistanceForOtherCastles, Utils.computeSquareDistance(mine, p));
    		}
    	}
    	
    	int closestSquareDistanceForMe = Constants.MAX_INT;
    	for(Point p: enemyCastleLocations) {
    		closestSquareDistanceForMe = Math.min(closestSquareDistanceForMe, Utils.computeSquareDistance(Utils.myLocation(r), p));
    	}
    	
    	return closestSquareDistanceForMe <= closestSquareDistanceForOtherCastles;
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
        Point toBuild = pilgrimLocationQueue.peek();
        if (toBuild == null) {
            return;
        }

        if (toBuild.x == -1) {
            // Not our responsibility, but check if castle is dead and remove
            if (!otherCastleLocations.containsKey(toBuild.y)) {
                Point removed = pilgrimLocationQueue.dequeue();
                cleanupPilgrimQueue(r);
                return;
            }
        }

        handleFriendlyPilgrimSpawnedMessages(r);
    }

    private static BuildAction buildPilgrimIfNeeded(MyRobot r) {
        // Check if its our turn
        Point pilgrimTarget = pilgrimLocationQueue.peek();
        if (pilgrimTarget == null) {
            return null;
        }
        // Invariant: pilgrimTarget will never be the Node for a dead castle

        if (pilgrimTarget.x != -1) {
            // Our turn to build
            // TODO keep a counter if you're unable to build so you don't stall indefinitely
            CastleTalkUtils.sendFriendlyPilgrimSpawned(r);
            // TODO check to make sure we can both produce a unit and send a message to it
            CommunicationUtils.sendPilgrimTargetMessage(r, pilgrimTarget, CommunicationUtils.PILGRIM_TARGET_RADIUS_SQ);
            BuildAction action = Utils.tryAndBuildInDirectionOf(r, pilgrimTarget, r.SPECS.PILGRIM);
            if (action != null) {
                CastleTalkUtils.sendFriendlyPilgrimSpawned(r);
                Point result = pilgrimLocationQueue.dequeue();
                return action;
            } else {
                CastleTalkUtils.invalidate(r);
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

    private static int computeNumChurchesToAllowBuilding(MyRobot r, ArrayList<Point> candidates) {
        List<Point> centroids = Utils.getClusterLocations(candidates);

        List<Point> toBuild = new ArrayList<>();
        // Eliminate centroids that are within our radius
        for (Point point : centroids) {
            boolean shouldBuild = Utils.computeEuclideanDistance(Utils.myLocation(r), point) > Constants.MIN_CHURCH_BUILD_DISTANCE;
            for (Point otherCastleLoc : otherCastleLocations.values()) {
                if (Utils.computeEuclideanDistance(otherCastleLoc, point) < Constants.MIN_CHURCH_BUILD_DISTANCE) {
                    shouldBuild = false;
                    break;
                }
            }
            if (shouldBuild) {
                toBuild.add(point);
            }
        }

        r.log("We are going to build " + toBuild.size() + " churches.");
        return toBuild.size();
    }

    private static boolean pilgrimHadChanceToBuildChurch = false;
    private static void decrementChurchesToAllowBuildingIfNecessary(MyRobot r) {
        // Check if currently trying to build churches
        if (pickUnitToBuild(r) != r.SPECS.CHURCH) {
            return;
        }

        if (!pilgrimHadChanceToBuildChurch
                && r.fuel >= Utils.getSpecs(r, r.SPECS.CHURCH).CONSTRUCTION_FUEL
                && r.karbonite >= Utils.getSpecs(r, r.SPECS.CHURCH).CONSTRUCTION_KARBONITE) {
            pilgrimHadChanceToBuildChurch = true;
        } else if (pilgrimHadChanceToBuildChurch && Utils.canBuild(r, r.SPECS.CHURCH)) {
            churchesToAllowBuilding--;
            buildTypeTick++;
            pilgrimHadChanceToBuildChurch = false;
        } else {
            pilgrimHadChanceToBuildChurch = false;
        }

        for (Robot robot : r.getVisibleRobots()) {
            if (CastleTalkUtils.pilgrimDoneBuildingChurch(r, robot)) {
                churchesToAllowBuilding--;
                buildTypeTick++;
            }
        }
    }

    private static Point getClosestOtherCastleLocation(MyRobot r) {
        Point myLoc = new Point(r.me.x, r.me.y);
        int bestDist = Constants.MAX_INT;
        Point loc = null;
        for (Point enemyCastleLoc : enemyCastleLocations) {
            int dist = Utils.computeManhattanDistance(myLoc, enemyCastleLoc);
            if (dist < bestDist) {
                bestDist = dist;
                loc = enemyCastleLoc;
            }
        }
        if(loc == null) {
            r.log("Could not find any enemy castle locations");
        }
        return loc;
    }

    private static int getMinDistBetweenTwoCastles(MyRobot r) {
        ArrayList<Point> friendlyCastleLocs = new ArrayList<>();
        friendlyCastleLocs.add(Utils.myLocation(r));
        for (Integer id : otherCastleLocations.keySet()) {
            friendlyCastleLocs.add(otherCastleLocations.get(id));
        }

        int minDist = Constants.MAX_INT;
        for (Point friendly : friendlyCastleLocs) {
            for (Point enemy : enemyCastleLocations) {
                int distance = Utils.computeManhattanDistance(friendly, enemy);
                if (distance < minDist) {
                    minDist = distance;
                }
            }
        }
        return minDist;
    }

    public static boolean shouldRush(MyRobot r) {
        // TODO MAKE THIS A LOT BETTER
        // Castles are close together and there's only 1 castle
        return getMinDistBetweenTwoCastles(r) <= 15 && otherCastleLocations.size() == 0;
    }

    private static Action doRush(MyRobot r) {
        BuildAction action = Utils.tryAndBuildInOptimalSpace(r, r.SPECS.PREACHER);
        if (action != null) {
            CommunicationUtils.sendTurtleLocation(r, getClosestOtherCastleLocation(r));
            return action;
        }
        return null;
    }

    private static void calculateTickMax(MyRobot r) {
        // Protect against rush
        if (r.turn < Constants.TURN_THRESHOLD_PRIORITIZE_CLOSEST_CASTLE) {
            // TODO make this weighted
            tickMax = closestToEnemyCastle(r) ? 1 : otherCastleLocations.size() + 1;
        } else {
            tickMax = otherCastleLocations.size() + 1;
        }
    }

    public static Action act(MyRobot r) {
        removeDeadFriendlyCastles(r);
        calculateTickMax(r);
        if (r.turn > 5 && pilgrimLocationQueue != null) {
            cleanupPilgrimQueue(r);
        }
        decrementChurchesToAllowBuildingIfNecessary(r);
        updatePilgrimLocations(r);
        handleCastleTalk(r);


        // If it's close to the end of the game, and we're down Castles, send attack message!
        /*if (r.turn >= Constants.ATTACK_TURN && r.turn % 50 == 0 && otherCastleLocations.size() + 1 < enemyCastleLocations.size()) {
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
        }*/

        // Finish up broadcasting enemy castle location if needed. Commented in favor of telling where to go on lattice
        // boolean alreadyBroadcastedLocation = broadcastEnemyCastleLocationIfNeeded(r);

        // Build our initial 2 pilgrims for closest karb and fuel locations
        if (numFirstTwoPilgrimsBuilt == 0) {
            // Build karb pilgrim
            Point closestKarbPoint = Utils.getClosestKarbonitePoint(r);
            CommunicationUtils.sendPilgrimTargetMessage(r, closestKarbPoint, CommunicationUtils.PILGRIM_TARGET_RADIUS_SQ);
            BuildAction action = Utils.tryAndBuildInDirectionOf(r, closestKarbPoint, r.SPECS.PILGRIM);
            numFirstTwoPilgrimsBuilt++;
            return action;
        } else if (numFirstTwoPilgrimsBuilt == 1) {
            // Build Karb pilgrim if available, otherwise build fuel pilgrim
            List<Point> closestKarbonitePoints = Utils.getClosestKarbonitePoints(r, Utils.myLocation(r));
            Point pointToTarget;
            if (closestKarbonitePoints.size() < 2) {
                pointToTarget = Utils.getClosestFuelPoint(r);
            } else {
                pointToTarget = closestKarbonitePoints.get(1);
            }
            CommunicationUtils.sendPilgrimTargetMessage(r, pointToTarget, CommunicationUtils.PILGRIM_TARGET_RADIUS_SQ);
            BuildAction action = Utils.tryAndBuildInDirectionOf(r, pointToTarget, r.SPECS.PILGRIM);
            numFirstTwoPilgrimsBuilt++;
            return action;
        }

        if (shouldRush(r)) {
            return doRush(r);
        }

        // TODO figure out when to intersperse building combat units
        if (r.turn > 5 && pilgrimLocationQueue != null && pickUnitToBuild(r) == r.SPECS.PILGRIM) {
            // 2. Build pilgrims if its our turn
            BuildAction action = buildPilgrimIfNeeded(r);
            if (action != null) {
                buildTypeTick++;
                return action;
            }
        }

        // 1. If we haven't built any aggressive scout units yet, build them.
        if (Utils.getRobotsInRange(r, r.SPECS.PROPHET, true, 0, 1000).size() >= 4
                && mostContestedPoint != null
                && initialAggressiveScoutUnitsBuilt < Constants.NUM_AGGRESSIVE_SCOUT_UNITS_TO_BUILD) {
            Point toGo = Utils.getNonResourceSpotAround(r, mostContestedPoint);
            if (toGo != null) {
                BuildAction action = Utils.tryAndBuildInOptimalSpace(r, r.SPECS.PROPHET);
                if (action != null) {
                    // Send our combat unit to the spot we're assigned to that's the closest to enemy
                    r.log("Sending aggressive unit to " + toGo.x + " " + toGo.y);
                    CommunicationUtils.sendAggressiveScoutLocation(r, toGo);
                    initialAggressiveScoutUnitsBuilt++;
                    return action;
                }
            }
        }

        // 3. Spam crusaders at end of game
        if (r.turn > Constants.CASTLE_SPAM_CRUSADERS_TURN_THRESHOLD) {
            if (r.turn < Constants.FUEL_CAP_TURN_THRESHOLD || r.fuel > Constants.FUEL_CAP) {
                BuildAction action = Utils.tryAndBuildInOptimalSpace(r, r.SPECS.CRUSADER);
                if (action != null) {
                    Point crusaderLocation = lattice.popCrusaderLatticeLocation();
                    if (crusaderLocation != null) {
                        CommunicationUtils.sendTurtleLocation(r, crusaderLocation);
                        return action;
                    } else {
                        r.log("Not spawning crusader because nowhere to send it.");
                    }
                    /*
                    enemyCastleLocationIndex = 1;
                    if (!alreadyBroadcastedLocation) {
                        broadcastEnemyCastleLocationIfNeeded(r);
                    }
                    */
                }
            }
        }

        // 4. Build a prophet.
        if (pickUnitToBuild(r) == r.SPECS.PROPHET && lattice != null) {
            if (r.turn < Constants.FUEL_CAP_TURN_THRESHOLD || r.fuel > Constants.FUEL_CAP) {
                // TODO ignore the tick if in emergency (i.e. enemy military is approaching)
                // TODO prioritize the castle closer to the enemy at the beginning of the game
                // Increment tick when we have the opportunity to build
                if (Utils.canBuild(r, r.SPECS.PROPHET) && tick < tickMax) {
                    tick++;
                }
                if (tick >= tickMax) {
                    BuildAction action = Utils.tryAndBuildInOptimalSpace(r, r.SPECS.PROPHET);
                    if (action != null) {
                    /* Commented out for now in favor of telling it where to go on the lattice
                        enemyCastleLocationIndex = 1;
                    /*if (!alreadyBroadcastedLocation) {
                        broadcastEnemyCastleLocationIfNeeded(r);
                    }*/
                        Point prophetLocation = lattice.popProphetLatticeLocation();
                        if (prophetLocation != null) {
                            tick = 0;
                            CommunicationUtils.sendTurtleLocation(r, prophetLocation);
                            buildTypeTick++;
                            return action;
                        } else {
                            r.log("Not spawning prophet because nowhere to send it.");
                        }
                    }
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

