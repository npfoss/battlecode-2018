import bc.*;
import java.util.ArrayList;

/*
for Earth

creates and assigns RocketSquads when we build new rockets

NOTE: infoMan decides where to send rockets
*/

public class RocketManager{
    InfoManager infoMan;
    public GameController gc;

    public RocketManager(GameController g, InfoManager im){
        gc = g;
        infoMan = im;
    }

    public void update(Strategy strat){

        // adjust for rocket deaths
        ArrayList<Integer> toRemove = new ArrayList<Integer>();
        for (int i = infoMan.rocketSquads.size() - 1; i >= 0; i--){
            // make sure each squad has a rocket at index 0. if no rocket, delete it
        	RocketSquad rs = infoMan.rocketSquads.get(i);
            if (rs.units.size() < 1 || gc.unit(rs.units.get(0)).unitType() != UnitType.Rocket
            	|| rs.isInSpace){
                toRemove.add(i);
                for(int u: rs.units) {
                	infoMan.unassignedUnits.add(u);
                }
            }
        }
        
        for(int i : toRemove){
            infoMan.rocketSquads.remove(i);
        }
        

        // find new (unassigned) rockets and make squads
        for (Unit rocket : infoMan.newRockets){
        	RocketSquad rs = new RocketSquad(infoMan, rocket.location().mapLocation());
        	rs.units.add(rocket.id());
            infoMan.rocketSquads.add(rs);
        }

        // udpate() each squad so we know what units to find
        // and poach nearby units if reasonable
        for (RocketSquad rs : infoMan.rocketSquads){
            rs.update(strat.rocketComposition, strat);

            // how many to look for
            int[] requests = {0,0,0,0,0};
            for (UnitType t : rs.requestedUnits) {
                switch(t){
                    case Knight: requests[0]++; break;
                    case Mage  : requests[1]++; break;
                    case Ranger: requests[2]++; break;
                    case Healer: requests[3]++; break;
                    case Worker: requests[4]++; break;
                }
            }
            for (int ind = 0; ind < requests.length; ind++){
            	if(Utils.robotTypes[ind] == UnitType.Worker && rs.rocket.structureIsBuilt() == 0)
            		continue;
                stealClosestApplicableUnitsOfType(rs, Utils.robotTypes[ind], requests[ind], strat.takeAnyUnit);
            }
        }
    }

    public void stealClosestApplicableUnitsOfType(RocketSquad rs, UnitType type, int num, boolean takeAnyUnit){

        if (num == 0) return;
    	// Utils.log("Num = " + num);
        // get closest legal units
        Unit[] toSteal = new Unit[num];
        int maxInd = 0;
        long maxDist = 999999;
        ArrayList<Unit> list = type == UnitType.Worker ? infoMan.workers : infoMan.fighters;
        // Utils.log("List length = " + list.size());
        for (Unit unit : list){
        	if(!takeAnyUnit && unit.unitType() != type)
        		continue;
        	if(!unit.location().isOnMap())
        		continue;
            if ((toSteal[maxInd] == null || rs.targetLoc.distanceSquaredTo(unit.location().mapLocation()) < maxDist)
                    && canStealUnit(rs, unit)){
            	// Utils.log("Tryna steal a unit");
                toSteal[maxInd] = unit;
                maxInd = Utils.maxDistIndex(toSteal, rs.targetLoc);
                maxDist = toSteal[maxInd] == null ? 999999 : rs.targetLoc.distanceSquaredTo(unit.location().mapLocation());
            }
        }

        // do the deed
        for (Unit unit : toSteal){
            if (unit != null){
                Utils.log("actually stealing " + unit.id());
                Squad squad = infoMan.getSquad(unit);
                rs.units.add(unit.id());
                if(squad == null){
                	Utils.log("not actually removing " + unit.id());
                	continue;
                }
                squad.removeUnit(unit.id());
                squad.update();
            }
        }
    }

    public boolean canStealUnit(RocketSquad rs, Unit unit){
        Squad squad = infoMan.getSquad(unit);
        if (squad == null) return true;

        if (infoMan.isInSquads2(unit, infoMan.rocketSquads)) return false;
        if(squad.urgency > rs.urgency)
        	return false;

        return true;
    }
}
