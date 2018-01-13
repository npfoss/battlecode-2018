import bc.*;
import java.util.ArrayList;
/* please edit this it's just late at night
 updates enemy locations and buildings (where it last saw them etc)
    and what type (number of each)
+ info about when each area was last visited (map grid)
+ tracks deaths
 */
public class InfoManager {
	GameController gc;
	Comms comms;

	ArrayList<Unit> rockets;
	ArrayList<Unit> workers;
	ArrayList<Unit> factories;
	ArrayList<Unit> fighters;

	ArrayList<Unit> unassignedUnits;

	// TODO: should probably track visible enemies too

	// Squads
	ArrayList<WorkerSquad> workerSquads;
	ArrayList<RocketSquad> rocketSquads;
	ArrayList<CombatSquad> combatSquads;

	// here lies map info (mostly for nav)


	public InfoManager(GameController g) {
		gc = g;

		comms = new Comms(gc);

		workerSquads = new ArrayList<WorkerSquad>();
		rocketSquads = new ArrayList<RocketSquad>();
		combatSquads = new ArrayList<CombatSquad>();
	}

	public void update() {
		// called at the beginning of each turn
		comms.update();

		rockets = new ArrayList<Unit>();
		workers = new ArrayList<Unit>();
		factories = new ArrayList<Unit>();
		fighters = new ArrayList<Unit>();

		unassignedUnits = new ArrayList<Unit>();

		VecUnit units = gc.myUnits();
		for (int i = 0; i < units.size(); i++) {
			Unit unit = units.get(i);
			switch (unit.unitType()) {
			case Worker:
				workers.add(unit);
				if (!isInSquads1(unit, workerSquads) && !isInSquads2(unit, rocketSquads))
					unassignedUnits.add(unit);
				break;
			case Factory:
				factories.add(unit);
				break;
			default:
				fighters.add(unit);
				if (!isInSquads3(unit, combatSquads))
					break;
			}
		}
	}
	// update arraylists of units, make sure squads don't have dead units, etc

	public boolean isInSquads1(Unit unit, ArrayList<WorkerSquad> squad) {
		for (Squad s : squad) {
			for (Unit u : s.units) {
				if (unit.id() == u.id())
					return true;
			}
		}
		return false;
	}
	public boolean isInSquads2(Unit unit, ArrayList<RocketSquad> squad) {
		for (Squad s : squad) {
			for (Unit u : s.units) {
				if (unit.id() == u.id())
					return true;
			}
		}
		return false;
	}
	public boolean isInSquads3(Unit unit, ArrayList<CombatSquad> squad) {
		for (Squad s : squad) {
			for (Unit u : s.units) {
				if (unit.id() == u.id())
					return true;
			}
		}
		return false;
	}



/******** Map related functions below this line *******/
    // this means on map, walkable, AND no unit currently in the way
    // returns false if we can't see that loc
    public boolean isLocationClear(MapLocation loc){
        try{
            System.out.println("" + loc.getX() + ", " + loc.getY() + " " + isLocationWalkable(loc) + " " + (gc.isOccupiable(loc) > 0));
            // return isLocationWalkable(loc);// && !gc.hasUnitAtLocation(loc); //TODO THIS IS BROKEN IN THE API :(((((
            return isLocationWalkable(loc) && gc.isOccupiable(loc) > 0;
        } catch (Exception e) {
            System.out.println("this happened");
            return false;
        }
    }

    // means on the map, passable terrain, and none of our buildings there
    public boolean isLocationWalkable(MapLocation loc) throws Exception {
        // TODO: make it not throw errors when tile isn't visible (need mapinfo)
        // TODO: add the part about building (requires caching map info)
        // System.out.println(gc.startingMap(gc.planet()).isPassableTerrainAt(loc));
        return gc.startingMap(gc.planet()).isPassableTerrainAt(loc) > 0;
    }
}