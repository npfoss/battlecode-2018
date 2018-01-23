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

    /* modifies rocketSquads as necessary and launches rockets
            (remember, those are in infoMan) */
    public void update(Strategy strat){

        // adjust for rocket deaths
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

        // find new (unassigned) rockets and make squads
        for (Unit rocket : infoMan.newRockets){
            infoMan.rocketSquads.add(new RocketSquad(infoMan, rocket.location().mapLocation()));
        }

        // udpate() each squad so we know what units to find
        for (RocketSquad rs : infoMan.rocketSquads){
            rs.update(strat.rocketComposition);
        }

        // poach nearby units if reasonable
        
    }
}