import bc.*;
import java.util.ArrayList;

/*
Subclasses of these objects actually issue the move commands for their units
*/
public class Squad{
    ArrayList<Integer> units;
    double urgency;
    MapLocation targetLoc;
    UnitType toBuild;
    ArrayList<UnitType> requestedUnits;
    GameController gc;

    Objective objective;

    public Squad(GameController g){
        units = new ArrayList<Integer>();
        urgency = 0;
        targetLoc = null;
        toBuild = UnitType.Factory;
        requestedUnits = new ArrayList<UnitType>();
        objective = Objective.NONE;
        gc = g;
    }

    public void update(){
        // managers will update squad needs
    	// squads will only reset their objective once completed
    }

    public void move(Nav nav){
        // actually move the units
    }
}