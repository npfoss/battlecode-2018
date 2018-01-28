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
public class CombatUnit {
	int ID;
	int damage;
	UnitType type;
	long maxHealth;

	long health;
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
		stepsFromTarget = sft;
		switch(type){
			case Ranger: maxHealth = 200; break;
			case Knight: maxHealth = 250; break;
			case Mage: maxHealth = 80; break;
			case Healer: maxHealth = 100;
		}
		notOnMap = true;
	}
	
	public void update(GameController gc, int sft){
		Unit u = gc.unit(ID);
		stepsFromTarget = sft;
		health = u.health();
		canAttack = u.attackHeat() < 10 && !(type == UnitType.Ranger && u.rangerIsSniping() != 0);
		canMove = u.movementHeat() < 10 && !(type == UnitType.Ranger && u.rangerIsSniping() != 0);
		canSnipe = (gc.researchInfo().getLevel(UnitType.Ranger) == 3 && type == UnitType.Ranger && u.abilityHeat() < 10 && u.rangerIsSniping() == 0);
		canOvercharge = (gc.researchInfo().getLevel(UnitType.Healer) == 3 && type == UnitType.Healer && u.abilityHeat() < 10);
		// REFACTOR: should you be updating distFromNearestHostile here too?
		// REFACTOR: technically damage could change too
	}
	
	public boolean equals(Object o){
		if(!(o instanceof CombatUnit))
			return false;
		return ID == ((CombatUnit)o).ID;
	}

	public boolean updateOnMap(GameController gc) {
		notOnMap = !gc.unit(ID).location().isOnMap();
		if(!notOnMap)
			myLoc = gc.unit(ID).location().mapLocation();
		return !notOnMap;
	}
}
