import bc.*;
import java.util.ArrayList;

public class RocketSquad extends Squad{

	int rocketID;
	boolean isInSpace = false;
	int launchRound;

	// NOTE: rocket is always unit at index 0 (maintained by manager)

	public RocketSquad(GameController g, MapLocation rocketLoc, int lr){
		super(g);
		objective = Objective.BOARD_ROCKET;
		targetLoc = rocketLoc;
		launchRound = lr;
	}
	public void update(){
		
	}

	public void move(Nav nav){
		if(isInSpace)
			return;
		for(int id : units) {
			Unit astronaut = gc.unit(id);
			if(astronaut.location().isInGarrison())
				continue;
			if(gc.canLoad(rocketID, id))
				gc.load(rocketID, id);
			else if(!astronaut.location().mapLocation().isAdjacentTo(targetLoc) && gc.isMoveReady(id)) {
				//Move towards the target location
				Direction movedir = nav.dirToMoveSafely(astronaut.location().mapLocation(),targetLoc);
				if (movedir != Direction.Center) {
					gc.moveRobot(id, movedir);
				}
			}
		}
	}
}