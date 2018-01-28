/****************/
/* REFACTOR ME! */
/****************/

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

    public Strategy(InfoManager im, GameController g){
    	infoMan = im;
    	gc = g;
    	determineInitalStrat();
    	rocketsBuilt = 0;
    }

	private void determineInitalStrat() {
		//TODO: make this depend on stuff
		researchOrder = new UnitType[]{UnitType.Ranger,UnitType.Worker,UnitType.Healer,UnitType.Healer,UnitType.Healer,UnitType.Rocket,UnitType.Ranger,UnitType.Ranger};
		combatComposition = new int[]{0, 0, 3, 2}; //knight,mage,ranger,healer
        rocketComposition = defaultRocketComposition;
        rocketsToBuild = 0;
        maxFactories = 1;
        minFactories = 0;
        minWorkers = 3;
        maxWorkers = 20;
        takeAnyUnit = false;
	}

	public void update(){
		if(infoMan.myPlanet == Planet.Mars)
			return;
		if(gc.round() > MagicNumbers.SEND_EVERYTHING) {
			//Pack ur bags we gonna go to mars cuz earth is flooding and we dont wanna die
			rocketComposition = new int[]{0,0,4,2,2};
			takeAnyUnit = true;
			rocketsToBuild = 100;
		}
		if(gc.karbonite() >= MagicNumbers.FACTORY_COST ) {
			maxFactories = infoMan.factories.size() + 1 > 6 ? 6 : infoMan.factories.size() +1;
		}
		//increment rocketsToBuild appropriately
		//if you've totally dominated them, send a bunch at the same time.
		//otherwise if it's getting close to the end of the game send a bunch at the same time
		//otherwise even if you're not dominating, if you're not really engaging with the enemy/running out of space to build then steadily send.
		else if(infoMan.researchLevels[5] > 0 && infoMan.fighters.size() > (50 + rocketsBuilt*8)) {
			rocketsToBuild++;
			rocketsBuilt++;
		}
    }

	public int calcWorkerUrgency(int size, Objective objective, UnitType toBuild) {
		//TODO: make this better
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
		|| rocket.health() * 2 < rocket.maxHealth();
	}
	
	public boolean shouldGoToBuildLoc() {
		//TODO: make this better
		return true;
		//return gc.karbonite() >= MagicNumbers.FACTORY_COST - infoMan.workerCount * 10;
	}
}