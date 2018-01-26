import bc.*;
import java.util.ArrayList;

/*
controlled by WorkerManager
basically just carry out assigned objective (build thing or idly mine)
sets objective to NONE when done (to be reassigned by manager)
*/

/* Overall refactor thoughts: general logic is good but helper functions need to be optimized
 * less gc calls, cooperate with infoMan, tile, etc.
 */

//TODO: persist karbonite targets by worker between rounds

public class WorkerSquad extends Squad {

	UnitType toBuild;
	MapLocation targetKarboniteLoc = null;
	Strategy strat;
	private boolean blueprinted;

	public WorkerSquad(InfoManager im, Strategy s) {
		super(im);
		strat = s;
		blueprinted = false;
	}
	
	public void update() {
		if(requestedUnits.isEmpty())
			requestedUnits.add(UnitType.Worker);
		urgency = strat.calcWorkerUrgency(units.size(),objective,toBuild);
	}
	
	public void moveTowardsBuildLoc(int id, Unit worker, Nav nav) {
		if(!worker.location().mapLocation().isAdjacentTo(targetLoc) && gc.isMoveReady(id)) {
			//Move towards the target location
			Direction movedir = nav.dirToMoveSafely(worker.location().mapLocation(),targetLoc);
			if (movedir != Direction.Center) {
				infoMan.moveAndUpdate(id, movedir, UnitType.Worker);
				worker = gc.unit(id);
			}
			else if(worker.location().mapLocation()  == targetLoc) {//We are on top of the targetLoc, move away
				for(Direction dirToMove : Utils.orderedDirections) {
					if (gc.canMove(id, dirToMove)) {
						infoMan.moveAndUpdate(id, dirToMove, UnitType.Worker);
						worker = gc.unit(id);
					}
					break;
				}
			}
			//Last resort we're stuck and need to build
			if(worker.location().mapLocation()  == targetLoc) {
				gc.disintegrateUnit(id);
			}
		}
		
	}
	
	public void tryToBuild (int id, Unit worker) {
		int x = targetLoc.getX();
		int y = targetLoc.getY();
		Tile t = infoMan.tiles[x][y];
		if(t.unitID != -1 && t.myType == toBuild && gc.unit(t.unitID).structureIsBuilt() != 0) {
			objective = Objective.NONE;
			return; 
		}
		//We're here! Lets make a blueprint/work on building it up.
		if(worker.location().mapLocation().isAdjacentTo(targetLoc)) {
			if(t.unitID != -1 && t.myType == toBuild) {
				if (gc.canBuild(id, t.unitID)) {
					gc.build(id, t.unitID);
				}
			}
		}
		if(t.unitID != -1 && t.myType == toBuild &&  gc.unit(t.unitID).structureIsBuilt() != 0) {
			objective = Objective.NONE;
			return; 
		}
		if(t.unitID == -1) {
			Direction dirToBuild = worker.location().mapLocation().directionTo(targetLoc);
			if (gc.karbonite() > bc.bcUnitTypeBlueprintCost(toBuild)
					&& gc.canBlueprint(id, toBuild, dirToBuild)) {
				gc.blueprint(worker.id(), toBuild, dirToBuild);
			    Unit blueprint = gc.senseUnitAtLocation(targetLoc);
			    t.unitID = blueprint.id();
			    t.myType = toBuild;
			    blueprinted = true;
			    switch(toBuild){
			    case Factory: infoMan.factoriesToBeBuilt--;
			    case Rocket: infoMan.rocketsToBeBuilt--;
			    }
			}
		}
	}

	public int safeX(int x) {
		if(x < 0)
			return 0;
		if(x > infoMan.width-1)
			return infoMan.width-1;
		return x;
	}

	public int safeY(int y) {
		if(y < 0)
			return 0;
		if(y > infoMan.height-1)
			return infoMan.height-1;
		return y;
	}
	
	public void moveTowardsKarbonite(int id, Nav nav) {
		if(!gc.isMoveReady(id))
			return;
		MapLocation myLoc = gc.unit(id).location().mapLocation();
		MapLocation karbLoc = infoMan.getClosestKarbonite(myLoc);
		Direction d = nav.dirToMoveSafely(myLoc, karbLoc);
		infoMan.moveAndUpdate(id, d, UnitType.Worker);
	}
	
	public boolean tryToMine(int id) {
		if(gc.canHarvest(id,Direction.Center)) {
			gc.harvest(id, Direction.Center);
			return true;
		}
		if(gc.canHarvest(id,Direction.North)) {
			gc.harvest(id, Direction.North);
			return true;
		}
		if(gc.canHarvest(id,Direction.South)) {
			gc.harvest(id, Direction.South);
			return true;
		}
		if(gc.canHarvest(id,Direction.East)) {
			gc.harvest(id, Direction.East);
			return true;
		}
		if(gc.canHarvest(id,Direction.West)) {
			gc.harvest(id, Direction.West);
			return true;
		}
		if(gc.canHarvest(id,Direction.Northwest)) {
			gc.harvest(id, Direction.Northwest);
			return true;
		}
		if(gc.canHarvest(id,Direction.Northeast)) {
			gc.harvest(id, Direction.Northeast);
			return true;
		}
		if(gc.canHarvest(id,Direction.Southwest)) {
			gc.harvest(id, Direction.Southwest);
			return true;
		}
		if(gc.canHarvest(id,Direction.Southeast)) {
			gc.harvest(id, Direction.Southeast);
			return true;
		}
		return false;
	}
	
	public void replicateWorker(int id) {
		if(targetLoc != null) {
			for(Direction dirToReplicate : Utils.directionsTowardButNotIncluding(gc.unit(id).location().mapLocation().directionTo(targetLoc))) {
				if (gc.canReplicate(id, dirToReplicate)) {
					gc.replicate(id, dirToReplicate);
					infoMan.workerCount++;
					break;
				}
			}
		}
		else {
			for(Direction dirToReplicate : Utils.orderedDirections) {
				if (gc.canReplicate(id, dirToReplicate)) {
					gc.replicate(id, dirToReplicate);
					infoMan.workerCount++;
					break;
				}
			}
		}
	}
	public void move(Nav nav, Strategy strat) {
		Utils.log("ws reporting: size = " + units.size() + " toBuild = " + toBuild + " objective = " + objective + " urgency = " + urgency);
		if(targetLoc != null)
			Utils.log("targetLoc = " + targetLoc);
		for(int id: units) {
			Unit worker = gc.unit(id);
			if(worker.location().isInSpace() || worker.location().isInGarrison())
				continue;
			//TODO: replicate based on manager's choice
			if(worker.abilityHeat() < 10 && infoMan.workerCount < strat.minWorkers){
				replicateWorker(id);
			}
			switch (objective) {
			case BUILD:
				if(blueprinted || strat.shouldGoToBuildLoc()){
					if(targetLoc != null) {
						moveTowardsBuildLoc(id, worker, nav);					
						if(worker.location().mapLocation().isAdjacentTo(targetLoc)) {
							tryToBuild(id, worker);
						}
					}
					else {
						//we are told to build without a location??? angrily build in a random direction directions.
						VecUnit vu = gc.senseNearbyUnits(worker.location().mapLocation(), 2);
						for (int i = 0; i < vu.size(); i++) {
							if(vu.get(i).unitType() == toBuild) {
								if (gc.canBuild(id, vu.get(i).id())) {
									gc.build(id, vu.get(i).id());
								}
								if(vu.get(i).structureIsBuilt()!=0) {
									objective = Objective.NONE;
								}
							}
						}
						for(Direction dirToBuild : Utils.orderedDirections) {
							if (gc.karbonite() > bc.bcUnitTypeBlueprintCost(toBuild)
									&& gc.canBlueprint(id, toBuild, dirToBuild)) {
								gc.blueprint(worker.id(), toBuild, dirToBuild);
								MapLocation bloc = worker.location().mapLocation().add(dirToBuild);
								Unit blueprint = gc.senseUnitAtLocation(bloc);
							    infoMan.tiles[bloc.getX()][bloc.getY()].unitID = blueprint.id();
							    infoMan.tiles[bloc.getX()][bloc.getY()].myType = toBuild;
							    blueprinted = true;
							    switch(toBuild){
							    case Factory: infoMan.factoriesToBeBuilt--;
							    case Rocket: infoMan.rocketsToBeBuilt--;
							    }
							}
						}
					}
				}
				else if(!tryToMine(id))
					moveTowardsKarbonite(id,nav);
				tryToMine(id);
				break;
			case MINE:
				if(!tryToMine(id))
					moveTowardsKarbonite(id,nav);
				tryToMine(id);
				break;
			default:
				break;
			}
		}

		infoMan.logTimeCheckpoint("workers moved");
	}
}
