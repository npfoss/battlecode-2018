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

    public Overseer(){
        ///* complete garbage, this is from examplefuncs
        // MapLocation is a data structure you'll use a lot.
        MapLocation loc = new MapLocation(Planet.Earth, 10, 20);
        System.out.println("loc: "+loc+", one step to the Northwest: "+loc.add(Direction.Northwest));
        System.out.println("loc x: "+loc.getX());

        // One slightly weird thing: some methods are currently static methods on a static class called bc.
        // This will eventually be fixed :/
        System.out.println("Opposite of " + Direction.North + ": " + bc.bcDirectionOpposite(Direction.North));

        // Direction is a normal java enum.
        Direction[] directions = Direction.values();
        //*/ // end garbage



        // stuff that doesn't need gc
        strat = Strategy.UNSURE;


        // Connect to the manager, starting the game
        gc = new GameController();

        infoMan = new InfoManager(gc);
        workerMan = new WorkerManager(infoMan);
        combatMan = new CombatManager(infoMan);
        prodMan = new ProductionManager(infoMan, gc);
        nav = new Nav(infoMan);

        if(gc.planet() == Planet.Earth){
            // magic numbers
            magicNums = new MagicNumbersEarth();
            researchMan = new ResearchManagerEarth(infoMan);
            rocketMan = new RocketManager(gc, infoMan);
        } else {
            // magic numbers
            magicNums = new MagicNumbersMars();
            researchMan = new ResearchManagerMars(infoMan);
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




        ///* garbage from examplefuncs
        // VecUnit is a class that you can think of as similar to ArrayList<Unit>, but immutable.
        VecUnit units = gc.myUnits();
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);

            // Most methods on gc take unit IDs, instead of the unit objects themselves.
            if (gc.isMoveReady(unit.id()) && gc.canMove(unit.id(), Direction.Southeast)) {
                gc.moveRobot(unit.id(), Direction.Southeast);
            }
        }
        //*/ //end examplefuncs garbage





    }
}