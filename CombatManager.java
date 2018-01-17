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
	MagicNumbers magicNums;
	
	public CombatManager(InfoManager im, GameController g, MagicNumbers mn){
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
			boolean stop = false;
			for(MapLocation ml: enemyStartingLocs){
				if(ml.distanceSquaredTo(loc) < magicNums.SQUAD_SEPARATION_THRESHOLD){
					stop = true;
					break;
				}
			}
			if(stop)
				continue;
			CombatSquad cs = new CombatSquad(gc);
			cs.objective = Objective.ATTACK_LOC;
			cs.targetLoc = loc;
			cs.update();
			infoMan.combatSquads.add(cs);
		}
	}


	// assign unassigned fighters
	// shuffle fighter squads (and add to rocket squads if need be)
	// make sure their objectives and stuff are right
	// call update method of each squad?
	// remember, the squads will move on their own after you update everything
	public void update(Strategy strat){
		if(infoMan.combatSquads.size()==0) {
			CombatSquad cs = new CombatSquad(gc);
			cs.objective = Objective.EXPLORE;
			cs.update();
			infoMan.combatSquads.add(cs);
		}

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

		boolean didSomething = false;
		while(infoMan.unassignedUnits.size() > 0) {
			didSomething = false;
			infoMan.combatSquads.sort(Squad.byUrgency());
			boolean tryAgain = false;
			for(CombatSquad cs : infoMan.combatSquads) {
				for(UnitType u : cs.requestedUnits) {
					for(Unit a : infoMan.unassignedUnits) {
						if(a.unitType() == u) {
							System.out.println("adding to cs");
							cs.requestedUnits.remove(u);
							int toAdd = infoMan.unassignedUnits.get(infoMan.unassignedUnits.indexOf(a)).id();
							cs.units.add(toAdd);
							cs.separatedUnits.add(toAdd);
							infoMan.unassignedUnits.remove(a);
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