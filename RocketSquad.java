import bc.*;
import java.util.ArrayList;

/*
moves units to rocket
decides when to launch
launches rocket

NOTE: infoMan decides destination loc

TODO:
-improve launch trigger (take into account orbital stuff, coordinated launch from RocketManager)\
*/
public class RocketSquad extends Squad {

	boolean isInSpace = false;
	// NOTE: rocket is always unit at index 0 (maintained by manager)
	Unit rocket;
	long startRound;

	public RocketSquad(InfoManager infoMan, MapLocation rocketLoc){
		super(infoMan);
		objective = Objective.BOARD_ROCKET;
		targetLoc = rocketLoc;
		startRound = gc.round();
		urgency = 5; // meaningless and arbitrary
	}

	public void update(Strategy strat){
		update(Strategy.defaultRocketComposition, strat);
	}

	public void update(int[] idealComposition, Strategy strat){
		// first, are we in space?
		if (isInSpace) return;

		rocket = gc.unit(units.get(0));

		// see which units to request

		// NOTE: ideal composition is a list of ints:
		//		# knight, mage, ranger, healer, worker
		int[] counts = {0,0,0,0,0};
		for (int id : units) {
			switch(gc.unit(id).unitType()){
				case Knight: counts[0]++; break;
				case Mage  : counts[1]++; break;
				case Ranger: counts[2]++; break;
				case Healer: counts[3]++; break;
				case Worker: counts[4]++; break;
			}
		}
		requestedUnits.clear();
		long capacity = rocket.structureMaxCapacity();
		// *arbitrary order of importance*
		int[] order = {2, 3, 1, 0, 4};
		for (int ind : order){
			long diff = idealComposition[ind] - counts[ind];
			if (diff > capacity - units.size() - requestedUnits.size() + 1){
				diff = capacity - units.size() - requestedUnits.size() + 1;
			}
			for (int i = 0; i < diff; i++){
					requestedUnits.add(Utils.robotTypes[ind]);
			}
		}
		urgency = strat.calcRocketUrgency(units.size());
	}

	public void move(Nav nav, Strategy strat){
		if(isInSpace)
			return;
		
		int numUnitsInside = 0;
		// start at 1 because rocket is first one
		for(int i = 1; i < units.size(); i++) {
			int id = units.get(i);
			Unit astronaut = gc.unit(id);
			if(astronaut.location().isInGarrison()){
				numUnitsInside++;
			} else if(gc.canLoad(rocket.id(), id)){
				gc.load(rocket.id(), id);
				numUnitsInside++;
			} else if(!astronaut.location().mapLocation().isAdjacentTo(targetLoc) && gc.isMoveReady(id)) {
				//Move towards the target location
				/* TODO: this is not going to work if the tiles around the
				 * 		rocket are marked as dangerous (without a fix) */
				if(targetLoc == null) {
					Utils.log("This is the saddest time because we are about to throw an exception");
				}
				Direction movedir = nav.dirToMove(astronaut.location().mapLocation(),targetLoc);
				if (movedir != Direction.Center) {
					infoMan.moveAndUpdate(id, movedir, astronaut.unitType());
				}
			}
		}

		Utils.log("rocketsquad reporting size = " + units.size() + " urgency = " + urgency + " numUnitsInside = " + numUnitsInside);
		
		if (strat.shouldLaunch(rocket,numUnitsInside)){
			Utils.log("trying to launch rocket!");
			MapLocation dest = nav.getNextMarsDest();
			if(dest != null && gc.canLaunchRocket(rocket.id(), dest)){
				gc.launchRocket(rocket.id(), dest);
				isInSpace = true;
			}
		}
	}
}
