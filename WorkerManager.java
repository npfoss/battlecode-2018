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


	public WorkerManager(InfoManager im, GameController g){
		infoMan = im;
		gc = g;
		WorkerSquad ws = new WorkerSquad(gc,infoMan);
		ws.objective = Objective.MINE;
		ws.update();
		infoMan.workerSquads.add(ws);
	}

	public boolean okayToBuild(MapLocation loc) {
		//REFACTOR: replace gc calls by using tiles for everything
		
		if(!infoMan.isOnMap(loc))
			return false;
		
		int x = loc.getX();
		int y = loc.getY();
		if(!infoMan.tiles[x][y].isWalkable || infoMan.tiles[x][y].unitID > -1){//&& (gc.senseUnitAtLocation(loc).unitType() == UnitType.Factory|| gc.senseUnitAtLocation(loc).unitType() == UnitType.Rocket)))) {
			return false;
		}
		
		infoMan.tiles[loc.getX()][loc.getY()].updateEnemies(gc);
		if(infoMan.tiles[loc.getX()][loc.getY()].distFromNearestHostile < infoMan.magicNums.MAX_DIST_TO_CHECK)
			return false;
		
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

	public void update(Strategy strat,Nav nav){
		//Earth and Mars should probably do different things
		if(infoMan.myPlanet== Planet.Earth) {
			
			//REFACTOR: remove requiremenet of being within a certain distance, plus making new squads for no reason?
			// assign unassigned workers to build.
			boolean didSomething = false;
			while(infoMan.unassignedUnits.size() > 0) {
				didSomething = false;
				infoMan.workerSquads.sort(Squad.byUrgency());
				for(WorkerSquad ws : infoMan.workerSquads) {
					for(int i : infoMan.unassignedUnits) {
						Unit a = gc.unit(i);
						if(!a.location().isOnMap())
							continue;
						if(ws.units.size() == 0 || infoMan.isReachable(gc.unit(ws.units.get(0)).location().mapLocation(),a.location().mapLocation())){
							ws.units.add(a.id());
							infoMan.unassignedUnits.remove(i);
							ws.update();
							didSomething = true;
							break;
						}
					}
					if(didSomething)
						break;
				}
				if(!didSomething) {
					for(int i : infoMan.unassignedUnits) {
						Unit a = gc.unit(i);
						if(!a.location().isOnMap())
							continue;
						if(a.unitType() == UnitType.Worker) {
							WorkerSquad wsn = new WorkerSquad(gc,infoMan);
							wsn.objective = Objective.MINE;
							wsn.units.add(i);
							infoMan.unassignedUnits.remove(i);
							wsn.update();
							infoMan.workerSquads.add(wsn);
							didSomething = true;
							break;
						}
					}
				}

				if(!didSomething)
					break;
			}
			
			
			/*
			if(gc.round() == 1) {
				//Pick a place to build the first factory
				if(true) {
					int maxDist = 2;
					while(startingFactory1 == null && maxDist < 33) {
						VecMapLocation v =  gc.allLocationsWithin(gc.unit(infoMan.workerSquads.get(0).units.get(0)).location().mapLocation(), maxDist);
						maxDist = maxDist*2;
						for(int i= 0; i < v.size(); i++) {
							if(okayToBuild(v.get(i))) {
								startingFactory1 = v.get(i);
								infoMan.workerSquads.get(0).targetLoc = startingFactory1;
								break;
							}
						}

					}
				}
*/
				
			}

			if (gc.round() == strat.nextRocketBuild){
				// rocket!
				produceRocket();
			}

			//REFACTOR: build in opposite direction of nearest hostile instead of considering all possible locations?
			//REFACTOR: remove squads with objective set to none?

			if(infoMan.factories.size() < strat.maxFactories) {
				boolean mustSteal = (strat.minFactories > infoMan.factories.size());
				//give each miner a score indicating whether we should steal them
				//if we must steal or if the score is higher than a certain threshold, steal that miner and up to 7 miners within a magic num of it.
				double bestScore = -99999;
				int lameMiner = -1;
				WorkerSquad lameSquad = infoMan.workerSquads.get(0);
				for(WorkerSquad ws: infoMan.workerSquads) {
					if(ws.objective != Objective.MINE)
						continue;
					for(int u: ws.units) {
						double score = lameScore(u);
						if(score > bestScore) {
							lameMiner = u;
							bestScore = score;
							lameSquad = ws;
						}
					}
				}
				if(bestScore > MagicNumbers.MINIMUM_SCORE_TO_STEAL) {
					infoMan.factoriesToBeBuilt++;
					WorkerSquad newSquad = new WorkerSquad(gc,infoMan);
					newSquad.objective = Objective.BUILD;
					newSquad.toBuild = UnitType.Factory; 
					HashSet<Integer> toSteal = new HashSet<Integer>();
					TreeSet<Integer> distances = new TreeSet<Integer>();
					HashMap<Integer,Integer> distToID = new HashMap<Integer,Integer>();
					toSteal.add(lameMiner);
					MapLocation lmloc = gc.unit(lameMiner).location().mapLocation();
					for(int u: lameSquad.units) {
						if(u == lameMiner)
							continue;
						int dist = (int)gc.unit(u).location().mapLocation().distanceSquaredTo(lmloc);
						distances.add(dist);
						distToID.put(dist, u);
					}
					for(int d: distances.descendingSet()) {
						if(d > MagicNumbers.MAX_DIST_TO_STEAL)
						toSteal.add(distToID.get(d));
						if(toSteal.size() == 8)
							break;
					}
					for(int id: toSteal) {
						lameSquad.units.remove(id);
						newSquad.units.add(id);
					}
					//SPIRAL OUT SHIT
					
				}
			}

				
//			if(infoMan.factories.size() < strat.maxFactories && gc.karbonite() >= MagicNumbers.FACTORY_COST) {
//				for(WorkerSquad ws : infoMan.workerSquads) {
//					if((ws.objective == Objective.NONE  || ws.objective == Objective.MINE) && ws.units.size() > 0) {
//						// System.out.println("Trying to build a third factory");
//						int maxDist = 36;
//						VecMapLocation v =  gc.allLocationsWithin(gc.unit(ws.units.get(0)).location().mapLocation(), maxDist);
//						int maxHostileDist = 0;
//						boolean foundSomewhere = false;
//						for(int i= 0; i < v.size(); i++) {
//							if(okayToBuild(v.get(i))) {
//								int dist = infoMan.distToHostile(v.get(i));
//								if(dist>maxHostileDist){
//									maxHostileDist = dist;
//									ws.targetLoc = v.get(i);
//									foundSomewhere = true;
//								}
//								if(maxHostileDist == 250)
//									break;
//							}
//						}
//						if(foundSomewhere){
//							ws.objective = Objective.BUILD;
//							ws.toBuild = UnitType.Factory;
//						}
//							
//					}
//				}
//			}
			
			//REFACTOR: remove squads with objective set to none?
			
			
			for(WorkerSquad ws : infoMan.workerSquads) {
				if(ws.objective == Objective.NONE) {
					ws.objective = Objective.MINE;
					break;
				}
				// added for rocket stuff. TODO: make nicer
				//REFACTOR: again build based on score, try to build far away from enemies and far from friendlies to avoid crowding
				else if (ws.targetLoc == null && ws.units.size() > 0 && ws.objective == Objective.BUILD && ws.toBuild == UnitType.Rocket){
					// choose where to put the rocket
					// TODO: do this intelligently
					int maxDist = 36;
					VecMapLocation v =  gc.allLocationsWithin(gc.unit(ws.units.get(0)).location().mapLocation(), maxDist);
					int maxHostileDist = 0;
					for(int i= 0; i < v.size(); i++) {
						if(okayToBuild(v.get(i))) {
							int dist = infoMan.distToHostile(v.get(i));
							if(dist>maxHostileDist){
								maxHostileDist = dist;
								ws.targetLoc = v.get(i);
							}
							if(maxHostileDist == 150)
								break;
						}
					}
					if (ws.targetLoc != null) break;
				}
			}
		}
		else{
			//REFACTOR: just put everything in a mine squad?
			if(infoMan.workerSquads.size()==0) {
				WorkerSquad ws = new WorkerSquad(gc,infoMan);
				ws.objective = Objective.MINE;
				ws.update();
				infoMan.workerSquads.add(ws);
				Utils.log("creating new ws 1");
			}
			
			boolean didSomething = false;
			while(infoMan.unassignedUnits.size() > 0) {
				didSomething = false;
				infoMan.workerSquads.sort(Squad.byUrgency());
				boolean tryAgain = false;
				for(WorkerSquad ws : infoMan.workerSquads) {
					for(UnitType u : ws.requestedUnits) {
						for(int i : infoMan.unassignedUnits) {
							Unit a = gc.unit(i);
							if(!a.location().isOnMap())
								continue;
							if(a.unitType() == u) {
								if(ws.units.size() == 0 || infoMan.isReachable(gc.unit(ws.units.get(0)).location().mapLocation(),a.location().mapLocation()) && nav.optimalStepsTo(gc.unit(ws.units.get(0)).location().mapLocation(),a.location().mapLocation()) < 20){
									ws.requestedUnits.remove(ws.requestedUnits.indexOf(u));
									ws.units.add(a.id());
									infoMan.unassignedUnits.remove(i);
									ws.update();
									tryAgain = true;
									didSomething = true;
								}
							}
							if(tryAgain)
								break;
						}
						if(tryAgain)
							break;
					}
					if(tryAgain)
						break;
				}
				if(!tryAgain) {
					for(int i : infoMan.unassignedUnits) {
						Unit a = gc.unit(i);
						if(!a.location().isOnMap())
							continue;
						if(a.unitType() == UnitType.Worker) {
							WorkerSquad wsn = new WorkerSquad(gc,infoMan);
							wsn.objective = Objective.MINE;
							wsn.units.add(i);
							infoMan.unassignedUnits.remove(i);
							wsn.update();
							infoMan.workerSquads.add(wsn);
							didSomething = true;
							Utils.log("creating new ws 2");
							break;
						}
					}
				}

				if(!didSomething)
					break;
			}
		}

	}
public double lameScore(int id) {
	//if it can mine return a lower number
//else return a larger number (meaning its lame)
	return 0;
}
	public void produceRocket(){
		// choose a squad or create a new one
		WorkerSquad ws = null;
		for (WorkerSquad w : infoMan.workerSquads){
			if (w.objective == Objective.MINE/* || infoMan.factories.size() > 1 && w.objective == Objective.BUILD && w.toBuild == UnitType.Factory*/){
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
			ws = new WorkerSquad(gc,infoMan);
			ws.objective = Objective.BUILD;
			ws.toBuild = UnitType.Rocket;
			ws.targetLoc = null;
			ws.update();
		}
		infoMan.workerSquads.add(ws);
	}
}

