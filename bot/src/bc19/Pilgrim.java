package bc19;

import java.util.ArrayList;
import java.util.List;

public class Pilgrim implements BCRobot {

    MyRobot r;

    static Navigation karbMap;
    static Navigation fuelsMap;
    static Navigation castleMap;
    static State state = Math.random() < .5 ? State.GATHERING_KARB : State.GATHERING_FUEL;

    public enum State {
        GATHERING_KARB,
        GATHERING_FUEL,
        MOVING_RESOURCE_HOME,
    }

    public Pilgrim(MyRobot myRobot) {
        this.r = myRobot;
    }

    public void computeKarbMap() {
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

    public void computeFuelMap() {
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

    public void computeCastleMap() {
        List<Point> targets = new ArrayList<>();
        for (Robot robot : r.getVisibleRobots()) {
            if (robot.unit == r.SPECS.CASTLE || robot.unit == r.SPECS.CHURCH) {
                targets.add(new Point(robot.x, robot.y));
            }
        }
        castleMap = new Navigation(r, r.getPassableMap(), targets);
    }

    public void computeMaps() {
        computeKarbMap();
        computeFuelMap();
        computeCastleMap();
    }

    public Action act() {
        if (r.turn == 1) {
            computeMaps();
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
                    return act();
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
                    return act();
                }
            } else {
                // Move towards fuel
                return Utils.moveMapThenRandom(r, fuelsMap, 1);
            }
        }

        if (state == State.MOVING_RESOURCE_HOME) {
            // Check if next to Castle
            ArrayList<Point> adjacentCastles = Utils.getAdjacentUnits(r, r.SPECS.CASTLE);
            if (adjacentCastles.size() > 0) {
                // Check if Karbonite left
                if (r.me.karbonite > 0) {
                    // Drop off at Castle
                    Point adjacentCastle = adjacentCastles.get(0);
                    return r.give(adjacentCastle.x, adjacentCastle.y, r.me.karbonite, r.me.fuel);
                } else {
                    // Start going back to collect more resources. Pick weighted random between fuel and karb
                    state = Math.random() < .2 ? State.GATHERING_KARB : State.GATHERING_FUEL;
                    return act();
                }
            } else {
                // Move towards nearby Castle
                return Utils.moveMapThenRandom(r, castleMap, 1);
            }
        }

        return null;
    }
}
