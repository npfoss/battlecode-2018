import bc.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

/*
manages overall decisions of combat units
Assigns units to squads
Rearranges/makes/deletes combat squads as needed
(does not handle rocketSquads)
*/
public class CombatManager{
	InfoManager infoMan;
	GameController gc;
	HashMap<Integer,Integer> turnUnassigned;

	public CombatManager(InfoManager im, GameController g, Strategy strat){
		infoMan = im;
		gc = g;
		turnUnassigned = new HashMap<Integer,Integer>();
		if(infoMan.myPlanet == Planet.Mars)
			return;
		VecUnit vu = gc.startingMap(infoMan.myPlanet).getInitial_units();
		for(int i = 0; i < vu.size(); i++){
			Unit u = vu.get(i);
			if(u.team() == gc.team())
				continue;
			MapLocation loc = u.location().mapLocation();
			addCombatSquad(loc, Objective.ATTACK_LOC, strat);
		}
	}

	// assign unassigned fighters
	// shuffle fighter squads (and add to rocket squads if need be)
	// make sure their objectives and stuff are right
	// call update method of each squad?
	// remember, the squads will move on their own after you update everything
	public void update(Strategy strat){
		for(CombatSquad cs: infoMan.combatSquads){
			cs.unitCompGoal = strat.combatComposition;
		}
		
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

		for(CombatSquad cs: toRemove){
			infoMan.combatSquads.remove(cs);
		}
		
		//defend factories and rockets
		for(Unit u: infoMan.factories){
			MapLocation ml = u.location().mapLocation();
			TreeSet<TargetUnit> tus = infoMan.getTargetUnits(ml, MagicNumbers.DEFEND_RANGE, true);
			if(tus.size() > 0){ 
				addCombatSquad(Utils.midpoint(ml, tus.first().myLoc), Objective.DEFEND_LOC, strat);
			}
		}
		
		if(infoMan.myPlanet == Planet.Earth){
			for(Unit u: infoMan.rockets){
				MapLocation ml = u.location().mapLocation();
				TreeSet<TargetUnit> tus = infoMan.getTargetUnits(ml, MagicNumbers.DEFEND_RANGE, true);
				if(tus.size() > 0){
					addCombatSquad(Utils.midpoint(ml, tus.first().myLoc), Objective.DEFEND_LOC, strat);
				}
			}
		}
		
		// make more squads as necessary
		if(infoMan.combatSquads.size() == 0) {
			addCombatSquad(null, Objective.EXPLORE, strat);
		}	

		if((infoMan.combatSquads.size() == 1 && infoMan.combatSquads.get(0).objective == Objective.EXPLORE)
				|| infoMan.myPlanet == Planet.Mars){
			for(TargetUnit tu: infoMan.targetUnits.values()){
				addCombatSquad(tu.myLoc, Objective.ATTACK_LOC, strat);
			}
		}

		// now deal with unassigned units
		boolean didSomething = false;
		while(infoMan.unassignedUnits.size() > 0) {
			didSomething = false;
			infoMan.combatSquads.sort(Squad.byUrgency());
			for(CombatSquad cs : infoMan.combatSquads) {
				for(int i : infoMan.unassignedUnits) {
					Unit a = gc.unit(i);
					if(cs.requestedUnits.contains(a.unitType())) {
						if(cs.targetLoc != null
								&& ((!turnUnassigned.containsKey(a.id()) && gc.round() == 1)
									|| (turnUnassigned.containsKey(a.id()) && turnUnassigned.get(a.id()) == gc.round()))){
							MapLocation ml = cs.targetLoc;
							if(a.location().isOnMap())
								ml = a.location().mapLocation();
							else
								ml = gc.unit(a.location().structure()).location().mapLocation();
							if(!infoMan.isReachable(cs.targetLoc, ml))
								continue;
						}
						cs.addUnit(a);
						didSomething = true;
					}
					if(didSomething){
						break;
					}
				}
				if(didSomething)
					break;
			}
			if(!didSomething)
				break;
		}
		
		while(infoMan.unassignedUnits.size() > 0) {
			didSomething = false;
			infoMan.combatSquads.sort(Squad.byUrgency());
			for(CombatSquad cs : infoMan.combatSquads) {
				for(int i : infoMan.unassignedUnits) {
					if(gc.unit(i).unitType() == UnitType.Worker)
						continue;
					Unit a = gc.unit(i);
					if(cs.targetLoc != null
							&& ((!turnUnassigned.containsKey(a.id()) && gc.round() == 1)
								|| (turnUnassigned.containsKey(a.id()) && turnUnassigned.get(a.id()) == gc.round()))){
						MapLocation ml = cs.targetLoc;
						if(a.location().isOnMap())
							ml = a.location().mapLocation();
						else
							ml = gc.unit(a.location().structure()).location().mapLocation();
						if(!infoMan.isReachable(cs.targetLoc, ml))
							continue;
					}
					cs.addUnit(a);
					didSomething = true;
					if(didSomething){
						break;
					}
				}
				if(!didSomething)
					break;
			}
			if(!didSomething)
				break;
		}
		
		infoMan.logTimeCheckpoint("done updating CombatManager");
	}	
	
	public void addCombatSquad(MapLocation targetLoc, Objective obj, Strategy strat){
		for(CombatSquad cs: infoMan.combatSquads){
			if(cs.objective != Objective.EXPLORE
					&& (targetLoc.distanceSquaredTo(cs.targetLoc) < MagicNumbers.SQUAD_SEPARATION_THRESHOLD
			   				|| targetLoc.distanceSquaredTo(cs.swarmLoc) < MagicNumbers.SWARM_SEPARATION_THRESHOLD))
				return;
		}
		CombatSquad cs = new CombatSquad(gc, infoMan, strat, strat.combatComposition);
		cs.objective = obj;
		cs.targetLoc = targetLoc;
		cs.update();
		Utils.log("adding cs");
		infoMan.combatSquads.add(cs);
	}

}
