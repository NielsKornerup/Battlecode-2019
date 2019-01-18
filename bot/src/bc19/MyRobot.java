package bc19;

public class MyRobot extends BCAbstractRobot {
	public int turn;

	public Action turn() {
		doUnitPreTurnActions();

		Action actionToDo = null;
		if (me.unit == SPECS.CASTLE) {
		    actionToDo = Castle.act(this);
		} else if (me.unit == SPECS.PILGRIM) {
			actionToDo = Pilgrim.act(this);
		} else if (me.unit == SPECS.CHURCH) {
			actionToDo = Church.act(this);
		} else if (me.unit == SPECS.CRUSADER) {
			actionToDo = Crusader.act(this);
		} else if (me.unit == SPECS.PROPHET) {
			actionToDo = Prophet.act(this);
		} else if (me.unit == SPECS.PREACHER) {
			actionToDo = Preacher.act(this);
		}

		doUnitPostTurnActions();

		return actionToDo;
	}

	/*
	Actions that the robot should do at the beginning of the turn, regardless of what unit type they are.
	 */
	private void doUnitPreTurnActions() {
		turn++;
	}

	/*
	Actions that the robot should do at the end of the turn, regardless of what unit type they are.
	 */
	private void doUnitPostTurnActions() {

	}
}