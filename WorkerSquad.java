/****************/
/* REFACTOR ME! */
/****************/

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
public class WorkerSquad extends Squad {

	UnitType toBuild;
	MapLocation targetKarboniteLoc = null;

	public WorkerSquad(GameController g, InfoManager im) {
		super(im);
		//REFACTOR: remove the auto toBuild?
		toBuild = UnitType.Factory;
	}

	final int[] dx = {-1,-1,-1,0,0,0,1,1,1};
	final int[] dy = {-1,0,1,-1,0,1,-1,0,1};

	public void update() {
		//TODO: think more about this
		if(requestedUnits.isEmpty())
			requestedUnits.add(UnitType.Worker);
		urgency = 8-units.size();
	}
	
	public void moveTowardsBuildLoc(int id, Unit worker, Nav nav) {
		if(!worker.location().mapLocation().isAdjacentTo(targetLoc) && gc.isMoveReady(id)) {
			//Move towards the target location
			Direction movedir = nav.dirToMoveSafely(worker.location().mapLocation(),targetLoc);
			if (movedir != Direction.Center) {
				gc.moveRobot(id, movedir);
				worker = gc.unit(id);
			}
			else if(worker.location().mapLocation()  == targetLoc) {//We are on top of the targetLoc, move away
				for(Direction dirToMove : Utils.orderedDirections) {
					if (gc.canMove(id, dirToMove)) {
						gc.moveRobot(id, dirToMove);
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
		//REFACTOR: remove all/most of the gc calls
		if(gc.hasUnitAtLocation(targetLoc) && gc.senseUnitAtLocation(targetLoc).unitType() == toBuild && gc.senseUnitAtLocation(targetLoc).structureIsBuilt() != 0) {
			objective = Objective.NONE;
			return; 
		}
		//We're here! Lets make a blueprint/work on building it up.
		if(worker.location().mapLocation().isAdjacentTo(targetLoc)) {
			if(gc.hasUnitAtLocation(targetLoc) && gc.senseUnitAtLocation(targetLoc).unitType() == toBuild) {
				Unit blueprint = gc.senseUnitAtLocation(targetLoc);
				if (gc.canBuild(id, blueprint.id())) {
					gc.build(id, blueprint.id());
				}
			}
		}
		if(gc.hasUnitAtLocation(targetLoc) && gc.senseUnitAtLocation(targetLoc).unitType() == toBuild && gc.senseUnitAtLocation(targetLoc).structureIsBuilt() != 0) {
			objective = Objective.NONE;
			return; 
		}
		if(!(gc.hasUnitAtLocation(targetLoc))) {
			Direction dirToBuild = worker.location().mapLocation().directionTo(targetLoc);
			if (gc.karbonite() > bc.bcUnitTypeBlueprintCost(toBuild)
					&& gc.canBlueprint(id, toBuild, dirToBuild)) {
				gc.blueprint(worker.id(), toBuild, dirToBuild);
			}
		}
		
		//System.out.println(gc.senseUnitAtLocation(targetLoc).unitType());
		if(gc.hasUnitAtLocation(targetLoc) && gc.senseUnitAtLocation(targetLoc).unitType() == toBuild && gc.senseUnitAtLocation(targetLoc).structureIsBuilt() != 0) {
			objective = Objective.NONE;
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
	public boolean karboniteNearLoc(MapLocation loc) {
		for(int i = 0; i < 9; i++) {
			if(infoMan.tiles[safeX(loc.getX()+dx[i])][safeY(loc.getY()+dy[i])].karbonite > 0) {
				return true;
			}
		}
		return false;
	}
	public void moveTowardsKarbonite(int id, Nav nav) {
		//System.out.println("trying really hard to move towards karbonite");
		if(!gc.isMoveReady(id))
			return;
		//long start = System.nanoTime();
		for(Direction d : Utils.orderedDirections) {
			if(gc.canMove(id, d)) {
				if(karboniteNearLoc(gc.unit(id).location().mapLocation().add(d))) {
					gc.moveRobot(id, d);
					return;
				}
			}
		}
		MapLocation myLoc = gc.unit(id).location().mapLocation();
		int maxDist = 2;
		
		//REFACTOR: have infoMan keep track of karbonite regions and have a method to get the nearest one. just call that and go there.
		
		if(targetKarboniteLoc != null && infoMan.tiles[targetKarboniteLoc.getX()][targetKarboniteLoc.getY()].karbonite == 0)
			targetKarboniteLoc = null;
		while(targetKarboniteLoc == null && maxDist < 257) {
			VecMapLocation m = gc.allLocationsWithin(myLoc, maxDist);
			maxDist = maxDist*2;
			for(int i = 0; i < m.size(); i++) {
				int x = m.get(i).getX();
				int y= m.get(i).getY();
				if(infoMan.tiles[x][y].karbonite > 0 && infoMan.isReachable(myLoc, m.get(i)) && infoMan.distToHostile(m.get(i)) > 200) {
					targetKarboniteLoc = m.get(i);
					break;
				}
			}
		}
		if(targetKarboniteLoc != null) {
			//System.out.println("trying sososoosososososososo hard to move towards karbonite");
			Direction toMove = nav.dirToMoveSafely(myLoc, targetKarboniteLoc);
			if(gc.canMove(id, toMove)) {
				//System.out.println("IM MOVING ROAR");
				gc.moveRobot(id, toMove);
			}
		}else {
			//For now probably nothing better todo :(
			//gc.disintegrateUnit(id);
			Direction dirToMove = Utils.orderedDirections[(int) (8*Math.random())];
			if(gc.canMove(id, dirToMove))
				gc.moveRobot(id, dirToMove);
		}
		//long end = System.nanoTime();
		//Utils.log("aaron just wasted " + (end-start) + " ns.");
	}
	public boolean tryToMine(int id) {
		//REFACTOR: Use tile checks instead of gc.canHarvest
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
			//REFACTOR: only replicate if you are told to by your manager
			if((infoMan.workerCount < strat.maxWorkers && infoMan.myPlanet == Planet.Earth) || (infoMan.myPlanet == Planet.Mars && (gc.round() > 700 || gc.karbonite() > 200))) {
				replicateWorker(id);
			}
			switch (objective) {
			case BUILD:
				if(targetLoc != null) {
					moveTowardsBuildLoc(id, worker, nav);
					//System.out.println("Trying to build something useful at: " + targetLoc.getX() + ", " + targetLoc.getY());
					
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
							break;
						}
					}
				}
				tryToMine(id);
				break;
			case MINE:
				if(!tryToMine(id))
					moveTowardsKarbonite(id,nav);
				tryToMine(id);
				break;
			case BOARD_ROCKET:
				break;
			default:
				break;
			}
		}

		infoMan.logTimeCheckpoint("workers moved");
	}
}
