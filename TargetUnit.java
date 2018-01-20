import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;

import bc.*;

public class TargetUnit {
	int ID;
	long health;
	int damageDealingPower;
	MapLocation myLoc;
	UnitType type;
	ArrayList<Tile> tilesWhichHitMe;
	long range;
	long defense;
	
	public TargetUnit(int i, long h, int ddp, MapLocation ml, UnitType ut, long r, long d){
		ID = i;
		health = h;
		damageDealingPower = ddp;
		myLoc = ml;
		type = ut;
		tilesWhichHitMe = new ArrayList<Tile>();
		range = r;
		defense = d;
	}
	
	public boolean equals(Object o){
		if(!(o instanceof TargetUnit))
			return false;
		
		TargetUnit tu = (TargetUnit)o;
		
		return ID == tu.ID;
	}
}
