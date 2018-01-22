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
	int[] rocketLaunchSchedule;
	int[] rocketComposition;
	MapLocation[] rocketLaunchLocations;
	int maxWorkers;
	int maxFactories;
	
    public Strategy(InfoManager im, GameController g){
    	infoMan = im;
    	gc = g;
    	determineInitalStrat();
    }
	
	private void determineInitalStrat() {
		//TODO: make this depend on stuff
		researchOrder = new UnitType[]{UnitType.Ranger,UnitType.Worker,UnitType.Healer,UnitType.Healer,UnitType.Healer,UnitType.Ranger,UnitType.Ranger};
		combatComposition = new int[]{0, 0, 3, 2}; //knight,mage,ranger,healer
        rocketComposition = new int[]{0, 0, 4, 2, 2};
                            // knight, mage, ranger, healer, worker
	}

	public void update(){
        //TODO: make it adjust stuff if necessary
    }
}