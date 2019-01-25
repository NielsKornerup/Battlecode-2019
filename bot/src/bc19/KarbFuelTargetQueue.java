package bc19;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class KarbFuelTargetQueue {
    private ArrayList<Point> allCastlePilgrimBuildLocations = new ArrayList<>();
    private PriorityQueue pilgrimLocationQueue = null;
    private Point mostContestedPoint = null;
    
    public KarbFuelTargetQueue(MyRobot r, HashMap<Integer, Point> otherCastleLocations, List<Point> enemyCastleLocations) {
        HashMap<Integer, Navigation> castleIdToResourceMap = new HashMap<>();
        for (Integer id : otherCastleLocations.keySet()) {
            ArrayList<Point> targets = new ArrayList<>();
            targets.add(otherCastleLocations.get(id));
            Navigation map = new Navigation(r, r.getPassableMap(), targets);
            castleIdToResourceMap.put(id, map);
        }

        // Add enemy castle map
        ArrayList<Point> enemyTargets = new ArrayList<>();
        for (Point point : enemyCastleLocations) {
            enemyTargets.add(point);
        }
        Navigation enemyMap = new Navigation(r, r.getPassableMap(), enemyTargets);

        ArrayList<Point> myPosition = new ArrayList<>();
        myPosition.add(Utils.myLocation(r));
        Navigation myMap = new Navigation(r, r.getPassableMap(), myPosition);
        castleIdToResourceMap.put(r.me.id, myMap);

        List<Point> resourceLocationsToConsider = Utils.getKarbonitePoints(r);
        resourceLocationsToConsider.addAll(Utils.getFuelPoints(r));

        PriorityQueue pilgrimLocationQueue = new PriorityQueue();
        double smallestContestedSpread = 1000000000;
        for (Point point : resourceLocationsToConsider) {

            // Find the Castle ID with smallest potential
            int smallestId = -1;
            int smallestValue = 1000000;
            int multiplier = 5153;
            for (Integer id : castleIdToResourceMap.keySet()) {
                Navigation map = castleIdToResourceMap.get(id);
                int value = map.getPotential(point) * multiplier + (id % multiplier);
                if (value < smallestValue) {
                    smallestId = id;
                    smallestValue = value;
                }
            }

            double enemyComp = enemyMap.getPotential(point) * multiplier * Constants.ENEMY_RESOURCE_PENETRATION_PERCENTAGE;
            if (enemyComp < smallestValue) {
                continue;
            }

            allCastlePilgrimBuildLocations.add(point);
            if (smallestId == r.me.id) {
                pilgrimLocationQueue.enqueue(new Node(smallestValue, point));

                // Pick our most contested spot
                double spread = enemyComp - smallestValue;
                if (spread < smallestContestedSpread) {
                    smallestContestedSpread = spread;
                    mostContestedPoint = point;
                }
            } else {
                pilgrimLocationQueue.enqueue(new Node(smallestValue, new Point(-1, smallestId)));
            }
        }
        this.pilgrimLocationQueue = pilgrimLocationQueue;
    }

    public Point getMostContestedPoint() {
        return mostContestedPoint;
    }
    
    public Point dequeue() {
    	Node n = pilgrimLocationQueue.dequeue();
    	if(n==null) {
    		return null;
    	}
    	return n.p;
    }
    
    public boolean isEmpty() {
    	return pilgrimLocationQueue.isEmpty();
    }
    
    public Point peek() {
    	Node n = pilgrimLocationQueue.peek();
    	if(n==null) {
    		return null;
    	}
    	return n.p;
    }
    
    public ArrayList<Point> getAllCastlePilgrimBuildLocations() {
    	return this.allCastlePilgrimBuildLocations;
    }

}
