package bc19;

public class MyRobot extends BCAbstractRobot {
	public int turn;

	public Action turn() {
		doAllUnitActions();

		BCRobot robot = null;
		if (me.unit == SPECS.CASTLE) {
		    robot = new Castle(this);
		} else if (me.unit == SPECS.PILGRIM) {
			robot = new Pilgrim(this);
		} else if (me.unit == SPECS.CHURCH) {
			//robot = new Church(this);
		} else if (me.unit == SPECS.CRUSADER) {
		    //robot = new Crusader(this);
		} else if (me.unit == SPECS.PROPHET) {
			//robot = new Prophet(this);
		} else if (me.unit == SPECS.PREACHER) {
			//robot = new Preacher(this);
		}
		return robot.act();
	}

	private void doAllUnitActions() {
		turn++;
	}
}