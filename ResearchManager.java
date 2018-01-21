import bc.*;

public class ResearchManager{
    public InfoManager infoMan;
    public GameController gc;
    
    public ResearchManager(GameController g, InfoManager im){
        infoMan = im;
        gc = g;
    }

    // don't use. check out the planet-specific ones
    public void update(Strategy strat){}
}