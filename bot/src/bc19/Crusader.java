package bc19;

/**
 * Created by patil215 on 1/18/19.
 */
public class Crusader {

	private static Point initialCastleLocation;
	private static Point enemyCastleLocation;

	public static Action act(MyRobot r) {
		if (r.turn == 1) {
			initialCastleLocation = Utils.getSpawningCastleOrChurchLocation(r);
			enemyCastleLocation = Utils.getMirroredPosition(r, initialCastleLocation);
		}

		Action action = Utils.tryAndAttack(r, Constants.CRUSADER_ATTACK_RADIUS);
		if(action == null) {
			action = Ring.ringFormation(r, initialCastleLocation, enemyCastleLocation);
		}
		return action;
	}
}
