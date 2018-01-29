import bc.*;
import java.util.ArrayList;

/*
just does the turn, update()ing everything then move()ing everything
*/
public class Overseer{
    GameController gc;
    MagicNumbers magicNums;
    InfoManager infoMan;
    ResearchManager researchMan;
    RocketManager rocketMan;
    WorkerManager workerMan;
    CombatManager combatMan;
    ProductionManager prodMan;
    Strategy strat;

    Nav nav;

    public Overseer(GameController g){
        gc = g;

        if(gc.planet() == Planet.Earth){
        	 magicNums = new MagicNumbersEarth();
        }
        else
        	magicNums = new MagicNumbersMars();
        
        infoMan = new InfoManager(gc, magicNums);
        nav = new Nav(infoMan);
        strat = new Strategy(infoMan, gc, nav);
        workerMan = new WorkerManager(infoMan, gc, strat);
        combatMan = new CombatManager(infoMan, gc, strat);
        
        if(gc.planet() == Planet.Earth){
            prodMan = new ProductionManager(infoMan, gc);
            researchMan = new ResearchManagerEarth(gc, infoMan);
            rocketMan = new RocketManager(gc, infoMan);
        } else {
            prodMan = new ProductionManagerDoNothing();
            researchMan = new ResearchManagerMars(gc, infoMan);
            rocketMan = new RocketDoNothing(gc, infoMan);
        }
        
    }

    public void takeTurn(){
        Utils.log("Current round: " + gc.round());
        int start = gc.getTimeLeftMs();
        
        infoMan.update(strat);
        strat.update();
        researchMan.update(strat);
        rocketMan.update(strat);
        workerMan.update(nav);
        combatMan.update(strat);
        prodMan.update(strat);

        // try{
            prodMan.move();
        // } catch (Exception e) {
        //     // darn
        // }

        
        for(WorkerSquad ws : infoMan.workerSquads){
            // try{
                ws.move(nav,strat);
            // } catch (Exception e) {
            //     // darn
            // }
        }
        for(CombatSquad cs : infoMan.combatSquads){
            // try{
                cs.move(nav);
            // } catch (Exception e) {
            //     // darn
            // }
        }
        for(RocketSquad rs : infoMan.rocketSquads){
            // try{
        	   rs.move(nav,strat);
            // } catch (Exception e) {
            //     // darn
            // }
        }

        gc.nextTurn();
        // this has to go after
        //      because getTimeLeftMs is the same during the same turn
        Utils.log("previous turn took " + (start + 50 - gc.getTimeLeftMs()) + " ms. " + gc.getTimeLeftMs() + " ms left");
    }
}
