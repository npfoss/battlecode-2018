import bc.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

public class Tile{
    int x;
    int y;
    boolean isWalkable;
    long karbonite;
    Region region;
    MapLocation myLoc;
    MagicNumbers magicNums;

    int roundLastUpdated;
    HashSet<Integer> enemiesWhichCouldHitUs;
    int possibleDamage;
    HashMap<MapLocation, Signpost> destToDir;
    
    //for combat
    
    HashSet<Integer> enemiesWithinRangerRange; //list of IDs
    HashSet<Integer> enemiesWithinKnightRange;
    HashSet<Integer> enemiesWithinMageRange;
    boolean claimed;
    boolean enemiesUpdated;
    boolean accessible; //contains no unit or our unit that is move ready
    
    public Tile(int ex, int why, boolean walkable, long karb, Region reg, MapLocation ml, MagicNumbers mn){
        x = ex;
        y = why;
        isWalkable = walkable;
        karbonite = karb;
        region = reg;
        myLoc = ml;
        magicNums = mn;
        roundLastUpdated = 0;
        possibleDamage = 0;
        destToDir = new HashMap<MapLocation, Signpost>();
        enemiesWithinRangerRange = new HashSet<Integer>();
        enemiesWithinKnightRange = new HashSet<Integer>();
        enemiesWithinMageRange = new HashSet<Integer>();
        accessible = false;
        claimed = false;
        enemiesUpdated = false;
    }

    public void updateKarbonite(long newKarb){
        if (newKarb != karbonite){
            region.karbonite += newKarb - karbonite;
            karbonite = newKarb;
        }
    }
    
    public void removeEnemy(TargetUnit tu){
    	enemiesWithinRangerRange.remove(tu.ID);
    	enemiesWithinKnightRange.remove(tu.ID);
    	enemiesWithinMageRange.remove(tu.ID);
    	enemiesWhichCouldHitUs.remove(tu.ID);
    }
    
    public void updateEnemies(GameController gc){
    	if(enemiesUpdated)
    		return;
    	enemiesUpdated = true;
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
    	VecUnit enemies = gc.senseNearbyUnitsByTeam(myLoc, 72, Utils.enemyTeam(gc));
    	for(int i = 0; i < enemies.size(); i++){
    		Unit enemy = enemies.get(i);
    		MapLocation ml = enemy.location().mapLocation();
    		long dist = myLoc.distanceSquaredTo(ml);
    		if(magicNums.RANGER_MIN_RANGE <= dist && dist <= magicNums.RANGER_RANGE)
    			enemiesWithinRangerRange.add(enemy.id());
    		if(dist <= magicNums.KNIGHT_RANGE)
    			enemiesWithinKnightRange.add(enemy.id());
    		if(dist <= magicNums.MAGE_RANGE)
    			enemiesWithinMageRange.add(enemy.id());
    		//figure out if they can hit this tile next turn given that they can move once
    		int xDif = Math.abs(ml.getX() - x);
    		int yDif = Math.abs(ml.getY() - y);
    		int closestTheyCanGet = (xDif-1) * (xDif-1) + (yDif-1) * (yDif-1);
    		int farthestTheyCanGet = (xDif+1) * (xDif+1) + (yDif+1) * (yDif+1);
    		if(closestTheyCanGet <= enemy.attackRange() && 
    		   !(enemy.unitType() == UnitType.Ranger && farthestTheyCanGet <= enemy.rangerCannotAttackRange())){
    			enemiesWhichCouldHitUs.add(enemy.id());
    			possibleDamage += enemy.damage();
    		}
    	}
    }

	public HashSet<Integer> getEnemiesWithinRange(UnitType type) {
		switch(type){
		case Ranger: return enemiesWithinRangerRange;
		case Knight: return enemiesWithinKnightRange;
		case Mage: return enemiesWithinMageRange;
		default: return new HashSet<Integer>();
		}
	}
}