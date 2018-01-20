import bc.*;

public class ResearchManagerMars extends ResearchManager{
	// remember, has infoMan

	public ResearchManagerMars(GameController g, InfoManager im){
		super(g,im);
	}

	public void update(Strategy strat){
		if(gc.round() >= 750) {
			if(gc.researchInfo().hasNextInQueue())
				return;
			int numExpectedRanger = 0;
			int numExpectedMage = 0;
			int numExpectedKnight = 0;
			int numExpectedWorker = 0;
			int numExpectedRocket = 0;
			int numExpectedHealer = 0;
			
			for(UnitType ut: strat.researchOrder){
				if(ut == UnitType.Ranger){
					numExpectedRanger++;
					if(numExpectedRanger>gc.researchInfo().getLevel(UnitType.Ranger)){
						gc.queueResearch(UnitType.Ranger);
						break;
					}
				}
				if(ut == UnitType.Mage){
					numExpectedMage++;
					if(numExpectedMage>gc.researchInfo().getLevel(UnitType.Mage)){
						gc.queueResearch(UnitType.Mage);
						break;
					}
				}
				if(ut == UnitType.Knight){
					numExpectedKnight++;
					if(numExpectedKnight>gc.researchInfo().getLevel(UnitType.Knight)){
						gc.queueResearch(UnitType.Knight);
						break;
					}
				}
				if(ut == UnitType.Worker){
					numExpectedWorker++;
					if(numExpectedWorker>gc.researchInfo().getLevel(UnitType.Worker)){
						gc.queueResearch(UnitType.Worker);
						break;
					}
				}
				if(ut == UnitType.Healer){
					numExpectedHealer++;
					if(numExpectedHealer>gc.researchInfo().getLevel(UnitType.Healer)){
						gc.queueResearch(UnitType.Ranger);
						break;
					}
				}
				if(ut == UnitType.Rocket){
					numExpectedRocket++;
					if(numExpectedRocket>gc.researchInfo().getLevel(UnitType.Rocket)){
						gc.queueResearch(UnitType.Rocket);
						break;
					}
				}
			}
		}
	}
}