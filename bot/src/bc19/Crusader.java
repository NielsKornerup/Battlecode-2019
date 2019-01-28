package bc19;

import java.util.ArrayList;
/**
 * Created by patil215 on 1/18/19.
 */
public class Crusader {

	private static Point initialCastleLocation;
	private static Point enemyCastleLocation;
	
	private static boolean isTurtle = false;

	private static boolean attacking = false;

	private static Action beginAttack(MyRobot r) {
		attacking = true;
		enemyCastleMap = null;
		if (Math.random() < .5) {
			CommunicationUtils.sendAttackMessage(r);
		}
		return act(r);
	}
	
	private static Navigation enemyCastleMap;

	private static void computeEnemyCastlesMap(MyRobot r) {
		ArrayList<Point> targets = new ArrayList<>();
		targets.add(enemyCastleLocation);
		//enemyCastleMap = new Navigation(r, r.getPassableMap(), targets);
		enemyCastleMap = new Navigation(r, r.getPassableMap(), targets, r.getKarboniteMap(), r.getFuelMap());
	}

	public static Action act(MyRobot r) {
		if (r.turn == 1) {
            initialCastleLocation = Utils.getSpawningCastleOrChurchLocation(r);
            enemyCastleLocation = Utils.getMirroredPosition(r, initialCastleLocation);

            doCrusaderInitialization(r);
        }

		if (CommunicationUtils.receivedAttackMessage(r)) {
			return beginAttack(r);
		}

		if (attacking) {
			if (enemyCastleMap == null) {
				computeEnemyCastlesMap(r);
			}
			return Utils.moveDijkstraThenRandom(r, enemyCastleMap, 2);
		}

		// 1. Attack enemies if nearby
        AttackAction attackAction = Utils.tryAndAttack(r, Utils.mySpecs(r).ATTACK_RADIUS[1]);
        if (attackAction != null) {
            return attackAction;
        }

        // 2. Move towards aggression point
        return Utils.moveDijkstraThenRandom(r, enemyCastleMap, 3);
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
