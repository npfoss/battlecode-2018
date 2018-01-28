import bc.*;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;

/* [description]
any time you want to know the direction to go to a place
also exploring
important functs:

NOTE: always make sure unit doesn't need to cool down.
also, may return Direction.Center which will confuse gc.move()
    (so if it returns Center, don't move)(that's what it's telling you)
*/
/* 
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
        	//can't get there but just return distance
    		return (int)(start.distanceSquaredTo(target));
        }
        if (!infoMan.tiles[start.getX()][start.getY()].destToDir.containsKey(hashMapLoc(target))){
            generateBFSMap(target);
        }
        return infoMan.tiles[start.getX()][start.getY()].destToDir.get(hashMapLoc(target)).stepsToDest;
    }

    /*
    returns the optimal direction to reach the target,
    ignoring other units
    */
    public Direction directionTowards(MapLocation start, MapLocation target){
        if (!infoMan.tiles[start.getX()][start.getY()].destToDir.containsKey(hashMapLoc(target))){
            generateBFSMap(target);
        }
        return infoMan.tiles[start.getX()][start.getY()].destToDir.get(hashMapLoc(target)).direction;
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
    	if(infoMan.isReachable(start, target))
    		return dirToMoveSafely(start, directionTowards(start, target));
    	else
    		return dirToMoveSafely(start, start.directionTo(target));
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
    
    public Direction dirToMoveEfficient(MapLocation start, MapLocation target){
        return dirToMove(start, start.directionTo(target));
    }
    
    public Direction dirToMoveSafelyEfficient(MapLocation start, MapLocation target){
        return dirToMoveSafely(start, start.directionTo(target));
    }

    public Direction dirToExplore(MapLocation loc){
        return dirToMoveSafely(loc, Utils.orderedDirections[(int)(Math.random()*8)]);
    }

    public void generateBFSMap(MapLocation target){
        Utils.log("Generating map to " + mapLocToString(target));
        short dist = 0;
        ArrayList<List<Integer>> currentLocs = new ArrayList<List<Integer>>();
        ArrayList<List<Integer>> nextLocs = new ArrayList<List<Integer>>();
        infoMan.tiles[target.getX()][target.getY()].destToDir.put(hashMapLoc(target), new Signpost(Direction.Center, (short)0));

        currentLocs.add(Arrays.asList(target.getX(), target.getY()));
        dist++;
        Signpost sign;
        int x, y, nx, ny;
        while (currentLocs.size() > 0){
            for (List<Integer> coords: currentLocs){
                x = coords.get(0);
                y = coords.get(1);
                for (int i = 0; i < 8; i++){
                    nx = x + Utils.dx[i];
                    ny = y + Utils.dy[i];
                    if (infoMan.isOnMap(nx, ny)
                            && (infoMan.isLocationWalkable(nx, ny)
                                    || infoMan.tiles[nx][ny].myType == UnitType.Rocket
                                    || infoMan.tiles[nx][ny].myType == UnitType.Factory)){
                        if (infoMan.tiles[nx][ny].destToDir.containsKey(hashMapLoc(target))){
                            sign = infoMan.tiles[nx][ny].destToDir.get(hashMapLoc(target));
                            if (sign.stepsToDest == dist
                                    && !Utils.isDiagonalDirection(i)
                                    && Utils.isDiagonalDirection(sign.direction)){
                                // prefer to not move diagonaly if it's the same dist.
                                // may prevent bottlenecks
                                sign.stepsToDest = dist;
                                sign.direction = Utils.oppositeDirection(i);
                            } else if (sign.stepsToDest > dist){
                                sign.stepsToDest = dist;
                                sign.direction = Utils.oppositeDirection(i);
                                if(!(infoMan.tiles[nx][ny].myType == UnitType.Rocket
                                        || infoMan.tiles[nx][ny].myType == UnitType.Factory)){
                                    nextLocs.add(Arrays.asList(nx, ny));
                                }
                            }
                        } else {
                            infoMan.tiles[nx][ny].destToDir.put(hashMapLoc(target), new Signpost(Utils.oppositeDirection(i), dist));
                            if(!(infoMan.tiles[nx][ny].myType == UnitType.Rocket
                                    || infoMan.tiles[nx][ny].myType == UnitType.Factory)){
                                nextLocs.add(Arrays.asList(nx, ny));
                            }
                        }
                    }
                }
            }
            dist++;
            currentLocs = nextLocs;
            nextLocs = new ArrayList<List<Integer>>();
        }
        //Utils.log("done generating map");
    }

    public String mapLocToString(MapLocation loc){
        return coordToString(loc.getX(), loc.getY());
    }

    public String coordToString(int x, int y){
        return "" + x + "," + y;
    }

    static int hashCoords(int a, int b) {
        assert a >= 0;
        assert b >= 0;
        int sum = a + b;
        return (sum * (sum + 1) / 2) + a;
    }

    static int hashMapLoc(MapLocation loc){
        return hashCoords(loc.getX(), loc.getY());
    }

    public MapLocation getNextMarsDest(){
        return infoMan.getNextMarsDest();
    }
}
