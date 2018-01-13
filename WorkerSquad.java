import bc.*;
import java.util.ArrayList;

public class WorkerSquad extends Squad{
	
    public WorkerSquad(GameController g) {
    	super(g);
    }
	
	public void update(){
		
    }

    public void move(Nav nav){
    	for(Unit worker: units) {
    		Location l = worker.location();
    		if(l.isOnMap()) {
    			VecUnit vu = gc.senseNearbyUnits(l.mapLocation(), 2);
    			for(int i=0;i<vu.size();i++) {
    				if(gc.canBuild(worker.id(), vu.get(i).id())) {
    					gc.build(worker.id(), vu.get(i).id());
    				}
    			}
    		}
    		if(gc.karbonite() > bc.bcUnitTypeBlueprintCost(UnitType.Factory) && gc.canBlueprint(worker.id(),UnitType.Factory,Direction.North)) {
    			gc.blueprint(worker.id(), UnitType.Factory, Direction.North);
    		}
    	}
    	

    }
}