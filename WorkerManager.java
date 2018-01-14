import bc.*;
import java.util.ArrayList;
/*
rearrange worker squads (includes assigning to rocketsquads and
    empty squads for specific factories),
Re-allocate workers to new karbonite patches if mined out,
Send idle workers to gather karbonite
*/
public class WorkerManager{
    InfoManager infoMan;
    GameController gc;

    public WorkerManager(InfoManager im, GameController g){
        infoMan = im;
        gc = g;
    }

    public void update(Strategy strat){
    	// create new squads if necessary
    	if(infoMan.workerSquads.size()==0) {
    		WorkerSquad ws = new WorkerSquad(gc);
    		ws.objective = Objective.BUILD;
    		infoMan.workerSquads.add(ws);
    	}
    	// assign unassigned workers
    	for(Unit u : infoMan.unassignedUnits)
    		if(u.unitType() == UnitType.Worker) {
    			infoMan.workerSquads.get(0).units.add(u.id());
    			infoMan.workerSquads.get(0).update();
    		}
    	
    	//infoMan.workerSquads.get(0).targetLoc = Utils.averageMapLocationEarth(gc,infoMan.workerSquads.get(0).units);
    	//TODO:assign workers who are just mining karbonite if there's something better to do, add to rocket squads if necessary
    }
}