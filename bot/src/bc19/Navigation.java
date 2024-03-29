package bc19;

import java.util.ArrayList;
import java.util.List;

public class Navigation {

    private MyRobot r;
    private boolean[][] passableMap;
    private List<Point> targets;
    private int maxDistance;
    private int[][] distances;

    public void printDistances() {
        for (int i = 0; i < distances.length; i++) {
            String thing = "";
            for (int j = 0; j < distances[0].length; j++) {
                if (distances[i][j] < 10000) {
                    thing += (distances[i][j] + " ");
                } else {
                    thing += "inf";
                }
            }
            r.log(thing);
        }
    }

    // call recalculateDistanceMap
    public void setThreshold(int threshold) {
        this.maxDistance = threshold;
    }

    private ArrayList<Point> getPossibleMovementDeltas(int maxMovementR) {
        ArrayList<Point> deltas = new ArrayList<>();
        for (int dx = -maxMovementR; dx <= maxMovementR; dx++) {
            for (int dy = -maxMovementR; dy <= maxMovementR; dy++) {
            	/*
                if (dx == 0 && dy == 0) {
                    continue;
                }
                */
                if (dx * dx + dy * dy > maxMovementR * maxMovementR) {
                    continue;
                }

                deltas.add(new Point(dx, dy));
            }
        }
        return deltas;
    }

    private ArrayList<Point> getAdjacentDeltas() {
        ArrayList<Point> deltas = new ArrayList<>();
        int[] dxes = {-1, 0, 1};
        int[] dyes = {-1, 0, 1};
        for (int dx : dxes) {
            for (int dy : dyes) {
                deltas.add(new Point(dx, dy));
            }
        }
        return deltas;
    }


    public void recalculateDistanceMap() {
        //ArrayList<Point> movementDeltas = getPossibleMovementDeltas();
        ArrayList<Point> movementDeltas = getAdjacentDeltas();

        // Clear distance map
        for (int i = 0; i < distances.length; i++) {
            for (int j = 0; j < distances[0].length; j++) {
                distances[i][j] = Integer.MAX_VALUE;
            }
        }

        // Add targets

        Queue<Point> queue = new Queue<>();
        for (Point target : targets) {
            distances[target.y][target.x] = 0;
            queue.enqueue(new Point(target.x, target.y));
        }

        // BFS out
        while (!queue.isEmpty()) {
            Point loc = queue.dequeue();
            int curDistance = distances[loc.y][loc.x];

            for (Point disp : movementDeltas) {
                int newX = loc.getX() + disp.x;
                int newY = loc.getY() + disp.y;

                // Check on board
                if (newX < 0 || newY < 0 || newY >= distances.length || newX >= distances[0].length) {
                    continue;
                }

                // Check that square is open
                if (!passableMap[newY][newX]) {
                    continue;
                }

                int newDistance = 1 + curDistance;

                // Check that this isn't exceeding the distance we want units to "see"
                if (newDistance >= maxDistance) {
                    continue;
                }
                //int newDistance = curDistance + Utils.getFuelCost(r, disp.x, disp.y);

                // Check that this actually results in a shorter path
                if (distances[newY][newX] <= newDistance) {
                    continue;
                }

                distances[newY][newX] = newDistance;
                queue.enqueue(new Point(newX, newY));
            }
        }
    }


    public Navigation(MyRobot r, boolean[][] passableMap, List<Point> targets, int maxDistance) {
        // TODO MAKE THIS BASED OFF OF FUEL COST
        this.r = r;
        this.passableMap = passableMap;
        this.maxDistance = maxDistance;
        this.distances = new int[passableMap.length][passableMap[0].length];
        this.targets = targets;
        recalculateDistanceMap();
    }

    public Navigation(MyRobot r, boolean[][] passableMap, List<Point> targets) {
        this(r, passableMap, targets, Integer.MAX_VALUE);
    }
    
    public Navigation(MyRobot r, boolean[][] passableMap, List<Point> targets, boolean[][] karbMap, boolean[][] fuelMap){
    	Point myCastle = Utils.getSpawningCastleOrChurchLocation(r);
    	
    	for (int y = 0; y < karbMap.length; y++){
    		for (int x = 0; x < karbMap[0].length; x++){
    			boolean onLattice = false;
    			if ((r.me.x + r.me.y + 200) % 2 == (myCastle.x + myCastle.y) % 2 && Utils.computeManhattanDistance(new Point(x,y),myCastle)>2){
    				onLattice = true;
    			}
    			if (karbMap[y][x] || fuelMap[y][x]){
    				passableMap[y][x] = false;
    			}
    		}
    	}
    	
    	
    	this.r = r;
        this.passableMap = passableMap;
        this.maxDistance = Integer.MAX_VALUE;
        this.distances = new int[passableMap.length][passableMap[0].length];
        this.targets = targets;
        recalculateDistanceMap();
    	
    }


    /**
     * Returns a best delta to move according to a start location and radius (not r_squared, just r)
     * <p>
     * Tries all possible directions, returning their optimality in sorted order.
     * <p>
     * TODO: Uses some sort of heuristic to weight moving quick against using fuel.
     * <p>
     * Null is returned if all adjacent squares are 'too far' (over threshold)
     * or impossible to reach.
     */

    public Point getNextMove(int radius) {
        ArrayList<Point> possibleDeltas = getPossibleMovementDeltas(radius); // TODO shuffle for tiebreaking

        int minDist = Integer.MAX_VALUE;
        Point bestDelta = null;

        Point start = new Point(r.me.x, r.me.y);
        for (Point delta : possibleDeltas) {
            int newX = start.x + delta.x;
            int newY = start.y + delta.y;
            if ((Utils.canMove(r, delta) || (delta.x == 0 && delta.y == 0 )) && distances[newY][newX] < minDist) { // TODO optimize this
                bestDelta = delta;
                minDist = distances[newY][newX];
            }
        }
        if (bestDelta.x == 0 && bestDelta.y == 0){
            return null;
        }
        return bestDelta;
    }

    public void addTarget(Point pos) {
        targets.add(pos);
    }

    public void removeTarget(Point pos) {
        targets.remove(pos);
    }

    public void clearTargets() {
        targets.clear();
    }

    public List<Point> getTargets() {
        return targets;
    }

    public int getPotential(Point location) {
        int x = location.x;
        int y = location.y;
        if (x >= -1 && y >= -1 && y < distances.length && x < distances[y].length) {
            return distances[y][x];
        }
        return 1000;
    }
}