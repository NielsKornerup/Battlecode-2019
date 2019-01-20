package bc19;

public class CommunicationUtils {

	// Sum total should be 16
	private static final int INSTRUCTION_SIZE_BITS = 3;
	private static final int ARGUMENT_SIZE_BITS = 13;

	static final short PILGRIM_TARGET_MASK = (short) (0b111 << ARGUMENT_SIZE_BITS);
	static final short PROPHET_BUMP_MASK = (short) (0b101 << ARGUMENT_SIZE_BITS);
	static final short PROPHET_ATTACK_MASK = (short) (0b100 << ARGUMENT_SIZE_BITS);
	static final short ENEMY_CASTLE_LOCATION_MASK = (short) (0b011 << ARGUMENT_SIZE_BITS);
	static final short AGGRESSIVE_SCOUT_MASK = (short) (0b010 << ARGUMENT_SIZE_BITS);

	public static final int PILGRIM_TARGET_RADIUS_SQ = 2;
	private static final int ATTACK_SIGNAL_RADIUS_SQ = 5;
	private static final int BUMP_SIGNAL_RADIUS_SQ = 2;
	private static final int ENEMY_CASTLE_LOCATION_SQ = 2;
	private static final int AGGRESSIVE_SCOUT_LOCATION_SQ = 2;

	private static boolean sendBroadcast(MyRobot r, short message, int radiusSq) {
		if (Utils.canSignal(r, radiusSq)) {
			r.signal(message, radiusSq);
			return true;
		}
		return false;
	}

	/*
	Check that the instruction (i.e. upper 3 bits) of a signal matches the given mask.
	 */
	private static boolean instructionMatches(short instructionMask, int signal) {
		return instructionMask >>> ARGUMENT_SIZE_BITS == ((short) signal) >>> ARGUMENT_SIZE_BITS;
	}

	/*
	Check that the arguments (i.e. lower 13 bits) of a signal matches the given mask.
	 */
	private static boolean argumentMatches(short argumentMask, int signal) {
		return (short) (signal % (0b1 << ARGUMENT_SIZE_BITS)) == argumentMask;
	}

	public static boolean receivedAggressiveScoutLocation(MyRobot r, Robot other) {
		return (r.isRadioing(other) && instructionMatches(AGGRESSIVE_SCOUT_MASK, other.signal));
	}

	public static Point getAggressiveScoutLocation(MyRobot r, Robot other) {
		if (receivedAggressiveScoutLocation(r, other)) {
			short message = (short) other.signal;
			return new Point((message / (64)) % 64, message % 64);
		}
		return null;
	}

	public static boolean sendAggressiveScoutLocation(MyRobot r, Point target) {
		short message = (short) (AGGRESSIVE_SCOUT_MASK | ((short) target.x << 6) | ((short) target.y));
		return sendBroadcast(r, message, AGGRESSIVE_SCOUT_LOCATION_SQ);
	}

	public static boolean sendEnemyCastleLocation(MyRobot r, Point target) {
		short message = (short) (ENEMY_CASTLE_LOCATION_MASK | ((short) target.x << 6) | ((short) target.y));
		return sendBroadcast(r, message, ENEMY_CASTLE_LOCATION_SQ);
	}

	public static boolean receivedEnemyCastleLocation(MyRobot r, Robot other) {
		return (r.isRadioing(other) && instructionMatches(ENEMY_CASTLE_LOCATION_MASK, other.signal));
	}

	public static Point getEnemyCastleLocation(MyRobot r, Robot other) {
		if (receivedEnemyCastleLocation(r, other)) {
			short message = (short) other.signal;
			return new Point((message / (64)) % 64, message % 64);
		}
		return null;
	}

	public static boolean sendAttackMessage(MyRobot r) {
		return sendAttackMessage(r, ATTACK_SIGNAL_RADIUS_SQ);
	}

	public static boolean sendAttackMessage(MyRobot r, int radiusSq) {
		return sendBroadcast(r, PROPHET_ATTACK_MASK, radiusSq);
	}

	public static boolean sendGlobalAttackMessage(MyRobot r) {
		return sendBroadcast(r, PROPHET_ATTACK_MASK, Ring.MAX_RING_LEVEL * Ring.MAX_RING_LEVEL);
	}

	public static boolean receivedAttackMessage(MyRobot r) {
		for (Robot other : r.getVisibleRobots()) {
			if (r.isRadioing(other) && instructionMatches(PROPHET_ATTACK_MASK, (short) other.signal)) {
				return true;
			}
		}
		return false;
	}

	public static boolean sendBumpMessage(MyRobot r, int unitId) {
		short message = (short) (PROPHET_BUMP_MASK | ((short) unitId));
		return sendBroadcast(r, message, BUMP_SIGNAL_RADIUS_SQ);
	}

	public static boolean receivedBumpMessage(MyRobot r) {
		for (Robot other : r.getVisibleRobots()) {

			if (r.isRadioing(other) && instructionMatches(PROPHET_BUMP_MASK, (short) other.signal)) {
				if (argumentMatches((short) r.id, other.signal)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean sendPilgrimTargetMessage(MyRobot r, Point target) {
		short message = (short) (PILGRIM_TARGET_MASK | ((short) target.x << 6) | ((short) target.y));
		return sendBroadcast(r, message, PILGRIM_TARGET_RADIUS_SQ);
	}

	/*
	 * Returns a point that a pilgrim is targeting
	 * 
	 * @Return null if the robot is not signaling or if it is the wrong message type
	 */
	public static Point getPilgrimTargetInfo(MyRobot r, Robot other) {
		if (r.isRadioing(other) && instructionMatches(PILGRIM_TARGET_MASK, other.signal)) {
			short message = (short) other.signal;
			return new Point((message / (64)) % 64, message % 64);
		}
		return null;
	}

}
