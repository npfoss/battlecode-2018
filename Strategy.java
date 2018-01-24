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
	int nextRocketBuild;
	int[] rocketComposition;
    static int[] defaultRocketComposition = {0, 0, 4, 2, 2};
                            // knight, mage, ranger, healer, worker
	MapLocation[] rocketLaunchLocations;
    int rocketLaunchFrequency; // (# rounds between each)
	int maxWorkers;
	int maxFactories;

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
        nextRocketBuild = 101;
        maxWorkers = 6;
	}

	public void update(){
        //TODO: make it adjust stuff if necessary
		if(infoMan.myPlanet == Planet.Mars)
			return;
		if(gc.round() > nextRocketBuild){
            if (gc.round() > infoMan.magicNums.EARTH_FLOOD_ROUND - 10){
                // give up, not enough time
                nextRocketBuild = 999;
            }
            int roundsLeft = infoMan.magicNums.EARTH_FLOOD_ROUND - (int)(gc.round());
            if(roundsLeft < 300){
                rocketComposition = new int[]{0, 0, 5, 3, 0};
            }
			nextRocketBuild += 55 - (int)(((650.0 - roundsLeft) / 650)*30) - (int)((infoMan.fighters.size() / 150.0)*30);
			if(gc.round()<350)
				maxWorkers += 1;
		}
    }
}