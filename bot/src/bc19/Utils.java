package bc19;

import java.util.ArrayList;


public class Utils {

    public static boolean isNearbySpaceEmpty(MyRobot r, Point delta) {
        // TODO: should reduce calls to these functions
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

    public static boolean canBuild(MyRobot r, int unitToBuild) {
        return r.karbonite >= Utils.getSpecs(r, unitToBuild).CONSTRUCTION_KARBONITE && r.fuel >= Utils.getSpecs(r, unitToBuild).CONSTRUCTION_FUEL;
    }

    public static AttackAction tryAndAttack(MyRobot r, int attackRadiusSq) {
        ArrayList<Point> enemiesNearby = Utils.getUnitsInRange(r, -1, false, 0, attackRadiusSq);
        if (enemiesNearby.size() > 0) {
            // Attack an enemy at random
            // TODO: smart targeting of enemies (prioritize Castles, etc and sort list)
            for (Point attackPoint : enemiesNearby) {
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

    public static Action moveDijkstra(MyRobot r, Navigation map, int radius) {
        Point delta = map.getNextMove(radius);
        if (Utils.canMove(r, delta)) {
            return r.move(delta.x, delta.y);
        }
        return null;
    }

    public static Action moveDijkstraThenRandom(MyRobot r, Navigation map, int radius) {
        // TODO: this function will move randomly if it can't move according to the Dijkstra map. However, this isn't
        // necessarily what we want to happen in all cases where this method is called. We should investigate where it's being
        // called, and adjust accordingly.
        Action action = moveDijkstra(r, map, radius);
        if (action != null) {
            return action;
        }

        if (r.fuel > 5 * mySpecs(r).FUEL_PER_MOVE) { // TODO: adjust this heuristic
            return Utils.moveRandom(r);
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

            // TODO: a lot of this work overlaps with getFreeSpaces().
            // This is a good place to start if we want to reduce method calls to getVisibleMap() and getPassableMap().
            if (canMove(r, move)) {
                return r.move(move.x, move.y);
            }
            candidates.remove(index);
        }
        return null;

    }

    public static int getFuelCost(MyRobot r, int dx, int dy) {
        int rSquared = dx * dx + dy * dy;
        return mySpecs(r).FUEL_PER_MOVE * rSquared;
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

    /*
    Returns Arraylist of [dx, dy] of Units that are directly adjacent (i.e. in the 8 squares around the unit).
     */
    public static ArrayList<Point> getAdjacentUnits(MyRobot r, int unitType, boolean myTeam) {
        ArrayList<Point> nearby = new ArrayList<>();
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
                nearby.add(new Point(robot.x - r.me.x, robot.y - r.me.y));
            }
        }
        return nearby;
    }

    /*
    Returns ArrayList of [dx, dy] of Units that are in range.
     */
    public static ArrayList<Point> getUnitsInRange(MyRobot r, int unitType, boolean myTeam, int minRadiusSq, int maxRadiusSq) {
        ArrayList<Point> nearby = new ArrayList<>();
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
                nearby.add(new Point(robot.x - r.me.x, robot.y - r.me.y));
            }
        }
        return nearby;
    }


    /*
    Returns Point pairs of [dx, dy] that are empty (i.e. both passable and devoid of units) and within the 8 squares surrounding us.
     */
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

    /*
    Determines if the map is horizontally or vertically mirrored, and returns the mirrored position of the current robot.
     */
    public static Point getMirroredPosition(MyRobot rob, Point position) {
        boolean[][] passableMap = rob.getPassableMap();
        int ht = passableMap.length;
        int wid = passableMap[0].length;

        int locX = position.x;
        int locY = position.y;

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
        if (verticalSymmetry) {
            return new Point(locX, ht - locY - 1);
        }
        return new Point(wid - locX - 1, locY);
    }

}
