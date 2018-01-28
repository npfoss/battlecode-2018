/****************/
/* REFACTOR ME! */
/****************/

import java.util.ArrayList;

import bc.*;

/*controls:
 * research order
 * combatsquad composition
 * when to go to mars
 * what to send to mars
 * how many workers,factories to create
 */
public class Strategy{
	InfoManager infoMan;
	GameController gc;
	UnitType[] researchOrder;
	int[] combatComposition;
	int rocketsToBuild;
	int[] rocketComposition;
    static int[] defaultRocketComposition = {0, 0, 4, 2, 2};
                            // knight, mage, ranger, healer, worker
	MapLocation[] rocketLaunchLocations;
    int rocketLaunchFrequency; // (# rounds between each)
	int minWorkers;
    int maxWorkers;
	int maxFactories;
	int minFactories;
	int rocketsBuilt;
	boolean takeAnyUnit;
	Nav nav;

    public Strategy(InfoManager im, GameController g, Nav n){
    	infoMan = im;
    	gc = g;
    	nav = n;
    	determineInitalStrat();
    	rocketsBuilt = 0;
    }

	private void determineInitalStrat() {
		//TODO: make this depend on stuff
		researchOrder = new UnitType[]{UnitType.Ranger,UnitType.Worker,UnitType.Healer,UnitType.Healer,UnitType.Healer,UnitType.Rocket,UnitType.Ranger,UnitType.Ranger};
		if(infoMan.myPlanet == Planet.Mars)
			return;
		int rushDist = -1;
		VecUnit vu = gc.startingMap(infoMan.myPlanet).getInitial_units();
		ArrayList<Unit> ourStarts = new ArrayList<Unit>();
		ArrayList<Unit> theirStarts = new ArrayList<Unit>();
		for(int i = 0; i < vu.size(); i++){
			Unit u = vu.get(i);
			if(u.team() == gc.team()){
				ourStarts.add(u);
			}
			else{
				theirStarts.add(u);
			}
		}
		for(Unit u: ourStarts){
			int minDist = 10000;
			for(Unit u2: theirStarts){
				MapLocation ml1 = u.location().mapLocation();
				MapLocation ml2 = u2.location().mapLocation();
				if(infoMan.isReachable(ml1, ml2) && nav.optimalStepsTo(ml1,ml2) < minDist){
					minDist = nav.optimalStepsTo(ml1, ml2);
				}
			}
			if(minDist < 10000 && minDist > rushDist)
				rushDist = minDist;
		}
		combatComposition = new int[]{0, 0, 3, 2}; //knight,mage,ranger,healer
        rocketComposition = defaultRocketComposition;
        rocketsToBuild = 0;
        maxFactories = 1;
        minFactories = 0;
        minWorkers = 3;
        maxWorkers = rushDist;
        takeAnyUnit = false;
	}

	public void update(){
		if(infoMan.myPlanet == Planet.Mars)
			return;
		if(gc.round() > MagicNumbers.SEND_EVERYTHING) {
			//Pack ur bags we gonna go to mars cuz earth is flooding and we dont wanna die
			rocketComposition = new int[]{0,0,5,2,1};
			minWorkers = 0;
			takeAnyUnit = true;
			int numCombatants = 0;
			for(CombatSquad cs: infoMan.combatSquads){
				numCombatants += cs.units.size();
			}
			rocketsToBuild = (numCombatants + 5) / 6;
		}
		else if(gc.round() > MagicNumbers.BUILD_UP_WORKERS){
			minWorkers = (int) (infoMan.fighterCount / MagicNumbers.FIGHTERS_PER_WORKER);
		}
		if(gc.karbonite() >= MagicNumbers.FACTORY_COST ) {
			maxFactories = infoMan.factories.size() + 1 > 6 ? 6 : infoMan.factories.size() + 1;
			if(gc.karbonite() > 300 && minFactories < 3) {
				minFactories++;
			}
		}
		//increment rocketsToBuild appropriately
		//if you've totally dominated them, send a bunch at the same time.
		//otherwise if it's getting close to the end of the game send a bunch at the same time
		//otherwise even if you're not dominating, if you're not really engaging with the enemy/running out of space to build then steadily send.
		else if(infoMan.researchLevels[5] > 0 && infoMan.fighterCount > (40 + infoMan.rocketsToBeBuilt*8)) {
			rocketsToBuild++;
			rocketsBuilt++;
		}
    }

	public int calcWorkerUrgency(int size, Objective objective, UnitType toBuild) {
		if(objective == Objective.MINE)
			return 0;
		return MagicNumbers.MAX_WORKERS_PER_BUILDING - size;
	}
	
	public int calcRocketUrgency(int numUnits) {
		return (numUnits == 8 ? 0 : (gc.round() > MagicNumbers.SEND_EVERYTHING ? 100 : 50));
	}
	
	public boolean shouldLaunch(Unit rocket, int numUnitsInside) {
		//TODO: make this better
		return gc.round() + 1 == MagicNumbers.EARTH_FLOOD_ROUND
		|| (rocket.health() * 2 < rocket.maxHealth() 
		|| infoMan.tiles[rocket.location().mapLocation().getX()][rocket.location().mapLocation().getY()].possibleDamage > 0) && numUnitsInside > 0
		|| numUnitsInside == 8;
	}
	
	public boolean shouldGoToBuildLoc() {
		//TODO: make this better
		return true;
		//return gc.karbonite() >= MagicNumbers.FACTORY_COST - infoMan.workerCount * 10;
	}

	public static int calcCombatUrgency(int numEnemyUnits, int size) {
		return (numEnemyUnits * 2 - size + 15) * 10;
	}

	public static boolean shouldWeRetreat(int numEnemyUnits, int size) {
		return numEnemyUnits > size * MagicNumbers.AGGRESION_FACTOR;
	}

	public double getReplicateScore(long numKarbLeftInArea, int numMiners, long distToKarbonite) {
		return (((numKarbLeftInArea * 10.0) - numMiners*numMiners*50) /(distToKarbonite + 10)) + (infoMan.myPlanet == Planet.Mars && gc.round() >= 750 ? 100 : 0);
	}

}