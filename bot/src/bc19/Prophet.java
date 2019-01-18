package bc19;

import java.util.ArrayList;

public class Prophet {

    private static Navigation enemyCastleMap;

    public static void computeMaps(MyRobot r) {
        // We use our current castle location to infer the enemy's castle location. This is then used for Dijkstra map.
        // TODO: this means that a unit is not aware of all castles, which can be problematic.
        // TODO: we may need to remove this assumption at some point, especially if we're using
        //  Churches (since there are no guaranteed enemy counterparts to those)
        ArrayList<Point> targets = new ArrayList<>();
        targets.add(Utils.getMirroredCastle(r));
        enemyCastleMap = new Navigation(r, r.getPassableMap(), targets);
    }


    public static Action act(MyRobot r) {

        if (r.turn == 1) {
            computeMaps(r);
        }

        // Attack enemies if nearby
        AttackAction attackAction = Utils.tryAndAttack(r, Utils.mySpecs(r).ATTACK_RADIUS[1]);
        if (attackAction != null) {
            return attackAction;
        }

        // Do movement stuff.
        // If there are a few friendly units nearby, then move a step towards the enemy.
        int numFriendliesNearby = Utils.getUnitsInRange(r, -1, true, 0, Integer.MAX_VALUE).size();
        // TODO: adjust this movement function to balance well between clumping and trickling.
        double probabilityMoving = 1.0 / (1.0 + Math.exp(-(numFriendliesNearby - 4))); // Modified sigmoid function
        if (Math.random() < probabilityMoving) {
            return Utils.moveMapThenRandom(r, enemyCastleMap, 1);
        }

        return null;
    }

}