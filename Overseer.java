import bc.*;
import java.util.ArrayList;

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
        
        infoMan = new InfoManager(gc,magicNums);
        strat = new Strategy(infoMan,gc);
        workerMan = new WorkerManager(infoMan,gc);
        combatMan = new CombatManager(infoMan,gc,magicNums,strat);
        nav = new Nav(infoMan);
        
        if(gc.planet() == Planet.Earth){
            prodMan = new ProductionManager(infoMan, gc, magicNums);
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
        
        infoMan.update();
        strat.update();
        researchMan.update(strat);
        rocketMan.update(strat);
        workerMan.update(strat,nav);
        combatMan.update(strat);
        prodMan.update(strat);

        prodMan.move();

        for(RocketSquad rs : infoMan.rocketSquads){
        	rs.move(nav);
        }
        for(WorkerSquad ws : infoMan.workerSquads){
            ws.move(nav);
        }
        for(CombatSquad cs : infoMan.combatSquads){
            cs.move(nav);
        }

        gc.nextTurn();
        // this has to go after
        //      because getTimeLeftMs is the same during the same turn
        Utils.log("turn took " + (start + 50 - gc.getTimeLeftMs()) + ". " + gc.getTimeLeftMs() + " ms left");
    }
}