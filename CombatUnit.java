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
	int distFromNearestHostile;
	MapLocation myLoc;
	boolean notOnMap;
	
	public CombatUnit(Unit u, MapLocation ml, int sft){
		ID = u.id();
		type = u.unitType();
		damage = u.damage();
		health = u.health();
		canAttack = u.attackHeat() < 10;
		canMove = u.movementHeat() < 10;
		myLoc = ml;
		canSnipe = false;
		dependencyID = -1;
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
		} else if (!notOnMap && !u.location().isOnMap()){
			notOnMap = true;
			// didn't see this anywhere so I added it. probably didn't cause problems before but can't hurt
		}
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
}
