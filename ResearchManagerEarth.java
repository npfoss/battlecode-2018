import bc.*;

public class ResearchManagerEarth extends ResearchManager{
	// remember, has infoMan

	public ResearchManagerEarth(GameController g, InfoManager im){
		super(g,im);

	}

	public void update(Strategy strat){
		if(strat==Strategy.UNSURE || true) {
			int numRangerResearch = 0;
			if(!gc.researchInfo().hasNextInQueue() && numRangerResearch < 3) {
				gc.queueResearch(UnitType.Ranger);
				numRangerResearch++;
			}
		}
	}
}