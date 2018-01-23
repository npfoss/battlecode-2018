import bc.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

/* please edit this it's just late at night
 updates enemy locations and buildings (where it last saw them etc)
    and what type (number of each)
+ info about when each area was last visited (map grid)
+ tracks deaths
 */
public class InfoManager {
	GameController gc;
	Comms comms;
	Planet myPlanet;
	MagicNumbers magicNums;
	int height, width;
	long lastCheckpoint;
	boolean builtRocket;
	//int totalUnitCount;

	ArrayList<Unit> rockets;
	ArrayList<Unit> workers;
	ArrayList<Unit> factories;
	ArrayList<Unit> fighters;

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

	// here lies map info (mostly for nav)
    ArrayList<Region> regions;
    Tile[][] tiles;
    int marsx, marsy; // TODO: don't use this system, it sucks

	public InfoManager(GameController g, MagicNumbers mn) {
		gc = g;
		magicNums = mn;
		
		comms = new Comms(gc);

		workerSquads = new ArrayList<WorkerSquad>();
		rocketSquads = new ArrayList<RocketSquad>();
		combatSquads = new ArrayList<CombatSquad>();

		myPlanet = gc.planet();

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
		
        height = (int) gc.startingMap(myPlanet).getHeight();
        width = (int) gc.startingMap(myPlanet).getWidth();

        tiles = new Tile[width][height];
        regions = new ArrayList<Region>();
        initMap();

        marsx = 0;
        marsy = 0;
        builtRocket = true;
	}

	public void update(Strategy strat) {
		lastCheckpoint = System.nanoTime();
		
		if(gc.round() == strat.nextRocketBuild)
			builtRocket = false;
		
		// called at the beginning of each turn
		comms.update();

		rockets = new ArrayList<Unit>();
		workers = new ArrayList<Unit>();
		factories = new ArrayList<Unit>();
		fighters = new ArrayList<Unit>();

		unassignedUnits = new HashSet<Integer>();
        newRockets.clear();
		
		targetUnits.clear();

		//keeping track of our/enemy units, squad management
		VecUnit units = gc.units();
		HashSet<Integer> ids = new HashSet<Integer>();
		for (int i = 0; i < units.size(); i++) {
			Unit unit = units.get(i);
            if(unit.location().isInSpace()) continue;
			if(unit.team() == gc.team()){
				ids.add(unit.id());
				switch (unit.unitType()) {
				case Worker:
					workers.add(unit);
					if (!isInSquads1(unit, workerSquads) && !isInSquads2(unit, rocketSquads))
						unassignedUnits.add(unit.id());
					break;
				case Factory:
					factories.add(unit);
					break;
				case Rocket:
					rockets.add(unit);
                    Utils.log("THERE IS A ROCKET!");
                    builtRocket = true;
					if (!isInSquads2(unit, rocketSquads))
						newRockets.add(unit);
					break;
				default:
					fighters.add(unit);
					if (!isInSquads3(unit, combatSquads) && !isInSquads2(unit,rocketSquads))
						unassignedUnits.add(unit.id());
					break;
				}
			} else {
				addEnemyUnit(unit.id(),unit.unitType());
				enemyLastSeen.put(unit.id(),(int) gc.round());
				if(!unit.location().isOnMap())
					continue;
				long defense = 0;
				if(unit.unitType() == UnitType.Knight)
					defense = unit.knightDefense();
				int damage = 0;
				long range = 0;
				if(unit.unitType() != UnitType.Factory && unit.unitType() != UnitType.Rocket){
					damage = unit.damage();
					range = unit.attackRange();
				}
				TargetUnit tu = new TargetUnit(unit.id(),unit.health(),damage,
						unit.location().mapLocation(),unit.unitType(),range,defense, this);
				targetUnits.put(unit.id(), tu);
			}
		}

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

		//updating map info
		for(int x = 0; x < tiles.length; x++){
			for(int y = 0; y < tiles[0].length; y++){
				MapLocation loc = tiles[x][y].myLoc;
				if(gc.canSenseLocation(loc)){
					tiles[x][y].roundLastUpdated = (int) gc.round();
                    tiles[x][y].updateKarbonite(gc.karboniteAt(loc));
                    tiles[x][y].enemiesUpdated = false;
                    tiles[x][y].containsUpdated = false;
                    // TODO: check if there's now a factory there
                    //      (to update walkability)
                }
			}
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

/******** Map related functions below this line *******/
    
    // initializes all the Tile and Region stuff
    public void initMap(){
        PlanetMap startingMap = gc.startingMap(myPlanet);
        for (int x = 0; x < tiles.length; x++){
            for (int y = 0; y < tiles[0].length; y++){
                if (tiles[x][y] == null){
                    // it hasn't been initialized yet (or isn't passable)
                    MapLocation loc = new MapLocation(myPlanet, x, y);
                    if (startingMap.isPassableTerrainAt(loc) > 0){
                        // new region! floodfill it
                        Region newRegion = new Region();
                        floodfill(startingMap, newRegion, loc);
                        regions.add(newRegion);
                    } else {
                        // impassible terrain
                        tiles[x][y] = new Tile(x, y, false, startingMap.initialKarboniteAt(loc), null, loc, magicNums, this);
                    }
                }
            }
        }
    }

    // takes a passable maplocation, adds it and everything reachable
    //      from it to the given region
    public void floodfill(PlanetMap startingMap, Region region, MapLocation loc){
        long karbs = startingMap.initialKarboniteAt(loc);
        tiles[loc.getX()][loc.getY()] = new Tile(loc.getX(), loc.getY(), true, karbs, region, loc, magicNums, this);
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
    }

    public boolean isOnMap(int x, int y){
        return 0 <= x && 0 <= y && x < tiles.length && y < tiles[0].length;
    }

    public boolean isOnMap(MapLocation loc){
        return isOnMap(loc.getX(), loc.getY());
    }

    // this means on map, walkable, AND no unit currently in the way
    // returns false if we can't see that loc
    public boolean isLocationClear(MapLocation loc){
        try{
            return isLocationWalkable(loc) && gc.isOccupiable(loc) > 0;
        } catch (Exception e) {
            System.out.println("isLocationClear threw Exception. help");
            e.printStackTrace(System.out);
            return false;
        }
    }

    // means on the map, passable terrain, and none of our buildings there
    public boolean isLocationWalkable(MapLocation loc) {
        return isOnMap(loc) && tiles[loc.getX()][loc.getY()].isWalkable;
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
        PlanetMap startingMap = gc.startingMap(Planet.Mars);
        MapLocation bestloc = null;
        for (int x = marsx; x < startingMap.getWidth(); x++){
            for (int y = marsy; y < startingMap.getHeight(); y++){
                Utils.log("checking x = " + x + " y = " + y);
                MapLocation loc = new MapLocation(Planet.Mars, x, y);
                if (startingMap.isPassableTerrainAt(loc) > 0){
                    bestloc = loc;
                    if (x <= marsx && marsy <= y){
                        break;
                    }
                }
            }
        }
        // so we don't land in the same place twice (unless we run out)
        try{
            marsx = bestloc.getX();
            marsy = bestloc.getY() + 1;
            if (marsy == startingMap.getHeight()){
                marsy = 0;
                marsx++;
            }
            if (marsx == startingMap.getWidth()){
                marsx = 0;
            }
        } catch (Exception e) {
            // cry, mars is impassible
        }
        
        Utils.log("marsx = " + marsx + " marsy = " + marsy);

        return bestloc;
    }


/*******  FOR LOGGING AND DEBUGGING *********/
    
    public void logTimeCheckpoint(String identifier){
    	long duration = System.nanoTime() - lastCheckpoint;
    	lastCheckpoint = System.nanoTime();
    	// Utils.log(identifier + ": " + duration + " ns since last checkpoint.");
    }
}