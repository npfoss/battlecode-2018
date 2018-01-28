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
    }

    public void move(){
        // go through the factories (in infoMan) and make them produce stuff
    	for(Unit factory : infoMan.factories) {
    		int id = factory.id();
    		//TODO: pick an intelligent direction
    		boolean didSomething = false;
    		while(factory.structureGarrison().size() > 0) {
    			didSomething = false;
    			for(Direction dirToUnload : Utils.orderedDirections)
    				if(gc.canUnload(factory.id(), dirToUnload)) {
    					gc.unload(id, dirToUnload);
    					didSomething = true;
    					MapLocation locUnloaded = factory.location().mapLocation().add(dirToUnload);
    					//it's occupied now!
    					infoMan.tiles[locUnloaded.getX()][locUnloaded.getY()].unitID = 0;
    				}
    			if(!didSomething)
    				break;
    		}
    		Utils.log("money to save = " + infoMan.moneyToSave);
    		if(gc.karbonite() < infoMan.moneyToSave + 40)
    			continue;
    		infoMan.combatSquads.sort(Squad.byUrgency());
    		infoMan.rocketSquads.sort(Squad.byUrgency());
    		Squad toFill = null;
    		if(infoMan.combatSquads.size() > 0 && infoMan.fighters.size() < MagicNumbers.MAX_FIGHTER_COUNT)
    			toFill = infoMan.combatSquads.get(0);
    		if(infoMan.rocketSquads.size() > 0 && (toFill == null || infoMan.rocketSquads.get(0).urgency > toFill.urgency))
    			toFill = infoMan.rocketSquads.get(0);
    		UnitType toMake = null;
    		if(toFill != null && toFill.requestedUnits.size() > 0){
    			toMake = toFill.requestedUnits.get(0);
    			if(infoMan.workerCount < 1)
    				toMake = UnitType.Worker;
    		}
    		if(toMake != null && gc.canProduceRobot(id,toMake) && gc.round() < MagicNumbers.ROUND_TO_STOP_PRODUCTION) {
    			gc.produceRobot(id, toMake);
    		}
    	}
    }
}
