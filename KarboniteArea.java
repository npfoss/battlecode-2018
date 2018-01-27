import bc.*;
import java.util.ArrayList;

/*
just a data structure for storing region data
*/
public class KarboniteArea{
    ArrayList<Tile> tiles;
    MapLocation center;
    long karbonite;
    InfoManager infoMan;

    public KarboniteArea(InfoManager im){
        tiles = new ArrayList<Tile>();
        karbonite = 0;
        infoMan = im;
    }
    
    public void addTile(Tile t){
    	tiles.add(t);
    	karbonite += t.karbonite;
    	updateCenter();
    }
    
    public void removeTile(Tile t){
    	tiles.remove(t);
    	updateCenter();
    }
    
    private void updateCenter(){
    	if(tiles.size() == 0){
    		center = null;
    		return;
    	}
    	double xsum = 0;
    	double ysum = 0;
    	for(Tile t: tiles){
    		xsum += t.x;
    		ysum += t.y;
    	}
    	center = new MapLocation(infoMan.myPlanet,(int)(xsum/tiles.size()),(int)(ysum/tiles.size()));
    	infoMan.tiles[center.getX()][center.getY()].karbArea = this;
    }
    
    public boolean hasTileWithinDistance(MapLocation loc, long dist){
    	for(Tile t: tiles){
    		if(t.myLoc.distanceSquaredTo(loc) <= dist)
    			return true;
    	}
    	return false;
    }
    
    public Tile getClosestTile(MapLocation loc){
    	Tile closestTile = null;
    	long minDist = 100000;
    	for(Tile t: tiles){
    		if(t.myLoc.distanceSquaredTo(loc) < minDist){
    			minDist = t.myLoc.distanceSquaredTo(loc);
    			closestTile = t;
    		}
    	}
    	return closestTile;
    }
    
}
