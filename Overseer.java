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

        strat = Strategy.UNSURE;

        infoMan = new InfoManager(gc);
        workerMan = new WorkerManager(infoMan,gc);
        combatMan = new CombatManager(infoMan,gc);
        prodMan = new ProductionManager(infoMan, gc);
        nav = new Nav(infoMan);

        if(gc.planet() == Planet.Earth){
            magicNums = new MagicNumbersEarth();
            researchMan = new ResearchManagerEarth(gc, infoMan);
            rocketMan = new RocketManager(gc, infoMan);
        } else {
            magicNums = new MagicNumbersMars();
            researchMan = new ResearchManagerMars(gc, infoMan);
            rocketMan = new RocketDoNothing(gc, infoMan);
        }

    }

    public void takeTurn(){
        System.out.println("Current round: " + gc.round());
        
        infoMan.update();
        strat = strat.update(infoMan);
        researchMan.update(strat);
        rocketMan.update();
        workerMan.update(strat);
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
    }
}