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
    		if(gc.unit(s.units.get(0)).structureGarrison().size() == 0) {
    			gc.disintegrateUnit(s.units.get(0));
    		}
    	}
    }
}