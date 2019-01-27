package bc19;

import java.util.*;

public class Utils {

    public static Point findClosestPointManhattan(MyRobot r, List<Point> points) {
        Point bestPoint = null;
        int bestDistance = 100000;
        for (Point point : points) {
            int newDistance = computeManhattanDistance(Utils.myLocation(r), new Point(point.x, point.y));
            if (newDistance < bestDistance) {
                bestDistance = newDistance;
                bestPoint = point;
            }
        }
        return bestPoint;
    }

    public static boolean isNearbySpaceEmpty(MyRobot r, Point delta) {
        boolean[][] passableMap = r.getPassableMap();
        int[][] visibleRobotMap = r.getVisibleRobotMap();
        int newX = r.me.x + delta.x;
        int newY = r.me.y + delta.y;
        if (newX < 0 || newY < 0 || newY >= passableMap.length || newX >= passableMap[0].length) {
            return false;
        }
        return passableMap[newY][newX] && (visibleRobotMap[newY][newX] <= 0);
    }

    public static boolean canMove(MyRobot r, Point delta) {
         return isNearbySpaceEmpty(r, delta) && enoughFuelToMove(r, delta.x, delta.y);
    }

    public static boolean canMine(MyRobot r) {
        return r.fuel >= Constants.PILGRIM_MINE_FUEL_COST;
    }

    public static boolean canAttack(MyRobot r, Point delta) {
        int dx = delta.x;
        int dy = delta.y;
        int rSquared = dx * dx + dy * dy;

        return rSquared >= Utils.mySpecs(r).ATTACK_RADIUS[0]
                && rSquared <= Utils.mySpecs(r).ATTACK_RADIUS[1]
                && r.fuel >= Utils.mySpecs(r).ATTACK_FUEL_COST;

    }

    public static boolean canSignal(MyRobot r, int radiusSq) {
        return r.fuel >= Math.ceil(Math.sqrt(radiusSq));
    }

    public static boolean canBuild(MyRobot r, int unitToBuild) {
        return r.karbonite >= Utils.getSpecs(r, unitToBuild).CONSTRUCTION_KARBONITE && r.fuel >= Utils.getSpecs(r, unitToBuild).CONSTRUCTION_FUEL;
    }

    public static AttackAction tryAndAttack(MyRobot r, int attackRadiusSq) {
        List<RobotSort> enemiesNearby = Utils.getRobotSortInRange(r, false, 0, attackRadiusSq);
        if (enemiesNearby.size() > 0) {
            // Attack the enemy with highest priority
            for (RobotSort target : enemiesNearby) {
            	Point attackPoint = new Point(target.x - r.me.x, target.y - r.me.y);
                if (Utils.canAttack(r, attackPoint)) {
                    return r.attack(attackPoint.x, attackPoint.y);
                }
            }
        }
        return null;
    }

    public static BuildAction tryAndBuildInRandomSpace(MyRobot r, int unitToBuild) {
        ArrayList<Point> freeSpaces = Utils.getAdjacentFreeSpaces(r);
        if (freeSpaces.size() == 0) {
            return null;
        }
        Point location = freeSpaces.get((int) (Math.random() * freeSpaces.size()));
        if (canBuild(r, unitToBuild)) {
            return r.buildUnit(unitToBuild, location.x, location.y);
        }
        return null;
    }

    public static BuildAction tryAndBuildInDirectionOf(MyRobot r, Point point, int unitToBuild) {
        ArrayList<Point> freeSpaces = Utils.getAdjacentFreeSpaces(r);
        if (freeSpaces.size() == 0) {
            return null;
        }
        double minDist = 100000;
        Point myLoc = Utils.myLocation(r);
        Point bestPoint = freeSpaces.get(0);
        for (Point adj : freeSpaces) {
            Point absolute = new Point(myLoc.x + adj.x, myLoc.y + adj.y);
            double distance = computeEuclideanDistance(absolute, point);
            if (distance < minDist) {
                bestPoint = adj;
                minDist = distance;
            }
        }
        if (canBuild(r, unitToBuild)) {
            return r.buildUnit(unitToBuild, bestPoint.x, bestPoint.y);
        }
        return null;
    }
    
    public static BuildAction tryAndBuildInOptimalSpace(MyRobot r, int unitToBuild) {
        ArrayList<Point> freeSpaces = Utils.getAdjacentFreeSpaces(r);
        if (freeSpaces.size() == 0) {
            return null;
        }
        Point bestPoint = freeSpaces.get(0);
        
        Point myLoc = Utils.myLocation(r);
    	Point enemyLoc = Utils.getMirroredPosition(r, myLoc);
    	
        int smallestDistance = 1000;
        for (Point adj : freeSpaces){
        	int dist = Utils.computeManhattanDistance(new Point(adj.x+myLoc.x, adj.y+myLoc.y), enemyLoc);
        	if (dist<smallestDistance){
        		smallestDistance = dist;
        		bestPoint = adj;
        	}
        }
        if (canBuild(r, unitToBuild)) {
            return r.buildUnit(unitToBuild, bestPoint.x, bestPoint.y);
        }
        return null;
    }

    public static BuildAction tryAndBuildChurch(MyRobot r) {
        List<Point> deltas = Utils.getAdjacentFreeSpaces(r);
        if (deltas.size() == 0) {
            return null;
        }

        // Build church in spot adjacent to the most resource deposits
        Point bestDelta = deltas.get(0);
        int bestResourceCount = 0;
        for(Point delta : deltas) {
            Point absolute = new Point(r.me.x + delta.x, r.me.y + delta.y);
            if(Utils.hasResource(r, absolute)) {
                continue;
            }

            int newResourceCount = Utils.getAdjacentResourceCount(r, absolute);
            if(newResourceCount > bestResourceCount) {
                bestResourceCount = newResourceCount;
                bestDelta = delta;
            }
        }
        return r.buildUnit(r.SPECS.CHURCH, bestDelta.x, bestDelta.y);
    }

    public static Action moveDijkstra(MyRobot r, Navigation map, int radius) {
        Point delta = map.getNextMove(radius);
        if (delta != null) {
            return r.move(delta.x, delta.y);
        }
        return null;
    }

    public static Action moveDijkstraThenRandom(MyRobot r, Navigation map, int radius) {
        Action action = moveDijkstra(r, map, radius);
        if (action != null) {
            return action;
        }
        
        boolean[][] karb = r.getKarboniteMap();
        if (karb[r.me.y][r.me.x] && r.me.unit != r.SPECS.PILGRIM) {
        	return Utils.moveRandom(r);
        }

        if (r.fuel > 5 * mySpecs(r).FUEL_PER_MOVE) { // TODO: adjust this heuristic
        	return null;
            //return Utils.moveRandom(r);
        }

        return null;
    }

    public static UnitSpec getSpecs(MyRobot r, int unitType) {
        return r.SPECS.UNITS[unitType];
    }

    public static UnitSpec mySpecs(MyRobot r) {
        return getSpecs(r, r.me.unit);
    }

    public static Action moveRandom(MyRobot r) {
        ArrayList<Point> candidates = getFreeSpaces(r, (int) Math.sqrt(mySpecs(r).SPEED));
        if (candidates.size() == 0) {
            return null;
        }

        while (candidates.size() > 0) {
            int index = (int) (Math.random() * candidates.size());
            Point move = candidates.get(index);

            if (canMove(r, move)) {
                return r.move(move.x, move.y);
            }
            candidates.remove(index);
        }
        return null;

    }

    public static int getFuelCost(MyRobot r, int dx, int dy) {
        int rSquared = dx * dx + dy * dy;
        int fuelPerMove = 1;
        if (r.me.unit == r.SPECS.CRUSADER) {
            fuelPerMove = Constants.CRUSADER_FUEL_PER_MOVE;
        } else if (r.me.unit == r.SPECS.PILGRIM) {
            fuelPerMove = Constants.PILGRIM_FUEL_PER_MOVE;
        } else if (r.me.unit == r.SPECS.PREACHER) {
            fuelPerMove = Constants.PREACHER_FUEL_PER_MOVE;
        } else if (r.me.unit == r.SPECS.PROPHET) {
            fuelPerMove = Constants.PROPHET_FUEL_PER_MOVE;
        }
        return fuelPerMove * rSquared;
    }

    public static boolean enoughFuelToMove(MyRobot r, int dx, int dy) {
        return r.fuel >= getFuelCost(r, dx, dy);
    }

    /*
    Returns Point pairs of [dx, dy] that are empty (i.e. both passable and devoid of units) within a range.
    Range is NOT R SQUARED, it is just R.
     */
    public static ArrayList<Point> getFreeSpaces(MyRobot r, int range) {
        ArrayList<Point> freeSpaces = new ArrayList<>();
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                if (dx * dx + dy * dy > range * range) {
                    continue;
                }

                Point delta = new Point(dx, dy);
                if (isNearbySpaceEmpty(r, delta)) {
                    freeSpaces.add(delta);
                }
            }
        }
        return freeSpaces;
    }

    public static boolean isOn(MyRobot r, Point other) {
        return r.me.x == other.x && r.me.y == other.y;
    }

    public static boolean isAdjacentOrOn(MyRobot r, Point other) {
        return Math.abs(r.me.x - other.x) <= 1 && Math.abs(r.me.y - other.y) <= 1;
    }

    public static boolean isBetween(Point a, Point b, Point test) {
        return (b.x - a.x) * (test.x - a.x) + (b.y - a.y) * (test.y - a.y) >= 1;
    }

    public static List<Robot> getAdjacentRobots(MyRobot r, int unitType, boolean myTeam) {
        ArrayList<Robot> nearby = new ArrayList<>();
        for (Robot robot : r.getVisibleRobots()) {
            if (unitType != -1 && robot.unit != unitType) {
                continue;
            }
            if ((myTeam && (robot.team != r.me.team)) || (!myTeam && (robot.team == r.me.team))) {
                continue;
            }
            if (robot.x == r.me.x && robot.y == r.me.y) {
                continue;
            }
            if (Math.abs(robot.x - r.me.x) <= 1 && Math.abs(robot.y - r.me.y) <= 1) {
                nearby.add(robot);
            }
        }
        return nearby;
    }

    /*
    Returns Arraylist of [dx, dy] of Units that are directly adjacent (i.e. in the 8 squares around the unit).
     */
    public static List<Point> getAdjacentUnitDeltas(MyRobot r, int unitType, boolean myTeam) {
        List<Robot> nearby = getAdjacentRobots(r, unitType, myTeam);
        List<Point> deltas = new ArrayList<>();
        for (Robot robot : nearby) {
            deltas.add(new Point(robot.x - r.me.x, robot.y - r.me.y));
        }
        return deltas;
    }

    public static List<Robot> getRobotsInRange(MyRobot r, int unitType, boolean myTeam, int minRadiusSq, int maxRadiusSq) {
        ArrayList<Robot> nearby = new ArrayList<>();
        for (Robot robot : r.getVisibleRobots()) {
            if (unitType != -1 && robot.unit != unitType) {
                continue;
            }
            if ((myTeam && (robot.team != r.me.team)) || (!myTeam && (robot.team == r.me.team))) {
                continue;
            }
            if (robot.x == r.me.x && robot.y == r.me.y) {
                continue;
            }

            int distX = robot.x - r.me.x;
            int distY = robot.y - r.me.y;
            int distanceSquared = distX * distX + distY * distY;
            if (distanceSquared >= minRadiusSq && distanceSquared <= maxRadiusSq) {
                nearby.add(robot);
            }
        }
        return nearby;

    }


    public static List<Point> getUnitDeltasInRange(MyRobot r, int unitType, boolean myTeam, int minRadiusSq, int maxRadiusSq) {
        List<Robot> nearby = getRobotsInRange(r, unitType, myTeam, minRadiusSq, maxRadiusSq);
        ArrayList<Point> deltas = new ArrayList<>();
        for (Robot robot : nearby) {
            deltas.add(new Point(robot.x - r.me.x, robot.y - r.me.y));
        }
        return deltas;
    }

    public static List<RobotSort> getRobotSortInRange(MyRobot r, boolean myTeam, int minRadiusSq, int maxRadiusSq) {
        List<Robot> nearby = getRobotsInRange(r, -1, myTeam, minRadiusSq, maxRadiusSq);
        List<RobotSort> toSort = new ArrayList<>();
        for (Robot robot : nearby) {
            int distanceSquared = computeSquareDistance(Utils.myLocation(r), new Point(robot.x, robot.y));
            RobotSort rob = new RobotSort(robot.id, robot.unit, robot.x, robot.y, distanceSquared, robot.health);
            toSort.add(rob);
        }
        // TODO Collections.sort might not work
        Collections.sort(toSort);
        return toSort;
    }

    public static ArrayList<Point> getAdjacentFreeSpaces(MyRobot r) {
        ArrayList<Point> freeSpaces = new ArrayList<>();
        int[] dxes = {-1, 0, 1};
        int[] dyes = {-1, 0, 1};
        for (int dx : dxes) {
            for (int dy : dyes) {
                Point delta = new Point(dx, dy);
                if (isNearbySpaceEmpty(r, delta)) {
                    freeSpaces.add(delta);
                }
            }
        }
        return freeSpaces;
    }

    public static boolean hasResource(MyRobot r, Point loc) {
    	return r.karboniteMap[loc.y][loc.x] || r.fuelMap[loc.y][loc.x];
    }
    public static int getAdjacentResourceCount(MyRobot r, Point p) {
        int[] dxes = {-1, 0, 1};
        int[] dyes = {-1, 0, 1};
        int count = 0;
        for (int dx : dxes) {
            for (int dy : dyes) {
            	if(dx==0 && dy==0) {
            		continue;
            	}
            	if(hasResource(r, new Point(p.x + dx, p.y + dy))) {
            		count++;
            	}
            }
        }
        return count;
    }
    /*
    Determines if the map is horizontally or vertically mirrored, and returns the mirrored position of the current robot.
     */

    private static int symmetryType = 0; // 0 means unset, 1 means horizontal, 2 means vertical

    public static Point getMirroredPosition(MyRobot rob, Point position) {
        // TODO only compute this horizontal / vertical symmetry once
        boolean[][] passableMap = rob.getPassableMap();
        int ht = passableMap.length;
        int wid = passableMap[0].length;

        int locX = position.x;
        int locY = position.y;

        // Find the type of symmetry if necessary
        if (symmetryType == 0) {
            // Check for vertical symmetry
            boolean verticalSymmetry = true;
            for (int c = 0; c < wid; c++) {
                for (int r = 0; r < ht / 2 + 1; r++) {
                    if (passableMap[r][c] != passableMap[ht - r - 1][c]) {
                        verticalSymmetry = false;
                        break;
                    }

                }
            }
            symmetryType = verticalSymmetry ? 2: 1;
        }
        if (symmetryType == 2) { // Vertical symmetry
            return new Point(locX, ht - locY - 1);
        } else if (symmetryType == 1) {
            // Otherwise, horizontal symmetry
            return new Point(wid - locX - 1, locY);
        } else {
            rob.log("No symmetry set! This is bad.");
        }
        return null;
    }

    public static int computeSquareDistance(Point p1, Point p2) {
    	return (p1.x-p2.x)*(p1.x-p2.x) + (p1.y-p2.y)*(p1.y-p2.y);
    }

    public static double computeEuclideanDistance(Point p1, Point p2) {
        return Math.sqrt((double) computeSquareDistance(p1, p2));
    }

    public static int computeManhattanDistance(Point p1, Point p2) {
    	return Math.abs(p1.x-p2.x) + Math.abs(p1.y-p2.y);
    }

    public static Point getLocation(Robot r) {
    	return new Point(r.x, r.y);
    }

    public static Point myLocation(MyRobot r) {
        return new Point(r.me.x, r.me.y);
    }

    public static HashMap<Integer, ArrayList<Point>> generateRingLocations(MyRobot r, Point castle, Point enemyCastle) {
        HashMap<Integer, ArrayList<Point>> ringLocations = new HashMap<>();
        boolean[][] passableMap = r.getPassableMap();

        for (int y = 0; y < passableMap.length; y++) {
            for (int x = 0; x < passableMap[y].length; x++) {
                if (!passableMap[y][x] || !Utils.isBetween(castle, enemyCastle, new Point(x, y))) {
                    continue;
                }
                double dx = x - castle.x;
                double dy = y - castle.y;
                int distance = (int) Math.sqrt(dx * dx + dy * dy);
                if (!ringLocations.containsKey(distance)) {
                    ringLocations.put(distance, new ArrayList<>());
                }
                ringLocations.get(distance).add(new Point(x, y));
            }
        }
        return ringLocations;
    }
    // Must be called immediately after spawning to work

    public static Point getSpawningCastleOrChurchLocation(MyRobot r) {
        List<Point> initialCastleDeltas = Utils.getAdjacentUnitDeltas(r, r.SPECS.CASTLE, true);
        initialCastleDeltas.addAll(Utils.getAdjacentUnitDeltas(r, r.SPECS.CHURCH, true));
        if (initialCastleDeltas.size() > 0) {
            Point initialCastleDelta = initialCastleDeltas.get(0);
            return new Point(r.me.x + initialCastleDelta.x, r.me.y + initialCastleDelta.y);
        }
        return null;
    }

	public static List<Point> getFuelPoints(MyRobot r) {
        boolean[][] fuelMap = r.getFuelMap();

        List<Point> targets = new ArrayList<>();
        for (int y = 0; y < fuelMap.length; y++) {
            for (int x = 0; x < fuelMap[y].length; x++) {
                if (fuelMap[y][x]) {
                    targets.add(new Point(x, y));
                }
            }
        }
        return targets;
    }

    public static List<Point> getKarbonitePoints(MyRobot r) {
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

    public static Point getClosestKarbonitePoint(MyRobot r, Point point) {
        List<Point> karbPoints = getKarbonitePoints(r);
        int bestDist = Constants.MAX_INT;
        Point closest = null;
        for(Point kp : karbPoints) {
            // TODO ensure that this is deterministic with how ties work
            int dist = computeManhattanDistance(point, kp);
            if(dist < bestDist) {
                bestDist = dist;
                closest = kp;
            }
        }
        if(closest == null) {
            r.log("found no karbonite points");
        }
        return closest;
    }

    public static Point getClosestKarbonitePoint(MyRobot r) {
        return getClosestKarbonitePoint(r, Utils.myLocation(r));
    }

    /*
    Returns the two closest Karbonite points, if they are close to each other. Otherwise returns just the closest.
     */
    public static List<Point> getClosestKarbonitePoints(MyRobot r, Point point) {
        List<Point> karbPoints = getKarbonitePoints(r);
        int bestDist = Constants.MAX_INT;
        Point closest = null;
        int secondBestDist = Constants.MAX_INT;
        Point secondClosest = null;
        for (Point kp : karbPoints) {
            int dist = computeManhattanDistance(point, kp);
            if (dist < bestDist) {
                secondBestDist = bestDist;
                secondClosest = closest;

                bestDist = dist;
                closest = kp;
            } else if (dist < secondBestDist) {
                secondBestDist = dist;
                secondClosest = kp;
            }
        }
        List<Point> closestPoints = new ArrayList<>();
        closestPoints.add(closest);
        if (Utils.computeEuclideanDistance(point, secondClosest) <= 5) {
            closestPoints.add(secondClosest);
        }
        return closestPoints;
    }

    public static Point getClosestFuelPoint(MyRobot r, Point point) {
        List<Point> fuelPoints = getFuelPoints(r);
        int bestDist = Constants.MAX_INT;
        Point closest = null;
        for(Point fp : fuelPoints) {
            // TODO ensure that this is deterministic with how ties work
            int dist = computeManhattanDistance(point, fp);
            if(dist < bestDist) {
                bestDist = dist;
                closest = fp;
            }
        }
        if(closest == null) {
            r.log("found no fuel points");
        }
        return closest;
    }

    public static Point getClosestFuelPoint(MyRobot r) {
        return getClosestFuelPoint(r, Utils.myLocation(r));
    }

    public static Point getContestedKarboniteGuardPoint(MyRobot r){
        List<Point> karb = getKarbonitePoints(r);
        Point myLoc = Utils.myLocation(r);
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

    public static List<Point> getClusterLocations(ArrayList<Point> candidates) {
        HashMap<Integer, Integer> consumed = new HashMap<>();
        ArrayList<Point> clusters = new ArrayList<>();

        int index = 0;
        while (consumed.size() < candidates.size()) {
            while (consumed.containsKey(index)) {
                index++;
            }

            Point centroid = candidates.get(index);
            clusters.add(centroid);
            consumed.put(index, 0);

            for (int i = index; i < candidates.size(); i++) {
                Point other = candidates.get(i);
                if (computeEuclideanDistance(centroid, other) < Constants.MIN_CHURCH_BUILD_DISTANCE) {
                    consumed.put(i, 0);
                }
            }
        }
        return clusters;
    }

    public static Point getNonResourceSpotAround(MyRobot r, Point point) {
        int[] dxes = {-1, 0, 1};
        int[] dyes = {-1, 0, 1};
        boolean[][] passable = r.getPassableMap();
        boolean[][] karb = r.getKarboniteMap();
        boolean[][] fuel = r.getFuelMap();
        for (int dx : dxes) {
            for (int dy : dyes) {
                Point absolute = new Point(point.x + dx, point.y + dy);
                int x = absolute.x;
                int y = absolute.y;
                if (x < 0 || y < 0 || y >= passable.length || y >= passable[0].length) {
                    continue;
                }

                if (passable[y][x] && !karb[y][x] && !fuel[y][x]) {
                    return absolute;
                }
            }
        }
        return null;
    }

}
