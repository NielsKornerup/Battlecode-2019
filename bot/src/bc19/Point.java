package bc19;

public class Point implements Comparable<Point> {
    int x;
    int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public int compareTo(Point point) {
        if(x>point.x) {
        	return 1;
        }
        if(x<point.x) {
        	return -1;
        }
        return y-point.y;
    }
    
    public boolean equals(Object obj) {
    	if(!(obj instanceof Point)) {
    		return false;
    	}
    	Point point = (Point) obj;
    	return (x == point.x) && (y == point.y);
    }
    
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + this.x;
        hash = 71 * hash + this.y;
        return hash;
    }
    
    public String toString() {
    	return "("+x+", "+y+")";
    }
}
