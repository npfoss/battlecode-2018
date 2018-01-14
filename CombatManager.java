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
		boolean didSomething = false;
		while(infoMan.unassignedUnits.size() > 0) {
			didSomething = false;
			infoMan.combatSquads.sort(Squad.byUrgency());
			boolean tryAgain = false;
			for(CombatSquad cs : infoMan.combatSquads) {
				for(UnitType u : cs.requestedUnits) {
					for(Unit a : infoMan.unassignedUnits) {
						if(a.unitType() == u) {
							cs.requestedUnits.remove(cs.requestedUnits.indexOf(u));
							cs.units.add(infoMan.unassignedUnits.get(infoMan.unassignedUnits.indexOf(a)).id());
							infoMan.unassignedUnits.remove(infoMan.unassignedUnits.indexOf(a));
							cs.update();
							tryAgain = true;
							didSomething = true;
						}
						if(tryAgain)
							break;
					}
					if(tryAgain)
						break;
				}
				if(tryAgain)
					break;
			}
			if(!didSomething)
				break;
		}

	}	

}