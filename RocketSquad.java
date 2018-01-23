import bc.*;
import java.util.ArrayList;

public class RocketSquad extends Squad {

	boolean isInSpace = false;
	int launchRound;

	int countdown;

	// NOTE: rocket is always unit at index 0 (maintained by manager)

	public RocketSquad(InfoManager infoMan, MapLocation rocketLoc, int lr){
		super(infoMan);
		objective = Objective.BOARD_ROCKET;
		targetLoc = rocketLoc;
		launchRound = lr;
		countdown = 99999;
	}

	public void update(){

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
			} else if(gc.canLoad(units.get(0), id)){
				gc.load(units.get(0), id);
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

		if (numUnitsInside >= gc.unit(units.get(0)).structureMaxCapacity()){
			beginCountdown();
		}

		if (shouldLaunch(numUnitsInside)){
			MapLocation dest = nav.getNextMarsDest();
			if(gc.canLaunchRocket(units.get(0), dest)){
				gc.launchRocket(units.get(0), dest);
			}
		}
	}

	public void beginCountdown(){
		countdown = infoMan.magicNums.ROCKET_COUNTDOWN;
	}

	public boolean shouldLaunch(int numUnitsInside){
		/* TODO: things to consider:
		 	* countdown
		 	* orbital pattern
		 	* if the rocket is close to death
		 	* surrounding units (friends and foes)
		*/

		return countdown <= 0;
	}
}