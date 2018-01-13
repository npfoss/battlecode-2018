import bc.*;
import java.util.ArrayList;

/*
manages overall decisions of combat units
Assigns units to squads
Rearranges combat squads (and gives units to rocketsquads) as needed
*/
public class CombatManager{
    InfoManager infoMan;
    GameController gc;
    
    public CombatManager(InfoManager im, GameController g){
        infoMan = im; //contains squads
        gc = g;
    }

  
        // assign unassigned fighters
        // shuffle fighter squads (and add to rocket squads if needbe)
        // make sure their objectives and stuff are rigt
        // call update method of each squad?
        // remember, the squads will move on their own after you update everything
    public void update(Strategy strat){
    	if(infoMan.combatSquads.size()==0) {
    		CombatSquad ws = new CombatSquad(gc);
    		ws.objective = Objective.EXPLORE;
    		infoMan.combatSquads.add(ws);
    	}
    	for(Unit u : infoMan.unassignedUnits)
    		if(u.unitType() != UnitType.Worker && u.unitType() != UnitType.Factory &&  u.unitType() != UnitType.Rocket) {
    			infoMan.workerSquads.get(0).units.add(u);
    		}
    }	
    
}