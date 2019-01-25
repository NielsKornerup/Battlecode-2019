package bc19;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class KarbFuelTargetQueue {
    private ArrayList<Point> allCastlePilgrimBuildLocations = new ArrayList<>();
    private PriorityQueue pilgrimLocationQueue = null;
    
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

            if (enemyMap.getPotential(point) * multiplier * 1.2 < smallestValue) {
                continue;
            }

            allCastlePilgrimBuildLocations.add(point);
            if (smallestId == r.me.id) {
                pilgrimLocationQueue.enqueue(new Node(smallestValue, point));
            } else {
                pilgrimLocationQueue.enqueue(new Node(smallestValue, new Point(-1, smallestId)));
            }
        }
        this.pilgrimLocationQueue = pilgrimLocationQueue;
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
