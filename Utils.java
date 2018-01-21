import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;

import bc.*;

public class Utils{
    public static Direction[] orderedDirections =
        {Direction.East, Direction.Southeast, Direction.South, Direction.Southwest,
         Direction.West, Direction.Northwest, Direction.North, Direction.Northeast};
    public static Direction[] orderedDiagonals =
        {Direction.Southeast, Direction.Southwest,
         Direction.Northwest, Direction.Northeast};

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
         ret[6] = oppositeDir(dir);
         return ret;
    }
    
    public static Direction oppositeDir(Direction dir){
        switch(dir){
            case North: return Direction.South;
            case Northwest: return Direction.Southeast;
            case West: return Direction.East;
            case Southwest: return Direction.Northeast;
            case South: return Direction.North;
            case Southeast: return Direction.Northwest;
            case East: return Direction.West;
            case Northeast: return Direction.Southwest;
        }
        return Direction.Center;
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

    public static boolean isDiagonalDirection(Direction dir){
        switch(dir){
            case Northeast:
            case Northwest:
            case Southeast:
            case Southwest: return true;
        }
        return false;
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
    
    public static TreeSet<TargetUnit> getTargetUnits(MapLocation ml, int radius, boolean hostileOnly, InfoManager infoMan){
    	TreeSet<TargetUnit> ret = new TreeSet<TargetUnit>(new descendingPriorityComp());
    	for(TargetUnit tu: infoMan.targetUnits.values()){
    		if((!hostileOnly || isTypeHostile(tu.type)) && tu.myLoc.distanceSquaredTo(ml) <= radius)
    			ret.add(tu);
    	}
    	return ret;
    }
    
    public static boolean isTypeHostile(UnitType ut){
    	return ut == UnitType.Knight || ut == UnitType.Ranger || ut == UnitType.Mage;
    }
    
    public static Team enemyTeam(GameController gc){
    	return (gc.team() == Team.Blue ? Team.Red : Team.Blue);
    }
}
