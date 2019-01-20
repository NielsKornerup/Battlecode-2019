package bc19;

import java.util.ArrayList;
import java.util.List;

public class Navigation {

    private MyRobot r;
    private boolean[][] passableMap;
    private List<Point> targets;
    private int maxMovementRadius;
    private int[][] distances;

    public void printDistances() {
        for (int i = 0; i < distances.length; i++) {
            String thing = "";
            for (int j = 0; j < distances[0].length; j++) {
                if (distances[i][j] < Constants.MAX_INT) {
                    thing += (distances[i][j] + " ");
                } else {
                    thing += "inf ";
                }
            }
            r.log(thing);
        }
    }

    private ArrayList<Point> getPossibleMovementDeltas(int radius) {
        ArrayList<Point> deltas = new ArrayList<>();
        for (int dx = -1 * radius; dx <= radius; dx++) {
            for (int dy = -1 * radius; dy <= radius; dy++) {
                if(dx == 0 && dy == 0) {
                    continue;
                }
                if((dx * dx + dy * dy) > (radius * radius)) {
                    continue;
                }
                deltas.add(new Point(dx, dy));
            }
        }
        return deltas;
    }

    public void recalculateDistanceMap() {
        ArrayList<Point> movementDeltas = getPossibleMovementDeltas(maxMovementRadius);

        // Clear distance map
        for (int i = 0; i < distances.length; i++) {
            for (int j = 0; j < distances[0].length; j++) {
                distances[i][j] = Constants.MAX_INT;
            }
        }

        // Add targets
        PriorityQueue pq = new PriorityQueue();

        for (Point target : targets) {
            pq.enqueue(new Node(0, new Point(target.x, target.y)));
        }

        while (!pq.isEmpty()) {
            Node cur = pq.dequeue();
            int dist = cur.dist;
            Point loc = cur.p;
            int x = loc.x;
            int y = loc.y;
            if (distances[y][x] < Constants.MAX_INT) {
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


    public Navigation(MyRobot r, boolean[][] passableMap, List<Point> targets, int maxMovementRadius) {
        this.r = r;
        this.passableMap = passableMap;
        this.maxMovementRadius = maxMovementRadius;
        this.distances = new int[passableMap.length][passableMap[0].length];
        this.targets = targets;
        recalculateDistanceMap();
    }


    /**
     * Returns a best delta to move according to a start location and radius (not r_squared, just r)
     * <p>
     * Tries all possible directions, returning their optimality in sorted order.
     * <p>
     * Null is returned if all adjacent squares are 'too far' (over threshold)
     * or impossible to reach.
     */

    public Point getNextMove(int radius) {
        ArrayList<Point> possibleDeltas = getPossibleMovementDeltas(radius); // TODO shuffle for tiebreaking

        int minDist = Constants.MAX_INT;
        Point bestDelta = null;

        Point start = Utils.myLocation(r);
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
        int x = target.x;
        int y = target.y;
        if (x < 0 || y < 0 || y >= distances.length || x >= distances[y].length) {
            return 10000; // TODO should this be max or min?
        }
        return distances[y][x];
    }
}
