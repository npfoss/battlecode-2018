import bc.*;
import java.util.ArrayList;

/*
moves units to rocket
decides when to launch
launches rocket

NOTE: infoMan decides destination loc

TODO:
-improve launch trigger (take into account orbital stuff, coordinated launch from RocketManager)
*/
public class RocketSquad extends Squad {

	boolean isInSpace = false;
	int countdown;
	// NOTE: rocket is always unit at index 0 (maintained by manager)
	Unit rocket;
	long startRound;

	public RocketSquad(InfoManager infoMan, MapLocation rocketLoc){
		super(infoMan);
		objective = Objective.BOARD_ROCKET;
		targetLoc = rocketLoc;
		countdown = 99999;
		startRound = gc.round();
		urgency = 5; // meaningless and arbitrary
	}

	public void update(){
		update(Strategy.defaultRocketComposition);
	}

	public void update(int[] idealComposition){
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
				if(Utils.robotTypes[ind] != UnitType.Worker)
					requestedUnits.add(Utils.robotTypes[ind]);
			}
		}
		urgency = 60;
		if(requestedUnits.size() == 0)
			urgency = 0;
	}

	public void move(Nav nav){
		if(isInSpace)
			return;
		countdown--;
		
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
				Direction movedir = nav.dirToMoveSafely(astronaut.location().mapLocation(),targetLoc);
				if (movedir != Direction.Center) {
					gc.moveRobot(id, movedir);
				}
			}
		}

		Utils.log("rocketsquad reporting size = " + units.size() + " urgency = " + urgency + " numUnitsInside = " + numUnitsInside + " countdown = " + countdown);
		
		long roundsSinceStart = gc.round() - startRound;
		
		if (((numUnitsInside >= rocket.structureMaxCapacity() || (roundsSinceStart > 50 && numUnitsInside > 0))) && countdown > infoMan.magicNums.ROCKET_COUNTDOWN){
			beginCountdown();
		}

		if (shouldLaunch(numUnitsInside)){
			Utils.log("trying to launch rocket!");
			MapLocation dest = nav.getNextMarsDest();
			if(dest != null && gc.canLaunchRocket(rocket.id(), dest)){
				gc.launchRocket(rocket.id(), dest);
				isInSpace = true;
			}
		}
	}

	public void beginCountdown(){
		countdown = infoMan.magicNums.ROCKET_COUNTDOWN;
		// TODO: also warn nearby tiles of damage
	}

	public boolean shouldLaunch(int numUnitsInside){
		/* TODO: things to consider:
		 	* countdown (done)
		 	* flood (done)
		 	* orbital pattern
		 	* if the rocket is close to death
		 	* surrounding units (friends and foes)
		*/

		return countdown <= 0
				|| gc.round() + 1 == infoMan.magicNums.EARTH_FLOOD_ROUND
				|| rocket.health() * 2 < rocket.maxHealth();
	}
}
