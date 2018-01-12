import bc.*;
import java.util.ArrayList;

/*
rearrange worker squads (includes assigning to rocketsquads and
    empty squads for specific factories),
Re-allocate workers to new karbonite patches if mined out,
Send idle workers to gather karbonite
*/
public class WorkerManager{
    InfoManager infoMan;

    public WorkerManager(InfoManager im){
        infoMan = im;
    }

    public void update(Strategy strat){
        // assign unassigned workers
        // shuffle worker squads (and add to rocket squads if needbe)
        // make sure their objectives and stuff are rigt
        // call update method of each squad?
        // remember, the squads will move on their own after you update everything
    }
}