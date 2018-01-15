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

    public Direction dirToMove(MapLocation start, MapLocation target){
        //System.out.println("begin");
        Direction dirTowards = directionTowards(start, target);
        //System.out.println("dir to target " + dirTowards);
        
        // now check if legal and stuff
        if (infoMan.isLocationClear(start.add(dirTowards))){
            return dirTowards;
        }
        //System.out.println("ideal dir didn't work");
        // if not, try slight deviations
        Direction left = dirTowards;
        Direction right = dirTowards;
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

        return dirToMove(start, target);
    }

    public Direction dirToExplore(MapLocation loc){
        return dirToMoveSafely(loc, loc.add(Utils.orderedDirections[(int)(Math.random()*8)]));
    }
}