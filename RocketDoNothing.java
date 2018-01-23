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
    	Utils.log("sup fam");
    	for(Unit r: infoMan.rockets) {
    		boolean didSomething;
    		while(r.structureGarrison().size() > 0) {
    			didSomething = false;
    			for(Direction dirToUnload : Utils.orderedDirections)
    				if(gc.canUnload(r.id(), dirToUnload)) {
    					gc.unload(r.id(), dirToUnload);
    					didSomething = true;
    				}
    			if(!didSomething)
    				break;
    		}
    		if(r.structureGarrison().size() == 0) {
    			gc.disintegrateUnit(r.id());
    		}
    	}
    }
}