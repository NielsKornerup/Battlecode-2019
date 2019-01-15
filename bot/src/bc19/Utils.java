package bc19;

import java.util.ArrayList;


public class Utils {

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
            if (enoughFuelToMove(r, move.x, move.y)) {
               return r.move(move.x, move.y);
            }
            candidates.remove(index);
        }
        return null;

    }

    public static boolean enoughFuelToMove(MyRobot r, int dx, int dy) {
        int rSquared = dx * dx + dy * dy;
        return mySpecs(r).FUEL_PER_MOVE * rSquared <= r.fuel;
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
