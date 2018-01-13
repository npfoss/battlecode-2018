import bc.*;
import java.util.ArrayList;

public class CombatSquad extends Squad{
	
	public CombatSquad(GameController g) {
		super(g);
	}
    public void update(){

    }

    public void move(Nav nav){
    	for(int id: units) {
    		Unit fighter = gc.unit(id);
    		Location l = fighter.location();
    		if(l.isOnMap()) {
    			VecUnit nearby = gc.senseNearbyUnits(l.mapLocation(),70);
    			for(int i=0;i<nearby.size();i++) {
    				Unit other = nearby.get(i);
    				if(other.team() != gc.team() && gc.isAttackReady(fighter.id()) && gc.canAttack(fighter.id(), other.id())) {
    					gc.attack(fighter.id(),other.id());
    				}
    			}
    		}
    	}
    }

    // micro will probably go here
}