import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;
import bc.*;

/*
data structure for keeping track of enemy units
*/
public class TargetUnit {
	int ID;
	long health;
	int damageDealingPower;
	int priority;
	MapLocation myLoc;
	UnitType type;
	ArrayList<Tile> tilesWhichHitMe;
	long range;
	long defense;
	double snipePriority;
	long snipeDamageToDo;
	InfoManager infoMan;
	
	public TargetUnit(Unit unit, InfoManager im){
		infoMan = im;

		ID = unit.id();
		type = unit.unitType();
		health = unit.health();
		myLoc = unit.location().mapLocation();
		defense = type == UnitType.Knight ? unit.knightDefense() : 0;
		damageDealingPower = 0;
		range = 0;
		if(unit.unitType() != UnitType.Factory && unit.unitType() != UnitType.Rocket){
			damageDealingPower = unit.damage();
			range = unit.attackRange();
		}

		tilesWhichHitMe = new ArrayList<Tile>();
		switch(type){ // TWEAK: maybe
		    case Rocket: priority = (myLoc.getPlanet() == Planet.Earth ? 7 : 1); break;
		    case Factory: priority = 6; break;
		    case Mage: priority = 5; break;
		    case Healer: priority = 4; break;
		    case Knight: priority = 3; break;
		    case Ranger: priority = 2; break;
		    case Worker: priority = 1;
		}
	}
	
	// TWEAK: not exactly sure what, but maybe we want to? also see the priority list above
	public void updateSnipePriority(MapLocation swarmLoc){
		snipePriority = priority + 1 + swarmLoc.distanceSquaredTo(myLoc) / 25.0;
		snipeDamageToDo = health;
		for(TargetUnit tu: infoMan.getTargetUnits(myLoc, 2, false)){
			if(tu.ID != ID){
				snipePriority += tu.priority;
				if(tu.health > snipeDamageToDo)
					snipeDamageToDo = tu.health;
			}
		}
	}

	public boolean equals(Object o){
		if(!(o instanceof TargetUnit))
			return false;
		return ID == ((TargetUnit)o).ID;
	}
}
