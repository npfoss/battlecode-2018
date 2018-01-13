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
			switch (objective) {
			case BUILD:
				if(targetLoc != null) {
					if(!worker.location().mapLocation().isAdjacentTo(targetLoc)) {
						Direction movedir = nav.dirToMove(worker.location().mapLocation(),targetLoc);
						if (movedir != Direction.Center) {
							gc.moveRobot(id, movedir);
							worker = gc.unit(id);
						}

					}
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
					if(gc.senseUnitAtLocation(targetLoc).structureIsBuilt() != 0) {
						objective = Objective.NONE;
					}

				}
			else {
				if(!madeBlueprint) {
					for(Direction dirToBuild : Utils.orderedDirections) {
						if (gc.karbonite() > bc.bcUnitTypeBlueprintCost(toBuild)
								&& gc.canBlueprint(id, toBuild, dirToBuild)) {
							gc.blueprint(worker.id(), toBuild, dirToBuild);
							madeBlueprint = true;
							break;
						}
					}
				}
				VecUnit vu = gc.senseNearbyUnits(worker.location().mapLocation(), 2);
				for (int i = 0; i < vu.size(); i++) {
					if (gc.canBuild(id, vu.get(i).id())) {
						gc.build(id, vu.get(i).id());
					}
				}
					
			}
			break;
		case MINE:
			break;
		case BOARD_ROCKET:
			break;
		case EXPLORE:
			break;
		default:
			break;
		}
	}
}
}