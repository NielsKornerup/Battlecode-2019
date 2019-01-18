package bc19;

import java.util.ArrayList;
import java.util.List;

public class Pilgrim {

    static Navigation karbMap;
    static Navigation fuelsMap;
    static Navigation castleMap;
    static State state = Math.random() < .5 ? State.GATHERING_KARB : State.GATHERING_FUEL;

    public enum State {
        GATHERING_KARB,
        GATHERING_FUEL,
        MOVING_RESOURCE_HOME,
    }

    public static void computeKarbMap(MyRobot r) {
        boolean[][] karboniteMap = r.getKarboniteMap();

        List<Point> targets = new ArrayList<>();
        for (int y = 0; y < karboniteMap.length; y++) {
            for (int x = 0; x < karboniteMap[y].length; x++) {
                if (karboniteMap[y][x]) {
                    targets.add(new Point(x, y));
                }
            }
        }

        karbMap = new Navigation(r, r.getPassableMap(), targets);
    }

    public static void computeFuelMap(MyRobot r) {
        boolean[][] fuelMap = r.getFuelMap();

        List<Point> targets = new ArrayList<>();
        for (int y = 0; y < fuelMap.length; y++) {
            for (int x = 0; x < fuelMap[y].length; x++) {
                if (fuelMap[y][x]) {
                    targets.add(new Point(x, y));
                }
            }
        }

        fuelsMap = new Navigation(r, r.getPassableMap(), targets);
    }

    public static void computeCastleMap(MyRobot r) {
        List<Point> targets = new ArrayList<>();
        for (Robot robot : r.getVisibleRobots()) {
            if (robot.unit == r.SPECS.CASTLE || robot.unit == r.SPECS.CHURCH) {
                targets.add(new Point(robot.x, robot.y));
            }
        }
        castleMap = new Navigation(r, r.getPassableMap(), targets);
    }

    public static void computeMaps(MyRobot r) {
        computeKarbMap(r);
        computeFuelMap(r);
        computeCastleMap(r);
    }

    public static Action act(MyRobot r) {
        if (r.turn == 1) {
            computeMaps(r);
        }

        /*if (r.karbonite >= Utils.getSpecs(r, r.SPECS.CHURCH).CONSTRUCTION_KARBONITE && r.fuel >= Utils.getSpecs(r, r.SPECS.CHURCH).CONSTRUCTION_FUEL) {
            ArrayList<Point> freeSpaces = Utils.getAdjacentFreeSpaces(r);
            Point move = freeSpaces.get((int) (Math.random() * freeSpaces.size()));
            return r.buildUnit(r.SPECS.CHURCH, move.x, move.y);
        }*/

        // TODO LOAD UP ON BOTH KARB AND FUEL BEFORE MOVING BACK
        if (state == State.GATHERING_KARB) {
            // Check if on top of karbonite
            if (r.getKarboniteMap()[r.me.y][r.me.x]) {
                // Check if Karbonite not filled up
                if (r.me.karbonite < Utils.mySpecs(r).KARBONITE_CAPACITY) {
                    if (Utils.canMine(r)) {
                        // Harvest
                        return r.mine();
                    }
                } else {
                    // Start bringing resource home
                    state = State.MOVING_RESOURCE_HOME;
                    return act(r);
                }
            } else {
                // Move towards Karbonite
                return Utils.moveMapThenRandom(r, karbMap, 1);
            }
        }

        if (state == State.GATHERING_FUEL) {
            // Check if on top of fuel
            if (r.getFuelMap()[r.me.y][r.me.x]) {
                // Check if fuel not filled up
                if (r.me.fuel < Utils.mySpecs(r).FUEL_CAPACITY) {
                    if (Utils.canMine(r)) {
                        // Harvest
                        return r.mine();
                    }
                } else {
                    // Start bringing resource home
                    state = State.MOVING_RESOURCE_HOME;
                    return act(r);
                }
            } else {
                // Move towards fuel
                return Utils.moveMapThenRandom(r, fuelsMap, 1);
            }
        }

        if (state == State.MOVING_RESOURCE_HOME) {
            // Check if next to Castles or churches
            ArrayList<Point> adjacentPlacesToDeposit = Utils.getAdjacentUnits(r, r.SPECS.CASTLE, true);
            adjacentPlacesToDeposit.addAll(Utils.getAdjacentUnits(r, r.SPECS.CHURCH, true));
            if (adjacentPlacesToDeposit.size() > 0) {
                // Check if Karbonite or Fuel left to drop off
                // TODO don't bother dropping off if only have trivial amounts to give
                if (r.me.karbonite > 0 || r.me.fuel > 0) {
                    // Drop off at Castle/Church
                    Point adjacentDeposit = adjacentPlacesToDeposit.get(0);
                    return r.give(adjacentDeposit.x, adjacentDeposit.y, r.me.karbonite, r.me.fuel);
                } else {
                    // Start going back to collect more resources. Pick weighted random between fuel and karb
                    state = Math.random() < .2 ? State.GATHERING_KARB : State.GATHERING_FUEL;
                    return act(r);
                }
            } else {
                // Move towards nearby Castle
                return Utils.moveMapThenRandom(r, castleMap, 1);
            }
        }

        return null;
    }
}
