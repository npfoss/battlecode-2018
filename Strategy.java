import bc.*;

/* Enum - choose general strategy mode
didn't call it StrategyManager because it's an enum so this makes more sense
*/

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
    	if(infoMan.myPlanet == Planet.Earth)
    		determineInitalStrat();
    }
	
	private void determineInitalStrat() {
		//eventually make this depend on stuff
		researchOrder = new UnitType[]{UnitType.Ranger,UnitType.Ranger,UnitType.Healer,UnitType.Healer,UnitType.Healer,UnitType.Ranger};
		combatComposition = new int[]{0,0,1,0}; //knight,mage,ranger,healer
	}

	public void update(){
        //eventually make it do stuff
    }
}