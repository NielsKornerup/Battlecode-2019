package bc19;

public class MyRobot extends BCAbstractRobot {
	public int turn;

	public Action turn() {
		doAllUnitActions();

		if (me.unit == SPECS.CASTLE) {
			return doCastle();
		} else if (me.unit == SPECS.PILGRIM) {
			return doPilgrim();
		} else if (me.unit == SPECS.CHURCH) {

		} else if (me.unit == SPECS.CRUSADER) {

		} else if (me.unit == SPECS.PROPHET) {

		} else if (me.unit == SPECS.PREACHER) {

		}
		return null;
	}

	private void doAllUnitActions() {
		turn++;
	}

	private Action doCastle() {
		if (turn == 1) {
			log("Building a pilgrim.");
			return buildUnit(SPECS.PILGRIM, 1, 0);
		}
		return null;
	}

	private Action doPilgrim() {
		if (me.unit == SPECS.PILGRIM) {
			if (turn == 1) {
				log("I am a pilgrim.");
			}
		}
		return null;
	}
}