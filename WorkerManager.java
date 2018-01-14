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
	MapLocation initialHub;

	public WorkerManager(InfoManager im, GameController g){
		infoMan = im;
		gc = g;
		initialHub = null;
	}

	public void update(Strategy strat){
		switch(strat) {
		case RUSH:

			//Earth and Mars should probably do different things
			if(gc.planet() == Planet.Earth) {

				// create new squads if necessary
				if(infoMan.workerSquads.size()==0) {
					WorkerSquad ws = new WorkerSquad(gc);
					ws.objective = Objective.BUILD;
					infoMan.workerSquads.add(ws);
				}

				// assign unassigned workers to build.
				for(Unit u : infoMan.unassignedUnits)
					if(u.unitType() == UnitType.Worker) {
						infoMan.workerSquads.get(0).units.add(u.id());
						infoMan.workerSquads.get(0).update();
					}
				if(initialHub == null) {
					//Pick a place to build the first factory
					//Todo floodfill to see if all workers can reach location
					initialHub = Utils.averageMapLocationEarth(gc,infoMan.workerSquads.get(0).units);
					if(gc.startingMap(Planet.Earth).isPassableTerrainAt(initialHub)>0){
						infoMan.workerSquads.get(0).targetLoc = initialHub;
					}
				}

				//TODO intelligently pick locations for the next factories
				/*System.out.println("My objective is: " + ((infoMan.workerSquads.get(0).objective == Objective.BUILD) ? "Building" : "NONE"));
				System.out.println(infoMan.factories.size());
				if(infoMan.workerSquads.get(0).objective == Objective.NONE && infoMan.factories.size() < 3) {
					for(Direction dirToNextFactory : Utils.orderedDirections) {
						MapLocation possibleNext = initialHub.addMultiple(dirToNextFactory, 3);
						if(gc.startingMap(Planet.Earth).isPassableTerrainAt(possibleNext)>0) {
							infoMan.workerSquads.get(0).targetLoc = initialHub.addMultiple(dirToNextFactory, 3);
							infoMan.workerSquads.get(0).objective = Objective.BUILD;
							infoMan.workerSquads.get(0).madeBlueprint = false;
							initialHub = possibleNext;
							break;
						}
					}
				}*/
			}
			break;
		default:
			break;
		}
		//TODO:assign workers who are just mining karbonite if there's something better to do, add to rocket squads if necessary
	}
}