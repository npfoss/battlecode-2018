import bc.*;
import java.lang.Math;

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
        // will actually use pathfinding at some point

        //for now
        return start.directionTo(target);
    }

    /*
    actual direction you should move to go from start to target
    takes into account movable obstructions like other robots
    */
    public Direction dirToMove(MapLocation start, MapLocation target){
        return dirToMove(start, directionTowards(start, target));
    }

    // overloaded for convenience
    public Direction dirToMove(MapLocation start, Direction preferredDir){

        // now check if legal and stuff
        if (infoMan.isLocationClear(start.add(preferredDir))){
            return preferredDir;
        }
        
        // if not, try slight deviations
        Direction left = preferredDir;
        Direction right = preferredDir;
        for (int i = 0; i < 3; i++){
            // try everything short of going backwards
            left = Utils.rotateLeft(left);
            right = Utils.rotateRight(right);

            if (infoMan.isLocationClear(start.add(left))){
                return left;
            }
            if (infoMan.isLocationClear(start.add(right))){
                return right;
            }
        }

        // give up
        return Direction.Center;
    }

    public Direction dirToMoveSafely(MapLocation start, MapLocation target){
        // Direction dirTowards = directionTowards(start, target);
        // now check if legal and safe and stuff

        // for now (TODO)
        return dirToMove(start, target);
    }

    public Direction dirToExplore(MapLocation loc){
        return dirToMoveSafely(loc, loc.add(Utils.orderedDirections[(int)(Math.random()*8)]));
    }
}