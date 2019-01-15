package bc19;

public class Pilgrim implements BCRobot {

    MyRobot r;

    public Pilgrim(MyRobot myRobot){
        this.r = myRobot;
    }

    public Action act() {
        return Utils.moveRandom(r);
    }
}
