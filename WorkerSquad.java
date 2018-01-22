import bc.*;
import java.util.ArrayList;

public class WorkerSquad extends Squad {

	UnitType toBuild;
	InfoManager infoMan;
	MapLocation targetKarboniteLoc = null;
	public WorkerSquad(GameController g, InfoManager im) {
		super(g);
		infoMan = im;
		toBuild = UnitType.Factory;
	}

	public void update() {
		if(requestedUnits.isEmpty())
			requestedUnits.add(UnitType.Worker);
		urgency = 64-8*units.size();
	}
	public void moveTowardsKarbonite(int id, Nav nav) {
		//System.out.println("trying really hard to move towards karbonite");
		if(!gc.isMoveReady(id))
			return;
		//long start = System.nanoTime();
		MapLocation myLoc = gc.unit(id).location().mapLocation();
		int maxDist = 2;
		
		if(targetKarboniteLoc != null && infoMan.tiles[targetKarboniteLoc.getX()][targetKarboniteLoc.getY()].karbonite == 0)
			targetKarboniteLoc = null;
		while(targetKarboniteLoc == null && maxDist < 257) {
			VecMapLocation m = gc.allLocationsWithin(myLoc, maxDist);
			maxDist = maxDist*2;
			for(int i = 0; i < m.size(); i++) {
				int x = m.get(i).getX();
				int y= m.get(i).getY();
				if(infoMan.tiles[x][y].karbonite > 0 && infoMan.isReachable(myLoc, m.get(i))) {
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
		}
		//long end = System.nanoTime();
		//Utils.log("aaron just wasted " + (end-start) + " ns.");
	}
	public void tryToMine(int id) {
		if(gc.canHarvest(id,Direction.Center)) {
			gc.harvest(id, Direction.Center);
			return;
		}
		if(gc.canHarvest(id,Direction.North)) {
			gc.harvest(id, Direction.North);
			return;
		}
		if(gc.canHarvest(id,Direction.South)) {
			gc.harvest(id, Direction.South);
			return;
		}
		if(gc.canHarvest(id,Direction.East)) {
			gc.harvest(id, Direction.East);
			return;
		}
		if(gc.canHarvest(id,Direction.West)) {
			gc.harvest(id, Direction.West);
			return;
		}
		if(gc.canHarvest(id,Direction.Northwest)) {
			gc.harvest(id, Direction.Northwest);
			return;
		}
		if(gc.canHarvest(id,Direction.Northeast)) {
			gc.harvest(id, Direction.Northeast);
			return;
		}
		if(gc.canHarvest(id,Direction.Southwest)) {
			gc.harvest(id, Direction.Southwest);
			return;
		}
		if(gc.canHarvest(id,Direction.Southeast)) {
			gc.harvest(id, Direction.Southeast);
			return;
		}
	}
	public void replicateWorker(int id) {
		if(targetLoc != null) {
			for(Direction dirToReplicate : Utils.directionsTowardButNotIncluding(gc.unit(id).location().mapLocation().directionTo(targetLoc))) {
				if (gc.canReplicate(id, dirToReplicate)) {
					gc.replicate(id, dirToReplicate);
					break;
				}
			}
		}
		else {
			for(Direction dirToReplicate : Utils.orderedDirections) {
				if (gc.canReplicate(id, dirToReplicate)) {
					gc.replicate(id, dirToReplicate);
					break;
				}
			}
		}
	}
	public void move(Nav nav) {
		for(int id: units) {
			Unit worker = gc.unit(id);
			if(worker.location().isInSpace() || worker.location().isInGarrison())
				continue;
			//For now we shall replicate once at the start.
			if(gc.round() == 1) {
				replicateWorker(id);
				
			}
			switch (objective) {
			case BUILD:
				if(targetLoc != null) {
					//System.out.println("Trying to build something useful at: " + targetLoc.getX() + ", " + targetLoc.getY());
					if(!worker.location().mapLocation().isAdjacentTo(targetLoc) && gc.isMoveReady(id)) {
						//Move towards the target location
						Direction movedir = nav.dirToMoveSafely(worker.location().mapLocation(),targetLoc);
						if (movedir != Direction.Center) {
							gc.moveRobot(id, movedir);
							worker = gc.unit(id);
						}
						else {//We are on top of the targetLoc, move away
							for(Direction dirToMove : Utils.orderedDirections) {
								if (gc.canMove(id, dirToMove)) {
									gc.moveRobot(id, dirToMove);
									worker = gc.unit(id);
								}
								break;
							}
						}
					}
					//Last resort we're stuck and need to build
					if(worker.location().mapLocation()  == targetLoc) {
						gc.disintegrateUnit(id);
					}
					
					if(worker.location().mapLocation().isAdjacentTo(targetLoc)) {
						if(gc.hasUnitAtLocation(targetLoc) && gc.senseUnitAtLocation(targetLoc).unitType() == toBuild && gc.senseUnitAtLocation(targetLoc).structureIsBuilt() != 0) {
							objective = Objective.NONE;
							break; 
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
							break; 
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
				tryToMine(id);
				moveTowardsKarbonite(id,nav);
				tryToMine(id);
				break;
			case BOARD_ROCKET:
				break;
			default:
				break;
			}
		}
	}
}