import bc.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/*
controlled by WorkerManager
basically just carry out assigned objective (build thing or idly mine)
sets objective to NONE when done (to be reassigned by manager)
*/

public class WorkerSquad extends Squad {

	UnitType toBuild;
	Strategy strat;
	private boolean blueprinted;
	HashMap<Integer, MapLocation> targetKarbLocs;
	
	public WorkerSquad(InfoManager im, Strategy s) {
		super(im);
		strat = s;
		blueprinted = false;
		targetKarbLocs = new HashMap<Integer,MapLocation>();
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
			else if(Utils.equalsMapLocation(worker.location().mapLocation(),targetLoc)) {//We are on top of the targetLoc, move away
				Utils.log("Trying to move away from build location");
				for(Direction dirToMove : Utils.orderedDirections) {
					if (gc.canMove(id, dirToMove)) {
						infoMan.moveAndUpdate(id, dirToMove, UnitType.Worker);
						worker = gc.unit(id);
						break;
					}
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
		if(t.unitID == -1 && blueprinted) {
			//oh shit someone killed our building better get away
			objective = Objective.NONE;
			infoMan.factoriesToBeBuilt--;
			return;
		}
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
			    	break;
			    case Rocket: infoMan.rocketsToBeBuilt--;
			    	break;
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
		MapLocation karbLoc = myLoc;
		if(targetKarbLocs.containsKey(id)){
			MapLocation targetKarbLoc = targetKarbLocs.get(id);
			if(targetKarbLoc != null && infoMan.tiles[targetKarbLoc.getX()][targetKarbLoc.getY()].karbonite == 0){
				Utils.log("trying to find new stuff!");
				karbLoc = infoMan.getClosestKarbonite(myLoc);
			}
			else
				karbLoc = targetKarbLoc;
		}
		else{
			karbLoc = infoMan.getClosestKarbonite(myLoc);
		}

		if(karbLoc == null){
			Utils.log("running away :(");
			runAway(id,myLoc);
			return;
		}
		
		targetKarbLocs.put(id, karbLoc);
		Direction d = nav.dirToMoveSafely(myLoc, karbLoc);
		Utils.log("just a worker trying to move to " + karbLoc);
		infoMan.moveAndUpdate(id, d, UnitType.Worker);
	
	}
	
	public void runAway(int id, MapLocation loc){
		double bestScore = -10000000;
		int bestInd = 8;
		int x = loc.getX();
		int y = loc.getY();
		int nx,ny;
		for(int i=0; i < 8; i++){
			nx = x + Utils.dx[i];
			ny = y + Utils.dy[i];
			if(!infoMan.isOnMap(nx,ny))
				continue;
			Tile t = infoMan.tiles[nx][ny];
			if(!t.isWalkable || !infoMan.isLocationClear(nx, ny))
				continue;
			t.updateEnemies(gc);
			double score = -100.0 * t.possibleDamage + t.distFromNearestHostile;
			if(score>bestScore){
				bestScore = score;
				bestInd = i;
			}
		}
		infoMan.moveAndUpdate(id, Utils.indexToDirection(bestInd), UnitType.Worker);
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
			Direction tempDirection = gc.unit(id).location().mapLocation().directionTo(targetLoc);
			if(tempDirection == Direction.Center) {
				tempDirection = Direction.North;
			}
			for(Direction dirToReplicate : Utils.directionsToward(tempDirection)) {
				//Utils.log("My direction to replicate is " + dirToReplicate + "," + targetLoc);
				if (gc.canReplicate(id, dirToReplicate)) {
					gc.replicate(id, dirToReplicate);
					MapLocation rloc = gc.unit(id).location().mapLocation().add(dirToReplicate);
					Unit repped = gc.senseUnitAtLocation(rloc);
				    infoMan.tiles[rloc.getX()][rloc.getY()].unitID = repped.id();
				    infoMan.tiles[rloc.getX()][rloc.getY()].myType = UnitType.Worker;
					infoMan.workerCount++;
					infoMan.workersToRep.remove(id);
					break;
				}
			}
		}
		else {
			for(Direction dirToReplicate : Utils.orderedDirections) {
				if (gc.canReplicate(id, dirToReplicate)) {
					gc.replicate(id, dirToReplicate);
					MapLocation rloc = gc.unit(id).location().mapLocation().add(dirToReplicate);
					Unit repped = gc.senseUnitAtLocation(rloc);
				    infoMan.tiles[rloc.getX()][rloc.getY()].unitID = repped.id();
				    infoMan.tiles[rloc.getX()][rloc.getY()].myType = UnitType.Worker;
					infoMan.workerCount++;
					infoMan.workersToRep.remove(id);
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
			if(worker.abilityHeat() < 10 && infoMan.workersToRep.contains(id)){
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
							    break;
							    case Rocket: infoMan.rocketsToBeBuilt--;
							    break;
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
				if(!tryToMine(id)) {
					moveTowardsKarbonite(id,nav);
					tryToMine(id);
				}else {
					moveTowardsKarbonite(id,nav);
				}
				break;
			default:
				break;
			}
		}

		infoMan.logTimeCheckpoint("workers moved");
	}
}
