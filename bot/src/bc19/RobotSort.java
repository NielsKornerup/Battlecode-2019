package bc19;
import java.util.Comparator;

public class RobotSort implements Comparable<RobotSort> {
	
	public int id;
	public int unit;
	public int x;
	public int y;
	public int dist; //in r^2
	public int hp;
	
	
	public RobotSort(int id_, int unit_, int x_, int y_, int dist_, int hp_){
		id = id_;
		unit = unit_;
		x = x_;
		y = y_;
		dist = dist_;
		hp = hp_;
	}
	
	//0 stands for Castle, 1 stands for Church,2 stands for Pilgrim,
	//3 stands for Crusader, 4 stands for Prophet and 5 stands for Preacher
	
	/*
	 * Priority List in order of importance
	 * Preacher (5)
	 * Prophet (4)
	 * Crusader (3)
	 * Castle (0)
	 * Church (1)
	 * Pilgrim (2)
	*/
	
	public static int getPriority(int type){
		if (type==5)
			return 0;
		if (type==4)
			return 1;
		if (type==3)
			return 2;
		if (type==0)
			return 3;
		if (type==1)
			return 4;
		if (type==2)
			return 5;
		
		return -1;
	}
	
	@Override
	public int compareTo(RobotSort r){
		if (this.unit == r.unit){
			if (this.hp == r.hp){
				return this.dist - r.dist;
			} else {
				return this.hp - r.hp;
			}
		} else {
			return getPriority(unit) - getPriority(r.unit);
		}
	}
	
}