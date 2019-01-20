package bc19;

import java.util.ArrayList;
import java.util.List;

import static bc19.Constants.MAX_INT;

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
                if (distances[i][j] < MAX_INT) {
                    thing += (distances[i][j] + " ");
                } else {
                    thing += "inf ";
                }
            }
            r.log(thing);
        }
    }

    // call recalculateDistanceMap
    public void setThreshold(int threshold) {
        this.maxDistance = threshold;
    }

    private ArrayList<Point> getPossibleMovementDeltas() {
        ArrayList<Point> deltas = new ArrayList<>();
        for (int dx = -1 * maxDistance; dx <= maxDistance; dx++) {
            for (int dy = -1 * maxDistance; dy <= maxDistance; dy++) {
                if(dx == 0 && dy == 0) {
                    continue;
                }
                if((dx * dx + dy * dy) > (maxDistance * maxDistance)) {
                    continue;
                }
                deltas.add(new Point(dx, dy));
            }
        }
        return deltas;
    }

    public void recalculateDistanceMap() {
        ArrayList<Point> movementDeltas = getPossibleMovementDeltas();

        // Clear distance map
        for (int i = 0; i < distances.length; i++) {
            for (int j = 0; j < distances[0].length; j++) {
                distances[i][j] = MAX_INT;
            }
        }

        // Add targets
        PriorityQueue pq = new PriorityQueue();

        for (Point target : targets) {
            pq.enqueue(new Node(0, new Point(target.x, target.y)));
        }

        int i = 0;
        while (!pq.isEmpty()) {
            i++;
            int sz = pq.size();
            Node cur = pq.dequeue();
            int dist = cur.dist;
            Point loc = cur.p;
            int x = loc.getX();
            int y = loc.getY();
            if (distances[y][x] < MAX_INT) {
                continue;
            }
            distances[y][x] = dist;
            for (Point disp : movementDeltas) {
                int newX = x + disp.getX();
                int newY = y + disp.getY();

                // Check on board
                if (newX < 0 || newY < 0 || newY >= distances.length || newX >= distances[0].length) {
                    continue;
                }

                // Check that square is open
                if (!passableMap[newY][newX]) {
                    continue;
                }

                int newDist = dist + Utils.getFuelCost(r, disp.getX(), disp.getY());
                Point newPoint = new Point(newX, newY);

                pq.enqueue(new Node(newDist, newPoint));
            }
        }
    }


    public Navigation(MyRobot r, boolean[][] passableMap, List<Point> targets, int maxDistance) {
        // TODO MAKE THIS BASED OFF OF FUEL COST
        // TODO ACCOUNT FOR CASE WHERE THERE IS INPENETRABLE WALL SEPARATING THINGS
        this.r = r;
        this.passableMap = passableMap;
        this.maxDistance = maxDistance;
        this.distances = new int[passableMap.length][passableMap[0].length];
        this.targets = targets;
        recalculateDistanceMap();
    }

    public Navigation(MyRobot r, boolean[][] passableMap, List<Point> targets) {
        this(r, passableMap, targets, 2);
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
        // TODO: remove radius parameter
        ArrayList<Point> possibleDeltas = getPossibleMovementDeltas(); // TODO shuffle for tiebreaking

        int minDist = MAX_INT;
        Point bestDelta = null;

        Point start = new Point(r.me.x, r.me.y);
        for (Point delta : possibleDeltas) {
            int newX = start.x + delta.x;
            int newY = start.y + delta.y;
            if (Utils.canMove(r, delta) && distances[newY][newX] < minDist) { // TODO optimize this
                bestDelta = delta;
                minDist = distances[newY][newX];
            }
        }
        return bestDelta;
    }

    public int getDijkstraMapValue(Point location) {
        int x = location.x;
        int y = location.y;
        if (x >= -1 && y >= -1 && y < distances.length && x < distances[y].length) {
            return distances[y][x];
        }
        return Integer.MIN_VALUE; // TODO should this be max or min?
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
    
    public int getPotential(Point target) {
    	int res = distances[target.y][target.x];
    	return res;
    }
}
