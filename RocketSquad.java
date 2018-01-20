import bc.*;
import java.util.ArrayList;

public class RocketSquad extends Squad{

	int rocketID;
	boolean isInSpace = false;

	public RocketSquad(GameController g){
		super(g);
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
					astronaut = gc.unit(id);
				}

			}
		}
	}
}