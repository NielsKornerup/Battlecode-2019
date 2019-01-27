package bc19;

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
	 * Preacher
	 * Prophet
	 * Crusader
	 * Castle
	 * Church
	 * Pilgrim
	*/
	
	public static int getPriority(int type){
		if (type == Constants.PREACHER)
			return 0;
		if (type == Constants.PROPHET)
			return 1;
		if (type == Constants.CRUSADER)
			return 2;
		if (type == Constants.CASTLE)
			return 3;
		if (type == Constants.CHURCH)
			return 4;
		if (type == Constants.PILGRIM)
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
	
	public static int getScore(RobotSort r){
		int value = getPriority(r.unit);
		value*=10000;
		value+=r.hp*1000;
		value+=r.dist;
		return value;
	}
	
}