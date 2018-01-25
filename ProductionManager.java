import bc.*;
import java.util.ArrayList;

/*
Production Manager (based on StrategyManager and squad requests)

takes into account squad requests to control factory production.
does NOT handle worker replication, WorkerManager does that
*/
public class ProductionManager{
    InfoManager infoMan;
    GameController gc;

    public ProductionManager(){}
    
    public ProductionManager(InfoManager im, GameController g){
        infoMan = im;
        gc = g;
    }

    public void update(Strategy strat){
    	//REFACTOR: actually do the calcs here and find the n highest urgency squads for n factories, assign units to factories
        // find squads with highest urgency and find factories to build them. etc junk
    	// TODO: stuff
    }

    public void move(){
        // go through the factories (in infoMan) and make them produce stuff
    	for(Unit factory : infoMan.factories) {
    		int id = factory.id();
    		//TODO: pick an intelligent direction
    		boolean didSomething = false;
    		while(gc.unit(id).structureGarrison().size() > 0) {
    			didSomething = false;
    			for(Direction dirToUnload : Utils.orderedDirections)
    				if(gc.canUnload(factory.id(), dirToUnload)) {
    					gc.unload(id, dirToUnload);
    					didSomething = true;
    				}
    			if(!didSomething)
    				break;
    		}
    		if(!infoMan.builtRocket)
    			continue;
    		infoMan.combatSquads.sort(Squad.byUrgency());
    		infoMan.rocketSquads.sort(Squad.byUrgency());
    		infoMan.workerSquads.sort(Squad.byUrgency());
    		Squad toFill = null;
    		if(infoMan.combatSquads.size() > 0 && infoMan.fighters.size() < MagicNumbers.MAX_FIGHTER_COUNT)
    			toFill = infoMan.combatSquads.get(0);
    		if(infoMan.rocketSquads.size() > 0 && (toFill == null || infoMan.rocketSquads.get(0).urgency > toFill.urgency))
    			toFill = infoMan.rocketSquads.get(0);
    		//if(infoMan.workerSquads.size()>0 && (toFill == null || infoMan.workerSquads.get(0).urgency > toFill.urgency))
    			//toFill = infoMan.workerSquads.get(0);
    		UnitType toMake = null;
    		if(toFill != null && toFill.requestedUnits.size() > 0){
    			toMake = toFill.requestedUnits.get(0);
    			//REFACTOR: make 3 a magic number
    			if(infoMan.workers.size() < 3)
    				toMake = UnitType.Worker;
    		}
    		//REFACTOR: make 650 a magic number
    		if(toMake != null && gc.canProduceRobot(id,toMake) && gc.round() < 650) {
    			gc.produceRobot(id, toMake);
    		}
    	}
    }
}
