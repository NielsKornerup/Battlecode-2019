package bc19;

import java.util.ArrayList;


public class Utils {

    private static final int PILGRIM_MINE_FUEL_COST = 1;

    public static boolean canMove(MyRobot r, Point delta) {
        // TODO need to reduce calls to these functions
        boolean[][] passableMap = r.getPassableMap();
        int[][] visibleRobotMap = r.getVisibleRobotMap();
        int newX = r.me.x + delta.x;
        int newY = r.me.y + delta.y;
        if (newX < 0 || newY < 0 || newY > passableMap.length || newX > passableMap[0].length) {
            return false;
        }
        return passableMap[newY][newX] && (visibleRobotMap[newY][newX] <= 0) && enoughFuelToMove(r, delta.x, delta.y);
    }

    public static boolean canMine(MyRobot r) {
        return r.fuel >= PILGRIM_MINE_FUEL_COST;
    }

    public static Action moveMapThenRandom(MyRobot r, Navigation map, int radius) {
        Point delta = map.getNextMove(radius);
        if (Utils.canMove(r, delta)) {
            return r.move(delta.x, delta.y);
        } else {
            if (r.fuel > 5 * mySpecs(r).FUEL_PER_MOVE) {
                return Utils.moveRandom(r);
            }
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
        return mySpecs(r).FUEL_PER_MOVE * rSquared;
    }

    public static boolean enoughFuelToMove(MyRobot r, int dx, int dy) {
        return getFuelCost(r, dx, dy) <= r.fuel;
    }

    /*
    Returns Point pairs of [dx, dy] that are empty (i.e. both passable and devoid of units) within a range.
    Range is NOT R SQUARED, it is just R.
     */
    @SuppressWarnings("Duplicates")
    public static ArrayList<Point> getFreeSpaces(MyRobot r, int range) {
        boolean[][] passableMap = r.getPassableMap();
        int[][] visibleRobotMap = r.getVisibleRobotMap();
        ArrayList<Point> freeSpaces = new ArrayList<>();
        int x = r.me.x;
        int y = r.me.y;
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                if (dx * dx + dy * dy > range * range) {
                    continue;
                }
                int newX = x + dx;
                int newY = y + dy;
                if (newX < 0 || newY < 0 || newY >= passableMap.length || newX >= passableMap[0].length) {
                    continue;
                }
                if (passableMap[newY][newX] && visibleRobotMap[newY][newX] <= 0) {
                    freeSpaces.add(new Point(dx, dy));
                }
            }
        }
        return freeSpaces;
    }

    /*
    Returns arraylist of [dx, dy] of Units that are directly adjacent
     */
    public static ArrayList<Point> getAdjacentUnits(MyRobot r, int unitType) {
        ArrayList<Point> nearby = new ArrayList<>();
        for (Robot robot : r.getVisibleRobots()) {
            if (robot.unit != unitType) {
                continue;
            }
            if (Math.abs(robot.x - r.me.x) <= 1 && Math.abs(robot.y - r.me.y) <= 1) {
                nearby.add(new Point(robot.x - r.me.x, robot.y - r.me.y));
            }
        }
        return nearby;
    }

    /*
    Returns ArrayList of [dx, dy] of Units that are
     */
    public static ArrayList<Point> getUnitsInRange(MyRobot r, int unitType, boolean myTeam,  int minRadius, int maxRadius) {
        ArrayList<Point> nearby = new ArrayList<>();
        for (Robot robot : r.getVisibleRobots()) {
            if ((robot.unit != unitType && unitType != -1) || ((robot.team == r.me.team) == myTeam)) {
                continue;
            }
            int distX = robot.x - r.me.x;
            int distY = robot.y - r.me.y;
            int distanceSquared = distX * distX + distY * distY;
            if (distanceSquared >= minRadius * minRadius && distanceSquared <= maxRadius * maxRadius) {
                nearby.add(new Point(robot.x - r.me.x, robot.y - r.me.y));
            }
        }
        return nearby;
    }


    /*
    Returns Point pairs of [dx, dy] that are empty (i.e. both passable and devoid of units).
     */
    public static ArrayList<Point> getAdjacentFreeSpaces(MyRobot r) {
        boolean[][] passableMap = r.getPassableMap();
        int[][] visibleRobotMap = r.getVisibleRobotMap();
        ArrayList<Point> freeSpaces = new ArrayList<>();
        int x = r.me.x;
        int y = r.me.y;
        int[] dxes = {-1, 0, 1};
        int[] dyes = {-1, 0, 1};
        for (int dx : dxes) {
            for (int dy : dyes) {
                int newX = x + dx;
                int newY = y + dy;
                if (passableMap[newY][newX] && visibleRobotMap[newY][newX] <= 0) {
                    freeSpaces.add(new Point(dx, dy));
                }
            }
        }
        return freeSpaces;
    }

}
