package bc19;

import java.util.ArrayList;

public class BCAbstractRobot {
	public bc19.SpecHolder SPECS;
	private bc19.GameState gameState;
	private ArrayList<String> logs;
	private int signal;
	private int signalRadius;
	private int castleTalk;
	public bc19.Robot me;
	public int id;
	public int fuel;
	public int karbonite;
	public int[][] lastOffer;
	public boolean[][] map;
	public boolean[][] karboniteMap;
	public boolean[][] fuelMap;

	public BCAbstractRobot() {
		resetState();
	}

	public void setSpecs(bc19.SpecHolder specs) {
		SPECS = specs;
	}

	private void resetState() {
		logs = new ArrayList<String>();
		signal = 0;
		signalRadius = 0;
		castleTalk = 0;
	}

	public bc19.Action _do_turn(bc19.GameState gameState) {
		this.gameState = gameState;

		id = gameState.id;
		karbonite = gameState.karbonite;
		fuel = gameState.fuel;
		lastOffer = gameState.last_offer;
		me = getRobot(this.id);
		if (me.turn == 1) {
			map = gameState.map;
			karboniteMap = gameState.karbonite_map;
			fuelMap = gameState.fuel_map;
		}
		bc19.Action t = null;

		try {
			t = turn();
		} catch (Exception e) {
			t = new bc19.ErrorAction(e, signal, signalRadius, logs, castleTalk);
		}
		if (t == null) t = new bc19.Action(signal, signalRadius, logs, castleTalk);

		t.signal = signal;
		t.signal_radius = signalRadius;
		t.logs = logs;
		t.castle_talk = castleTalk;

		resetState();
		return t;
	}

	private boolean checkOnMap(int x, int y) {
		return x >= 0 && x < gameState.shadow[0].length && y >= 0 && y < gameState.shadow.length;
	}

	public void log(String message) {
		logs.add(message);
	}

	public void signal(int value, int radius) {
		if (fuel < radius) throw new bc19.BCException("Not enough fuel to signal given radius.");
		if (value < 0 || value >= Math.pow(2, SPECS.COMMUNICATION_BITS))
			throw new bc19.BCException("Invalid signal, must be within bit range.");
		if (radius > 2 * Math.pow(SPECS.MAX_BOARD_SIZE - 1, 2)) throw new bc19.BCException("Signal radius is too big.");
		signal = value;
		signalRadius = radius;
		fuel -= radius;
	}

	public void castleTalk(int value) {
		if (value < 0 || value >= Math.pow(2, SPECS.CASTLE_TALK_BITS))
			throw new bc19.BCException("Invalid castle talk, must be between 0 and 2^8.");
		castleTalk = value;
	}

	public bc19.TradeAction proposeTrade(int k, int f) {
		if (me.unit != SPECS.CASTLE) throw new bc19.BCException("Only castles can trade.");
		if (Math.abs(k) >= SPECS.MAX_TRADE || Math.abs(f) >= SPECS.MAX_TRADE)
			throw new bc19.BCException("Cannot trade over " + Integer.toString(SPECS.MAX_TRADE) + " in a given turn.");
		return new bc19.TradeAction(f, k, signal, signalRadius, logs, castleTalk);
	}

	public bc19.BuildAction buildUnit(int unit, int dx, int dy) {
		if (me.unit != SPECS.PILGRIM && me.unit != SPECS.CASTLE && me.unit != SPECS.CHURCH)
			throw new bc19.BCException("This unit type cannot build.");
		if (me.unit == SPECS.PILGRIM && unit != SPECS.CHURCH)
			throw new bc19.BCException("Pilgrims can only build churches.");
		if (me.unit != SPECS.PILGRIM && unit == SPECS.CHURCH)
			throw new bc19.BCException("Only pilgrims can build churches.");

		if (dx < -1 || dy < -1 || dx > 1 || dy > 1) throw new bc19.BCException("Can only build in adjacent squares.");
		if (!checkOnMap(me.x + dx, me.y + dy)) throw new bc19.BCException("Can't build units off of map.");
		if (gameState.shadow[me.y + dy][me.x + dx] != 0) throw new bc19.BCException("Cannot build on occupied tile.");
		if (!map[me.y + dy][me.x + dx]) throw new bc19.BCException("Cannot build onto impassable terrain.");
		if (karbonite < SPECS.UNITS[unit].CONSTRUCTION_KARBONITE || fuel < SPECS.UNITS[unit].CONSTRUCTION_FUEL)
			throw new bc19.BCException("Cannot afford to build specified unit.");
		return new bc19.BuildAction(unit, dx, dy, signal, signalRadius, logs, castleTalk);
	}

	public bc19.MoveAction move(int dx, int dy) {
		if (me.unit == SPECS.CASTLE || me.unit == SPECS.CHURCH)
			throw new bc19.BCException("Churches and Castles cannot move.");
		if (!checkOnMap(me.x + dx, me.y + dy)) throw new bc19.BCException("Can't move off of map.");
		if (gameState.shadow[me.y + dy][me.x + dx] == -1)
			throw new bc19.BCException("Cannot move outside of vision range.");
		if (gameState.shadow[me.y + dy][me.x + dx] != 0) throw new bc19.BCException("Cannot move onto occupied tile.");
		if (!map[me.y + dy][me.x + dx]) throw new bc19.BCException("Cannot move onto impassable terrain.");
		int r = dx * dx + dy * dy;  // Squared radius
		if (r > SPECS.UNITS[me.unit].SPEED)
			throw new bc19.BCException("Slow down, cowboy.  Tried to move faster than unit can.");
		if (fuel < r * SPECS.UNITS[this.me.unit].FUEL_PER_MOVE)
			throw new bc19.BCException("Not enough fuel to move at given speed.");
		return new bc19.MoveAction(dx, dy, signal, signalRadius, logs, castleTalk);
	}

	public bc19.MineAction mine() {
		if (me.unit != SPECS.PILGRIM) throw new bc19.BCException("Only Pilgrims can mine.");
		if (fuel < SPECS.MINE_FUEL_COST) throw new bc19.BCException("Not enough fuel to mine.");

		if (karboniteMap[me.y][me.x]) {
			if (me.karbonite >= SPECS.UNITS[SPECS.PILGRIM].KARBONITE_CAPACITY)
				throw new bc19.BCException("Cannot mine, as at karbonite capacity.");
		} else if (fuelMap[me.y][me.x]) {
			if (me.fuel >= SPECS.UNITS[SPECS.PILGRIM].FUEL_CAPACITY)
				throw new bc19.BCException("Cannot mine, as at fuel capacity.");
		} else throw new bc19.BCException("Cannot mine square without fuel or karbonite.");
		return new bc19.MineAction(signal, signalRadius, logs, castleTalk);
	}

	public bc19.GiveAction give(int dx, int dy, int k, int f) {
		if (dx > 1 || dx < -1 || dy > 1 || dy < -1 || (dx == 0 && dy == 0))
			throw new bc19.BCException("Can only give to adjacent squares.");
		if (!checkOnMap(me.x + dx, me.y + dy)) throw new bc19.BCException("Can't give off of map.");
		if (gameState.shadow[me.y + dy][me.x + dx] <= 0) throw new bc19.BCException("Cannot give to empty square.");
		if (k < 0 || f < 0 || me.karbonite < k || me.fuel < f)
			throw new bc19.BCException("Do not have specified amount to give.");
		return new bc19.GiveAction(k, f, dx, dy, signal, signalRadius, logs, castleTalk);
	}

	public AttackAction attack(int dx, int dy) {
		if (me.unit != SPECS.CRUSADER && this.me.unit != SPECS.PREACHER && me.unit != SPECS.PROPHET)
			throw new bc19.BCException("Given unit cannot attack.");
		if (fuel < SPECS.UNITS[me.unit].ATTACK_FUEL_COST) throw new bc19.BCException("Not enough fuel to attack.");
		if (!checkOnMap(me.x + dx, me.y + dy)) throw new bc19.BCException("Can't attack off of map.");
		if (gameState.shadow[me.y + dy][me.x + dx] == -1)
			throw new bc19.BCException("Cannot attack outside of vision range.");
		if (!map[me.y + dy][me.x + dx]) throw new bc19.BCException("Cannot attack impassable terrain.");
		if (gameState.shadow[me.y + dy][me.x + dx] == 0) throw new bc19.BCException("Cannot attack empty tile.");
		int r = dx * dx + dy * dy;
		if (r > SPECS.UNITS[me.unit].ATTACK_RADIUS[1] || r < SPECS.UNITS[me.unit].ATTACK_RADIUS[0])
			throw new bc19.BCException("Cannot attack outside of attack range.");
		return new AttackAction(dx, dy, signal, signalRadius, logs, castleTalk);

	}

	public bc19.Robot getRobot(int id) {
		if (id <= 0) return null;
		for (int i = 0; i < gameState.visible.length; i++) {
			if (gameState.visible[i].id == id) {
				return gameState.visible[i];
			}
		}
		return null;
	}

	public boolean isVisible(bc19.Robot robot) {
		for (int x = 0; x < gameState.shadow[0].length; x++) {
			for (int y = 0; y < gameState.shadow.length; y++) {
				if (robot.id == gameState.shadow[y][x]) return true;
			}
		}
		return false;
	}

	public boolean isRadioing(bc19.Robot robot) {
		return robot.signal >= 0;
	}

	// Get map of visible robot IDs.
	public int[][] getVisibleRobotMap() {
		return gameState.shadow;
	}

	// Get boolean map of passable terrain.
	public boolean[][] getPassableMap() {
		return map;
	}

	// Get boolean map of karbonite points.
	public boolean[][] getKarboniteMap() {
		return karboniteMap;
	}

	// Get boolean map of impassable terrain.
	public boolean[][] getFuelMap() {
		return fuelMap;
	}

	// Get a list of robots visible to you.
	public bc19.Robot[] getVisibleRobots() {
		return gameState.visible;
	}

	public bc19.Action turn() {
		return null;
	}
}