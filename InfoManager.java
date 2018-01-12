import bc.*;
import java.util.ArrayList;

/* please edit this it's just late at night
 updates enemy locations and buildings (where it last saw them etc)
    and what type (number of each)
+ info about when each area was last visited (map grid)
+ tracks deaths
 */
public class InfoManager{
	GameController gc;
	Comms comms;

	ArrayList<Unit> rockets;
	ArrayList<Unit> workers;
	ArrayList<Unit> factories;
	ArrayList<Unit> fighters;

	ArrayList<Unit> unassignedUnits;

	// TODO: should probably track visible enemies too

	//Squads
	ArrayList<WorkerSquad> workerSquads;
	ArrayList<RocketSquad> rocketSquads;
	ArrayList<CombatSquad> combatSquads;

	// here lies map info (mostly for nav)


	public InfoManager(GameController g){
		gc = g;

		comms = new Comms(gc);

		workerSquads = new ArrayList<WorkerSquad>();
		rocketSquads = new ArrayList<RocketSquad>();
		combatSquads = new ArrayList<CombatSquad>();
	}

	public void update(){
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
			switch(unit.unitType()) {
			case Worker:
				workers.add(unit);
				if(!isInSquads(unit,workerSquads) && !isInSquads(unit,rocketSquads))
					unassignedUnits.add(unit);
				break;
			case Factory:
				factories.add(unit);
				break;
			default:
				fighters.add(unit);
				if(!isInSquads(unit,combatSquads))
					break;
			}
		}
	}
	// update arraylists of units, make sure squads don't have dead units, etc

}
public boolean isInSquads(Unit unit, ArrayList<Squad> squads) {
	for(Squad s : squads) {
		for(Unit u : s.units) {
			if(unit.id() == u.id())
				return true;
		}
	}
	return false;
}
}