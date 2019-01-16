package bc19;

import java.util.ArrayList;
import java.util.List;

public class Pilgrim implements BCRobot {

    MyRobot r;

    static Navigation karbMap;
    static Navigation castleMap;
    static State state = State.GATHERING_KARB;

    public enum State {
        GATHERING_KARB,
        MOVING_RESOURCE_HOME,
    }

    public Pilgrim(MyRobot myRobot) {
        this.r = myRobot;
    }

    public void computeKarbMap() {
        boolean[][] karboniteMap = r.karboniteMap;

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

    public void computeCastleMap() {
        List<Point> targets = new ArrayList<>();
        for (Robot robot : r.getVisibleRobots()) {
            if (robot.unit == r.SPECS.CASTLE) {
                targets.add(new Point(robot.x, robot.y));
            }
        }
        castleMap = new Navigation(r, r.getPassableMap(), targets);
    }

    public void computeMaps() {
        computeKarbMap();
        computeCastleMap();
    }

    public Action act() {
        if (r.turn == 1) {
            computeMaps();
        }

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

        if (state == State.MOVING_RESOURCE_HOME) {
            // Check if next to Castle
            ArrayList<Point> adjacentCastles = Utils.getAdjacentUnits(r, r.SPECS.CASTLE, true);
            if (adjacentCastles.size() > 0) {
                // Check if Karbonite left
                if (r.me.karbonite > 0) {
                    // Drop off at Castle
                    Point adjacentCastle = adjacentCastles.get(0);
                    //r.log(String.valueOf(adjacentCastle.x) + " " + String.valueOf(adjacentCastle.y));
                    //r.log(String.valueOf(r.me.x) + " " + String.valueOf(r.me.y));
                    return r.give(adjacentCastle.x, adjacentCastle.y, r.me.karbonite, r.me.fuel);
                } else {
                    // Start going back to collect more resources
                    state = State.GATHERING_KARB;
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
