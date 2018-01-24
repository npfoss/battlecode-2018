/****************/
/* REFACTOR ME! */
/****************/

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
	
	public TargetUnit(int i, long h, int ddp, MapLocation ml, UnitType ut, long r, long d, InfoManager im){
		ID = i;
		health = h;
		damageDealingPower = ddp;
		myLoc = ml;
		type = ut;
		tilesWhichHitMe = new ArrayList<Tile>();
		range = r;
		defense = d;
		switch(ut){
		    case Rocket: priority = (ml.getPlanet() == Planet.Earth ? 7 : 1); break;
		    case Factory: priority = 6; break;
		    case Mage: priority = 5; break;
		    case Healer: priority = 4; break;
		    case Knight: priority = 3; break;
		    case Ranger: priority = 2; break;
		    case Worker: priority = 1;
		}
		infoMan = im;
	}
	
	public boolean equals(Object o){
		if(!(o instanceof TargetUnit))
			return false;
		
		TargetUnit tu = (TargetUnit)o;
		
		return ID == tu.ID;
	}
	
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
}
