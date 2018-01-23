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

        
        // first, adjust for rocket deaths
        ArrayList<Integer> toRemove = new ArrayList<Integer>();
        for (int i = infoMan.rocketSquads.size() - 1; i >= 0; i--){
            // make sure each squad has a rocket at index 0. if no rocket, delete it
            if (infoMan.rocketSquads.get(i).units.size() < 1 || gc.unit(infoMan.rocketSquads.get(i).units.get(0)).unitType() != UnitType.Rocket){
                toRemove.add(i);
            }
        }
        for(int i : toRemove){
            infoMan.rocketSquads.remove(i);
        }

        // second, find new (unassigned) rockets and make squads

    }
}