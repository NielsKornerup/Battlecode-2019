package bc19;

import java.util.ArrayList;
import java.util.List;

public class CommunicationUtils {
	static final short PILGRIM_TARGET_MASK = (short) (0b111 << 13);
	static final short CASTLE_INFORM_MASK = (short) (0b110 << 13);

	private static void sendBroadcast(MyRobot r, short message, int radius) {
		r.signal(message, radius);
	}

	public static void sendPilgrimInfo(MyRobot r, Point target, int range) {
		short message = (short) (PILGRIM_TARGET_MASK + (target.x << 6) + target.y);
		sendBroadcast(r, message, range);
	}

	/*
	 * Returns a point for the pilgrim to target from a passed in castle
	 * 
	 * @Return null if the robot is not signaling or if it is the wrong message type
	 */
	public static Point getPilgrimTargetInfo(MyRobot r, Robot rob) {
		if (r.isRadioing(rob) && (rob.signal >>> 12 == PILGRIM_TARGET_MASK >>> 12)) {
			short message = (short) rob.signal;
			return new Point((message / (64)) % 64, message % 64);
		}
		r.log("failed to get target info. isRadioing: " + r.isRadioing(rob) + " found mask "
				+ ((short) (rob.signal >>> 12)) + " wanted mask " + (PILGRIM_TARGET_MASK >>> 12));
		return null;
	}

	public static void sendPilgrimInfoToCastle(MyRobot r, Point target, int range) {
		short message = (short) (CASTLE_INFORM_MASK + (target.x << 6) + target.y);
		sendBroadcast(r, message, range);
	}
	
	public static Point getPilgrimTargetForCastle(MyRobot r, Robot rob) {
		if (r.isRadioing(rob) && (rob.signal >>> 12 == CASTLE_INFORM_MASK >>> 12)) {
			short message = (short) rob.signal;
			return new Point((message / (64)) % 64, message % 64);
		}
		return null;
	}

}
