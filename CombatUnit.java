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
	boolean canSnipe;
	boolean canOvercharge;
	int stepsFromTarget;
	MapLocation myLoc;
	UnitType type;
	long maxHealth;
	
	public CombatUnit(){
		
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
		case Ranger: maxHealth = 200; break;
		case Knight: maxHealth = 250; break;
		case Mage: maxHealth = 80; break;
		case Healer: maxHealth = 100;
		}
	}
	
	public void update(GameController gc, int sft){
		System.out.println("updating " + ID);
		System.out.flush();
		Unit u = gc.unit(ID);
		stepsFromTarget = sft;
		health = u.health();
		canAttack = u.attackHeat() < 10 && !(type == UnitType.Ranger && u.rangerIsSniping() != 0);
		canMove = u.movementHeat() < 10 && !(type == UnitType.Ranger && u.rangerIsSniping() != 0);
		canSnipe = (gc.researchInfo().getLevel(UnitType.Ranger) == 3 && type == UnitType.Ranger && u.abilityHeat() < 10 && u.rangerIsSniping() == 0);
		canSnipe = (gc.researchInfo().getLevel(UnitType.Healer) == 3 && type == UnitType.Healer && u.abilityHeat() < 10);
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
