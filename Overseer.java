import bc.*;
import java.util.ArrayList;

// import java.util.HashMap;

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
        nav = new Nav(infoMan);

        if(gc.planet() == Planet.Earth){
            magicNums = new MagicNumbersEarth();
            prodMan = new ProductionManager(infoMan, gc);
            researchMan = new ResearchManagerEarth(gc, infoMan);
            rocketMan = new RocketManager(gc, infoMan);
        } else {
            magicNums = new MagicNumbersMars();
            prodMan = new ProductionManagerDoNothing();
            researchMan = new ResearchManagerMars(gc, infoMan);
            rocketMan = new RocketDoNothing(gc, infoMan);
        }

        // // delete pls
        // HashMap<String, Integer> m = new HashMap<String, Integer>();
        // MapLocation loc = new MapLocation(gc.planet(), 5, 5);
        // m.put(loc.toString(), 19);
        // MapLocation loc2 = new MapLocation(gc.planet(), 5, 5);
        // System.out.println("88asdfasdfkjasdf: " + m.containsKey(loc2.toString()) + " " + loc2.toString());
        // MapLocation loc3 = new MapLocation(gc.planet(), 6, 5);
        // System.out.println("second one: " + m.containsKey(loc3.toString()) + " " + loc3.toString());

        // //
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