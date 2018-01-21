import bc.*;
import java.lang.Math;
import java.util.ArrayList;
import java.util.HashMap;

/* TODO
maybe have units all claim tiles and optimize that?
-or maybe look at the duck's code for pathing around units
-only if current one is bad I think
account for karbonite on adjacent impassible tiles for regional totals
*/
public class Nav{
    InfoManager infoMan;
    // should not have to use gc, do all that through infoMan (to cache results)

    public Nav(InfoManager im){
        infoMan = im;
    }

    /*
    gives the number of steps from start to target,
    assuming no movable units obstruct you
    */
    public int optimalStepsTo(MapLocation start, MapLocation target){
    	if(!infoMan.isReachable(start, target)){
        	return 1000;
        	//arbitrarily large number
        }
        if (!infoMan.tiles[start.getX()][start.getY()].destToDir.containsKey(target.toString())){
            generateBFSMap(target);
        }
        return infoMan.tiles[start.getX()][start.getY()].destToDir.get(target.toString()).stepsToDest;
    }

    /*
    returns the optimal direction to reach the target,
    ignoring other units
    */
    public Direction directionTowards(MapLocation start, MapLocation target){
        if (!infoMan.tiles[start.getX()][start.getY()].destToDir.containsKey(target.toString())){
            generateBFSMap(target);
        }
        return infoMan.tiles[start.getX()][start.getY()].destToDir.get(target.toString()).direction;
    }

    /*
    actual direction you should move to go from start to target
    takes into account movable obstructions like other robots
    */
    public Direction dirToMove(MapLocation start, MapLocation target){
    	if(infoMan.isReachable(start, target))
    		return dirToMove(start, directionTowards(start, target));
    	else
    		return dirToMove(start, start.directionTo(target));
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
        return dirToMoveSafely(start, directionTowards(start, target));
    }

    public Direction dirToMoveSafely(MapLocation start, Direction preferredDir){
        // Direction dirTowards = directionTowards(start, target);
        // now check if legal and safe and stuff
        MapLocation loc = start.add(preferredDir);
        double danger;
        double bestDanger = 999999;
        Direction bestDir = Direction.Center;

        if (infoMan.isLocationClear(loc)){
            danger = infoMan.tiles[loc.getX()][loc.getY()].dangerRating();
            if (danger < bestDanger){
                bestDanger = danger;
                bestDir = preferredDir;
            }
            if (bestDanger <= 0){
                return bestDir;
            }
        }
        
        // if not, try slight deviations
        Direction left = preferredDir;
        Direction right = preferredDir;
        for (int i = 0; i < 3; i++){
            // try everything short of going backwards
            left = Utils.rotateLeft(left);
            right = Utils.rotateRight(right);

            loc = start.add(left);

            if (infoMan.isLocationClear(loc)){
                danger = infoMan.tiles[loc.getX()][loc.getY()].dangerRating();
                if (danger < bestDanger){
                    bestDanger = danger;
                    bestDir = left;
                }
                if (bestDanger <= 0){
                    return bestDir;
                }
            }

            loc = start.add(right);

            if (infoMan.isLocationClear(loc)){
                danger = infoMan.tiles[loc.getX()][loc.getY()].dangerRating();
                if (danger < bestDanger){
                    bestDanger = danger;
                    bestDir = right;
                }
                if (bestDanger <= 0){
                    return bestDir;
                }
            }
        }

        // maybe going backwards is safer?
        right = Utils.rotateRight(right);
        loc = start.add(right);
        if (infoMan.isLocationClear(loc)){
            danger = infoMan.tiles[loc.getX()][loc.getY()].dangerRating();
            if (danger < bestDanger){
                bestDanger = danger;
                bestDir = right;
            }
            if (bestDanger <= 0){
                return bestDir;
            }
        }

        // last resort: stay put
        if(infoMan.tiles[start.getX()][start.getY()].dangerRating() < bestDanger){
            bestDir = Direction.Center;
        }

        return bestDir;
    }

    public Direction dirToExplore(MapLocation loc){
        return dirToMoveSafely(loc, Utils.orderedDirections[(int)(Math.random()*8)]);
    }

    public void generateBFSMap(MapLocation target){
        System.out.println("Generating map to " + target.toString());
        int dist = 0;
        ArrayList<MapLocation> currentLocs = new ArrayList<MapLocation>();
        ArrayList<MapLocation> nextLocs = new ArrayList<MapLocation>();
        infoMan.tiles[target.getX()][target.getY()].destToDir.put(target.toString(), new Signpost(Direction.Center, 0));

        currentLocs.add(target);
        dist++;
        Signpost sign;
        while (currentLocs.size() > 0){
            for (MapLocation loc : currentLocs){
                for (Direction dir : Utils.orderedDirections){
                    MapLocation neighbor = loc.add(dir);
                    if (infoMan.isOnMap(neighbor) && infoMan.isLocationWalkable(neighbor)){
                        if (infoMan.tiles[neighbor.getX()][neighbor.getY()].destToDir.containsKey(target.toString())){
                            sign = infoMan.tiles[neighbor.getX()][neighbor.getY()].destToDir.get(target.toString());
                            if (sign.stepsToDest == dist
                                    && !Utils.isDiagonalDirection(dir)
                                    && Utils.isDiagonalDirection(sign.direction)){
                                // prefer to not move diagonaly if it's the same dist.
                                // may prevent bottlenecks
                                sign.stepsToDest = dist;
                                sign.direction = Utils.oppositeDirection(dir);
                            } else if (sign.stepsToDest > dist){
                                sign.stepsToDest = dist;
                                sign.direction = Utils.oppositeDirection(dir);
                                nextLocs.add(neighbor);
                            }
                        } else {
                            infoMan.tiles[neighbor.getX()][neighbor.getY()].destToDir.put(target.toString(), new Signpost(Utils.oppositeDirection(dir), dist));
                            nextLocs.add(neighbor);
                        }
                    }
                }
            }
            dist++;
            currentLocs = nextLocs;
            nextLocs = new ArrayList<MapLocation>();
        }
    }
}