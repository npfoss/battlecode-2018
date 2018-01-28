/****************/
/* REFACTOR ME! */
/****************/

import bc.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TreeSet;

/*
updates enemy locations and buildings (where it last saw them etc)
    and what type (number of each)
+ info about when each area was last visited (map grid)
+ tracks deaths

basically all of the shared data of the team
*/

public class InfoManager {
	GameController gc;
	Comms comms;
	Planet myPlanet;
    Team myTeam;
	MagicNumbers magicNums;
	int height, width;
	long lastCheckpoint;
	int rocketsToBeBuilt;
	int factoriesToBeBuilt;
	boolean saveMoney;
	PlanetMap startingMap;
	//int totalUnitCount;

	ArrayList<Unit> rockets;
	ArrayList<Unit> workers;
	ArrayList<Unit> factories;
	ArrayList<Unit> fighters;
	
	int workerCount;
	int fighterCount;
	
	HashSet<Integer> unassignedUnits; // *no rockets*
    ArrayList<Unit> newRockets;

	// tracking enemies
	HashSet<Integer> enemyRockets;
	HashSet<Integer> enemyWorkers;
	HashSet<Integer> enemyFactories;
	HashSet<Integer> enemyRangers;
	HashSet<Integer> enemyMages;
	HashSet<Integer> enemyHealers;
	HashSet<Integer> enemyKnights;

	//for knowing when you last saw a given enemy unit (unit id -> turn last seen)
	HashMap<Integer,Integer> enemyLastSeen;
	
	//for combat
	HashMap<Integer,TargetUnit> targetUnits;
	
	// Squads
	ArrayList<WorkerSquad> workerSquads;
	ArrayList<RocketSquad> rocketSquads;
	ArrayList<CombatSquad> combatSquads;
	HashSet<Integer> workersToRep;

	// here lies map info (mostly for nav)
    ArrayList<Region> regions;
    ArrayList<KarboniteArea> karbAreas;
    Tile[][] tiles;
    ArrayList<MapLocation> placesWeveSentTo;
    // short[][][][][] destToDir; // startx, starty, targetx, targety, [Direction, stepsToDest]

    // research
    int[] researchLevels;

	public InfoManager(GameController g, MagicNumbers mn) {
		gc = g;
		magicNums = mn;
		
		comms = new Comms(gc);

		workerSquads = new ArrayList<WorkerSquad>();
		rocketSquads = new ArrayList<RocketSquad>();
		combatSquads = new ArrayList<CombatSquad>();

		myPlanet = gc.planet();
        myTeam = gc.team();

		enemyRockets = new HashSet<Integer>();
		enemyWorkers = new HashSet<Integer>();
		enemyFactories = new HashSet<Integer>();
		enemyRangers = new HashSet<Integer>();
		enemyMages = new HashSet<Integer>();
		enemyKnights = new HashSet<Integer>();
		enemyHealers = new HashSet<Integer>();
		
		enemyLastSeen = new HashMap<Integer,Integer>();

		targetUnits = new HashMap<Integer,TargetUnit>();

        newRockets = new ArrayList<Unit>();
		
        startingMap = gc.startingMap(myPlanet);
        height = (int) startingMap.getHeight();
        width = (int) startingMap.getWidth();

        tiles = new Tile[width][height];
        // destToDir = new short[width][height][width][height][2];
        regions = new ArrayList<Region>();
        workersToRep = new HashSet<Integer>();
        karbAreas = new ArrayList<KarboniteArea>();
        initMap();
        
        placesWeveSentTo = new ArrayList<MapLocation>();
        factoriesToBeBuilt = 0;
        rocketsToBeBuilt = 0;
        
        rockets = new ArrayList<Unit>();
        workers = new ArrayList<Unit>();
        fighters = new ArrayList<Unit>();
        factories = new ArrayList<Unit>();
        unassignedUnits = new HashSet<Integer>();
        researchLevels = new int[]{0,0,0,0,0,0}; //knight, mage, ranger, healer, worker, rocket
	}

	public void update(Strategy strat) {
		//TODO: if you're mars add karbonite to tiles according to weather.
		
		lastCheckpoint = System.nanoTime();
		
		// called at the beginning of each turn
		comms.update();

		rockets.clear();
		workers.clear();
		factories.clear();
		fighters.clear();

		unassignedUnits.clear();
        newRockets.clear();
		
		targetUnits.clear();

		//updating map info
		for(int x = 0; x < tiles.length; x++){
			for(int y = 0; y < tiles[0].length; y++){
				MapLocation loc = tiles[x][y].myLoc;
				if(gc.canSenseLocation(loc)){
					tiles[x][y].roundLastUpdated = (int) gc.round();
					tiles[x][y].updateKarbonite(gc.karboniteAt(loc));
					tiles[x][y].enemiesUpdated = false;
					tiles[x][y].unitID = -1;
					tiles[x][y].isWalkable = startingMap.isPassableTerrainAt(loc) > 0;
				}
			}
		}
		
		//keeping track of our/enemy units, squad management
		//REFACTOR: while going through units, add to tiles whether or not there is a unit there so we don't have to call gc.hasUnitAtLocation;
		VecUnit units = gc.units();
		workerCount = 0;
		fighterCount = 0;
		HashSet<Integer> ids = new HashSet<Integer>();
		for (int i = 0; i < units.size(); i++) {
			Unit unit = units.get(i);
			Location loc = unit.location();
            if(loc.isInSpace()){
                continue;
            }
            if(loc.isOnMap()) {
            	int x = loc.mapLocation().getX();
            	int y = loc.mapLocation().getY();
            	//Utils.log("setting tile " + x + " " + y);
            	tiles[x][y].unitID = unit.id();
            	tiles[x][y].myType = unit.unitType();
            	if(unit.unitType() == UnitType.Factory || unit.unitType() == UnitType.Rocket)
            		tiles[x][y].isWalkable = false;
            }
			if(unit.team() == myTeam){
				ids.add(unit.id());
				switch (unit.unitType()) {
				case Worker:
					workers.add(unit);
					if(loc.isOnMap())
						workerCount++;
					if (!isInSquads(unit)){
						unassignedUnits.add(unit.id());
					}
					break;
				case Factory:
					factories.add(unit);
					break;
				case Rocket:
					rockets.add(unit);
					if (!isInSquads(unit))
						newRockets.add(unit);
					break;
				default:
					fighters.add(unit);
					if(loc.isOnMap())
						fighterCount++;
					if (!isInSquads(unit)){
						unassignedUnits.add(unit.id());
					}
					break;
				}
			} else {
				addEnemyUnit(unit.id(), unit.unitType());
				enemyLastSeen.put(unit.id(), (int) gc.round());
				if(!unit.location().isOnMap())
					continue;
				targetUnits.put(unit.id(), new TargetUnit(unit, this));
			}
		}
		units.delete();

		//check for dead units + remove from squads
		for(Squad s: workerSquads){
			for(int i = s.units.size()-1; i >= 0; i--){
				int id = s.units.get(i);
				if(!ids.contains(id)){
					s.removeUnit(id);
				}
			}
			s.update();
		}

		for(Squad s: rocketSquads){
			for(int i = s.units.size()-1; i >= 0; i--){
				int id = s.units.get(i);
				if(!ids.contains(id)){
					s.removeUnit(id);
				}
			}
			// s.update(); // rocket squads get updated by rocketMan anyways
		}

		for(CombatSquad s: combatSquads){
			for(int i = s.units.size()-1; i >= 0; i--){
				int id = s.units.get(i);
				if(!ids.contains(id)){
					s.removeUnit(id);
				}
			}
			s.update();
		}
		
		logTimeCheckpoint("infoMan update done");
	}

	private void addEnemyUnit(int ID, UnitType ut){
		switch (ut){
		case Rocket: enemyRockets.add(ID);
		case Factory: enemyFactories.add(ID);
		case Worker: enemyWorkers.add(ID);
		case Ranger: enemyRangers.add(ID);
		case Mage: enemyMages.add(ID);
		case Knight: enemyKnights.add(ID);
		case Healer: enemyHealers.add(ID);
		}	
	}

	//if you're about to deal the finishing blow to a unit, remove it from our tracking
	//necessary to do by ID because Unit object changes every turn
	public void removeEnemyUnit(int ID, UnitType ut){
		switch (ut){
		case Rocket: enemyRockets.remove(ID);
		case Factory: enemyFactories.remove(ID);
		case Worker: enemyWorkers.remove(ID);
		case Ranger: enemyRangers.remove(ID);
		case Mage: enemyMages.remove(ID);
		case Knight: enemyKnights.remove(ID);
		case Healer: enemyHealers.remove(ID);
		}	
	}

    public boolean isInSquads(Unit unit){
        return getSquad(unit) != null;
    }
	public boolean isInSquads1(Unit unit, ArrayList<WorkerSquad> squad) {
		for (Squad s : squad) {
			for (int uid : s.units) {
				if (unit.id() == uid){
					return true;
				}
			}
		}
		return false;
	}
	public boolean isInSquads2(Unit unit, ArrayList<RocketSquad> squad) {
		for (Squad s : squad) {
			for (int uid : s.units) {
				if (unit.id() == uid){
					return true;
				}
			}
		}
		return false;
	}
	public boolean isInSquads3(Unit unit, ArrayList<CombatSquad> squad) {
		for (Squad s : squad) {
			for (int uid : s.units) {
				if (unit.id() == uid){
					return true;
				}
			}
		}
		return false;
	}

    public Squad getSquad(Unit unit){
        if (!unassignedUnits.contains(unit.id())){
            Squad s;
            switch(unit.unitType()){
                case Rocket: return getSquad2(unit, rocketSquads);
                case Worker:
                    s = getSquad2(unit, rocketSquads);
                    return s == null ? getSquad1(unit, workerSquads) : s;
                case Ranger:
                case Mage:
                case Knight:
                case Healer:
                    s = getSquad2(unit, rocketSquads);
                    return s == null ? getSquad3(unit, combatSquads) : s;
            }
        }
        return null;
    }

    public Squad getSquad1(Unit unit, ArrayList<WorkerSquad> squad){
        for (Squad s : squad) {
            for (int uid : s.units) {
                if (unit.id() == uid){
                    return s;
                }
            }
        }
        return null;
    }
    public Squad getSquad2(Unit unit, ArrayList<RocketSquad> squad){
        for (Squad s : squad) {
            for (int uid : s.units) {
                if (unit.id() == uid){
                    return s;
                }
            }
        }
        return null;
    }
    public Squad getSquad3(Unit unit, ArrayList<CombatSquad> squad){
        for (Squad s : squad) {
            for (int uid : s.units) {
                if (unit.id() == uid){
                    return s;
                }
            }
        }
        return null;
    }

    public TreeSet<TargetUnit> getTargetUnits(MapLocation ml, int radius, boolean hostileOnly){
        TreeSet<TargetUnit> ret = new TreeSet<TargetUnit>(new descendingPriorityComp());
        for(TargetUnit tu: targetUnits.values()){
            if((!hostileOnly || Utils.isTypeHostile(tu.type)) && tu.myLoc.distanceSquaredTo(ml) <= radius)
                ret.add(tu);
        }
        return ret;
    }
    
    public int distToHostile(MapLocation ml){
    	TreeSet<TargetUnit> tus = getTargetUnits(ml,150,true);
    	int closest = 150;
    	for(TargetUnit tu: tus){
    		if(ml.distanceSquaredTo(tu.myLoc) < closest){
    			closest = (int) ml.distanceSquaredTo(tu.myLoc);
    		}
    	}
    	return closest;
    }

/******** Map related functions below this line *******/
    
    // initializes all the Tile and Region stuff
    public void initMap(){
        for (int x = 0; x < tiles.length; x++){
            for (int y = 0; y < tiles[0].length; y++){
                if (tiles[x][y] == null){
                    // it hasn't been initialized yet (or isn't passable)
                    MapLocation loc = new MapLocation(myPlanet, x, y);
                    if (startingMap.isPassableTerrainAt(loc) > 0){
                        // new region! floodfill it
                        Region newRegion = new Region();
                        floodfill(newRegion, loc);
                        regions.add(newRegion);
                    } else {
                        // impassible terrain
                        tiles[x][y] = new Tile(false, startingMap.initialKarboniteAt(loc), null, loc, this, null);
                    }
                }
            }
        }
    }

    // takes a passable maplocation, adds it and everything reachable
    //      from it to the given region
    public void floodfill(Region region, MapLocation l){
    	Queue<MapLocation> q = new LinkedList<MapLocation>();
    	q.add(l);
    	while(!q.isEmpty()){
    		MapLocation loc = q.poll();
    		if(tiles[loc.getX()][loc.getY()] != null)
    			continue;
	        long karbs = startingMap.initialKarboniteAt(loc);
	        KarboniteArea karbArea = null;
	        if(karbs > 0)
	        	karbArea = getKarbArea(loc, region);
	        tiles[loc.getX()][loc.getY()] = new Tile(true, karbs, region, loc, this, karbArea);
	        if(karbArea != null) {
	        	karbArea.addTile(tiles[loc.getX()][loc.getY()]);
	        	//Utils.log("adding " + loc + " to an area.");
	        }
	        region.tiles.add(tiles[loc.getX()][loc.getY()]);
	        region.karbonite += karbs;
	        for (Direction dir : Utils.orderedDirections){
	            MapLocation neighbor = loc.add(dir);
	            if (isOnMap(neighbor)
	                    && tiles[neighbor.getX()][neighbor.getY()] == null
	                    && startingMap.isPassableTerrainAt(neighbor) > 0){
	            	//Utils.log("adding " + neighbor.getX() + " " + neighbor.getY());
	                q.add(neighbor);
	            }
	        }
    	}
    }
    
    /* old one that maybe works better? for debugging
    public void floodfill(PlanetMap startingMap, Region region, MapLocation loc){
    	Utils.log("x = " + loc.getX() + " y = " + loc.getY());
        long karbs = startingMap.initialKarboniteAt(loc);
        KarboniteArea karbArea = null;
        if(karbs > 0)
        	karbArea = getKarbArea(loc, region);
        tiles[loc.getX()][loc.getY()] = new Tile(true, karbs, region, loc, this, karbArea);
        if(karbArea != null) {
        	karbArea.addTile(tiles[loc.getX()][loc.getY()]);
        	//Utils.log("adding " + loc + " to an area.");
        }
        region.tiles.add(tiles[loc.getX()][loc.getY()]);
        region.karbonite += karbs;

        // now floodfill
        for (Direction dir : Utils.orderedDirections){
            MapLocation neighbor = loc.add(dir);
            if (isOnMap(neighbor)
                    && tiles[neighbor.getX()][neighbor.getY()] == null
                    && startingMap.isPassableTerrainAt(neighbor) > 0){
                floodfill(startingMap, region, neighbor);
            }
        }
    }*/

    public KarboniteArea getKarbArea(MapLocation loc, Region r) {
		for(KarboniteArea kA: karbAreas){
			if(kA.tiles.size() == 0 || r != kA.tiles.get(0).region)
				continue;
			if(kA.hasTileWithinDistance(loc, MagicNumbers.KARB_SEPARATION_DISTANCE)){
				return kA;
			}
		}
		KarboniteArea kA = new KarboniteArea(this);
		karbAreas.add(kA);
		return kA;
	}

    public boolean isOnMap(int x, int y){
        return 0 <= x && 0 <= y && x < width && y < height;
    }

    public boolean isOnMap(MapLocation loc){
        return isOnMap(loc.getX(), loc.getY());
    }

    // this means on map, walkable, AND no unit currently in the way
    // returns false if we can't see that loc
    public boolean isLocationClear(MapLocation loc){
        return isLocationWalkable(loc) && tiles[loc.getX()][loc.getY()].unitID == -1 && gc.canSenseLocation(loc);
    }

    public boolean isLocationClear(int x, int y){
        return isLocationWalkable(x, y) && tiles[x][y].unitID == -1 && gc.canSenseLocation(tiles[x][y].myLoc);
    }

    // means on the map, passable terrain, and none of our buildings there
    public boolean isLocationWalkable(MapLocation loc) {
        return isLocationWalkable(loc.getX(), loc.getY());
    }

    public boolean isLocationWalkable(int x, int y) {
        return isOnMap(x, y) && tiles[x][y].isWalkable;
    }

    // are two map locations reachable from each other? if in same region
    public boolean isReachable(MapLocation loc1, MapLocation loc2){
        // may need to turn on commented bits if we accidentally
        //      check illegal locs sometimes
        return /*isOnMap(loc1) && isOnMap(loc2) &&*/ tiles[loc1.getX()][loc1.getY()].region == tiles[loc2.getX()][loc2.getY()].region;
    }

    public MapLocation getNextMarsDest(){
        if (myPlanet == Planet.Earth){
            // TODO: check comms for what Mars is saying

        } else {
            // TODO: calculate something intelligent, send it to earth

        }
        // TODO: what if mars has nowhere to land???
        
        // for now :(
        // TODO: remove
        long bestDist = -1;
        PlanetMap marsStart = gc.startingMap(Planet.Mars);
        MapLocation bestloc = null;
        int numChecked = 0;
        int x,y;
        while(numChecked < 7){
        	// Utils.log("checking x = " + x + " y = " + y);
        	x = (int)(Math.random()*(marsStart.getWidth()));
        	y = (int)(Math.random()*(marsStart.getHeight()));
        	MapLocation loc = new MapLocation(Planet.Mars, x, y);
        	if (marsStart.isPassableTerrainAt(loc) > 0){
        		numChecked++;
        		long minDist = 10000;
        		for(MapLocation l: placesWeveSentTo){
        			if(l.distanceSquaredTo(loc) < minDist){
        				minDist = l.distanceSquaredTo(loc);
        			}
        		}
        		if(minDist > bestDist){
        			bestDist = minDist;
        			bestloc = loc;
        		}
        	}
        }
        placesWeveSentTo.add(bestloc);
        return bestloc;
    }

    public void moveAndUpdate(int id, Direction d, UnitType type) {
    	if(d == Direction.Center)
    		return;
    	MapLocation start = gc.unit(id).location().mapLocation();
    	gc.moveRobot(id, d);
    	tiles[start.getX()][start.getY()].unitID = -1;
    	//Utils.log("unsetting " + start.getX() + " " + start.getY());
    	MapLocation end = start.add(d);
    	tiles[end.getX()][end.getY()].unitID = id;
    	tiles[end.getX()][end.getY()].myType = type;
    }
    
    public Double distToClosestKarbonite(MapLocation loc) {
    	long minDist = 1000000;
    	KarboniteArea closest = null;
    	for(KarboniteArea kA: karbAreas){
    		if(kA.center.distanceSquaredTo(loc) < minDist){
    			minDist = kA.center.distanceSquaredTo(loc);
    			closest = kA;
    		}
    	}
    	if(closest == null)
    		return null;
    	return (double) closest.getClosestTile(loc).myLoc.distanceSquaredTo(loc);
    }
    
    public MapLocation getClosestKarbonite(MapLocation loc){
    	long minDist = 1000000;
    	KarboniteArea closest = null;
    	for(KarboniteArea kA: karbAreas){
    		if(kA.tiles.size() > 0 && kA.center.distanceSquaredTo(loc) < minDist && isReachable(loc,kA.tiles.get(0).myLoc)){
    			minDist = kA.center.distanceSquaredTo(loc);
    			closest = kA;
    		}
    	}
    	if(closest == null)
    		return null;
    	//Utils.log("closest karb is in area with center " + closest.center);
    	return closest.getClosestTile(loc).myLoc;
    }
    
/*******  FOR LOGGING AND DEBUGGING *********/
    
    public void logTimeCheckpoint(String identifier){
    	long duration = System.nanoTime() - lastCheckpoint;
    	lastCheckpoint = System.nanoTime();
    	//Utils.log(identifier + ": " + duration + " ns since last checkpoint.");
    }

    public boolean isReachable(int x, int y, MapLocation loc) {
        return isOnMap(x,y) && isOnMap(loc) && tiles[x][y].region == tiles[loc.getX()][loc.getY()].region;
    }

}
