import bc.*;

public class ResearchManagerMars extends ResearchManager{
	// remember, has infoMan

	public ResearchManagerMars(GameController g, InfoManager im){
		super(g,im);
	}

	public void update(Strategy strat){
		if(gc.round() >= 750) {
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
}