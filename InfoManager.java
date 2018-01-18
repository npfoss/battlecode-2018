import bc.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

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

	ArrayList<Unit> rockets;
	ArrayList<Unit> workers;
	ArrayList<Unit> factories;
	ArrayList<Unit> fighters;

	ArrayList<Unit> unassignedUnits;

	// tracking enemies
	ArrayList<Unit> enemyRockets;
	ArrayList<Unit> enemyWorkers;
	ArrayList<Unit> enemyFactories;
	ArrayList<Unit> enemyRangers;
	ArrayList<Unit> enemyMages;
	ArrayList<Unit> enemyHealers;
	ArrayList<Unit> enemyKnights;

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

	public InfoManager(GameController g, MagicNumbers mn) {
		gc = g;
		magicNums = mn;
		
		comms = new Comms(gc);

		workerSquads = new ArrayList<WorkerSquad>();
		rocketSquads = new ArrayList<RocketSquad>();
		combatSquads = new ArrayList<CombatSquad>();

		myPlanet = gc.planet();

		enemyRockets = new ArrayList<Unit>();
		enemyWorkers = new ArrayList<Unit>();
		enemyFactories = new ArrayList<Unit>();
		enemyRangers = new ArrayList<Unit>();
		enemyMages = new ArrayList<Unit>();
		enemyKnights = new ArrayList<Unit>();
		enemyHealers = new ArrayList<Unit>();
		
		enemyLastSeen = new HashMap<Integer,Integer>();

		targetUnits = new HashMap<Integer,TargetUnit>();
		
        int height = (int) gc.startingMap(myPlanet).getHeight();
        int width = (int) gc.startingMap(myPlanet).getWidth();

        tiles = new Tile[width][height];
        regions = new ArrayList<Region>();
        initMap();
	}

	public void update() {
		// called at the beginning of each turn
		comms.update();

		rockets = new ArrayList<Unit>();
		workers = new ArrayList<Unit>();
		factories = new ArrayList<Unit>();
		fighters = new ArrayList<Unit>();

		unassignedUnits = new ArrayList<Unit>();
		
		targetUnits.clear();

		//keeping track of our/enemy units, squad management
		VecUnit units = gc.units();
		HashSet<Integer> ids = new HashSet<Integer>();
		for (int i = 0; i < units.size(); i++) {
			Unit unit = units.get(i);
			if(unit.team() == gc.team()){
				ids.add(unit.id());
				switch (unit.unitType()) {
				case Worker:
					workers.add(unit);
					if (!isInSquads1(unit, workerSquads) && !isInSquads2(unit, rocketSquads))
						unassignedUnits.add(unit);
					break;
				case Factory:
					factories.add(unit);
					break;
				case Rocket:
					rockets.add(unit);
					if (!isInSquads2(unit, rocketSquads))
						unassignedUnits.add(unit);
					break;
				default:
					fighters.add(unit);
					if (!isInSquads3(unit, combatSquads) && !isInSquads2(unit,rocketSquads))
						unassignedUnits.add(unit);
					break;
				}
			}
			else{
				addEnemyUnit(unit);
				enemyLastSeen.put(unit.id(),(int) gc.round());
				TargetUnit tu = new TargetUnit(unit.id(),unit.health(),unit.damage(),unit.location().mapLocation(),unit.unitType());
				targetUnits.put(unit.id(), tu);
			}
		}

		//check for dead units + remove from squads
		for(Squad s: workerSquads){
			for(int i = s.units.size()-1; i >= 0; i--){
				int id = s.units.get(i);
				if(!ids.contains(id)){
					s.units.remove(i);
				}
			}
			s.update();
		}

		for(Squad s: rocketSquads){
			for(int i = s.units.size()-1; i >= 0; i--){
				int id = s.units.get(i);
				if(!ids.contains(id)){
					s.units.remove(i);
				}
			}
			s.update();
		}

		for(CombatSquad s: combatSquads){
			for(int i = s.units.size()-1; i >= 0; i--){
				int id = s.units.get(i);
				if(!ids.contains(id)){
					s.units.remove(i);
					if(s.separatedUnits.contains(id))
						s.separatedUnits.remove(s.separatedUnits.indexOf(id));
					if(s.swarmUnits.contains(id))
						s.swarmUnits.remove(s.swarmUnits.indexOf(id));
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
                    // TODO: check if there's now a factory there
                    //      (to update walkability)
                }
			}
		}
	}

	private void addEnemyUnit(Unit unit){
		switch (unit.unitType()){
		case Rocket: enemyRockets = updateUnit(enemyRockets,unit);
		case Factory: enemyFactories = updateUnit(enemyFactories,unit);
		case Worker: enemyWorkers = updateUnit(enemyWorkers,unit);
		case Ranger: enemyRangers = updateUnit(enemyRangers,unit);
		case Mage: enemyMages = updateUnit(enemyMages,unit);
		case Knight: enemyKnights = updateUnit(enemyKnights,unit);
		case Healer: enemyHealers = updateUnit(enemyHealers,unit);
		}	
	}

	//if you're about to deal the finishing blow to a unit, remove it from our tracking
	//necessary to do by ID because Unit object changes every turn
	public void removeEnemyUnit(Unit unit){
		switch (unit.unitType()){
		case Rocket: enemyRockets = removeByID(enemyRockets,unit.id());
		case Factory: enemyFactories = removeByID(enemyFactories,unit.id());
		case Worker: enemyWorkers = removeByID(enemyWorkers,unit.id());
		case Ranger: enemyRangers = removeByID(enemyRangers,unit.id());
		case Mage: enemyMages = removeByID(enemyMages,unit.id());
		case Knight: enemyKnights = removeByID(enemyKnights,unit.id());
		case Healer: enemyHealers = removeByID(enemyHealers,unit.id());
		}	
	}

	private ArrayList<Unit> removeByID(ArrayList<Unit> al, int id){
		al.remove(gc.unit(id));
		return al;
	}

	private ArrayList<Unit> updateUnit(ArrayList<Unit> al, Unit unit){
		Unit toRemove = unit;
		boolean remove = false;
		for(Unit u: al){
			if(u.id()==unit.id()){
				remove = true;
				toRemove = u;
				break;
			}
		}
		if(remove) al.remove(toRemove);
		al.add(unit);
		return al;
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
}