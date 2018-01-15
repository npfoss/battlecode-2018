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
					ws.update();
					infoMan.workerSquads.add(ws);
				}

				// assign unassigned workers to build.
				boolean didSomething = false;
				while(infoMan.unassignedUnits.size() > 0) {
					didSomething = false;
					infoMan.workerSquads.sort(Squad.byUrgency());
					boolean tryAgain = false;
					for(WorkerSquad ws : infoMan.workerSquads) {
						for(UnitType u : ws.requestedUnits) {
							for(Unit a : infoMan.unassignedUnits) {
								if(a.unitType() == u) {
									ws.requestedUnits.remove(ws.requestedUnits.indexOf(u));
									ws.units.add(infoMan.unassignedUnits.get(infoMan.unassignedUnits.indexOf(a)).id());
									infoMan.unassignedUnits.remove(infoMan.unassignedUnits.indexOf(a));
									ws.update();
									tryAgain = true;
									didSomething = true;
								}
								if(tryAgain)
									break;
							}
							if(tryAgain)
								break;
						}
						if(tryAgain)
							break;
					}
					if(!didSomething)
						break;
				}
				if(initialHub == null) {
					//Pick a place to build the first factory
					//TODO floodfill to see if all workers can reach location
					initialHub = Utils.averageMapLocationEarth(gc,infoMan.workerSquads.get(0).units);
					if(gc.startingMap(Planet.Earth).isPassableTerrainAt(initialHub)>0){
						infoMan.workerSquads.get(0).targetLoc = initialHub;
					}
				}
				//Temp solution build random factories
				if(infoMan.workerSquads.get(0).objective == Objective.NONE) {
					infoMan.workerSquads.get(0).objective = Objective.BUILD;
							infoMan.workerSquads.get(0).targetLoc = null;
				}
				//TODO intelligently pick locations for the next factories
				/*System.out.println("My objective is: " + ((infoMan.workerSquads.get(0).objective == Objective.BUILD) ? "Building" : "NONE"));
				System.out.println(infoMan.factories.size());
				if(infoMan.workerSquads.get(0).objective == Objective.NONE && infoMan.factories.size() < 3) {
					for(Direction dirToNextFactory : Utils.orderedDirections) {
						MapLocation possibleNext = initialHub.addMultiple(dirToNextFactory, 3);
						if(gc.startingMap(Planet.Earth).onMap(possibleNext)) {
							if(gc.startingMap(Planet.Earth).isPassableTerrainAt(possibleNext)>0) {
								infoMan.workerSquads.get(0).targetLoc = possibleNext;
								infoMan.workerSquads.get(0).objective = Objective.BUILD;
								infoMan.workerSquads.get(0).madeBlueprint = false;
								break;
							}
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