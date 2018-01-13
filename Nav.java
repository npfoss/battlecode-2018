import bc.*;

public class Nav{
    InfoManager infoMan;
    // should not have to use gc, do all that through infoMan (to cache results)

    public Nav(InfoManager im){
        infoMan = im;
    }

    /*
    returns the optimal direction to reach the target,
    ignoring other units
    */
    public Direction directionTowards(MapLocation start, MapLocation target){
        return Direction.Southeast;
    }

    public Direction dirToMove(MapLocation start, MapLocation target){
        Direction dirTowards = directionTowards(start, target);
        // now check if legal and stuff

        return dirTowards;
    }

    public Direction dirToMoveSafely(MapLocation start, MapLocation target){
        Direction dirTowards = directionTowards(start, target);
        // now check if legal and safe and stuff

        return dirTowards;
    }
}