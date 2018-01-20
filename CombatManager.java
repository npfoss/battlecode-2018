import bc.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/*
manages overall decisions of combat units
Assigns units to squads
Rearranges combat squads (and gives units to rocketsquads) as needed
 */
public class CombatManager{
	InfoManager infoMan;
	GameController gc;
	MagicNumbers magicNums;

	public CombatManager(InfoManager im, GameController g, MagicNumbers mn, Strategy strat){
		infoMan = im;
		gc = g;
		magicNums = mn;
		if(infoMan.myPlanet == Planet.Mars)
			return;
		VecUnit vu = gc.startingMap(infoMan.myPlanet).getInitial_units();
		ArrayList<MapLocation> enemyStartingLocs = new ArrayList<MapLocation>();
		for(int i=0; i<vu.size(); i++){
			Unit u = vu.get(i);
			if(u.team() == gc.team())
				continue;
			MapLocation loc = u.location().mapLocation();
			addCombatSquad(loc,Objective.ATTACK_LOC,strat);
		}
	}


	// assign unassigned fighters
	// shuffle fighter squads (and add to rocket squads if need be)
	// make sure their objectives and stuff are right
	// call update method of each squad?
	// remember, the squads will move on their own after you update everything
	public void update(Strategy strat){
		// set units whose objective is NONE (meaning they completed it) to unassignedUnits
		ArrayList<CombatSquad> toRemove = new ArrayList<CombatSquad>();
		for(CombatSquad cs: infoMan.combatSquads){
			if(cs.objective == Objective.NONE){
				for(int uid: cs.units){
					infoMan.unassignedUnits.add(gc.unit(uid));
				}
				toRemove.add(cs);
			}
		}

		for(CombatSquad cs: toRemove)
			infoMan.combatSquads.remove(cs);
		
		//defend factories and rockets
		for(Unit u: infoMan.factories){
			MapLocation ml = u.location().mapLocation();
			if(Utils.getTargetUnits(ml, 100, true, infoMan).size()>0){
				addCombatSquad(ml,Objective.DEFEND_LOC,strat);
			}
		}
		
		if(infoMan.myPlanet == Planet.Earth){
			for(Unit u: infoMan.rockets){
				MapLocation ml = u.location().mapLocation();
				if(Utils.getTargetUnits(ml, 100, true, infoMan).size()>0){
					addCombatSquad(ml,Objective.DEFEND_LOC,strat);
				}
			}
		}
		
		if(infoMan.combatSquads.size()==0) {
			//go after enemies
			if(infoMan.targetUnits.size()>0){
				for(TargetUnit tu: infoMan.targetUnits.values()){
					addCombatSquad(tu.myLoc,Objective.ATTACK_LOC,strat);
				}
			}
			//otherwise find one
			else{
				CombatSquad cs = new CombatSquad(gc,infoMan,magicNums,strat.combatComposition);
				cs.objective = Objective.EXPLORE;
				cs.update();
				infoMan.combatSquads.add(cs);
			}
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
							MapLocation ml = cs.targetLoc;
							if(a.location().isOnMap())
								ml = a.location().mapLocation();
							else
								ml = gc.unit(a.location().structure()).location().mapLocation();
							if(!infoMan.isReachable(cs.targetLoc, ml))
								continue;
							//System.out.println("adding to cs");
							Unit toAdd = infoMan.unassignedUnits.get(infoMan.unassignedUnits.indexOf(a));
							cs.addUnit(toAdd);
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
	
	public void addCombatSquad(MapLocation targetLoc, Objective obj, Strategy strat){
		for(CombatSquad cs: infoMan.combatSquads){
			if(targetLoc.distanceSquaredTo(cs.targetLoc) < magicNums.SQUAD_SEPARATION_THRESHOLD || 
			   targetLoc.distanceSquaredTo(cs.swarmLoc) < 75)
				return;
		}
		CombatSquad cs = new CombatSquad(gc,infoMan,magicNums,strat.combatComposition);
		cs.objective = obj;
		cs.targetLoc = targetLoc;
		cs.update();
		infoMan.combatSquads.add(cs);
	}

}