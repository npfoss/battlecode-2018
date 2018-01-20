import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import bc.*;

public class CombatUnit {
	int ID;
	int damage;
	long health;
	int dependencyID;
	boolean canAttack;
	boolean canMove;
	int stepsFromTarget;
	MapLocation myLoc;
	UnitType type;
	
	public CombatUnit(){
		
	}
	
	public CombatUnit(int i, int d, long h, boolean ca, boolean cm, MapLocation ml, UnitType ut, int sft){
		ID = i;
		damage = d;
		health = h;
		canAttack = ca;
		canMove = cm;
		dependencyID = -1;
		myLoc = ml;
		type = ut;
		stepsFromTarget = sft;
	}
	
	public void update(GameController gc){
		Unit u = gc.unit(ID);
		health = u.health();
		canAttack = u.attackHeat() < 10;
		canMove = u.movementHeat() < 10;
	}
	
	public boolean equals(Object o){
		if(!(o instanceof CombatUnit))
			return false;
		
		CombatUnit cu = (CombatUnit)o;
		
		return ID == cu.ID;
	}
	/*
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
