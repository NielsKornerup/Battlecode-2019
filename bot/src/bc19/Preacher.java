package bc19;

/**
 * Created by patil215 on 1/18/19.
 */

import java.util.ArrayList;
import java.util.List;

public class Preacher {

    private static Point initialCastleLocation;
    private static Point enemyCastleLocation;

    private static Navigation enemyCastleMap;

    private static boolean shouldClump = false;

    private static void invalidateEnemyCastleTargetsIfNecessary(MyRobot r) {
        if (enemyCastleMap == null) {
            return;
        }

        List<Point> targets = enemyCastleMap.getTargets();

        Point myLoc = Utils.myLocation(r);
        for (Point target : targets) {
            if (Utils.computeSquareDistance(target, myLoc) > Utils.mySpecs(r).VISION_RADIUS) {
                continue;
            }
            boolean foundCastle = false;
            for (Robot robot : Utils.getRobotsInRange(r, r.SPECS.CASTLE, false, 0, 10000)) {
                if (Utils.getLocation(robot).equals(target)) {
                    foundCastle = true;
                }
            }
            if (!foundCastle) {
                CastleTalkUtils.sendEnemyCastleKilled(r, target);
            }
        }
    }

    public static Action act(MyRobot r) {
        if (r.turn == 1) {
            initialCastleLocation = Utils.getSpawningCastleOrChurchLocation(r);
            enemyCastleLocation = Utils.getMirroredPosition(r, initialCastleLocation);
        }

        if (!shouldClump && CommunicationUtils.receivedRushClumpMessage(r)) {
            shouldClump = true;
        }

        doCrusaderInitialization(r);

        invalidateEnemyCastleTargetsIfNecessary(r);

        // 1. Attack enemies if nearby
        AttackAction attackAction = Utils.tryAndAttack(r, Utils.mySpecs(r).ATTACK_RADIUS[1]);
        if (attackAction != null) {
            return attackAction;
        }

        if (shouldClump) {
            if (nearOtherPreachers(r)) {
                shouldClump = false;
            }
            return null;
        }

        // 2. Move towards aggression point
        return Utils.moveDijkstraThenRandom(r, enemyCastleMap, 2);
    }

    private static boolean nearOtherPreachers(MyRobot r) {
        List<Robot> closePreachers = Utils.getRobotsInRange(r, r.SPECS.PREACHER, true, 0, 3);
        return closePreachers.size() > 0;
    }

    private static void doCrusaderInitialization(MyRobot r) {
        // Check if we are a turtle
        for (Robot robot : r.getVisibleRobots()) {
            if (CommunicationUtils.receivedTurtleLocation(r, robot)) {
                ArrayList<Point> targets = new ArrayList<>();
                targets.add(CommunicationUtils.getTurtleLocation(r, robot));
                r.log("My turn is " + r.turn + " and new Preacher target " + targets.get(0).toString());
                enemyCastleMap = new Navigation(r, r.getPassableMap(), targets);
                break;
            }
        }
    }
}
