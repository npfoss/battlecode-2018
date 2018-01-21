import bc.*;
import java.util.ArrayList;

/*
creates and assigns empty RocketSquads to rockets
launches rockets when it's time

Schedules rocket launches.
As the scheduled launch nears, sets up rocket squads to fill each rocket
*/
public class RocketManager{
    InfoManager infoMan;
    public static GameController gc;

    public RocketManager(GameController g, InfoManager im){
        gc = g;
        infoMan = im;
    }

    public void update(Strategy strat){
        // modifies rocketSquads as necessary and launches rockets
        // remember, those are in infoMan
    	//sets the squad to be in space or not
    	
    	//move unassigned units into rocket squads
    	//get info from mars to make smart decisions
    	
        // also update() on each squad after
        
    }
}