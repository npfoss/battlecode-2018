import bc.*;

public class ResearchManager{
    public InfoManager infoMan;
    public GameController gc;
    
	int numWorkerRes = 0;
	int numKnightRes = 0;
	int numRangerRes = 0;
	int numMageRes = 0;
	int numHealerRes = 0;
	int numRocketRes = 0;
	
    public ResearchManager(GameController g, InfoManager im){
        infoMan = im;
        gc = g;
    }

    // don't use. check out the planet-specific ones
    public void update(Strategy strat){}
}