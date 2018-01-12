import bc.*;
import java.util.ArrayList;

/*
manages overall decisions of combat units
Assigns units to squads
Rearranges combat squads (and gives units to rocketsquads) as needed
*/
public class CombatManager{
    InfoManager infoMan;

    public CombatManager(InfoManager im){
        infoMan = im; //contains squads
    }

    public void update(Strategy strat){
        // assign unassigned fighters
        // shuffle fighter squads (and add to rocket squads if needbe)
        // make sure their objectives and stuff are rigt
        // call update method of each squad?
        // remember, the squads will move on their own after you update everything
    }
}