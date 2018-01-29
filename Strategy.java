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
	boolean knightRush;
	int rushDist;
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
		
		researchOrder = new UnitType[]{UnitType.Worker,UnitType.Ranger,UnitType.Healer,UnitType.Healer,UnitType.Healer,UnitType.Rocket,UnitType.Ranger,UnitType.Ranger};
		
		if(infoMan.myPlanet == Planet.Mars){
			maxWorkers = 10000;
			minWorkers = 10;
			return;
		}
		rushDist = -1;
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
			int maxDist = -1;
			for(Unit u2: theirStarts){
				MapLocation ml1 = u.location().mapLocation();
				MapLocation ml2 = u2.location().mapLocation();
				if(infoMan.isReachable(ml1, ml2) && nav.optimalStepsTo(ml1,ml2) > maxDist){
					maxDist = nav.optimalStepsTo(ml1, ml2);
				}
			}
			if(maxDist >-1 && maxDist > rushDist)
				rushDist = maxDist;
		}
		if(rushDist == -1)
			rushDist = 100000; //can't get to enemy
        rocketsToBuild = 0;
        takeAnyUnit = false;
		knightRush = rushDist < MagicNumbers.MAX_DIST_RUSH;
        rocketComposition = defaultRocketComposition;
		if(!knightRush){
			combatComposition = new int[]{0, 0, 3, 2}; //knight,mage,ranger,healer
	        maxFactories = 1;
	        minFactories = 0;
	        minWorkers = 4;
	        maxWorkers = 30;
		}
		else{
			combatComposition = new int[]{1, 0, 0, 0}; //knight,mage,ranger,healer
			researchOrder = new UnitType[]{UnitType.Knight,UnitType.Knight,UnitType.Ranger,UnitType.Healer,UnitType.Healer,UnitType.Healer,UnitType.Rocket,UnitType.Ranger,UnitType.Ranger};
			maxFactories = 1;
			minFactories = 0;
			minWorkers = 4;
			maxWorkers = rushDist + 3;
		}
	}

	public void update(){
		if(infoMan.myPlanet == Planet.Mars){
			return;
		}
		if(knightRush){
			if(gc.round() > 150 && (infoMan.enemyMages.size() > 0 || infoMan.enemyRangers.size() > 3 || gc.round() > 300)){
				//end the rush
				Utils.log("ending knight rush");
				knightRush = false;
				combatComposition = new int[]{0, 0, 3, 2};
			}
			else{
				if(gc.karbonite() >= MagicNumbers.FACTORY_COST) {
					maxFactories = infoMan.factories.size() + 1 > 6 ? 6 : infoMan.factories.size() + 1;
					if(gc.karbonite() > 200 && minFactories < 3) {
						Utils.log("plz build more factories");
						minFactories++;
					}
				}
				return;
			}
		}
		int numCombatants = 0;
		for(CombatSquad cs: infoMan.combatSquads){
			numCombatants += cs.units.size();
		}
		if(gc.round() >= MagicNumbers.SEND_EVERYTHING) {
			//Pack ur bags we gonna go to mars cuz earth is flooding and we dont wanna die
			rocketComposition = new int[]{0,0,5,2,1};
			minWorkers = 0;
			minFactories = 0;
			maxFactories = 0;
			takeAnyUnit = true;
			rocketsToBuild = (numCombatants + infoMan.workerCount + 5) / 6;
		}
		else if(infoMan.researchLevels[5] > 0 && (numCombatants > ((750 - gc.round()) /(rushDist == 100000 ? 20 : 5)) || infoMan.tilesWeCanSee > infoMan.tiles.length * infoMan.tiles[0].length * 0.65 && infoMan.targetUnits.size() < 2	)) {

			rocketsToBuild++;
			rocketsBuilt++;
		}
//		if(gc.round() > MagicNumbers.BUILD_UP_WORKERS && gc.round() < MagicNumbers.SEND_EVERYTHING){
//			minWorkers = (int) (infoMan.fighterCount / MagicNumbers.FIGHTERS_PER_WORKER);
//		}
		if(gc.karbonite() >= MagicNumbers.FACTORY_COST && gc.round() < MagicNumbers.SEND_EVERYTHING) {
			maxFactories = infoMan.factories.size() + 1 > 6  && gc.karbonite() < 600 ? 6 : infoMan.factories.size() + 1;
			if(gc.karbonite() > 300 && minFactories < 3) {
				minFactories++;
			}
		}
    }

	public int calcWorkerUrgency(int size, Objective objective, UnitType toBuild) {
		if(objective == Objective.MINE)
			return 0;
		return MagicNumbers.MAX_WORKERS_PER_BUILDING - size;
	}
	
	public int calcRocketUrgency(int numUnits) {
		return 100;
	}
	
	public boolean shouldLaunch(Unit rocket, int numUnitsInside) {
        //TODO: make this better
        return gc.round() + 1 == MagicNumbers.EARTH_FLOOD_ROUND
            || numUnitsInside > 0 && (rocket.health() * 2 < rocket.maxHealth() 
                    && infoMan.tiles[rocket.location().mapLocation().getX()][rocket.location().mapLocation().getY()].possibleDamage >= rocket.health())
            || numUnitsInside == 8 && (infoMan.orbitPattern.duration(gc.round()) < infoMan.orbitPattern.duration(gc.round() + 10) + 10);
	}
	
	public boolean shouldGoToBuildLoc() {
		//TODO: make this better
		return true;
		//return gc.karbonite() >= MagicNumbers.FACTORY_COST - infoMan.workerCount * 10;
	}

	public static int calcCombatUrgency(int numEnemyUnits, int size, Objective o) {
		return (numEnemyUnits * 2 - size) * (o == Objective.DEFEND_LOC ? 15 : 5);
	}

	public boolean shouldWeRetreat(int numEnemyUnits, int size) {
		return numEnemyUnits > size * (knightRush ? 2.0 : MagicNumbers.AGGRESION_FACTOR);
	}

	public double getReplicateScore(long numKarbLeftInArea, int numMiners, long distToKarbonite, long distToHostile) {
		return (((numKarbLeftInArea * 9.0) - numMiners*numMiners*70.0) /(distToKarbonite/2.0 + 10.0)) 
				+ (infoMan.myPlanet == Planet.Mars && gc.round() >= 750 ? 100 : 0)
				- (infoMan.myPlanet == Planet.Earth && infoMan.targetUnits.size() > 0 ? MagicNumbers.REPLICATION_ENEMY_FACTOR : 0)
				- (infoMan.myPlanet == Planet.Earth ? (MagicNumbers.MAX_DIST_TO_CHECK - distToHostile) * MagicNumbers.REPLICATION_NEARBY_ENEMY_FACTOR : 0);
	}

}