import bc.*;
import java.util.ArrayList;

public class WorkerSquad extends Squad {

	UnitType toBuild;
	public WorkerSquad(GameController g) {
		super(g);
		toBuild = UnitType.Factory;
	}

	public void update() {
		if(requestedUnits.isEmpty())
			requestedUnits.add(UnitType.Worker);
		urgency = 64-8*units.size();
	}
	public void move(Nav nav) {
		for(int id: units) {
			Unit worker = gc.unit(id);
			if(worker.location().isInSpace() || worker.location().isInGarrison())
				continue;
			//For now we shall replicate once at the start.
			if(gc.round() == 1) {
				if(targetLoc != null) {
					for(Direction dirToReplicate : Utils.directionsTowardButNotIncluding(worker.location().mapLocation().directionTo(targetLoc))) {
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
			switch (objective) {
			case BUILD:
				if(targetLoc != null) {
					System.out.println("Trying to build something useful");
					if(!worker.location().mapLocation().isAdjacentTo(targetLoc) && gc.isMoveReady(id)) {
						//Move towards the target location
						Direction movedir = nav.dirToMove(worker.location().mapLocation(),targetLoc);
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
				break;
			case MINE:
				gc.disintegrateUnit(id);
				/*
			 if !(nav.isSafe???){
			 	runAway();
			 }
			 else{
			 		if we're blocking the way, move out of the way
			 		if we are at mine loc, mine
			 		if loc is dead, pick new loc
			 		goto mine loc
			 }
				 */
				break;
			case BOARD_ROCKET:
				break;
			default:
				break;
			}
		}
	}
}