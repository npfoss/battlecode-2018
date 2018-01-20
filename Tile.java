import bc.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class Tile{
    int x;
    int y;
    boolean isWalkable;
    long karbonite;
    Region region;
    MapLocation myLoc;
    MagicNumbers magicNums;
    InfoManager infoMan;
    boolean containsUnit;

    int roundLastUpdated;
    TreeSet<TargetUnit> enemiesWhichCouldHitUs;
    int possibleDamage;
    HashMap<String, Signpost> destToDir;
    int distFromNearestHostile;
    
    //for combat
    
    TreeSet<TargetUnit> enemiesWithinRangerRange;
    TreeSet<TargetUnit> enemiesWithinKnightRange;
    TreeSet<TargetUnit> enemiesWithinMageRange;
    //boolean claimed;
    boolean enemiesUpdated;
    boolean containsUpdated;
    //boolean accessible; //contains no unit or our unit that is move ready
    
    public Tile(int ex, int why, boolean walkable, long karb, Region reg, MapLocation ml, MagicNumbers mn, InfoManager im){
        x = ex;
        y = why;
        isWalkable = walkable;
        karbonite = karb;
        region = reg;
        myLoc = ml;
        magicNums = mn;
        infoMan = im;
        roundLastUpdated = 0;
        possibleDamage = 0;
        destToDir = new HashMap<String, Signpost>();
        enemiesWhichCouldHitUs = new TreeSet<TargetUnit>(new ascendingHealthComp());
        enemiesWithinRangerRange = new TreeSet<TargetUnit>(new ascendingHealthComp());
        enemiesWithinKnightRange = new TreeSet<TargetUnit>(new ascendingHealthComp());
        enemiesWithinMageRange = new TreeSet<TargetUnit>(new ascendingHealthComp());
        //accessible = false;
        //claimed = false;
        enemiesUpdated = false;
        containsUnit = false;
        containsUpdated = false;
        distFromNearestHostile = 100;
    }

    public void updateKarbonite(long newKarb){
        if (newKarb != karbonite){
            region.karbonite += newKarb - karbonite;
            karbonite = newKarb;
        }
    }
    
    public void removeEnemy(TargetUnit tu){
    	enemiesWithinRangerRange.remove(tu);
    	enemiesWithinKnightRange.remove(tu);
    	enemiesWithinMageRange.remove(tu);
    	enemiesWhichCouldHitUs.remove(tu);
    	possibleDamage -= tu.damageDealingPower;
    }
    
    public void updateTarget(TargetUnit tu){
    	if(enemiesWithinRangerRange.remove(tu)){
    		enemiesWithinRangerRange.add(tu);
    	}
    	if(enemiesWithinKnightRange.remove(tu)){
    		enemiesWithinKnightRange.add(tu);
    	}
    	if(enemiesWithinMageRange.remove(tu)){
    		enemiesWithinMageRange.add(tu);
    	}
    	if(enemiesWhichCouldHitUs.remove(tu)){
    		enemiesWhichCouldHitUs.add(tu);
    	}
    }
    
    public void updateContains(GameController gc){
    	if(containsUpdated)
    		return;
    	containsUpdated = true;
    	containsUnit = gc.hasUnitAtLocation(myLoc);
    }
    
    public void updateEnemies(GameController gc){
    	if(enemiesUpdated)
    		return;
    	enemiesUpdated = true;
    	enemiesWithinRangerRange.clear();
    	enemiesWithinMageRange.clear();
    	enemiesWithinKnightRange.clear();
    	enemiesWhichCouldHitUs.clear();
    	possibleDamage = 0;
    	/*
    	claimed = false;
    	if(!gc.hasUnitAtLocation(myLoc)){
        	accessible = false;
        }
        else if(!gc.hasUnitAtLocation(myLoc)){
        	accessible = true;
        }
        else{
        	Unit u = gc.senseUnitAtLocation(myLoc);
        	if(u.team() == gc.team() && u.movementHeat() < 10)
        		accessible = true;
        	accessible = false;
        }
        */
    	TreeSet<TargetUnit> enemies = Utils.getTargetUnits(myLoc, 72, false, infoMan);
    	distFromNearestHostile = 100;
    	boolean didSomething;
    	for(TargetUnit tu: enemies){
    		MapLocation ml = tu.myLoc;
    		long dist = myLoc.distanceSquaredTo(ml);
    		if(Utils.isTypeHostile(tu.type) && dist<distFromNearestHostile){
    			distFromNearestHostile = (int)dist;
    		}
    		didSomething = false;
    		//System.out.println("here");
    		if(magicNums.RANGER_MIN_RANGE <= dist && dist <= magicNums.RANGER_RANGE){
    			enemiesWithinRangerRange.add(tu);
    			didSomething = true;
    		}
    		if(dist <= magicNums.KNIGHT_RANGE){
    			enemiesWithinKnightRange.add(tu);
    			didSomething = true;
    		}
    		if(dist <= magicNums.MAGE_RANGE){
    			enemiesWithinMageRange.add(tu);
    			didSomething = true;
    		}
    		if(!Utils.isTypeHostile(tu.type))
    			continue;
    		//figure out if they can hit this tile next turn given that they can move once
    		int xDif = Math.abs(ml.getX() - x);
    		int yDif = Math.abs(ml.getY() - y);
    		int closestTheyCanGet = (xDif-1) * (xDif-1) + (yDif-1) * (yDif-1);
    		int farthestTheyCanGet = (xDif+1) * (xDif+1) + (yDif+1) * (yDif+1);
    		if(closestTheyCanGet <= tu.range && 
    		   !(tu.type == UnitType.Ranger && farthestTheyCanGet <= magicNums.RANGER_MIN_RANGE)){
    			enemiesWhichCouldHitUs.add(tu);
    			possibleDamage += tu.damageDealingPower;
    		}
    		if(didSomething){
    			tu.tilesWhichHitMe.add(this);
    			infoMan.targetUnits.put(tu.ID, tu);
    		}
    	}
    }

	public TreeSet<TargetUnit> getEnemiesWithinRange(UnitType type) {
		switch(type){
		case Ranger: return enemiesWithinRangerRange;
		case Knight: return enemiesWithinKnightRange;
		case Mage: return enemiesWithinMageRange;
		default: return new TreeSet<TargetUnit>();
		}
	}
}