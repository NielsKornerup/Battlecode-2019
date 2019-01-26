package bc19;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by patil215 on 1/26/19.
 */
public class Lattice {

	private MyRobot r;
	private HashMap<Integer, Point> otherCastleLocations;
	private ArrayList<Point> enemyCastleLocations;

	private ArrayList<Point> prophetLatticeLocations = new ArrayList<>();
	private ArrayList<Point> crusaderLatticeLocations = new ArrayList<>();
	private int crusaderUnitIndex = 0;
	private int prophetUnitIndex = 6; // Starts at 6 to start far away from Castle

	public Lattice(MyRobot r, HashMap<Integer, Point> otherCastleLocations, ArrayList<Point> enemyCastleLocations) {
		this.r = r;
		this.otherCastleLocations = otherCastleLocations;
		this.enemyCastleLocations = enemyCastleLocations;
		initializeLattice();
	}

	//https://www.programcreek.com/2013/01/leetcode-spiral-matrix-java/

	public void initializeLattice() {
		ArrayList<Point> result = new ArrayList<>();

		int m = 63; //row
		int n = 63; //col

		int left = 0;
		int right = n - 1;
		int top = 0;
		int bottom = m - 1;

		while (result.size() < m * n) {
			for (int j = left; j <= right; j++) {
				result.add(0, new Point(top, j));
			}
			top++;

			for (int i = top; i <= bottom; i++) {
				result.add(0, new Point(i, right));
			}
			right--;

			//prevent duplicate row
			if (bottom < top)
				break;

			for (int j = right; j >= left; j--) {
				result.add(0, new Point(bottom, j));
			}
			bottom--;

			// prevent duplicate column
			if (right < left)
				break;

			for (int i = bottom; i >= top; i--) {
				result.add(0, new Point(i, left));
			}
			left++;
		}

		Point myLoc = new Point(r.me.x, r.me.y);
		boolean[][] passableMap = r.getPassableMap();
		boolean[][] karbMap = r.getKarboniteMap();
		boolean[][] fuelMap = r.getFuelMap();

		for (Point offset : result) {
			int dx = offset.x - ((n - 1) / 2);
			int dy = offset.y - ((m - 1) / 2);
			//checkerboard pattern

        	/*
			if (Math.abs(dx) < 2 || Math.abs(dy) < 2){
        		continue;
        	}
        	*/
			Point mapLoc = new Point(myLoc.x + dx, myLoc.y + dy);

			for (Point otherCastleLoc : otherCastleLocations.values()) {
				if (Utils.computeManhattanDistance(myLoc, mapLoc) > Utils.computeManhattanDistance(otherCastleLoc, mapLoc)) {
					continue;
				}

			}

			if (mapLoc.x >= 0 && mapLoc.x < passableMap[0].length && mapLoc.y >= 0 && mapLoc.y < passableMap.length
					&& passableMap[mapLoc.y][mapLoc.x] && !karbMap[mapLoc.y][mapLoc.x] && !fuelMap[mapLoc.y][mapLoc.x]) {
				if ((dx + dy + 200) % 2 == (myLoc.x + myLoc.y) % 2) {
					prophetLatticeLocations.add(mapLoc);
				} else {
					crusaderLatticeLocations.add(0, mapLoc);
				}

				//r.log("X: "+mapLoc.x+" Y: "+mapLoc.y);
			}
		}
		//Collections.reverse(crusaderLatticeLocations);
		crusaderUnitIndex = crusaderLatticeLocations.size() - 70;
	}

	public Point popProphetLatticeLocation() {
		if (prophetUnitIndex < prophetLatticeLocations.size()) {
			Point location = prophetLatticeLocations.get(prophetUnitIndex);
			prophetUnitIndex++;
			return location;
		}
		return null;
	}

	public Point popCrusaderLatticeLocation() {
		if (crusaderUnitIndex < crusaderLatticeLocations.size()) {
			Point location = crusaderLatticeLocations.get(crusaderUnitIndex);
			crusaderUnitIndex++;
			return location;
		}
		return null;
	}
}
