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
    MagicNumbers magicNums;

    public ProductionManager(){}
    
    public ProductionManager(InfoManager im, GameController g, MagicNumbers mn){
    	magicNums = mn;
        infoMan = im;
        gc = g;
    }

    public void update(Strategy strat){
        // find squads with highest urgency and find factories to build them. etc junk
    	// TODO: stuff
    }

    public void move(){
        // go through the factories (in infoMan) and make them produce stuff
    	for(Unit factory : infoMan.factories) {
    		int id = factory.id();
    		//TODO: pick an intelligent direction
    		boolean didSomething = false;
    		while(gc.unit(id).structureGarrison().size() >0) {
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
    		if(infoMan.combatSquads.size()>0 && infoMan.fighters.size() < magicNums.MAX_FIGHTER_COUNT)
    			toFill = infoMan.combatSquads.get(0);
    		if(infoMan.rocketSquads.size()>0 && (toFill == null || infoMan.rocketSquads.get(0).urgency > toFill.urgency))
    			toFill = infoMan.rocketSquads.get(0);
    		if(infoMan.workerSquads.size()>0 && (toFill == null || infoMan.workerSquads.get(0).urgency > toFill.urgency))
    			toFill = infoMan.workerSquads.get(0);
    		UnitType toMake = UnitType.Ranger;
    		if(toFill != null && toFill.requestedUnits.size() > 0){
    			toMake = toFill.requestedUnits.get(0);
    		}
    		if(gc.canProduceRobot(id,toMake)) {
    			gc.produceRobot(id, toMake);
    		}
    	}
    }
}