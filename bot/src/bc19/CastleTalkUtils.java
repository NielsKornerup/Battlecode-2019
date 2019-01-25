package bc19;

public class CastleTalkUtils {

    private static final int INSTRUCTION_SIZE_BITS = 2;
    private static final int ARGUMENT_SIZE_BITS = 6;

    private static final byte CASTLE_LOCATION_COMMUNICATION_MASK = (byte) (0b11 << ARGUMENT_SIZE_BITS);
    private static final byte ENEMY_CASTLE_KILLED_MASK = (byte) (0b10 << ARGUMENT_SIZE_BITS);
    private static final byte FRIENDLY_CASTLE_KILLED_MASK = (byte) (0b01 << ARGUMENT_SIZE_BITS);
    private static final byte FRIENDLY_PILGRIM_SPAWNED_MASK = (byte) (0b00101000);

    private static void sendCastleTalk(MyRobot r, byte message) {
        r.castleTalk(message);
    }

    private static boolean instructionMatches(byte instructionMask, int castleTalk) {
        return instructionMask >>> ARGUMENT_SIZE_BITS == ((byte) castleTalk) >>> ARGUMENT_SIZE_BITS;
    }

    public static int getCastleCoord(MyRobot r, Robot other) {
        if (instructionMatches(CASTLE_LOCATION_COMMUNICATION_MASK, other.castle_talk)) {
            return other.castle_talk % (0b1 << ARGUMENT_SIZE_BITS);
        }
        return -1;
    }

    // Send either an X or Y coordinate.
    public static void sendCastleCoord(MyRobot r, int coordinate) {
        byte message = (byte) (CASTLE_LOCATION_COMMUNICATION_MASK | ((byte) coordinate));
        sendCastleTalk(r, message);
    }

    public static boolean enemyCastleKilled(MyRobot r, Robot other) {
        return instructionMatches(ENEMY_CASTLE_KILLED_MASK, other.castle_talk);
    }

    public static boolean enemyCastleKilledLocationMatches(MyRobot r, Robot other, Point position) {
        return (position.x ^ position.y) == (other.castle_talk % (0b1 << ARGUMENT_SIZE_BITS));
    }

    public static void sendEnemyCastleKilled(MyRobot r, Point enemyCastleLocation) {
        byte message = (byte) (ENEMY_CASTLE_KILLED_MASK | ((byte) (enemyCastleLocation.x ^ enemyCastleLocation.y)));
        sendCastleTalk(r, message);
    }

    public static void sendFriendlyPilgrimSpawned(MyRobot r) {
        sendCastleTalk(r, FRIENDLY_PILGRIM_SPAWNED_MASK);
    }

    public static boolean friendlyPilgrimSpawned(MyRobot r, Robot other) {
        return ((byte) other.castle_talk) == FRIENDLY_PILGRIM_SPAWNED_MASK;
    }

    public static void invalidate(MyRobot r) {
        sendCastleTalk(r, (byte) 0);
    }
}
