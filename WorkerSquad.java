import bc.*;
import java.util.ArrayList;

public class WorkerSquad extends Squad {
	Boolean madeBlueprint = false;
	public WorkerSquad(GameController g) {
		super(g);
	}

	public void update() {
	}
	public void move(Nav nav) {
		for(int id : units) {
			Unit worker = gc.unit(id);
			//For now we shall replicate once at the start.
			if(gc.round() == 1)
			for(Direction dirToReplicate : Utils.orderedDirections) {
				if (gc.canReplicate(id, dirToReplicate)) {
					gc.replicate(id, dirToReplicate);
					break;
				}
			}

			switch (objective) {
			case BUILD:
				if(targetLoc != null) {
					if(!worker.location().mapLocation().isAdjacentTo(targetLoc) && gc.isMoveReady(id)) {
						//Move towards the target location
						Direction movedir = nav.dirToMove(worker.location().mapLocation(),targetLoc);
						if (movedir != Direction.Center) {
							gc.moveRobot(id, movedir);
							worker = gc.unit(id);
						}
						else {//We are on top of the targetLoc, move away
							for(Direction dirToMove : Utils.orderedDirections) {
								if (gc.canMove(id, dirToMove))
									gc.moveRobot(id, dirToMove);
								break;
							}
						}
					}
					if(worker.location().mapLocation().isAdjacentTo(targetLoc)) {
						//We're here! Lets make a blueprint/work on building it up.
						if(!madeBlueprint) {
							Direction dirToBuild = worker.location().mapLocation().directionTo(targetLoc);
							if (gc.karbonite() > bc.bcUnitTypeBlueprintCost(toBuild)
									&& gc.canBlueprint(id, toBuild, dirToBuild)) {
								gc.blueprint(worker.id(), toBuild, dirToBuild);
								madeBlueprint = true;
							}
						}
						if(worker.location().mapLocation().isAdjacentTo(targetLoc)) {
							Unit blueprint = gc.senseUnitAtLocation(targetLoc);
							if (gc.canBuild(id, blueprint.id())) {
								gc.build(id, blueprint.id());
							}
						}
						//System.out.println(gc.senseUnitAtLocation(targetLoc).unitType());
						if(gc.senseUnitAtLocation(targetLoc)!= null && gc.senseUnitAtLocation(targetLoc).structureIsBuilt() != 0) {
							objective = Objective.NONE;
						}
					}
				}
				else {
					//we are told to build without a location??? angrily build in all directions.
					VecUnit vu = gc.senseNearbyUnits(worker.location().mapLocation(), 2);
					for (int i = 0; i < vu.size(); i++) {
						if (gc.canBuild(id, vu.get(i).id())) {
							gc.build(id, vu.get(i).id());
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
			break;
		case BOARD_ROCKET:
			break;
		default:
			break;
		}
	}
}
}