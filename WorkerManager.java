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

	MapLocation startingFactory1;
	MapLocation startingFactory2;
	MapLocation startingFactory3;

	public WorkerManager(InfoManager im, GameController g){
		infoMan = im;
		gc = g;
		startingFactory1 = null;
		startingFactory2 = null;
		startingFactory3 = null;
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
									//todo add this once Nate writes isReachable
									//if gc.unit(ws.units.get(0)).location().mapLocation() is reachable to a.location().mapLocation()
									ws.requestedUnits.remove(ws.requestedUnits.indexOf(u));
									ws.units.add(infoMan.unassignedUnits.get(infoMan.unassignedUnits.indexOf(a)).id());
									infoMan.unassignedUnits.remove(infoMan.unassignedUnits.indexOf(a));
									ws.update();

									/*else
									{
									 WorkerSquad wsn = new WorkerSquad()
									 wsn.objective = Objective.BUILD;
									 wsn.units.add(infoMan.unassignedUnits.get(infoMan.unassignedUnits.indexOf(a)).id());
									 wsn.update();
									}
									 */
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
				if(gc.round() == 1) {
					//Pick a place to build the first factory
					if(infoMan.workerSquads.get(0).units.size() > 1) {
						startingFactory1 = Utils.averageMapLocation(gc,infoMan.workerSquads.get(0).units);
						if(gc.startingMap(Planet.Earth).isPassableTerrainAt(startingFactory1)>0){
							infoMan.workerSquads.get(0).targetLoc = startingFactory1;
						}
						else {
							//This means it should build in any possible direction
							infoMan.workerSquads.get(0).targetLoc = null;
						}
					}

					if(infoMan.workerSquads.size() > 1) {
						//Pick a place to build a second factory
						if(infoMan.workerSquads.get(0).units.size() > 1) {
							startingFactory2 = Utils.averageMapLocation(gc,infoMan.workerSquads.get(0).units);
							if(gc.startingMap(Planet.Earth).isPassableTerrainAt(startingFactory1)>0){
								infoMan.workerSquads.get(0).targetLoc = startingFactory1;
							}
							else {
								//This means it should build in any possible direction
								infoMan.workerSquads.get(0).targetLoc = null;
							}
						}
					}
					if(infoMan.workerSquads.size() > 1) {
						//Pick a place to build a third factory
						//This means it should build in any possible direction
						infoMan.workerSquads.get(0).targetLoc = null;
					}
				}


				//TODO intelligently pick locations for the next factories
				System.out.println("My objective is: " + ((infoMan.workerSquads.get(0).objective == Objective.BUILD) ? "Building" : "NONE"));
				System.out.println(infoMan.factories.size());

				if(infoMan.factories.size() < 3) {
					for(WorkerSquad ws : infoMan.workerSquads) {
						if(ws.objective == Objective.NONE) {
							for(Direction dirToNextFactory : Utils.orderedDiagonals){
								MapLocation possibleNext = ws.targetLoc.addMultiple(dirToNextFactory, 2);
								System.out.println("Trying to pick a direction!");
								if(gc.startingMap(Planet.Earth).onMap(possibleNext) && gc.startingMap(Planet.Earth).isPassableTerrainAt(possibleNext)>0) {
									if(!(gc.canSenseLocation(possibleNext) && gc.hasUnitAtLocation(possibleNext) && gc.senseUnitAtLocation(possibleNext).unitType() == UnitType.Factory)){
									ws.targetLoc = possibleNext;
									ws.objective = Objective.BUILD;
									System.out.println("Setting");
									break;
									}
								}
							}
						}
					}
				}
				for(WorkerSquad ws : infoMan.workerSquads) {
					if(ws.objective == Objective.NONE) {
						ws.objective = Objective.MINE;
						break;


					}


				}
				System.out.flush();
			}
			break;
		default:
			break;
		}
		//TODO:assign workers who are just mining karbonite if there's something better to do, add to rocket squads if necessary
	}
}