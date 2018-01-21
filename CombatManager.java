import bc.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/*
manages overall decisions of combat units
Assigns units to squads
Rearranges combat squads (and gives units to rocketsquads) as needed
 */
public class CombatManager{
	InfoManager infoMan;
	GameController gc;
	MagicNumbers magicNums;
	HashMap<Integer,Integer> turnUnassigned;

	public CombatManager(InfoManager im, GameController g, MagicNumbers mn, Strategy strat){
		infoMan = im;
		gc = g;
		magicNums = mn;
		turnUnassigned = new HashMap<Integer,Integer>();
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
					infoMan.unassignedUnits.add(uid);
					turnUnassigned.put(uid, (int)(gc.round()));
				}
				toRemove.add(cs);
			}
		}

		for(CombatSquad cs: toRemove)
			infoMan.combatSquads.remove(cs);
		
		//defend factories and rockets
		for(Unit u: infoMan.factories){
			MapLocation ml = u.location().mapLocation();
			if(infoMan.getTargetUnits(ml, 100, true).size() > 0){
				addCombatSquad(ml,Objective.DEFEND_LOC,strat);
			}
		}
		
		if(infoMan.myPlanet == Planet.Earth){
			for(Unit u: infoMan.rockets){
				MapLocation ml = u.location().mapLocation();
				if(infoMan.getTargetUnits(ml, 100, true).size() > 0){
					addCombatSquad(ml,Objective.DEFEND_LOC,strat);
				}
			}
		}
		
		if(infoMan.combatSquads.size()==1 && infoMan.combatSquads.get(0).objective == Objective.EXPLORE){
			for(TargetUnit tu: infoMan.targetUnits.values()){
				addCombatSquad(tu.myLoc,Objective.ATTACK_LOC, strat);
			}
		}
		
		if(infoMan.combatSquads.size() == 0) {
			CombatSquad cs = new CombatSquad(gc,infoMan,magicNums,strat.combatComposition);
			cs.objective = Objective.EXPLORE;
			cs.update();
			infoMan.combatSquads.add(cs);
		}

		boolean didSomething = false;
		while(infoMan.unassignedUnits.size() > 0) {
			didSomething = false;
			infoMan.combatSquads.sort(Squad.byUrgency());
			boolean tryAgain = false;
			for(CombatSquad cs : infoMan.combatSquads) {
				for(int i : infoMan.unassignedUnits) {
					Unit a = gc.unit(i);
					if(cs.requestedUnits.contains(a.unitType())) {
						if(cs.targetLoc != null && 
						((!turnUnassigned.containsKey(a.id()) && gc.round() == 1) || (turnUnassigned.containsKey(a.id()) && turnUnassigned.get(a.id()) == gc.round()))){
							MapLocation ml = cs.targetLoc;
							if(a.location().isOnMap())
								ml = a.location().mapLocation();
							else
								ml = gc.unit(a.location().structure()).location().mapLocation();
							if(!infoMan.isReachable(cs.targetLoc, ml))
								continue;
						}
						//System.out.println("adding to cs");
						cs.addUnit(a);
						tryAgain = true;
						didSomething = true;
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
			if(cs.objective != Objective.EXPLORE &&
			   (targetLoc.distanceSquaredTo(cs.targetLoc) < magicNums.SQUAD_SEPARATION_THRESHOLD || 
			   targetLoc.distanceSquaredTo(cs.swarmLoc) < 75))
				return;
		}
		CombatSquad cs = new CombatSquad(gc,infoMan,magicNums,strat.combatComposition);
		cs.objective = obj;
		cs.targetLoc = targetLoc;
		cs.update();
		infoMan.combatSquads.add(cs);
	}

}