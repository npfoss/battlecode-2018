/****************/
/* REFACTOR ME! */
/****************/

import bc.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.function.BiFunction;

// REFACTOR: oops missed a description... help

public class CombatSquad extends Squad{

	//keep track of units into two groups: those with the main swarm and those separated from it
	HashMap<Integer,CombatUnit> combatUnits; //ID to CombatUnit
	//ArrayList<Integer> separatedUnits;// add back in
	MapLocation swarmLoc;
	int numEnemyUnits;
	int goalRangerDistance;
	int[] unitCounts;
	int[] unitCompGoal;

/************ ALL THE THINGS TO TWEAK ****************/

	public double testScore(Tile t, CombatUnit cu){
		return 10.0;
	}

	public double rangerMoveScore(Tile t, CombatUnit cu){
		return t.distFromNearestHostile * MagicNumbers.HOSTILE_FACTOR_RANGER_MOVE
			- (t.distFromNearestHostile - goalRangerDistance > 0 ? t.distFromNearestHostile - goalRangerDistance : 0) * MagicNumbers.DISTANCE_FACTOR_RANGER_MOVE
			- t.possibleDamage * MagicNumbers.DAMAGE_FACTOR_RANGER_MOVE
			- t.myLoc.distanceSquaredTo(swarmLoc) * MagicNumbers.SWARM_FACTOR_RANGER_MOVE
			- t.myLoc.distanceSquaredTo(targetLoc) * MagicNumbers.TARGET_FACTOR_RANGER_MOVE;
	}


/******************** END TWEAKING *******************/

	public CombatSquad(GameController g, InfoManager im, int[] ucg) {
		super(im);
		combatUnits = new HashMap<Integer,CombatUnit>();
		//separatedUnits = new ArrayList<Integer>();// use or remove
		unitCounts = new int[]{0,0,0,0}; //knight,mage,ranger,healer
		unitCompGoal = ucg;
	}
	
	public void addUnit(Unit u){
		requestedUnits.remove(u.unitType());
		units.add(u.id());
		//separatedUnits.add(u.id());
		infoMan.unassignedUnits.remove(u.id());
		switch(u.unitType()){
		case Knight: unitCounts[0]++; break;
		case Mage: unitCounts[1]++; break;
		case Ranger: unitCounts[2]++; break;
		case Healer: unitCounts[3]++; break;
		default: break;
		}
		MapLocation ml;
		if(u.location().isOnMap())
			ml = u.location().mapLocation();
		else
			ml = gc.unit(u.location().structure()).location().mapLocation();
		CombatUnit cu = new CombatUnit(u, ml, 1000); // REFACTOR: why not actually calc steps to target?
		infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()].myUnit = cu.ID;
		combatUnits.put(cu.ID, cu);
		update();
	}
	
	public void removeUnit(int id){
		super.removeUnit(id);
		//if(separatedUnits.contains(id))// use or remove
			//separatedUnits.remove(separatedUnits.indexOf(id));
		removeCombatUnit(id);
		update();
	}

	private void removeCombatUnit(int id) {
		if(!combatUnits.containsKey(id))
			return;
		CombatUnit cu = combatUnits.get(id);
		infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()].myUnit = -1;
		combatUnits.remove(id);
		switch(cu.type){
		case Knight:unitCounts[0]--; break;
		case Mage:unitCounts[1]--; break;
		case Ranger:unitCounts[2]--; break;
		case Healer:unitCounts[3]--; 
		}
	}

	public void update(){
		if(objective == Objective.EXPLORE){
			requestedUnits.clear();
			requestedUnits.add(UnitType.Ranger);
			requestedUnits.add(UnitType.Healer);
			requestedUnits.add(UnitType.Mage);
			requestedUnits.add(UnitType.Knight);
			urgency = 0;
			return;
		}

		swarmLoc = targetLoc;
		if(combatUnits.size() > 0)
			swarmLoc = Utils.averageMapLocation(gc, combatUnits.values());
		if(infoMan.myPlanet == Planet.Mars){
			requestedUnits.clear();
			requestedUnits.add(UnitType.Ranger);
			requestedUnits.add(UnitType.Healer);
			requestedUnits.add(UnitType.Mage);
			requestedUnits.add(UnitType.Knight);
		}
		else if(requestedUnits.isEmpty())
			requestedUnits.add(getRequestedUnit());

		if(units.size() == 0)
			urgency = 100;
		else
			urgency = (numEnemyUnits * 2 - units.size() + 15) * 10; // TODO: tweak this formula, probably put it in strategy
		if(urgency < 0)
			urgency = 0;
		if(urgency > 100)
			urgency = 100;
	}

	private UnitType getRequestedUnit() {
		int bestIndex = 0;
		int bestScore = 10000;
		for(int i = 0; i < 4; i++){
			if(unitCompGoal[i] == 0)
				continue;
			int score = unitCounts[i] / unitCompGoal[i];
			if(score < bestScore){
				bestScore = score;
				bestIndex = i;
			}
		}
		return Utils.robotTypes[bestIndex];
	}

	public void move(Nav nav){
		if(units.size() == 0)
			return;

		numEnemyUnits = infoMan.getTargetUnits(swarmLoc, MagicNumbers.ENEMY_UNIT_DIST_THRESHOLD, false).size();

		if(objective == Objective.EXPLORE){
			infoMan.logTimeCheckpoint("start of explore move");
			// Utils.log("swarm size = " + units.size() + " obj = " + objective + " urgency = " + urgency);
			explore(nav);
			if(infoMan.combatSquads.size() > 1)
				objective = Objective.NONE; // @eli to @eli : move to manager
			infoMan.logTimeCheckpoint("done with CombatSquad move");
			return;
		}
		//reassign separated units to swarm if appropriate
		/* use or remove
		if(combatUnits.size()==0){
			for(int id: separatedUnits){
				Unit u = gc.unit(id);
				if(!u.location().isOnMap())
					continue;
				CombatUnit cu = new CombatUnit(id,u.damage(),u.health(),u.movementHeat()<10,u.attackHeat()<10,
						u.location().mapLocation(),u.unitType(),nav.optimalStepsTo(u.location().mapLocation(), targetLoc));
				infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()].myUnit = cu.ID;
				//System.out.println("adding " + cu.ID + " 1");
				//System.out.flush();
				combatUnits.put(cu.ID,cu);
			}
			separatedUnits.clear();
		}
		//TODO: think about if this is actually a good threshold
		int swarmThreshold = combatUnits.size()*2 + 10;
		for(int i = separatedUnits.size()-1; i>=0; i--){
			Unit u = gc.unit(separatedUnits.get(i));
			if(!u.location().isOnMap())
				continue;
			MapLocation ml = u.location().mapLocation();
			if(ml.distanceSquaredTo(swarmLoc) <= swarmThreshold ||
				infoMan.getTargetUnits(ml, MagicNumbers.MAX_DIST_THEY_COULD_HIT, false).size() > 0){
				separatedUnits.remove(i);
				CombatUnit cu = new CombatUnit(u.id(),u.damage(),u.health(),u.movementHeat()<10,u.attackHeat()<10,
						ml,u.unitType(),nav.optimalStepsTo(ml, targetLoc));
				infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()].myUnit = cu.ID;
				//System.out.println("adding " + cu.ID + " 2");
				//System.out.flush();
				combatUnits.put(cu.ID,cu);
				swarmThreshold+=2;
			}
		}*/
		Utils.log("ovr size = " + units.size() + " swarm size = " + combatUnits.size() + " obj = " + objective + " swarmLoc = " + swarmLoc + " targetLoc = " + targetLoc  
			  + " urgency = " + urgency);
		//moveToSwarm(nav);
		boolean retreat = shouldWeRetreat();
		infoMan.logTimeCheckpoint("starting micro");
		doSquadMicro(retreat, nav);
		//check if we're done with our objective
		if(areWeDone()){
			Utils.log("setting obj to none");
			objective = Objective.NONE;
		}
		infoMan.logTimeCheckpoint("done with CombatSquad move");
	}

	private boolean areWeDone(){
		switch(objective){
		case ATTACK_LOC: return gc.senseNearbyUnitsByTeam(targetLoc, 5, gc.team()).size() > 0 // get rid of gc call // make 5 magicnum
							&& infoMan.getTargetUnits(targetLoc, MagicNumbers.SQUAD_SEPARATION_THRESHOLD, false).size() == 0;
		case DEFEND_LOC: return infoMan.getTargetUnits(targetLoc, 100, false).size() == 0; // 100 magic num (also somewhere else)
		default: return false;
		}
	}
	
	/* use or remove
	private void moveToSwarm(Nav nav){
		//TODO: micro more if you see enemies on the way
		for(int uid: separatedUnits){
			if(!gc.isMoveReady(uid))
				continue;
			Unit u = gc.unit(uid);
			if(!u.location().isOnMap())
				continue;
			Direction moveDir = nav.dirToMove(u.location().mapLocation(),targetLoc);
			if(gc.canMove(uid, moveDir))
				gc.moveRobot(uid, moveDir);
		}
	}*/

	private void explore(Nav nav){
		Direction dirToMove = Utils.orderedDirections[(int) (8*Math.random())];
		for(int uid: units){
			if(gc.canMove(uid, dirToMove) && gc.unit(uid).movementHeat() < 10)
				gc.moveRobot(uid, dirToMove);
		}
	}

/***************************** SHARED MICRO STUFF ************************************/

	private void doSquadMicro(boolean retreat, Nav nav){
		/*
		 * General goals:
		 * 1. Maximize priority of enemy units killed
		 * 2. Maximize damage to enemy 
		 * 3. Minimize possible damage done to us next turn.
		 * 4. If not retreating, minimize distance between us and the enemy.
		 */

		TreeSet<CombatUnit> healers = new TreeSet<CombatUnit>(new AscendingStepsComp());
		TreeSet<CombatUnit> rangers = new TreeSet<CombatUnit>(new AscendingStepsComp());
		TreeSet<CombatUnit> knights = new TreeSet<CombatUnit>(new AscendingStepsComp());
		TreeSet<CombatUnit> mages = new TreeSet<CombatUnit>(new AscendingStepsComp());

		//update combat units and tiles near them
		/*long last = System.nanoTime(); // use or remove
		long enemiesAccum = 0;
		long updateAccum = 0;
		long otherAccum = 0;*/
		int x, y, nx, ny;
		goalRangerDistance = 9999;
		for(CombatUnit cu: combatUnits.values()){
			cu.update(gc, nav.optimalStepsTo(cu.myLoc, targetLoc));
			if(cu.notOnMap)
				continue;
			//updateAccum += System.nanoTime() - last; // use or remove
			//last = System.nanoTime();
			x = cu.myLoc.getX();
			y = cu.myLoc.getY();
			infoMan.tiles[x][y].containsUnit = true;
			infoMan.tiles[x][y].containsUpdated = true;
			//otherAccum += System.nanoTime() - last;// use or remove
			//last = System.nanoTime();
			infoMan.tiles[x][y].updateEnemies(gc);
			//enemiesAccum += System.nanoTime() - last;// use or remove
			//last = System.nanoTime();
			if(cu.type == UnitType.Ranger && infoMan.tiles[x][y].distFromNearestHostile < goalRangerDistance){
				goalRangerDistance = infoMan.tiles[x][y].distFromNearestHostile;
			}
			cu.distFromNearestHostile = infoMan.tiles[x][y].distFromNearestHostile;
			if(cu.canMove){
				//System.out.println("microing unit " + cu.ID + " x = " + x + " y = " + y);
				for(int i = 0; i < 8; i++){
					nx = x + Utils.dx[i];
					ny = y + Utils.dy[i];
					if(!infoMan.isOnMap(nx, ny) || !infoMan.tiles[nx][ny].isWalkable)
						continue;
					infoMan.tiles[nx][ny].updateContains(gc);
					//otherAccum += System.nanoTime() - last;// use or remove
					//last = System.nanoTime();
					infoMan.tiles[nx][ny].updateEnemies(gc);
					//enemiesAccum += System.nanoTime() - last;// use or remove
					//last = System.nanoTime();
				}
			}
			switch(cu.type){
			case Ranger: rangers.add(cu); break;
			case Knight: knights.add(cu); break;
			case Healer: healers.add(cu); break;
			default: mages.add(cu); 
			}
		}
		
		infoMan.logTimeCheckpoint("units and tiles updated");
		/*// use or remove
		Utils.log("updateAccum = " + updateAccum);
		Utils.log("otherAccum = " + otherAccum);
		Utils.log("enemiesAccum = " + enemiesAccum);
		*/
		
		goalRangerDistance = (goalRangerDistance < MagicNumbers.MIN_RANGER_GOAL_DIST ? MagicNumbers.MIN_RANGER_GOAL_DIST : goalRangerDistance); // magic number?

		doKnightMicro(knights, retreat, nav);
		infoMan.logTimeCheckpoint("knights microed");
		doMageMicro(mages, retreat, nav);
		infoMan.logTimeCheckpoint("mages microed");
		doRangerMicro(rangers, retreat, nav);
		infoMan.logTimeCheckpoint("rangers microed");
		doHealerMicro(healers, retreat, nav);
		infoMan.logTimeCheckpoint("healers microed");
		//update each unit from heal, overcharge (for sniping)
		TreeSet<CombatUnit> updatedRangers = new TreeSet<CombatUnit>(new AscendingStepsComp());
		for(CombatUnit cu: rangers){
			updatedRangers.add(combatUnits.get(cu.ID));// REFACTOR: shouldn't have to do this, cus get updated
		}
		doSnipes(updatedRangers, retreat, nav);
		infoMan.logTimeCheckpoint("snipes done");	
	}

	private CombatUnit runAway(CombatUnit cu) {
		int x,y,nx,ny;
		x = cu.myLoc.getX();
		y = cu.myLoc.getY();
		int bestIndex = -1;
		double bestScore = -1000000;
		double score;
		for(int i = 0; i < 9; i++){
			nx = x + Utils.dx[i];
			ny = y + Utils.dy[i];
			if(!infoMan.isOnMap(nx, ny))
				continue;
			Tile t = infoMan.tiles[nx][ny];
			if(!infoMan.isLocationClear(t.myLoc))
				continue;
			score = t.distFromNearestHostile*MagicNumbers.HOSTILE_FACTOR_RUN_AWAY 
					- t.myLoc.distanceSquaredTo(swarmLoc)*MagicNumbers.SWARM_FACTOR_RUN_AWAY
					- t.possibleDamage * MagicNumbers.DAMAGE_FACTOR_RUN_AWAY;
			if(score > bestScore){
				bestScore = score;
				bestIndex = i;
			}
		}
		Direction toMove = Utils.indexToDirection(bestIndex);
		cu = moveAndUpdate(cu, toMove);
		return cu;
	}

/*************************** UNIT-SPECIFIC MICRO STUFF *****************************/

	private void doRangerMicro(TreeSet<CombatUnit> rangers, boolean retreat, Nav nav) {
		//first go through rangers which can attack already
		for(CombatUnit cu: rangers.descendingSet()){
			if(!cu.canAttack)
				continue;
			Tile myTile = infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()];
			if(myTile.enemiesWithinRangerRange.size() > 0){
				gc.attack(cu.ID, myTile.enemiesWithinRangerRange.first().ID);
				updateDamage(cu, myTile.enemiesWithinRangerRange.first());
				cu.canAttack = false;
			}
		}

		//now if retreating, run away
		if(retreat){
			for(CombatUnit cu: rangers.descendingSet()){
				if(cu.canMove){
					runAway(cu);
				}
			}
			return;
		}

		//otherwise, do moves and attacks heuristically
		for(CombatUnit cu: rangers){
			if(!cu.canMove){
				continue;
			}
			if(cu.canAttack){
				rangerMoveAndAttack(cu,nav);
			} else {
				rangerMove(cu,nav);
			}
		}
	}
	
	private void doHealerMicro(TreeSet<CombatUnit> healers, boolean retreat, Nav nav) {
		//go through healers which can heal and heal units which are low on health and close to combat
		for(CombatUnit cu: healers.descendingSet()){
			if(cu.canAttack){
				healSomeone(cu);
			}
		}
		
		//overcharge then retreat if retreating, otherwise move up then overcharge
		if(retreat){
			for(CombatUnit cu: healers.descendingSet()){
				if(cu.canOvercharge){
					performOvercharge(cu, retreat, nav);
				}
				if(cu.canMove){
					runAway(cu);
				}
			}
			return;
		}
		
		for(CombatUnit cu: healers){
			if(cu.canMove){
				healerMove(cu,nav);
				if(cu.canAttack){
					healSomeone(cu);
				}
			}
			if(cu.canOvercharge){
				performOvercharge(cu, retreat, nav);
			}
		}
	}

	private void doMageMicro(TreeSet<CombatUnit> mages, boolean retreat, Nav nav) {
		// TODO Auto-generated method stub
	}

	private void doKnightMicro(TreeSet<CombatUnit> knights, boolean retreat, Nav nav) {
		// TODO Auto-generated method stub
	}
	
	private void doSnipes(TreeSet<CombatUnit> rangers, boolean retreat, Nav nav) {
		ArrayList<CombatUnit> snipers = new ArrayList<CombatUnit>();
		for(CombatUnit cu: rangers){
			if(!cu.canSnipe)
				continue;
			boolean wantToSnipe = infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()].possibleDamage == 0;
			if(wantToSnipe)
				snipers.add(cu);
		}
		
		if(snipers.size() == 0)
			return;
		
		TreeSet<TargetUnit> snipees = new TreeSet<TargetUnit>(new DescendingSnipePriorityComp());
		for(TargetUnit tu: infoMan.targetUnits.values()){
			tu.updateSnipePriority(swarmLoc);
			snipees.add(tu);
		}
		for(TargetUnit tu: snipees){
			if(tu.snipeDamageToDo <= snipers.size()*30){
				Utils.log("sniping " + tu.myLoc);
				for(int i = 0; i <= tu.snipeDamageToDo/30.0; i++){
					gc.beginSnipe(snipers.get(snipers.size()-1).ID, tu.myLoc);
					snipers.remove(snipers.size()-1);
				}
			}
		}
	}

	private CombatUnit performOvercharge(CombatUnit cu, boolean retreat, Nav nav) {
		TreeSet<CombatUnit> overchargees = getUnitsToHeal(cu.myLoc);
		boolean overchargeSomeone = false;
		CombatUnit tO = new CombatUnit(); // t0 is not very descriptive
		double bestScore = -10000;
		double score = 0;
		for(CombatUnit o: overchargees){
			switch(o.type){
			case Healer: continue;
			case Ranger: score = (gc.researchInfo().getLevel(UnitType.Ranger) == 3 ? gc.unit(o.ID).abilityHeat() * MagicNumbers.ABILITY_HEAT_OVERCHARGE_FACTOR : 0) - o.distFromNearestHostile; break;
			case Knight: score = (gc.researchInfo().getLevel(UnitType.Knight) == 3 ? gc.unit(o.ID).abilityHeat() * MagicNumbers.ABILITY_HEAT_OVERCHARGE_FACTOR : 0) - o.distFromNearestHostile; break;
			case Mage: score = (gc.researchInfo().getLevel(UnitType.Mage) == 4 ? gc.unit(o.ID).abilityHeat() * MagicNumbers.ABILITY_HEAT_OVERCHARGE_FACTOR : 0) - o.distFromNearestHostile;
			}
			if(score > bestScore){
				tO = o;
				overchargeSomeone = true;
				bestScore = score;
			}
		}
		if(overchargeSomeone){
			//Utils.log("overcharging unit " + tO.ID + " at " + tO.myLoc.getX() + " " + tO.myLoc.getY());
			gc.overcharge(cu.ID, tO.ID);
			cu.canOvercharge = false;
			tO.update(gc, nav.optimalStepsTo(tO.myLoc, targetLoc));
			//removeCombatUnit(toO);// use or remove
			TreeSet<CombatUnit> temp = new TreeSet<CombatUnit>(new AscendingStepsComp());
			temp.add(tO);
			int x = tO.myLoc.getX();
			int y = tO.myLoc.getY();
			int nx,ny;
			for(int i = 0; i < 9; i++){
				nx = x + Utils.dx[i];
				ny = y + Utils.dy[i];
				if(!infoMan.isOnMap(nx, ny) || !infoMan.tiles[nx][ny].isWalkable)
					continue;
				infoMan.tiles[nx][ny].updateContains(gc);
				infoMan.tiles[nx][ny].updateEnemies(gc);
			}
			switch(tO.type){
			case Ranger: doRangerMicro(temp,retreat,nav); break;
			case Knight: doKnightMicro(temp,retreat,nav); break;
			case Mage: doMageMicro(temp,retreat,nav); break;
			default:
			}
			//tO = combatUnits.get(tO.ID); // use or remove
			//tO.update(gc, nav.optimalStepsTo(tO.myLoc, targetLoc));
			combatUnits.put(tO.ID, tO);
			//System.out.println("adding " + temp.first().ID + " 3");
			//System.out.flush();
			//combatUnits.add(temp.first());// use or remove
		}
		return cu;
	}

	private CombatUnit healSomeone(CombatUnit cu) {
		TreeSet<CombatUnit> healees = getUnitsToHeal(cu.myLoc);
		int toHeal = -1;
		CombatUnit tH = new CombatUnit();
		double bestScore = -10000;
		for(CombatUnit h: healees){
			if(h.health + 10 >= h.maxHealth)
				continue;
			double score = -h.distFromNearestHostile - h.health * MagicNumbers.HEALER_HEALTH_FACTOR;
			if(score > bestScore){
				tH = h;
				toHeal = h.ID;
				bestScore = score;
			}
		}
		if(toHeal != -1){
			gc.heal(cu.ID, toHeal);
			cu.canAttack = false;
			switch((int)(gc.researchInfo().getLevel(UnitType.Healer))){ // REFACTOR: probably a better way to do this
			case 0: tH.health += 10; break;
			case 1: tH.health += 12; break;
			default: tH.health += 17;
			}
		}
		return cu;
	}
	
	/* not sure if it's actually faster :(// use or remove
	private static final int[] healdx = {0,0,0,0,0,1,1,1,1,1,1,2,2,2,2,2,2,3,3,3,3,3,4,4,4,4,5,5,5,
										 0,0,0,0,0,1,1,1,1,1,2,2,2,2,2,3,3,3,3,4,4,4,5,5,
										 -1,-1,-1,-1,-1,-1,-2,-2,-2,-2,-2,-2,-3,-3,-3,-3,-3,-4,-4,-4,-4,-5,-5,-5,
										 -1,-1,-1,-1,-1,-2,-2,-2,-2,-2,-3,-3,-3,-3,-4,-4,-4,-5,-5};
	private static final int[] healdy = {1,2,3,4,5,0,1,2,3,4,5,0,1,2,3,4,5,0,1,2,3,4,0,1,2,3,0,1,2,
										 -1,-2,-3,-4,-5,-1,-2,-3,-4,-5,-1,-2,-3,-4,-5,-1,-2,-3,-4,-1,-2,-3,-1,-2,
										 0,1,2,3,4,5,0,1,2,3,4,5,0,1,2,3,4,0,1,2,3,0,1,2,
										 -1,-2,-3,-4,-5,-1,-2,-3,-4,-5,-1,-2,-3,-4,-1,-2,-3,-1,-2};
	*/

	private TreeSet<CombatUnit> getUnitsToHeal(MapLocation ml){
    	TreeSet<CombatUnit> ret = new TreeSet<CombatUnit>(new AscendingStepsComp());
    	for(CombatUnit cu: combatUnits.values()){
    		int dist = (int) ml.distanceSquaredTo(cu.myLoc);
    		if(dist <= MagicNumbers.HEALER_RANGE)
    			ret.add(cu);
    	}
    	return ret;
    }

	private CombatUnit rangerMoveAndAttack(CombatUnit cu, Nav nav) {
		Tile myTile = infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()];
		//if we're not near any enemies nav, otherwise move and attack
		if(myTile.distFromNearestHostile > MagicNumbers.MAX_DIST_THEY_COULD_HIT_NEXT_TURN){
			//Utils.log("navving " + cu.myLoc.getX() + " " + cu.myLoc.getY());
			Direction d = nav.dirToMove(cu.myLoc, targetLoc);
			return moveAndUpdate(cu, d);
		}
		return (cu.health <= MagicNumbers.RANGER_RUN_AWAY_HEALTH_THRESH ? runAway(cu) : rangerMoveAndAttack(cu));
	}
	
	private CombatUnit rangerMove(CombatUnit cu, Nav nav) {
		Tile myTile = infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()];
		//if we're not near any enemies nav, otherwise move
		if(myTile.distFromNearestHostile > MagicNumbers.MAX_DIST_THEY_COULD_HIT_NEXT_TURN){
			//Utils.log("navving " + cu.myLoc.getX() + " " + cu.myLoc.getY());
			Direction d = nav.dirToMove(cu.myLoc, targetLoc);
			return moveAndUpdate(cu, d);
		}
		return (cu.health <= MagicNumbers.RANGER_RUN_AWAY_HEALTH_THRESH ? runAway(cu) : rangerMove(cu));
	}
	
	private CombatUnit healerMove(CombatUnit cu, Nav nav) {
		Tile myTile = infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()];
		//if we're not near any enemies nav, otherwise run away
		if(numEnemyUnits == 0){
			Direction d = nav.dirToMoveSafely(cu.myLoc, targetLoc);
			cu = moveAndUpdate(cu, d);
			return cu;
		}
		return healerMove(cu);
	}

	private CombatUnit healerMove(CombatUnit cu) {
		int x,y,nx,ny;
		x = cu.myLoc.getX();
		y = cu.myLoc.getY();
		int bestIndex = -1;
		double bestScore = -10000;
		double score;
		for(int i = 0; i < 9; i++){
			nx = x + Utils.dx[i];
			ny = y + Utils.dy[i];
			if(!infoMan.isOnMap(nx, ny))
				continue;
			Tile t = infoMan.tiles[nx][ny];
			if(!infoMan.isLocationClear(t.myLoc))
				continue;
			score = t.distFromNearestHostile * MagicNumbers.HOSTILE_FACTOR_HEALER_MOVE
					- (t.distFromNearestHostile - (goalRangerDistance+MagicNumbers.HEALER_RANGE) > 0 ? t.distFromNearestHostile - (goalRangerDistance+MagicNumbers.HEALER_RANGE) : 0) 
					* MagicNumbers.DISTANCE_FACTOR_RANGER_MOVE
					- t.possibleDamage * MagicNumbers.DAMAGE_FACTOR_HEALER_MOVE 
					- t.myLoc.distanceSquaredTo(swarmLoc) * MagicNumbers.SWARM_FACTOR_HEALER_MOVE;
			if(score > bestScore){
				bestScore = score;
				bestIndex = i;
			}
		}
		Direction toMove = Utils.indexToDirection(bestIndex);
		cu = moveAndUpdate(cu,toMove);
		return cu;
	}
	
	private CombatUnit rangerMove(CombatUnit cu) {
		Direction toMove = highestScoringDir(cu, true, true, this::rangerMoveScore);
		moveAndUpdate(cu, toMove);
		return cu;
	}

	public Direction highestScoringDir(CombatUnit cu, boolean includeCenter, boolean mustBeClear, BiFunction<Tile, CombatUnit, Double> scoreFunct){
		int x, y, nx, ny;
		x = cu.myLoc.getX();
		y = cu.myLoc.getY();
		int bestIndex = -1;
		double bestScore = -1000000;
		double score;
		for(int i = 0; i < 8 + (includeCenter?1:0); i++){
			nx = x + Utils.dx[i];
			ny = y + Utils.dy[i];
			if(!infoMan.isOnMap(nx, ny))
				continue;
			Tile t = infoMan.tiles[nx][ny];
			if(mustBeClear && !infoMan.isLocationClear(t.myLoc))
				continue;
			score = scoreFunct.apply(t, cu);
			if (score > bestScore){
				bestScore = score;
				bestIndex = i;
			}
		}
		return bestIndex == -1 ? null : Utils.indexToDirection(bestIndex);
	}
	
	private CombatUnit rangerMoveAndAttack(CombatUnit cu) {
		int x,y,nx,ny;
		x = cu.myLoc.getX();
		y = cu.myLoc.getY();
		// TODO: don't attack if it's not worth it to move there
		int toAttack = -1;
		int bestIndex = -1;
		double bestScore = -1000000;
		double bestNormalScore = -1000000;
		int bestNormalIndex = -1;
		double score;
		for(int i = 0; i < 9; i++){
			nx = x + Utils.dx[i];
			ny = y + Utils.dy[i];
			if(!infoMan.isOnMap(nx, ny))
				continue; // informan onMap (lots)
			Tile t = infoMan.tiles[nx][ny];
			if(!infoMan.isLocationClear(t.myLoc))
				continue;
			score = t.distFromNearestHostile * MagicNumbers.HOSTILE_FACTOR_HEALER_MOVE
					- (t.distFromNearestHostile - goalRangerDistance > 0 ? t.distFromNearestHostile - goalRangerDistance : 0) * MagicNumbers.DISTANCE_FACTOR_RANGER_MOVE
					- t.possibleDamage * MagicNumbers.DAMAGE_FACTOR_RANGER_MOVE
					- t.myLoc.distanceSquaredTo(swarmLoc) * MagicNumbers.SWARM_FACTOR_RANGER_MOVE
					- t.myLoc.distanceSquaredTo(targetLoc) * MagicNumbers.TARGET_FACTOR_RANGER_MOVE;
			if(score>bestNormalScore){
				bestNormalScore = score;
				bestNormalIndex = i;
			}
			if(t.enemiesWithinRangerRange.size()==0)
				continue;
			score = t.distFromNearestHostile * MagicNumbers.HOSTILE_FACTOR_RANGER_MOVE_ATTACK 
					- (t.distFromNearestHostile - goalRangerDistance > 0 ? t.distFromNearestHostile - goalRangerDistance : 0) * MagicNumbers.DISTANCE_FACTOR_RANGER_MOVE_ATTACK
					- t.possibleDamage * MagicNumbers.DAMAGE_FACTOR_RANGER_MOVE_ATTACK
					- t.myLoc.distanceSquaredTo(swarmLoc) * MagicNumbers.SWARM_FACTOR_RANGER_MOVE_ATTACK;
			if(score>bestScore){
				bestScore = score;
				bestIndex = i;
				toAttack = t.enemiesWithinRangerRange.first().ID;
			}
		}
		if(toAttack != -1){
			//we found someone to attack
			Direction toMove = Utils.indexToDirection(bestIndex);
			cu = moveAndUpdate(cu,toMove);
			//System.out.println("moving in direction " + bestIndex + " to loc " + cu.myLoc + " and attacking loc " + infoMan.targetUnits.get(toAttack).myLoc);
			//System.out.flush();
			gc.attack(cu.ID, toAttack);
			updateDamage(cu,infoMan.targetUnits.get(toAttack));
			cu.canAttack = false;
			return cu;
		}
		//otherwise do normal move
		//Direction d = nav.dirToMove(cu.myLoc, targetLoc);// use or remove
		//cu = moveAndUpdate(cu, d);
		//return cu;
		Direction d = Utils.indexToDirection(bestNormalIndex);
		return moveAndUpdate(cu, d);
	}

	private CombatUnit moveAndUpdate(CombatUnit cu, Direction d){
		if(d == Direction.Center)
			return cu;
		infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()].containsUnit = false;
		infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()].myUnit = -1;
		cu.canMove = false;
		cu.myLoc = cu.myLoc.add(d);
		//Utils.log(cu.ID + " moving to " + cu.myLoc.getX() + " " + cu.myLoc.getY());
		infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()].containsUnit = true;
		infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()].myUnit = cu.ID;
		cu.distFromNearestHostile = infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()].distFromNearestHostile;
		gc.moveRobot(cu.ID, d);
		return cu;
	}
	
	private void updateDamage(CombatUnit cu, TargetUnit tu){
		tu = infoMan.targetUnits.get(tu.ID);
		int damageDone = (int) (cu.damage - tu.defense);
		if(damageDone >= tu.health){
			infoMan.targetUnits.remove(tu.ID);
			infoMan.removeEnemyUnit(tu.ID, tu.type);
			for(Tile t: tu.tilesWhichHitMe){
				infoMan.tiles[t.x][t.y].removeEnemy(tu);
			}
			return;
		}
		tu.health -= damageDone;
		for(Tile t: tu.tilesWhichHitMe){
			infoMan.tiles[t.x][t.y].updateTarget(tu);
		}
		infoMan.targetUnits.put(tu.ID, tu);
	}

	private boolean shouldWeRetreat(){
		// TODO: make this better
		// put in strategy too
		return numEnemyUnits > combatUnits.size() * MagicNumbers.AGGRESION_FACTOR;
	}

/*************** utility functs that shouldn't be in Utils **************/

	public double scoreWithFunct(Tile t, CombatUnit cu, BiFunction<Tile, CombatUnit, Double> scoreFunct){
		return scoreFunct.apply(t, cu);
	}
}