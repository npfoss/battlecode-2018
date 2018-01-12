import bc.*;
import java.util.ArrayList;

/*
Subclasses of these objects actually issue the move commands for their units
*/
public class Squad{
    ArrayList<Unit> units;
    double urgency;
    MapLocation targetLoc;
    ArrayList<UnitType> requestedUnits;
    GameController gc;
    public enum Objective{
        NONE(),
        EXPLORE(),
        MINE(),
        BUILD(),
        BOARD_ROCKET(),
        DEFEND_LOC(),
        ATTACK_LOC();
    }

    Objective objective;

    public Squad(GameController g){
        units = new ArrayList<Unit>();
        urgency = 0;
        targetLoc = null;
        requestedUnits = new ArrayList<UnitType>();
        objective = Objective.NONE;
        gc = g;
    }

    public void update(){
        // udpates fields every turn? managers might do that
    }

    public void move(Nav nav){
        // actually move the units
    }
}