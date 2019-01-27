package bc19;
import java.util.*;

public class Castle {

    private static int initialAggressiveScoutUnitsBuilt = 0;

    private static HashMap<Integer, Point> pilgrimToTarget = new HashMap<>();

    private static int enemyCastleLocationIndex = Constants.MAX_NUM_CASTLES; // Starts greater than # castles index

    private static HashMap<Integer, Point> otherCastleLocations = new HashMap<>(); // Maps from unit ID to location
    private static ArrayList<Point> enemyCastleLocations = new ArrayList<>(); // Doesn't include the enemy castle that mirrors ours

    private static KarbFuelTargetQueue pilgrimLocationQueue = null;
    private static Point mostContestedPoint = null; // Tracks the point closest to enemy that aggressive unit should go for

    private static Lattice lattice;

    // Variables related to economy management
    private static int churchesToAllowBuilding = 0;
    private static int numFirstTwoPilgrimsBuilt = 0;
    private static int numCombatUnitsToSurviveRush = 2;
    private static boolean waitingToBuildChurch = false;

    private static int buildTurnTick = 0;

    private static int MAX_BUILD_TURN_TICK = 2;
    private static boolean castleKilled = false;

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
                    castleKilled = true;
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

    private static double onePointFivedDistanceToEnemyLocation(MyRobot r, Point point) {
        double smallestDistance = Constants.MAX_INT;
        for (Point enemy : enemyCastleLocations) {
            double dist = Math.pow(Utils.computeEuclideanDistance(point, enemy), 1.5);
            if (dist < smallestDistance) {
                smallestDistance = dist;
            }
        }
        return smallestDistance;
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
        if (!waitingToBuildChurch) {
            return;
        }

        // Check if currently trying to build churches
        if (!pilgrimHadChanceToBuildChurch
                && r.fuel >= Utils.getSpecs(r, r.SPECS.CHURCH).CONSTRUCTION_FUEL
                && r.karbonite >= Utils.getSpecs(r, r.SPECS.CHURCH).CONSTRUCTION_KARBONITE) {
            pilgrimHadChanceToBuildChurch = true;
        } else if (pilgrimHadChanceToBuildChurch && Utils.canBuild(r, r.SPECS.CHURCH)) {
            churchesToAllowBuilding--;
            waitingToBuildChurch = false;
            pilgrimHadChanceToBuildChurch = false;
        } else {
            pilgrimHadChanceToBuildChurch = false;
        }

        for (Robot robot : r.getVisibleRobots()) {
            if (CastleTalkUtils.pilgrimDoneBuildingChurch(r, robot)) {
                churchesToAllowBuilding--;
                waitingToBuildChurch = false;
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
        return r.me.team == 0;
        // return getMinDistBetweenTwoCastles(r) <= 15 && otherCastleLocations.size() == 0;
    }

    private static Action doRush(MyRobot r) {
        if (castleKilled) {
            // TODO : reduce this radius size
            r.log("Castle killed so sending global new target location " + getClosestOtherCastleLocation(r).toString() + " distance " + r.karboniteMap.length);
            CommunicationUtils.sendTurtleLocation(r, getClosestOtherCastleLocation(r), r.karboniteMap.length);
        }

        BuildAction action = Utils.tryAndBuildInOptimalSpace(r, r.SPECS.PREACHER);
        if (action != null) {
            if(r.turn >= Constants.START_RUSH_CLUMPING) {
                CommunicationUtils.sendRushClumpMessage(r, getClosestOtherCastleLocation(r));
            } else {
                CommunicationUtils.sendTurtleLocation(r, getClosestOtherCastleLocation(r));
            }
            return action;
        }
        return null;
    }

    private static Action buildFirstTwoPilgrims(MyRobot r) {
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
        return null;
    }

    private static void calculateBuildTurnTickMax(MyRobot r) {
        if (r.turn <= 5) {
            MAX_BUILD_TURN_TICK = 2;
        } else if (r.turn <= Constants.TURN_THRESHOLD_PRIORITIZE_CLOSE_CASTLES) {
            // 1 castle: I have 1
            // 2 castles: everyone has 2
            // 3 castles: everyone has 3

            // Distribute in weighted fashion based on distance to closest enemy castle
            double sum = 0;

            double myValue = onePointFivedDistanceToEnemyLocation(r, Utils.myLocation(r));
            double smallest = myValue;
            sum += myValue;
            for (Point location : otherCastleLocations.values()) {
                double other = onePointFivedDistanceToEnemyLocation(r, location);
                sum += other;
                if (other < smallest) {
                    smallest = other;
                }
            }

            int totalCastles = otherCastleLocations.size() + 1;
            MAX_BUILD_TURN_TICK = (int) Math.round((myValue / sum) * totalCastles * totalCastles);

            if (r.turn < 7 && smallest == myValue) {
                r.log("Setting build turn tick");
                buildTurnTick = MAX_BUILD_TURN_TICK - 1;
            }
        } else {
            // Distribute evenly
            MAX_BUILD_TURN_TICK = otherCastleLocations.size() + 1;
        }
    }

    private static void computeCombatUnitsToSurviveRush(MyRobot r) {
        int numEnemyProphets = Utils.getRobotsInRange(r, r.SPECS.PROPHET, false, 0, 1000).size();
        int numEnemyPreachers = Utils.getRobotsInRange(r, r.SPECS.PREACHER, false, 0, 1000).size();
        int numEnemyCrusaders = Utils.getRobotsInRange(r, r.SPECS.CRUSADER, false, 0, 1000).size();
        int totalEnemyMilitary = numEnemyProphets + numEnemyPreachers + numEnemyCrusaders;
        numCombatUnitsToSurviveRush = totalEnemyMilitary + 3;
    }

    private static void computeEconomyVariables(MyRobot r) {
        calculateBuildTurnTickMax(r);
        decrementChurchesToAllowBuildingIfNecessary(r);
        computeCombatUnitsToSurviveRush(r);

        // Allow for building a church
        if (r.turn % 10 == 9 && churchesToAllowBuilding > 0) {
            waitingToBuildChurch = true;
        }
    }

    private static BuildAction buildCrusader(MyRobot r) {
        if (Utils.canBuild(r, r.SPECS.CRUSADER) && buildTurnTick < MAX_BUILD_TURN_TICK) {
            buildTurnTick++;
        }

        if (buildTurnTick < MAX_BUILD_TURN_TICK) {
            return null;
        }

        BuildAction action = Utils.tryAndBuildInOptimalSpace(r, r.SPECS.CRUSADER);
        if (action != null) {
            Point crusaderLocation = lattice.popCrusaderLatticeLocation();
            if (crusaderLocation != null) {
                CommunicationUtils.sendTurtleLocation(r, crusaderLocation);
                buildTurnTick = 0;
                return action;
            } else {
                r.log("Not spawning crusader because nowhere to send it.");
            }
        }

        return null;
    }

    private static BuildAction buildProphet(MyRobot r) {
        if (Utils.canBuild(r, r.SPECS.PROPHET) && buildTurnTick < MAX_BUILD_TURN_TICK) {
            buildTurnTick++;
        }

        if (buildTurnTick < MAX_BUILD_TURN_TICK) {
            return null;
        }

        BuildAction action = Utils.tryAndBuildInOptimalSpace(r, r.SPECS.PROPHET);
        if (action != null) {
            Point prophetLocation = lattice.popProphetLatticeLocation();
            if (prophetLocation != null) {
                CommunicationUtils.sendTurtleLocation(r, prophetLocation);
                buildTurnTick = 0;
                return action;
            } else {
                r.log("Not spawning prophet because nowhere to send it.");
            }
        }
        return null;
    }

    public static Action act(MyRobot r) {
        castleKilled = false;
        removeDeadFriendlyCastles(r);
        if (r.turn > 5 && pilgrimLocationQueue != null) {
            cleanupPilgrimQueue(r);
        }
        computeEconomyVariables(r);
        updatePilgrimLocations(r);
        handleCastleTalk(r);

        // 0. Build first two pilgrims
        Action buildAction = buildFirstTwoPilgrims(r);
        if (buildAction != null) {
            return buildAction;
        }

        // 1A. Rush if we've determined we want rush bot
        if (shouldRush(r)) {
            return doRush(r);
        }

        // 1B. Build combat units to survive rush
        int numFriendlyProphets = Utils.getRobotsInRange(r, r.SPECS.PROPHET, true, 0, 81).size();
        if (r.turn > 5 && numFriendlyProphets < numCombatUnitsToSurviveRush) {
            BuildAction action = buildProphet(r);
            if (action != null) {
                return action;
            }
        }

        // TODO move to appropriate spot
        // Don't spawn units if waiting to build church
        if (waitingToBuildChurch) {
            AttackAction attackAction = Utils.tryAndAttack(r, Constants.CASTLE_ATTACK_RADIUS_SQ);
            if (attackAction != null) {
                return attackAction;
            }
            return null;
        }

        // 2. Build remaining pilgrims
        if (r.turn > 5 && pilgrimLocationQueue != null && !pilgrimLocationQueue.isEmpty()) {
            BuildAction action = buildPilgrimIfNeeded(r);
            if (action != null) {
                return action;
            }
        }

        // 3. Build aggressive scouts
        if (numFriendlyProphets >= numCombatUnitsToSurviveRush && r.turn > 5 && mostContestedPoint != null && initialAggressiveScoutUnitsBuilt < Constants.NUM_AGGRESSIVE_SCOUT_UNITS_TO_BUILD) {
            if (Utils.canBuild(r, r.SPECS.PROPHET) && buildTurnTick < MAX_BUILD_TURN_TICK) {
                buildTurnTick++;
            }
            if (buildTurnTick >= MAX_BUILD_TURN_TICK) {
                Point toGo = Utils.getNonResourceSpotAround(r, mostContestedPoint);
                if (toGo != null) {
                    BuildAction action = Utils.tryAndBuildInOptimalSpace(r, r.SPECS.PROPHET);
                    if (action != null) {
                        // Send our combat unit to the spot we're assigned to that's the closest to enemy
                        r.log("Sending aggressive unit to " + toGo.x + " " + toGo.y);
                        CommunicationUtils.sendAggressiveScoutLocation(r, toGo);
                        initialAggressiveScoutUnitsBuilt++;
                        buildTurnTick = 0;
                        return action;
                    }
                }
            }
        }

        // 4. Spam crusaders if end game
        if (r.turn > Constants.CASTLE_SPAM_CRUSADERS_TURN_THRESHOLD) {
            if (r.turn < Constants.FUEL_CAP_TURN_THRESHOLD || r.fuel > Constants.FUEL_CAP) {
                BuildAction action = buildCrusader(r);
                if (action != null) {
                    return action;
                }
            }
        }

        // 5. Contribute to prophet turtle
        if (r.turn > 5) {
            if (r.turn < Constants.FUEL_CAP_TURN_THRESHOLD || r.fuel > Constants.FUEL_CAP) {
                BuildAction action = buildProphet(r);
                if (action != null) {
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

