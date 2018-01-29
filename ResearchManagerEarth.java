import bc.*;

/*
controls research related things
*/
public class ResearchManagerEarth extends ResearchManager{
	// remember, has infoMan
	public ResearchManagerEarth(GameController g, InfoManager im){
		super(g,im);
	}

	public void update(Strategy strat){
		if(gc.researchInfo().hasNextInQueue())
			return;
		int[] numExpected = new int[]{0,0,0,0,0,0};
		
		for(UnitType ut: strat.researchOrder){
			int i = Utils.typeToInd(ut);
			numExpected[i]++;
			if(numExpected[i] > infoMan.researchLevels[i]){
				gc.queueResearch(ut);
				infoMan.researchLevels[i]++;
				break;
			}
		}
	}
}
