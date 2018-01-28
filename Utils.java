/****************/
/* REFACTOR ME! */
/****************/

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;

import bc.*;

public class Utils{
	public static Direction[] orderedDirections =
        {Direction.East, Direction.Southeast, Direction.South, Direction.Southwest,
         Direction.West, Direction.Northwest, Direction.North, Direction.Northeast};
    public static final int[] dx = {1, 1, 0,-1,-1,-1,0,1,0};
    public static final int[] dy = {0,-1,-1,-1, 0, 1,1,1,0};
    public static Direction[] orderedDiagonals =
        {Direction.Southeast, Direction.Southwest,
         Direction.Northwest, Direction.Northeast};
    public static UnitType[] robotTypes =
        {UnitType.Knight, UnitType.Mage, UnitType.Ranger, UnitType.Healer, UnitType.Worker};

    public static int typeToInd(UnitType ut){
        switch(ut){
            case Knight: return 0;
            case Mage  : return 1;
            case Ranger: return 2;
            case Healer: return 3;
            case Worker: return 4;
            case Rocket: return 5;
            default: return 6;
        }
    }

    public static Direction indexToDirection(int i){
        switch(i){
            case 0: return Direction.East;
            case 1: return Direction.Southeast;
            case 2: return Direction.South;
            case 3: return Direction.Southwest;
            case 4: return Direction.West;
            case 5: return Direction.Northwest;
            case 6: return Direction.North;
            case 7: return Direction.Northeast;
            default: return Direction.Center;
        }
    }

    public static Direction[] directionsTowardButNotIncluding(Direction dir) {
    	 Direction left = dir;
         Direction right = dir;
         Direction[] ret = new Direction[7];
         //ret[0] = dir;
         int counter = 0;
         for (int i = 0; i < 3; i++){
             left = Utils.rotateLeft(left);
             right = Utils.rotateRight(right);
             ret[counter++] = left;
             ret[counter++] = right;
         }
         ret[6] = oppositeDirection(dir);
         return ret;
    }
    
    public static Direction rotateLeft(Direction dir){
        switch(dir){
            case North: return Direction.Northwest;
            case Northwest: return Direction.West;
            case West: return Direction.Southwest;
            case Southwest: return Direction.South;
            case South: return Direction.Southeast;
            case Southeast: return Direction.East;
            case East: return Direction.Northeast;
            case Northeast: return Direction.North;
        }
        return Direction.Center;
    }

    public static Direction rotateRight(Direction dir){
        switch(dir){
            case North: return Direction.Northeast;
            case Northwest: return Direction.North;
            case West: return Direction.Northwest;
            case Southwest: return Direction.West;
            case South: return Direction.Southwest;
            case Southeast: return Direction.South;
            case East: return Direction.Southeast;
            case Northeast: return Direction.East;
        }
        return Direction.Center;
    }

    public static Direction oppositeDirection(Direction dir){
        switch(dir){
            case North: return Direction.South;
            case South: return Direction.North;
            case East: return Direction.West;
            case West: return Direction.East;
            case Northeast: return Direction.Southwest;
            case Southwest: return Direction.Northeast;
            case Northwest: return Direction.Southeast;
            case Southeast: return Direction.Northwest;
        }
        return Direction.Center;
    }

    public static Direction oppositeDirection(int ind){
        return oppositeDirection(indexToDirection(ind));
    }

    public static boolean isDiagonalDirection(Direction dir){
        switch(dir){
            case Northeast:
            case Northwest:
            case Southeast:
            case Southwest: return true;
        }
        return false;
    }

    public static boolean isDiagonalDirection(int ind){
        return ind % 2 == 1;
    }

    public static MapLocation averageMapLocation(GameController gc, ArrayList<Integer> units) {
    	if(units.size() == 0)
    		return null;
    	int x = 0;
    	int y = 0;
    	for(int i : units) {
    		Unit u = gc.unit(i);
    		x += u.location().mapLocation().getX();
    		y += u.location().mapLocation().getY();
    	}
    	return new MapLocation(gc.planet(),x/units.size(), y/units.size());
    }
    
    public static MapLocation averageMapLocation(GameController gc, TreeSet<CombatUnit> units) {
    	if(units.size() == 0)
    		return null;
    	int x = 0;
    	int y = 0;
    	for(CombatUnit cu : units) {
    		x += cu.myLoc.getX();
    		y += cu.myLoc.getY();
    	}
    	return new MapLocation(gc.planet(),x/units.size(), y/units.size());
    }
    
    public static void log(String s){
    	System.out.println(s);
    	System.out.flush();
    }
    
    public static MapLocation averageMapLocation(GameController gc, Collection<CombatUnit> units) {
    	if(units.size() == 0)
    		return null;
    	int x = 0;
    	int y = 0;
    	for(CombatUnit cu : units) {
    		x += cu.myLoc.getX();
    		y += cu.myLoc.getY();
    	}
    	return new MapLocation(gc.planet(),x/units.size(), y/units.size());
    }
    
    public static boolean isTypeHostile(UnitType ut){ // should healers be included?
    	return ut == UnitType.Knight || ut == UnitType.Ranger || ut == UnitType.Mage;
    }
    
    public static Team enemyTeam(GameController gc){
    	return (gc.team() == Team.Blue ? Team.Red : Team.Blue);
    }

    public static int maxDistIndex(Unit[] units, MapLocation loc){
        int maxInd = -1;
        long maxDist = 0;
        for (int i = 0; i < units.length; i++){
            if (units[i] == null) return i;
            long dist = loc.distanceSquaredTo(units[i].location().mapLocation());
            if (dist > maxDist){
                maxDist = dist;
                maxInd = i;
            }
        }
        return maxInd;
    }
}
