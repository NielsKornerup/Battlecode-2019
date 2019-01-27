package bc19;

/**
 * Created by patil215 on 1/18/19.
 */
public class Constants {

	/* Castle-specific */
	public static final int CASTLE_ATTACK_RADIUS_SQ = 64;

	/* Pilgrim-specific */
	public static final int PILGRIM_MINE_FUEL_COST = 1;


	public static final int CASTLE = 0;
	public static final int CHURCH = 1;
	public static final int PILGRIM = 2;
	public static final int CRUSADER = 3;
	public static final int PROPHET = 4;
	public static final int PREACHER = 5;
	public static final int MAX_INT = 9999999;


	public static final int CRUSADER_FUEL_PER_MOVE = 1;
	public static final int PILGRIM_FUEL_PER_MOVE = 1;
	public static final int PROPHET_FUEL_PER_MOVE = 2;
	public static final int PREACHER_FUEL_PER_MOVE = 3;

	public static final int CASTLE_SPAM_CRUSADERS_TURN_THRESHOLD = 800;

	public static final int CRUSADER_ATTACK_RADIUS = 4;

	// TODO adjust this per castle depending on how many "outlier" areas you're responsible for
	public static final int NUM_AGGRESSIVE_SCOUT_UNITS_TO_BUILD = 1;

	public static final int MAX_NUM_CASTLES = 3;
	public static final int TURNS_BEFORE_DONE_RECEIVING_ENEMY_CASTLE_LOCATIONS = MAX_NUM_CASTLES - 1;

	public static final int FUEL_CAP_TURN_THRESHOLD = 700;
	public static final int FUEL_CAP = 400;

	// TODO the way this constant is used, things could go bad if we change how potentials work in Dijkstra map
	public static final int MIN_CHURCH_BUILD_DISTANCE = 4;

	public static final double ENEMY_RESOURCE_PENETRATION_PERCENTAGE = 1.2;

	public static final int ATTACK_TURN = 800;

	public static final int TURN_THRESHOLD_PRIORITIZE_CLOSE_CASTLES = 50;
}
