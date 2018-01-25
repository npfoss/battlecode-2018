/****************/
/* REFACTOR ME! */
/****************/

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import bc.*;

/*
data structure for storing unit info.
basically a copy of Unit augmented for our needs
like TargetUnit, but for friendlies
*/
// can this be combined with target unit?
public class CombatUnit {
	int ID; // technically shouldn't be captialized right?
	int damage; // unused?
	UnitType type;
	long maxHealth;

	long health;
	int dependencyID; // what does this mean?
	boolean canAttack;
	boolean canMove;
	boolean canSnipe;
	boolean canOvercharge;
	int stepsFromTarget;
	int distFromNearestHostile; // unused?
	MapLocation myLoc;
	boolean notOnMap;
	
	public CombatUnit(){ // does this need to be here?
		
	}
	
	public CombatUnit(int i, int d, long h, boolean ca, boolean cm, MapLocation ml, UnitType ut, int sft){
		ID = i;
		damage = d;
		health = h;
		canAttack = ca;
		canMove = cm;
		canSnipe = false;
		dependencyID = -1;
		myLoc = ml;
		type = ut;
		stepsFromTarget = sft;
		switch(type){
			case Ranger: maxHealth = 200; break; // should be magic numbers in case they change the specs
			case Knight: maxHealth = 250; break;
			case Mage: maxHealth = 80; break;
			case Healer: maxHealth = 100;
		}
		notOnMap = true;
	}
	
	public void update(GameController gc, int sft){
		Unit u = gc.unit(ID);
		if(notOnMap && u.location().isOnMap()){
			myLoc = u.location().mapLocation();
			notOnMap = false;
		}
		// should there be a check for being on the map here? you can't attack and stuff if not on the map
		stepsFromTarget = sft;
		health = u.health();
		canAttack = u.attackHeat() < 10 && !(type == UnitType.Ranger && u.rangerIsSniping() != 0);
		canMove = u.movementHeat() < 10 && !(type == UnitType.Ranger && u.rangerIsSniping() != 0);
		canSnipe = (gc.researchInfo().getLevel(UnitType.Ranger) == 3 && type == UnitType.Ranger && u.abilityHeat() < 10 && u.rangerIsSniping() == 0);
		canOvercharge = (gc.researchInfo().getLevel(UnitType.Healer) == 3 && type == UnitType.Healer && u.abilityHeat() < 10);
		// should you be updating distFromNearestHostile here too?
		// technically damage could change too
	}
	
	public boolean equals(Object o){
		if(!(o instanceof CombatUnit))
			return false;
		return ID == ((CombatUnit)o).ID;
	}
	
	/* // use or remove
	public void addOption(int opt, Tile tile) {
		if(attackOptions.containsKey(opt)){
			ArrayList<Tile> alt = attackOptions.get(opt);
			alt.add(tile);
			attackOptions.put(opt, alt);
		}
		else{
			ArrayList<Tile> alt = new ArrayList<Tile>();
			alt.add(tile);
			attackOptions.put(opt, alt);
		}
	}*/
}
