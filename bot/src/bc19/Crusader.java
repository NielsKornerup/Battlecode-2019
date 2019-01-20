package bc19;

/**
 * Created by patil215 on 1/18/19.
 */
public class Crusader {

	public static Action act(MyRobot r) {
		Action action = Utils.tryAndAttack(r, Constants.CRUSADER_ATTACK_RADIUS);
		if(action == null) {
			action = Prophet.ringFormation(r);
		}
		return action;
	}
}
