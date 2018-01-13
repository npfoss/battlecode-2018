import bc.*;
import java.util.ArrayList;

/*
Production Manager (based on StrategyManager and squad requests)
BuildOrder Search - possibly change build order based on what happens in game, decide on new build order from strategy manager
BuildOrder Queue - decides what should be built based on strategy and where to build them
Creates (empty) WorkerSquad to build factory (workers assigned by WorkerManager)
*/
public class ProductionManager{
    InfoManager infoMan;
    GameController gc;
    public ProductionManager(InfoManager im, GameController g){
        infoMan = im;
        gc = g;
    }

    public void update(Strategy strat){
        // find squads with highest urgency and find factories to build them. etc junk
    	// stuff

    }

    public void move(){
    	for(Unit factory : infoMan.factories) {
    		int id = factory.id();
    		//TODO: pick an intelligent direction
    		if(gc.unit(id).structureGarrison().size() >0) {
    			for(Direction dirToUnload : Utils.orderedDirections)
    				if(gc.canUnload(factory.id(), dirToUnload)) {
    					gc.unload(id, dirToUnload);
    				}
    		}
    		if(gc.canProduceRobot(id,UnitType.Ranger)) {
    			gc.produceRobot(id, UnitType.Ranger);
    		}
    	}
        // go through the factories (in infoMan) and make them produce stuff

    }
}