package bc19;

import java.util.ArrayList;

public class Prophet implements BCRobot {

    MyRobot r;

    private static Navigation enemyCastleMap;

    public Prophet(MyRobot myRobot) {
        this.r = myRobot;
    }

    public void computeMaps() {
        // We use our current castle location to infer the enemy's castle location. This is then used for Dijkstra map.
        // TODO: this means that a unit is not aware of all castles, which can be problematic.
        // TODO: we may need to remove this assumption at some point, especially if we're using
        //  Churches (since there are no guaranteed enemy counterparts to those)
        ArrayList<Point> targets = new ArrayList<>();
        targets.add(Utils.getMirroredCastle(r));
        enemyCastleMap = new Navigation(r, r.getPassableMap(), targets);
    }


    public Action act() {

        if (r.turn == 1) {
            computeMaps();
        }

        // Look around to see if there are enemies
        ArrayList<Point> enemiesNearby = Utils.getUnitsInRange(r, -1, false, 0, Integer.MAX_VALUE);
        if (enemiesNearby.size() > 0) {
            r.log("Enemy nearby!");

            // Attack an enemy at random
            // TODO: smart targeting of enemies (prioritize Castles, etc and sort list)
            for (Point attackPoint : enemiesNearby) {
                if (Utils.canAttack(r, attackPoint.x, attackPoint.y)) {
                    return r.attack(attackPoint.x, attackPoint.y);
                }
            }
        } else {
            // Do movement stuff.
            // If there are a few friendly units nearby, then move a step towards the enemy.
            int numFriendliesNearby = Utils.getUnitsInRange(r, -1, true, 0, Integer.MAX_VALUE).size();
            // TODO: adjust this movement function to balance well between clumping and trickling.
            double probabilityMoving = 1.0 / (1.0 + Math.exp(-(numFriendliesNearby - 4))); // Modified sigmoid function
            if (Math.random() < probabilityMoving) {
                return Utils.moveMapThenRandom(r, enemyCastleMap, 1);
            } else {
                // Stay put. Don't do anything because it wastes fuel lol.
            }
        }

        return null;
    }

}