import bc.*;
import java.util.ArrayList;
import java.util.HashMap;
/*
rearrange worker squads
    (includes assigning (sometimes) empty squads for factories and rockets),
Re-allocate workers to new karbonite patches if mined out,
Send idle workers to gather karbonite
controlls worker replication (based on squad urgency)

TODO: delete/reorganize squads more
--have a single 'mining' squad which doesn't stick together, is just an idle pool basically
 */
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;

/* Overall refactor thoughts:
 * - remove squads which have objective set to none
 * - add control of worker replication
 * - always have one mining squad which workers join if they can't do anything else
 * - cooperate with prod manager to not make new things when we are about to build a new factory
 */
public class WorkerManager{
	InfoManager infoMan;
	GameController gc;
	Strategy strat;

	public WorkerManager(InfoManager im, GameController g, Strategy s){
		infoMan = im;
		gc = g;
		strat = s;
		WorkerSquad ws = new WorkerSquad(infoMan,s);
		ws.objective = Objective.MINE;
		ws.update();
		infoMan.workerSquads.add(ws);
	}

	public boolean okayToBuild(int x, int y, UnitType type) {	
		if(!infoMan.isOnMap(x,y))
			return false;

		if(!infoMan.tiles[x][y].isWalkable || infoMan.tiles[x][y].unitID > -1){//&& (gc.senseUnitAtLocation(loc).unitType() == UnitType.Factory|| gc.senseUnitAtLocation(loc).unitType() == UnitType.Rocket)))) {
			return false;
		}
		
		//don't build a rocket if there's a rocket or factory on an adjacent tile
		if(type == UnitType.Rocket){
			int nx,ny;
			for(int i = 0; i < 9; i++){
				nx = x + Utils.dx[i];
				ny = y + Utils.dy[i];
				if(!infoMan.isOnMap(nx,ny))
					continue;
				if(infoMan.tiles[nx][ny].unitID != -1 && (infoMan.tiles[nx][ny].myType == UnitType.Rocket || infoMan.tiles[nx][ny].myType == UnitType.Factory))
					return false;
			}
		}

		infoMan.tiles[x][y].updateEnemies(gc);
		if(infoMan.tiles[x][y].distFromNearestHostile < MagicNumbers.MAX_DIST_TO_CHECK)
			return false;

		MapLocation loc = new MapLocation(infoMan.myPlanet,x,y);
		
		for(WorkerSquad ws: infoMan.workerSquads){
			if(ws.targetLoc != null && ws.targetLoc.distanceSquaredTo(loc) <= 2)
				return false;
		}
		
		MapLocation n = loc.add(Direction.North);
		MapLocation s = loc.add(Direction.South);
		MapLocation e = loc.add(Direction.East);
		MapLocation w = loc.add(Direction.West);

		boolean bn = infoMan.isLocationWalkable(n);
		boolean bs = infoMan.isLocationWalkable(s);
		boolean be = infoMan.isLocationWalkable(e);
		boolean bw = infoMan.isLocationWalkable(w);

		boolean bnn = !infoMan.isOnMap(n) ? false : infoMan.startingMap.isPassableTerrainAt(n) > 0;
		boolean bss = !infoMan.isOnMap(s) ? false : infoMan.startingMap.isPassableTerrainAt(s) > 0;
		boolean bee = !infoMan.isOnMap(e) ? false : infoMan.startingMap.isPassableTerrainAt(e) > 0;
		boolean bww = !infoMan.isOnMap(w) ? false : infoMan.startingMap.isPassableTerrainAt(w) > 0;
		
		if(bnn == bn && bss == bs && bee == be && bww == bw)
			return(bn || bs) && (be || bw);
		return bn && bs && be && bw;
	}

	public void update(Nav nav){
		if(infoMan.myPlanet== Planet.Earth) {
			ArrayList<WorkerSquad> toRemove = new ArrayList<WorkerSquad>();
			for(WorkerSquad ws: infoMan.workerSquads){
				if(ws.objective == Objective.NONE){
					for(int uid: ws.units){
						// Utils.log("uu adding " + uid);
						infoMan.unassignedUnits.add(uid);
					}
					toRemove.add(ws);
				}
			}

			for(WorkerSquad ws: toRemove)
				infoMan.workerSquads.remove(ws);
			
			assignUnassignedUnits();

			while(strat.rocketsToBuild > 0){
				if(createBuildSquad(UnitType.Rocket,true)){
					strat.rocketsToBuild--;
				}
				else{
					break;
				}
			}

			if(infoMan.factories.size() < strat.maxFactories && infoMan.factoriesToBeBuilt == 0) {
				boolean mustSteal = (strat.minFactories > infoMan.factories.size());
				if(gc.karbonite() >= 200)
					createBuildSquad(UnitType.Factory, mustSteal);
			}
			
			infoMan.moneyToSave = 150 * strat.rocketsToBuild +
								  200 * infoMan.factoriesToBeBuilt;
			
    		Utils.log("money to save = " + infoMan.moneyToSave);
    		Utils.log("factories to be built = " + infoMan.factoriesToBeBuilt);
			
			if(infoMan.workerCount < strat.maxWorkers){
				tellWorkersToReplicate();
			}
			
		}
		else{
			assignUnassignedUnits();
			if(infoMan.workerCount < strat.maxWorkers){
				tellWorkersToReplicate();
			}
		}

	}
	
	private void tellWorkersToReplicate() {
		//give each miner a score indicating whether we should steal them
		//if we must steal or if the score is higher than a certain threshold, steal that miner and up to 7 miners within a magic num of it.
		int mustRepNum = (strat.minWorkers > infoMan.workerCount ? strat.minWorkers - infoMan.workerCount : 0);
		// Utils.log("Number to Rep: "+mustRepNum);
		int maxToRep = (int) (mustRepNum + (((gc.karbonite() - infoMan.moneyToSave) / 60) > 0 ? ((gc.karbonite() - infoMan.moneyToSave) / 60) : 0));
		if(maxToRep == 0)
			return;
		maxToRep = (maxToRep > strat.maxWorkers - infoMan.workerCount ? strat.maxWorkers - infoMan.workerCount : maxToRep);
		TreeMap<Double,ArrayList<Integer>> replicateScores = new TreeMap<Double,ArrayList<Integer>>();
	
		for(WorkerSquad ws: infoMan.workerSquads) {
			for(int u: ws.units) {
				Unit temp = gc.unit(u);
				if(temp.abilityHeat() >= 10 || !temp.location().isOnMap())
					continue;
				double score = replicateScore(temp,ws);
				ArrayList<Integer> al = new ArrayList<Integer>();
				if(replicateScores.containsKey(score)) {
					al = replicateScores.get(score);
				}
				al.add(u);
				replicateScores.put(score, al);
			}
		}	
		
		int numAdded = 0;
		for(Double score: replicateScores.descendingKeySet()) {
			for(int toRep: replicateScores.get(score)) {
				if(numAdded >= mustRepNum && score < MagicNumbers.MIN_SCORE_TO_REPLICATE || numAdded == maxToRep)
					break;
				infoMan.workersToRep.add(toRep);
				numAdded++;
			}
			if(numAdded >= mustRepNum && score < MagicNumbers.MIN_SCORE_TO_REPLICATE || numAdded == maxToRep)
				break;
		}
		
	}

	private double replicateScore(Unit u, WorkerSquad ws) {
		long numKarbLeftInArea = 0; 
		long distToKarbonite = 100;
		MapLocation karbLoc;
		if(ws.targetKarbLocs.containsKey(u.id())){
			MapLocation targetKarbLoc = ws.targetKarbLocs.get(u.id());
			if(targetKarbLoc == null)
				return 0;
			if(infoMan.tiles[targetKarbLoc.getX()][targetKarbLoc.getY()].karbonite == 0)
				karbLoc = infoMan.getClosestKarbonite(u.location().mapLocation());
			else
				karbLoc = targetKarbLoc;
			if(karbLoc == null)
				return 0;
		}
		else{
			karbLoc = infoMan.getClosestKarbonite(u.location().mapLocation());
			ws.targetKarbLocs.put(u.id(), karbLoc);
			if(karbLoc == null)
				return 0;
		}
		
		if(ws.targetKarbLocs.containsKey(u.id())) {
			karbLoc = ws.targetKarbLocs.get(u.id());
				distToKarbonite = u.location().mapLocation().distanceSquaredTo(karbLoc);
				numKarbLeftInArea = infoMan.tiles[karbLoc.getX()][karbLoc.getY()].karbArea.karbonite;
		}
		//Utils.log("checking for sanity: " + numKarbLeftInArea);
		//Utils.log("The region im looking at has a size of " + infoMan.tiles[karbLoc.getX()][karbLoc.getY()].karbArea.tiles.size() + " and has this much karbonite on it: " + infoMan.tiles[karbLoc.getX()][karbLoc.getY()].karbArea.karbonite);
		double score = strat.getReplicateScore(numKarbLeftInArea,ws.units.size(),distToKarbonite);
		//Utils.log("This unit: " + u.location().mapLocation() + " has a score of: "+ score);
		return (score <= 100 ? score : 100);
	}

	private void assignUnassignedUnits() {
		boolean didSomething = false;
		while(infoMan.unassignedUnits.size() > 0) {
			didSomething = false;
			int toR = 0;
			infoMan.workerSquads.sort(Squad.byUrgency());
			for(WorkerSquad ws : infoMan.workerSquads) {
				if(ws.objective == Objective.BUILD && ws.units.size() == MagicNumbers.MAX_WORKERS_PER_BUILDING)
					continue;
				for(int i : infoMan.unassignedUnits) {
					Unit a = gc.unit(i);
					if(!a.location().isOnMap() || a.unitType() != UnitType.Worker)
						continue;
					Unit wsunit = null;
					if(ws.units.size() > 0)
						wsunit = gc.unit(ws.units.get(0));
					if(ws.units.size() == 0 || !wsunit.location().isOnMap() || infoMan.isReachable(wsunit.location().mapLocation(),a.location().mapLocation())){
						ws.units.add(i);
						toR = i;
						//Utils.log("1 adding " + i);
						ws.update();
						didSomething = true;
						break;
					}
				}
				if(didSomething){
					infoMan.unassignedUnits.remove(toR);
					break;
				}
			}
			if(!didSomething) {
				HashSet<Integer> toRemove = new HashSet<Integer>();
				for(int i : infoMan.unassignedUnits) {
					Unit a = gc.unit(i);
					if(!a.location().isOnMap())
						continue;
					if(a.unitType() == UnitType.Worker) {
						WorkerSquad wsn = new WorkerSquad(infoMan,strat);
						wsn.objective = Objective.MINE;
						wsn.units.add(i);
						//Utils.log("2 adding " + i);
						toRemove.add(i);
						wsn.update();
						infoMan.workerSquads.add(wsn);
						didSomething = true;
						break;
					}
				}
				for(int id: toRemove){
					infoMan.unassignedUnits.remove(id);
				}
			}

			if(!didSomething)
				break;
		}
	}

	private boolean createBuildSquad(UnitType type, boolean mustSteal){
		//give each miner a score indicating whether we should steal them
		//if we must steal or if the score is higher than a certain threshold, steal that miner and up to 7 miners within a magic num of it.
		double bestScore = -99999;
		int lameMiner = -1;
		WorkerSquad lameSquad = infoMan.workerSquads.get(0);
		for(WorkerSquad ws: infoMan.workerSquads) {
			if(ws.objective != Objective.MINE)
				continue;
			for(int u: ws.units) {
				if(!gc.unit(u).location().isOnMap())
					continue;
				double score = lameScore(u,ws);
				if(score > bestScore) {
					lameMiner = u;
					bestScore = score;
					lameSquad = ws;
				}
			}
		}
		if(lameMiner != -1 && (bestScore > MagicNumbers.MINIMUM_SCORE_TO_STEAL || mustSteal)) {
			switch(type){
			case Factory: infoMan.factoriesToBeBuilt++;
			break;
			default: infoMan.rocketsToBeBuilt++;
			break;
			}
			WorkerSquad newSquad = new WorkerSquad(infoMan,strat);
			newSquad.objective = Objective.BUILD;
			newSquad.toBuild = type; 
			HashSet<Integer> toSteal = new HashSet<Integer>();
			TreeSet<Integer> distances = new TreeSet<Integer>();
			HashMap<Integer,ArrayList<Integer>> distToID = new HashMap<Integer,ArrayList<Integer>>();
			toSteal.add(lameMiner);
			MapLocation lmloc = gc.unit(lameMiner).location().mapLocation();
			for(int u: lameSquad.units) {
				
				if(u == lameMiner || !gc.unit(u).location().isOnMap() || !infoMan.isReachable(gc.unit(u).location().mapLocation(), lmloc))
					continue;
				int dist = (int)gc.unit(u).location().mapLocation().distanceSquaredTo(lmloc);
				distances.add(dist);
				if(distToID.containsKey(dist)) {
					ArrayList<Integer> al = distToID.get(dist);
					al.add(u);
					distToID.put(dist, al);
				}
				else {
					ArrayList<Integer> al = new ArrayList<Integer>();
					al.add(u);
					distToID.put(dist, al);
				}
			}
			for(int d: distances) {
				if(d <= MagicNumbers.MAX_DIST_TO_STEAL) {
					for(int toAdd: distToID.get(d)) {
						toSteal.add(toAdd);
						if(toSteal.size() == MagicNumbers.MAX_WORKERS_PER_BUILDING)
							break;
					}
				}
				else {
					break;
				}
				if(toSteal.size() == MagicNumbers.MAX_WORKERS_PER_BUILDING)
					break;
			}
			for(int id: toSteal) {
				lameSquad.removeUnit(id);
				newSquad.units.add(id);
				//Utils.log("3 adding " + id);
			}
			newSquad.targetLoc = findBuildLoc(newSquad);
			infoMan.workerSquads.add(newSquad);
			return true;
		}
		return false;
	}
	
	private MapLocation findBuildLoc(WorkerSquad newSquad) {
		MapLocation start = Utils.averageMapLocation(gc, newSquad.units);
		MapLocation sampleLoc = gc.unit(newSquad.units.get(0)).location().mapLocation();
		int x = start.getX();
		int y = start.getY();
		if(okayToBuild(x,y,newSquad.toBuild))
			return start;
		int increment = 1;
		boolean dir = true;
		for(int n = 0; n < 100; n++){
			if(dir){
				for(int a = 0; a < increment; a++){
					y--;
					if(infoMan.isReachable(x, y, sampleLoc) && okayToBuild(x,y,newSquad.toBuild))
						return new MapLocation(infoMan.myPlanet,x,y);
				}
				for(int a = 0; a < increment; a++){
					x++;
					if(infoMan.isReachable(x, y, sampleLoc) && okayToBuild(x,y,newSquad.toBuild))
						return new MapLocation(infoMan.myPlanet,x,y);
				}
				increment++;
				dir = false;
			}
			else{
				for(int a = 0; a < increment; a++){
					y++;
					if(infoMan.isReachable(x, y, sampleLoc) && okayToBuild(x,y,newSquad.toBuild))
						return new MapLocation(infoMan.myPlanet,x,y);
				}
				for(int a = 0; a < increment; a++){
					x--;
					if(infoMan.isReachable(x, y, sampleLoc) && okayToBuild(x,y,newSquad.toBuild))
						return new MapLocation(infoMan.myPlanet,x,y);
				}
				increment++;
				dir = true;
			}
		}
		return null;
	}

	public double lameScore(int id, WorkerSquad ws) {
		//if it can mine return a lower number
		//else return a larger number (meaning its lame)
		//TODO improve
		long distToKarbonite = 100;
		if(ws.targetKarbLocs.containsKey(id)) {
			MapLocation karbLoc = ws.targetKarbLocs.get(id);
			if(karbLoc != null)
				distToKarbonite = gc.unit(id).location().mapLocation().distanceSquaredTo(karbLoc);
		}
		else{
			Double distToK = infoMan.distToClosestKarbonite(gc.unit(id).location().mapLocation());
			if(distToK != null)
				distToKarbonite = distToK.longValue();
		}
		//Utils.log("giving out a lame score of " + distToKarbonite);
		return  distToKarbonite > 100 ? 100 : distToKarbonite;
	}
	
	/* unused
	public void produceRocket(){
		// choose a squad or create a new one
		WorkerSquad ws = null;
		for (WorkerSquad w : infoMan.workerSquads){
			if (w.objective == Objective.MINE || infoMan.factories.size() > 1 && w.objective == Objective.BUILD && w.toBuild == UnitType.Factory){
				w.objective = Objective.BUILD;
				w.toBuild = UnitType.Rocket;
				w.targetLoc = null;
				w.update();
				ws = w;
				Utils.log("creating new ws");
				break;
			}
		}
		if (ws == null){
			ws = new WorkerSquad(gc,infoMan,strat);
			ws.objective = Objective.BUILD;
			ws.toBuild = UnitType.Rocket;
			ws.targetLoc = null;
			ws.update();
		}
		infoMan.workerSquads.add(ws);
	}*/
}

