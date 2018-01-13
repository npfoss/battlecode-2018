import bc.*;
import java.util.ArrayList;

public class CombatSquad extends Squad{

	public CombatSquad(GameController g) {
		super(g);
	}
	public void update(){

	}

	public void move(Nav nav){

		System.out.println("Trying to move");
		for(int id : units) {
			System.out.println("Trying to move: " + id);
			Unit fighter = gc.unit(id);
			switch (objective) {
			case EXPLORE:
				System.out.println("Trying to Explore");
				Direction dirToMove = Utils.orderedDirections[(int) (8*Math.random())];
				if(gc.isMoveReady(id) && gc.canMove(id, dirToMove))
					gc.moveRobot(id, dirToMove);
				fighter = gc.unit(id);
				VecUnit nearby = gc.senseNearbyUnits(fighter.location().mapLocation(),80);
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