import bc.*;

public class ResearchManagerEarth extends ResearchManager{
	// remember, has infoMan
	public ResearchManagerEarth(GameController g, InfoManager im){
		super(g,im);

	}

	public void update(Strategy strat){
		switch(strat) {
		case UNSURE:
		default:
			if(!gc.researchInfo().hasNextInQueue()) {
				if(numRangerRes < 3) {
					gc.queueResearch(UnitType.Ranger);
					numRangerRes++;
				}
				else if (numRocketRes < 3) {
					gc.queueResearch(UnitType.Rocket);
					numRocketRes++;
				}
			}
		}
	}
}