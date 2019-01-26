package bc19;

/**
 * Created by patil215 on 1/18/19.
 */

import java.util.ArrayList;

public class Preacher {

	private static Point initialCastleLocation;
	private static Point enemyCastleLocation;

	private static boolean isTurtle = false;

	private static Navigation enemyCastleMap;

	public static Action act(MyRobot r) {
		if (r.turn == 1) {
            initialCastleLocation = Utils.getSpawningCastleOrChurchLocation(r);
            enemyCastleLocation = Utils.getMirroredPosition(r, initialCastleLocation);

            doCrusaderInitialization(r);
        }

		// 1. Attack enemies if nearby
        AttackAction attackAction = Utils.tryAndAttack(r, Utils.mySpecs(r).ATTACK_RADIUS[1]);
        if (attackAction != null) {
            return attackAction;
        }

        // 2. Move towards aggression point
        return Utils.moveDijkstraThenRandom(r, enemyCastleMap, 2);
	}

	private static void doCrusaderInitialization(MyRobot r) {
        // Check if we are a turtle
        for (Robot robot : r.getVisibleRobots()) {
            if (CommunicationUtils.receivedTurtleLocation(r, robot)) {
                isTurtle = true;
                ArrayList<Point> targets = new ArrayList<>();
                targets.add(CommunicationUtils.getTurtleLocation(r, robot));
                enemyCastleMap = new Navigation(r, r.getPassableMap(), targets);
                break;
            }
        }
    }
}
