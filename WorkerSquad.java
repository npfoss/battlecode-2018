import bc.*;
import java.util.ArrayList;

public class WorkerSquad extends Squad{
	
    public WorkerSquad(GameController g) {
    	super(g);
    }
	
	public void update(){
		
    }

    public void move(Nav nav){
    	for(int id: units) {
    		Unit worker = gc.unit(id);
    		Location l = worker.location();
    		if(l.isOnMap()) {
                //// this is just here to test Nav
                MapLocation fivefive = l.mapLocation().translate(5-l.mapLocation().getX(), 5-l.mapLocation().getY());
                if(worker.unitType() == UnitType.Worker && gc.isMoveReady(worker.id())){
                    //System.out.println("my loc: " + l.mapLocation().getX() + " " + l.mapLocation().getY());
                    Direction movedir = nav.dirToMove(l.mapLocation(), fivefive);
                    //System.out.println("final move choice " + movedir);
                    if(movedir != Direction.Center)
                        gc.moveRobot(worker.id(), movedir);
                }
                //// end Nav test
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