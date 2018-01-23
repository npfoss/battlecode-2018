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
	GameController gc;

	MapLocation startingFactory1;
	MapLocation startingFactory2;
	MapLocation startingFactory3;

	public WorkerManager(InfoManager im, GameController g){
		infoMan = im;
		gc = g;
		startingFactory1 = null;
		startingFactory2 = null;
		startingFactory3 = null;
	}

	public boolean okayToBuild(MapLocation loc) {
		if(!gc.startingMap(Planet.Earth).onMap(loc))
			return false;

		if(!(gc.startingMap(Planet.Earth).isPassableTerrainAt(loc) > 0 && !(gc.hasUnitAtLocation(loc)))){//&& (gc.senseUnitAtLocation(loc).unitType() == UnitType.Factory|| gc.senseUnitAtLocation(loc).unitType() == UnitType.Rocket)))) {
			return false;
		}
		
		infoMan.tiles[loc.getX()][loc.getY()].updateEnemies(gc);
		if(infoMan.tiles[loc.getX()][loc.getY()].distFromNearestHostile < infoMan.magicNums.MAX_DIST_TO_CHECK)
			return false;
		
		MapLocation n = loc.add(Direction.North);
		MapLocation s = loc.add(Direction.South);
		MapLocation e = loc.add(Direction.East);
		MapLocation w = loc.add(Direction.West);
		/*MapLocation nn = n.add(Direction.North);
		MapLocation ss= s.add(Direction.South);
		MapLocation ee = e.add(Direction.East);
		MapLocation ww = w.add(Direction.West);*/
		boolean bn = !gc.startingMap(Planet.Earth).onMap(n) ? false : gc.startingMap(Planet.Earth).isPassableTerrainAt(n) > 0 && !(gc.hasUnitAtLocation(n)&& (gc.senseUnitAtLocation(n).unitType() == UnitType.Factory|| gc.senseUnitAtLocation(n).unitType() == UnitType.Rocket));
		boolean bs = !gc.startingMap(Planet.Earth).onMap(s) ? false : gc.startingMap(Planet.Earth).isPassableTerrainAt(s) > 0 && !(gc.hasUnitAtLocation(s)&& (gc.senseUnitAtLocation(s).unitType() == UnitType.Factory|| gc.senseUnitAtLocation(s).unitType() == UnitType.Rocket));
		boolean be = !gc.startingMap(Planet.Earth).onMap(e) ? false : gc.startingMap(Planet.Earth).isPassableTerrainAt(e) > 0 && !(gc.hasUnitAtLocation(e)&& (gc.senseUnitAtLocation(e).unitType() == UnitType.Factory|| gc.senseUnitAtLocation(e).unitType() == UnitType.Rocket));
		boolean bw = !gc.startingMap(Planet.Earth).onMap(w) ? false : gc.startingMap(Planet.Earth).isPassableTerrainAt(w) > 0 && !(gc.hasUnitAtLocation(w)&& (gc.senseUnitAtLocation(w).unitType() == UnitType.Factory|| gc.senseUnitAtLocation(w).unitType() == UnitType.Rocket));
		/*boolean bnn = !gc.startingMap(Planet.Earth).onMap(nn) ? false : gc.startingMap(Planet.Earth).isPassableTerrainAt(nn) > 0 && !(gc.hasUnitAtLocation(nn)&& (gc.senseUnitAtLocation(nn).unitType() == UnitType.Factory|| gc.senseUnitAtLocation(nn).unitType() == UnitType.Rocket));
		boolean bss = !gc.startingMap(Planet.Earth).onMap(ss) ? false : gc.startingMap(Planet.Earth).isPassableTerrainAt(ss) > 0 && !(gc.hasUnitAtLocation(ss)&& (gc.senseUnitAtLocation(ss).unitType() == UnitType.Factory|| gc.senseUnitAtLocation(ss).unitType() == UnitType.Rocket));
		boolean bee = !gc.startingMap(Planet.Earth).onMap(ee) ? false : gc.startingMap(Planet.Earth).isPassableTerrainAt(ee) > 0 && !(gc.hasUnitAtLocation(ee)&& (gc.senseUnitAtLocation(ee).unitType() == UnitType.Factory|| gc.senseUnitAtLocation(ee).unitType() == UnitType.Rocket));
		boolean bww = !gc.startingMap(Planet.Earth).onMap(ww) ? false : gc.startingMap(Planet.Earth).isPassableTerrainAt(ww) > 0 && !(gc.hasUnitAtLocation(ww)&& (gc.senseUnitAtLocation(ww).unitType() == UnitType.Factory|| gc.senseUnitAtLocation(ww).unitType() == UnitType.Rocket));
		if(bnn && bn || bss && bs || bee && be || bww && bw)
			return false;
		 */
		boolean bnn = !gc.startingMap(Planet.Earth).onMap(n) ? false : gc.startingMap(Planet.Earth).isPassableTerrainAt(n) > 0;
		boolean bss = !gc.startingMap(Planet.Earth).onMap(s) ? false : gc.startingMap(Planet.Earth).isPassableTerrainAt(s) > 0;
		boolean bee = !gc.startingMap(Planet.Earth).onMap(e) ? false : gc.startingMap(Planet.Earth).isPassableTerrainAt(e) > 0;
		boolean bww = !gc.startingMap(Planet.Earth).onMap(w) ? false : gc.startingMap(Planet.Earth).isPassableTerrainAt(w) > 0;
		if(bnn == bn && bss == bs && bee == be && bww == bw)
			return(bn || bs) && (be || bw);
		return bn && bs && be && bw;
	}

	public void update(Strategy strat,Nav nav){
		//Earth and Mars should probably do different things
		if(gc.planet() == Planet.Earth) {
			// create new squads if necessary
			if(infoMan.workerSquads.size()==0) {
				WorkerSquad ws = new WorkerSquad(gc,infoMan);
				ws.objective = Objective.BUILD;
				ws.update();
				infoMan.workerSquads.add(ws);
				Utils.log("creating new ws 1");
			}

			// assign unassigned workers to build.
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
							wsn.objective = Objective.BUILD;
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

				if(infoMan.workerSquads.size() > 1) {
					//Pick a place to build a second factory
					int maxDist = 2;
					while(startingFactory2 == null && maxDist < 33) {
						VecMapLocation v =  gc.allLocationsWithin(gc.unit(infoMan.workerSquads.get(1).units.get(0)).location().mapLocation(), maxDist);
						maxDist = maxDist*2;
						for(int i= 0; i < v.size(); i++) {
							if(okayToBuild(v.get(i))) {
								startingFactory2 = v.get(i);
								infoMan.workerSquads.get(1).targetLoc = startingFactory2;
								// System.out.println("Trying to build a second factory");
								break;
							}

						}
					}
				}
				if(infoMan.workerSquads.size() > 2) {
					//Pick a place to build a third factory
					int maxDist = 2;
					while(startingFactory3 == null && maxDist < 33) {
						//Utils.log("basing off " + gc.unit(infoMan.workerSquads.get(2).units.get(0)).location().mapLocation());
						VecMapLocation v =  gc.allLocationsWithin(gc.unit(infoMan.workerSquads.get(2).units.get(0)).location().mapLocation(), maxDist);
						maxDist = maxDist*2;
						for(int i= 0; i < v.size(); i++) {
							if(okayToBuild(v.get(i))) {
								startingFactory3 = v.get(i);
								infoMan.workerSquads.get(2).targetLoc = startingFactory3;
								break;
							}
						}
					}


				}
			}
			// TODO: VERY TEMPORARY
			if (gc.round() == strat.nextRocketBuild){
				// rocket!
				produceRocket();
			}

			//TODO intelligently pick locations for the next factories
			//System.out.println("My objective is: " + ((infoMan.workerSquads.get(0).objective == Objective.BUILD) ? "Building" : "NONE"));
			//System.out.println(infoMan.factories.size());

			if(infoMan.factories.size() < 4 && gc.karbonite() > 100) {
				for(WorkerSquad ws : infoMan.workerSquads) {
					if((ws.objective == Objective.NONE  || ws.objective == Objective.MINE) && ws.units.size() > 0) {
						// System.out.println("Trying to build a third factory");
						int maxDist = 36;
						VecMapLocation v =  gc.allLocationsWithin(gc.unit(ws.units.get(0)).location().mapLocation(), maxDist);
						int maxHostileDist = 0;
						boolean foundSomewhere = false;
						for(int i= 0; i < v.size(); i++) {
							if(okayToBuild(v.get(i))) {
								int dist = infoMan.distToHostile(v.get(i));
								if(dist>maxHostileDist){
									maxHostileDist = dist;
									ws.targetLoc = v.get(i);
									foundSomewhere = true;
								}
								if(maxHostileDist == 250)
									break;
							}
						}
						if(foundSomewhere){
							ws.objective = Objective.BUILD;
							ws.toBuild = UnitType.Factory;
						}
							
					}
				}
			}
			for(WorkerSquad ws : infoMan.workerSquads) {
				if(ws.objective == Objective.NONE) {
					ws.objective = Objective.MINE;
					break;
				}
				// added for rocket stuff. TODO: make nicer
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
							if(maxHostileDist == 250)
								break;
						}
					}
					if (ws.targetLoc != null) break;
				}
			}
		}
		else{
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
			ws = new WorkerSquad(gc,infoMan);
			ws.objective = Objective.BUILD;
			ws.toBuild = UnitType.Rocket;
			ws.targetLoc = null;
			ws.update();
		}
		infoMan.workerSquads.add(ws);
	}
}

