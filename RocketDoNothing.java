import bc.*;
import java.util.ArrayList;

/*
responsible for unloading rockets which are on Mars
TODO: don't destroy our rockets once empty
We can pass through them by loading and unloading units. perhaps have something to store unit directions in which to unload since unloading happens FIFO
Each unit if they want to pass through a rocket, they will load, add direction to queue of directions on rocketDoNothing, and then will be unloaded next turn.
both loading and unloading are taken to be a move action. 
This should be done in this file since its pointless to create a rocketsquad just for 1 unit at a time. 
basically controls all rockets :)
Also don't unload in a random direction, do it in the directions toward ur target!
*/
public class RocketDoNothing extends RocketManager{

    public RocketDoNothing(GameController g, InfoManager im){
        super(g, im);
    }

    public void update(Strategy strat){
    	for(Unit r: infoMan.rockets) {
    		boolean didSomething;
    		while(r.structureGarrison().size() > 0) { //could probably get rid of this for an if statements since below for loop checks all directions anyway?
    			didSomething = false;
    			for(Direction dirToUnload : Utils.orderedDirections){
    				if(gc.canUnload(r.id(), dirToUnload)) {
    					gc.unload(r.id(), dirToUnload);
    					didSomething = true;
    					MapLocation locUnloaded = r.location().mapLocation().add(dirToUnload);
    					//it's occupied now!
    					infoMan.tiles[locUnloaded.getX()][locUnloaded.getY()].unitID = 0;
    				}
                }
    			if(!didSomething)
    				break;
    		}
    	}
    }
}
