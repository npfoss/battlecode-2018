import bc.*;
import java.util.ArrayList;

/*
for Mars
*/
public class RocketDoNothing extends RocketManager{

    public RocketDoNothing(GameController g, InfoManager im){
        super(g, im);
    }

    public void update(){
    	for(RocketSquad s : infoMan.rocketSquads) {
    		boolean didSomething;
    		while(gc.unit(s.units.get(0)).structureGarrison().size() > 0) {
    			didSomething = false;
    			for(Direction dirToUnload : Utils.orderedDirections)
    				if(gc.canUnload(s.units.get(0), dirToUnload)) {
    					gc.unload(s.units.get(0), dirToUnload);
    					didSomething = true;
    				}
    			if(!didSomething)
    				break;
    		}
    		if(gc.unit(s.units.get(0)).structureGarrison().size() == 0) {
    			gc.disintegrateUnit(s.units.get(0));
    		}
    	}
    }
}