package bc19;

public class CommunicationUtils {

	// Sum total should be 16
	private static final int INSTRUCTION_SIZE_BITS = 3;
	private static final int ARGUMENT_SIZE_BITS = 13;

	static final short PILGRIM_TARGET_MASK = (short) (0b111 << ARGUMENT_SIZE_BITS);
	static final short CASTLE_INFORM_MASK = (short) (0b110 << ARGUMENT_SIZE_BITS);
	static final short PROPHET_BUMP_MASK = (short) (0b101 << ARGUMENT_SIZE_BITS);
	static final short PROPHET_ATTACK_MASK = (short) (0b100 << ARGUMENT_SIZE_BITS);

	private static final int ATTACK_SIGNAL_RADIUS_SQ = 5;
	private static final int BUMP_SIGNAL_RADIUS_SQ = 2;

	private static void sendBroadcast(MyRobot r, short message, int radiusSq) {
		if (Utils.canSignal(r, radiusSq)) {
			r.signal(message, radiusSq);
		}
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
		return (short) (((short) signal << INSTRUCTION_SIZE_BITS) >>> INSTRUCTION_SIZE_BITS) == argumentMask;
	}

	public static void sendAttackMessage(MyRobot r) {
		sendBroadcast(r, PROPHET_ATTACK_MASK, ATTACK_SIGNAL_RADIUS_SQ);
	}

	public static boolean receivedAttackMessage(MyRobot r) {
		for (Robot other : r.getVisibleRobots()) {
			if (r.isRadioing(other)
					&& instructionMatches(PROPHET_ATTACK_MASK, (short) other.signal)
					&& argumentMatches((short) r.id, other.signal)) {
				return true;
			}
		}
		return false;
	}

	public static void sendBumpMessage(MyRobot r, int unitId) {
		short message = (short) (PROPHET_BUMP_MASK & ((short) unitId));
		sendBroadcast(r, message, BUMP_SIGNAL_RADIUS_SQ);
	}

	public static boolean receivedBumpMessage(MyRobot r) {
		for (Robot other : r.getVisibleRobots()) {
			if (r.isRadioing(other) && instructionMatches(PROPHET_BUMP_MASK, (short) other.signal)) {
				return true;
			}
		}
		return false;
	}

	public static void sendPilgrimInfoMessage(MyRobot r, Point target, int range) {
		short message = (short) (PILGRIM_TARGET_MASK + (target.x << 6) + target.y);
		sendBroadcast(r, message, range);
	}

	/*
	 * Returns a point for the pilgrim to target from a passed in castle
	 * 
	 * @Return null if the robot is not signaling or if it is the wrong message type
	 */
	public static Point getPilgrimTargetInfo(MyRobot r, Robot other) {
		if (r.isRadioing(other) && instructionMatches(PILGRIM_TARGET_MASK, other.signal)) {
			short message = (short) other.signal;
			return new Point((message / (64)) % 64, message % 64);
		}
		r.log("failed to get target info. isRadioing: " + r.isRadioing(other) + " found mask "
				+ ((short) (other.signal >>> ARGUMENT_SIZE_BITS)) + " wanted mask " + (PILGRIM_TARGET_MASK >>> ARGUMENT_SIZE_BITS));
		return null;
	}

	public static void sendPilgrimInfoToCastle(MyRobot r, Point target, int range) {
		short message = (short) (CASTLE_INFORM_MASK + (target.x << 6) + target.y);
		sendBroadcast(r, message, range);
	}
	
	public static Point getPilgrimTargetForCastle(MyRobot r, Robot other) {
		if (r.isRadioing(other) && instructionMatches(CASTLE_INFORM_MASK, other.signal)) {
			short message = (short) other.signal;
			return new Point((message / (64)) % 64, message % 64);
		}
		return null;
	}

}
