import java.util.ArrayList;

import bc.*;

public class Utils{
    public static Direction[] orderedDirections =
        {Direction.East, Direction.Southeast, Direction.South, Direction.Southwest,
         Direction.West, Direction.Northwest, Direction.North, Direction.Northeast};

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
    public static Team enemyTeam(GameController gc){
    	return (gc.team() == Team.Blue ? Team.Red : Team.Blue);
    }
}
