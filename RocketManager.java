import bc.*;
import java.util.ArrayList;

/*
creates and assigns empty RocketSquads to rockets
launches rockets when it's time

Schedules rocket launches.
As the scheduled launch nears, sets up rocket squads to fill each rocket
*/
public class RocketManager{
    InfoManager infoMan;
    public GameController gc;

    public RocketManager(GameController g, InfoManager im){
        gc = g;
        infoMan = im;
    }

    /* modifies rocketSquads as necessary and launches rockets
            (remember, those are in infoMan) */
    public void update(Strategy strat){

        // adjust for rocket deaths
        ArrayList<Integer> toRemove = new ArrayList<Integer>();
        for (int i = infoMan.rocketSquads.size() - 1; i >= 0; i--){
            // make sure each squad has a rocket at index 0. if no rocket, delete it
            if (infoMan.rocketSquads.get(i).units.size() < 1 || gc.unit(infoMan.rocketSquads.get(i).units.get(0)).unitType() != UnitType.Rocket){
                toRemove.add(i);
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
        
        while(infoMan.unassignedUnits.size() > 0) {
			boolean didSomething = false;
			infoMan.rocketSquads.sort(Squad.byUrgency());
			boolean tryAgain = false;
			for(RocketSquad rs : infoMan.rocketSquads) {
				for(int i : infoMan.unassignedUnits) {
					Unit a = gc.unit(i);
					if(rs.requestedUnits.contains(a.unitType())) {
						/*
						if(rs.targetLoc != null && 
						((!turnUnassigned.containsKey(a.id()) && gc.round() == 1) || (turnUnassigned.containsKey(a.id()) && turnUnassigned.get(a.id()) == gc.round()))){
							MapLocation ml = cs.targetLoc;
							if(a.location().isOnMap())
								ml = a.location().mapLocation();
							else
								ml = gc.unit(a.location().structure()).location().mapLocation();
							if(!infoMan.isReachable(cs.targetLoc, ml))
								continue;
						}*/
						Utils.log("adding to rs");
						//if(cs.targetLoc != null)
						//	Utils.log("targetLoc = " + cs.targetLoc);
						rs.units.add(i);
						rs.update();
						tryAgain = true;
						didSomething = true;
					}
					if(tryAgain)
						break;
				}
				if(tryAgain)
					break;
			}
			if(!didSomething)
				break;
		}

        // udpate() each squad so we know what units to find
        // and poach nearby units if reasonable
        for (RocketSquad rs : infoMan.rocketSquads){
            rs.update(strat.rocketComposition);

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
            /*
            for (int ind = 0; ind < requests.length; ind++){
                stealClosestApplicableUnitsOfType(rs, Utils.robotTypes[ind], requests[ind]);
            }*/
        }
    }

    public void stealClosestApplicableUnitsOfType(RocketSquad rs, UnitType type, int num){
        if (num <= 0) return;

        // get closest legal units
        Unit[] toSteal = new Unit[num];
        int maxInd = 0;
        long maxDist = 999999;
        ArrayList<Unit> list = type == UnitType.Worker ? infoMan.workers : infoMan.fighters;
        for (Unit unit : list){
        	if(!unit.location().isOnMap())
        		continue;
            if ((toSteal[maxInd] == null || rs.targetLoc.distanceSquaredTo(unit.location().mapLocation()) < maxDist)
                    && canStealUnit(rs, unit)){
                toSteal[maxInd] = unit;
                maxInd = Utils.maxDistIndex(toSteal, rs.targetLoc);
                maxDist = toSteal[maxInd] == null ? 999999 : rs.targetLoc.distanceSquaredTo(unit.location().mapLocation());
            }
        }

        // do the deed
        for (Unit unit : toSteal){
            if (unit != null){
                Squad squad = infoMan.getSquad(unit);
                rs.units.add(unit.id());
                if(squad == null)
                	continue;
                squad.removeUnit(unit.id());
                squad.update();
            }
        }
    }

    public boolean canStealUnit(RocketSquad rs, Unit unit){
        Squad squad = infoMan.getSquad(unit);
        if (squad == null) return true;

        if (infoMan.isInSquads2(unit, infoMan.rocketSquads)) return false;

        // TODO: compare urgencies of rs and the unit's squad

        return true;
    }
}