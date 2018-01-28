/****************/
/* REFACTOR ME! */
/****************/

import bc.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;

/*
data structure for caching all kinds of map data and doing nav
*/
public class Tile{
    int x;
    int y;
    boolean isWalkable;
    long karbonite;
    Region region;
    MapLocation myLoc;
    InfoManager infoMan;
    KarboniteArea karbArea;

    int roundLastUpdated;
    TreeSet<TargetUnit> enemiesWhichCouldHitUs;
    int possibleDamage;
    HashMap<Integer, Signpost> destToDir;
    int distFromNearestHostile;
    int distFromNearestTarget;
    
    //for combat
    TreeSet<TargetUnit> enemiesWithinRangerRange;
    TreeSet<TargetUnit> enemiesWithinKnightRange;
    TreeSet<TargetUnit> enemiesWithinMageRange;
    //boolean claimed;
    boolean enemiesUpdated;
    int unitID;
    UnitType myType;
    //boolean accessible; //contains no unit or our unit that is move ready
    
    public Tile(boolean walkable, long karb, Region reg, MapLocation ml, InfoManager im, KarboniteArea kA){
        x = ml.getX();
        y = ml.getY();
        isWalkable = walkable;
        karbonite = karb;
        region = reg;
        myLoc = ml;
        infoMan = im;
        karbArea = kA;
        roundLastUpdated = 0;
        possibleDamage = 0;
        destToDir = new HashMap<Integer, Signpost>();
        enemiesWhichCouldHitUs = new TreeSet<TargetUnit>(new descendingPriorityComp());
        enemiesWithinRangerRange = new TreeSet<TargetUnit>(new descendingPriorityComp());
        enemiesWithinKnightRange = new TreeSet<TargetUnit>(new descendingPriorityComp());
        enemiesWithinMageRange = new TreeSet<TargetUnit>(new descendingPriorityComp());
        //accessible = false;
        //claimed = false;
        enemiesUpdated = false;
        unitID = -1;
        distFromNearestHostile = 100;
        distFromNearestTarget = 100;
    }

    public void updateKarbonite(long newKarb){
        if (newKarb != karbonite){
            if(region != null){
                region.karbonite += newKarb - karbonite;
            }
            if(karbonite == 0){
            	if(karbArea == null){
            		karbArea = infoMan.getKarbArea(myLoc,region);
            	}
            	karbArea.addTile(this);
            }
            else{
            	karbArea.karbonite += newKarb - karbonite;
            }
            if(newKarb == 0){
            	karbArea.removeTile(this);
            }
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
    
    public void updateEnemies(GameController gc){
    	if(enemiesUpdated)
    		return;
    	//infoMan.logTimeCheckpoint("before update");
    	//Utils.log("updating tile " + x + " " + y);
    	enemiesUpdated = true;
    	enemiesWithinRangerRange.clear();
    	enemiesWithinMageRange.clear();
    	enemiesWithinKnightRange.clear();
    	enemiesWhichCouldHitUs.clear();
    	possibleDamage = 0;
    	/* // use or remove
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
    	for(Unit u: infoMan.rockets) {
    		if(!u.location().isOnMap())
    			continue;
            // TODO: make this only happen during the rocket countdown
    		if(u.location().mapLocation().distanceSquaredTo(myLoc) <= 2)
    			possibleDamage += 100;
    	}
    	TreeSet<TargetUnit> enemies = infoMan.getTargetUnits(myLoc, MagicNumbers.MAX_DIST_TO_CHECK, false);
    	distFromNearestHostile = MagicNumbers.MAX_DIST_TO_CHECK + 1;
    	distFromNearestTarget = MagicNumbers.MAX_DIST_TO_CHECK + 1;
    	boolean didSomething;
    	for(TargetUnit tu: enemies){
    		MapLocation ml = tu.myLoc;
    		long dist = myLoc.distanceSquaredTo(ml);
    		if(Utils.isTypeHostile(tu.type) && dist < distFromNearestHostile){
    			distFromNearestHostile = (int)dist;
    		}
    		if(dist < distFromNearestTarget){
    			distFromNearestTarget = (int)dist;
    		}
    		if(dist > MagicNumbers.MAX_DIST_THEY_COULD_HIT_NEXT_TURN)
    			continue;
    		didSomething = false;
    		//System.out.println("here");
    		if(MagicNumbers.RANGER_MIN_RANGE <= dist && dist <= MagicNumbers.RANGER_RANGE){
    			enemiesWithinRangerRange.add(tu);
    			didSomething = true;
    		}
    		if(dist <= MagicNumbers.KNIGHT_RANGE){
    			enemiesWithinKnightRange.add(tu);
    			didSomething = true;
    		}
    		if(dist <= MagicNumbers.MAGE_RANGE){
    			enemiesWithinMageRange.add(tu);
    			didSomething = true;
    		}
    		if(didSomething){
    			tu.tilesWhichHitMe.add(this);
    			infoMan.targetUnits.put(tu.ID, tu);
    		}
    		if(!Utils.isTypeHostile(tu.type))
    			continue;
    		//figure out if they can hit this tile next turn given that they can move once
    		int xDif = Math.abs(ml.getX() - x);
    		int yDif = Math.abs(ml.getY() - y);
    		int closestTheyCanGet = (xDif-1) * (xDif-1) + (yDif-1) * (yDif-1);
    		int farthestTheyCanGet = (xDif+1) * (xDif+1) + (yDif+1) * (yDif+1);
    		if(closestTheyCanGet <= tu.range
                    && !(tu.type == UnitType.Ranger && farthestTheyCanGet <= MagicNumbers.RANGER_MIN_RANGE)){
    			enemiesWhichCouldHitUs.add(tu);
    			possibleDamage += tu.damageDealingPower;
    		}
    	}
    	//infoMan.logTimeCheckpoint("after update");
    }
    
	public TreeSet<TargetUnit> getEnemiesWithinRange(UnitType type) {
		switch(type){
		case Ranger: return enemiesWithinRangerRange;
		case Knight: return enemiesWithinKnightRange;
		case Mage: return enemiesWithinMageRange;
		default: return new TreeSet<TargetUnit>();
		}
	}

    public double dangerRating(){
        if (!enemiesUpdated){
            updateEnemies(infoMan.gc);
        }
        return possibleDamage + (100 - distFromNearestHostile);
    }
}
