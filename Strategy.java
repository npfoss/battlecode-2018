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

    public Strategy(InfoManager im, GameController g){
    	infoMan = im;
    	gc = g;
    	determineInitalStrat();
    }

	private void determineInitalStrat() {
		//TODO: make this depend on stuff
		researchOrder = new UnitType[]{UnitType.Ranger,UnitType.Worker,UnitType.Rocket,UnitType.Healer,UnitType.Healer,UnitType.Healer,UnitType.Ranger,UnitType.Ranger};
		combatComposition = new int[]{0, 0, 3, 2}; //knight,mage,ranger,healer
        rocketComposition = defaultRocketComposition;
        rocketsToBuild = 0;
        maxFactories = 1;
        minFactories = 0;
        minWorkers = 3;
        maxWorkers = 20;
	}

	public void update(){
        //TODO: make it adjust stuff if necessary
		if(infoMan.myPlanet == Planet.Mars)
			return;
		//TODO: replace with variable from research refactor plus make better
		if(gc.round() % 50 == 0 && gc.researchInfo().getLevel(UnitType.Rocket) > 0)
			rocketsToBuild++;
    }

	public int calcWorkerUrgency(int size, Objective objective, UnitType toBuild) {
		//TODO: make this better
		if(objective == Objective.MINE)
			return 0;
		return MagicNumbers.MAX_WORKERS_PER_BUILDING - size;
	}

	public boolean shouldGoToBuildLoc() {
		//TODO: make this better
		return true;
		//return gc.karbonite() >= MagicNumbers.FACTORY_COST - infoMan.workerCount * 10;
	}
}