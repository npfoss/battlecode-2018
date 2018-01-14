import bc.*;
import java.util.ArrayList;

public class CombatSquad extends Squad{

	public CombatSquad(GameController g) {
		super(g);
	}
	public void update(){
		if(requestedUnits.isEmpty())
			requestedUnits.add(UnitType.Ranger);
	}

	public void move(Nav nav){
		for(int id : units) {
			Unit fighter = gc.unit(id);
			switch (objective) {
			case EXPLORE:
				VecUnit nearby = gc.senseNearbyUnits(fighter.location().mapLocation(),50);
				for(int i=0;i<nearby.size();i++) {
					Unit other = nearby.get(i);
					if(other.team() != gc.team() && gc.isAttackReady(fighter.id()) && gc.canAttack(fighter.id(), other.id())) {
						gc.attack(fighter.id(),other.id());
					}
				}
				Direction dirToMove = Utils.orderedDirections[(int) (8*Math.random())];
				if(gc.isMoveReady(id) && gc.canMove(id, dirToMove)&&!gc.unit(id).location().isInGarrison())
					gc.moveRobot(id, dirToMove);
				fighter = gc.unit(id);
				nearby = gc.senseNearbyUnits(fighter.location().mapLocation(),50);
				for(int i=0;i<nearby.size();i++) {
					Unit other = nearby.get(i);
					if(other.team() != gc.team() && gc.isAttackReady(fighter.id()) && gc.canAttack(fighter.id(), other.id())) {
						gc.attack(fighter.id(),other.id());
					}
				}
				break;
			default:
				break;
			}
		}
	}

	// micro will probably go here
}