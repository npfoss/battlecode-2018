import bc.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Comparator;
import java.util.HashMap;

public class CombatSquad extends Squad{

	//keep track of units into two groups: those with the main swarm and those separated from it
	HashMap<Integer,CombatUnit> combatUnits; //ID to CombatUnit
	//ArrayList<Integer> separatedUnits;
	MapLocation swarmLoc;
	int numEnemyUnits;
	int goalRangerDistance;
	MagicNumbers magicNums;
	int[] unitCounts;
	int[] unitCompGoal;

	public CombatSquad(GameController g, InfoManager im, MagicNumbers mn, int[] ucg) {
		super(im);
		combatUnits = new HashMap<Integer,CombatUnit>();
		//separatedUnits = new ArrayList<Integer>();
		magicNums = mn;
		unitCounts = new int[]{0,0,0,0}; //knight,mage,ranger,healer
		unitCompGoal = ucg;
		//System.out.println("ucg = " + ucg[0] + " " + ucg[1] + " " + ucg[2] + " " + ucg[3]);
		//System.out.flush();
	}
	
	public void addUnit(Unit u){
		requestedUnits.remove(u.unitType());
		units.add(u.id());
		//separatedUnits.add(u.id());
		infoMan.unassignedUnits.remove(u.id());
		switch(u.unitType()){
		case Knight:unitCounts[0]++; break;
		case Mage:unitCounts[1]++; break;
		case Ranger:unitCounts[2]++; break;
		case Healer:unitCounts[3]++; break;
		default: break;
		}
		MapLocation ml = new MapLocation(infoMan.myPlanet,0,0);
		if(u.location().isOnMap())
			ml = u.location().mapLocation();
		else
			ml = gc.unit(u.location().structure()).location().mapLocation();
		CombatUnit cu = new CombatUnit(u.id(),u.damage(),u.health(),u.movementHeat()<10,u.attackHeat()<10,
				ml,u.unitType(),1000);
		infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()].unitID = cu.ID;
		combatUnits.put(cu.ID, cu);
		update();
	}
	
	public void removeUnit(int id){
		super.removeUnit(id);
		//if(separatedUnits.contains(id))
			//separatedUnits.remove(separatedUnits.indexOf(id));
		removeCombatUnit(id);
		update();
	}

	private void removeCombatUnit(int id) {
		if(!combatUnits.containsKey(id))
			return;
		CombatUnit cu = combatUnits.get(id);
		infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()].unitID = -1;
		combatUnits.remove(id);
		switch(cu.type){
		case Knight:unitCounts[0]--; break;
		case Mage:unitCounts[1]--; break;
		case Ranger:unitCounts[2]--; break;
		case Healer:unitCounts[3]--; 
		}
		/*
		CombatUnit toRemove  = new CombatUnit();
		boolean remove = false;
		TreeSet<CombatUnit> newCombatUnits = new TreeSet<CombatUnit>(new AscendingStepsComp());
		for(CombatUnit cu: combatUnits){
			if(cu.ID == id){
				toRemove = cu;
				remove = true;
				continue;
			}
			newCombatUnits.add(cu);
		}
		if(remove){
			combatUnits = newCombatUnits;
			switch(toRemove.type){
			case Knight:unitCounts[0]--; break;
			case Mage:unitCounts[1]--; break;
			case Ranger:unitCounts[2]--; break;
			case Healer:unitCounts[3]--; break;
			default: break;
			}
		}*/
	}

	public void update(){
		//Utils.log("hi " + objective);
		if(objective == Objective.EXPLORE){
			requestedUnits.clear();
			requestedUnits.add(UnitType.Ranger);
			requestedUnits.add(UnitType.Healer);
			requestedUnits.add(UnitType.Mage);
			requestedUnits.add(UnitType.Knight);
			urgency = 0;
			return;
		}
		if(targetLoc == null)
			return;
		swarmLoc = targetLoc;
		if(combatUnits.size() > 0)
			swarmLoc = Utils.averageMapLocation(gc, combatUnits.values());
		numEnemyUnits = infoMan.getTargetUnits(swarmLoc, magicNums.ENEMY_UNIT_DIST_THRESHOLD, false).size();
		//System.out.println("ru.size = " + requestedUnits.size());
		//System.out.flush();
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
			urgency = (numEnemyUnits * 2 - units.size() + 15) * 10;
		if(urgency < 0)
			urgency = 0;
		if(urgency>100)
			urgency = 100;
	}

	private UnitType getRequestedUnit() {
		int bestIndex = 0;
		int bestScore = 10000;
		for(int i = 0; i < 4; i++){
			if(unitCompGoal[i]==0)
				continue;
			int score = unitCounts[i]/unitCompGoal[i];
			if(score<bestScore){
				bestScore = score;
				bestIndex = i;
			}
		}
		switch(bestIndex){
			case 0: return UnitType.Knight;
			case 1: return UnitType.Mage;
			case 2: return UnitType.Ranger;
			case 3: return UnitType.Healer;
			default: return UnitType.Knight;
		}
	}

	public void move(Nav nav){
		//reassign separated units to swarm if appropriate
		if(units.size()==0)
			return;
		//System.out.println("cs here");
		if(objective == Objective.EXPLORE){
			infoMan.logTimeCheckpoint("start of explore move");
			Utils.log("swarm size = " + units.size() + " obj = " + objective + " urgency = " + urgency);
			explore(nav);
			if(infoMan.combatSquads.size() > 1)
				objective = Objective.NONE;
			infoMan.logTimeCheckpoint("done with CombatSquad move");
			return;
		}
		/*
		if(combatUnits.size()==0){
			for(int id: separatedUnits){
				Unit u = gc.unit(id);
				if(!u.location().isOnMap())
					continue;
				CombatUnit cu = new CombatUnit(id,u.damage(),u.health(),u.movementHeat()<10,u.attackHeat()<10,
						u.location().mapLocation(),u.unitType(),nav.optimalStepsTo(u.location().mapLocation(), targetLoc));
				infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()].unitID = cu.ID;
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
				infoMan.getTargetUnits(ml, magicNums.MAX_DIST_THEY_COULD_HIT, false).size() > 0){
				separatedUnits.remove(i);
				CombatUnit cu = new CombatUnit(u.id(),u.damage(),u.health(),u.movementHeat()<10,u.attackHeat()<10,
						ml,u.unitType(),nav.optimalStepsTo(ml, targetLoc));
				infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()].unitID = cu.ID;
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
		doSquadMicro(retreat,nav);
		//check if we're done with our objective
		boolean done = areWeDone();
		if(done){
			Utils.log("setting obj to none");
			objective = Objective.NONE;
		}
		update();
		infoMan.logTimeCheckpoint("done with CombatSquad move");
		/*
		for(int id : units) {
			Unit fighter = gc.unit(id);
			if(fighter.location().isInSpace() || fighter.location().isInGarrison())
				continue;
			switch (objective) {
			case EXPLORE:
				VecUnit nearby = gc.senseNearbyUnits(fighter.location().mapLocation(),50);
				for(int i=0;i<nearby.size();i++) {
					Unit other = nearby.get(i);
					if(other.team() != gc.team() && gc.isAttackReaUtils.dy(fighter.id()) && gc.canAttack(fighter.id(), other.id())) {
						gc.attack(fighter.id(),other.id());
					}
				}
				Direction dirToMove = nav.dirToExplore(fighter.location().mapLocation());
				if(gc.isMoveReaUtils.dy(id) && gc.canMove(id, dirToMove)&&!gc.unit(id).location().isInGarrison())
					gc.moveRobot(id, dirToMove);
				fighter = gc.unit(id);
				nearby = gc.senseNearbyUnits(fighter.location().mapLocation(),50);
				for(int i=0;i<nearby.size();i++) {
					Unit other = nearby.get(i);
					if(other.team() != gc.team() && gc.isAttackReaUtils.dy(fighter.id()) && gc.canAttack(fighter.id(), other.id())) {
						gc.attack(fighter.id(),other.id());
					}
				}
				break;
			default:
				break;
			}
		}*/
	}

	private boolean areWeDone(){
		switch(objective){
		case ATTACK_LOC: return gc.senseNearbyUnitsByTeam(targetLoc, 5, gc.team()).size() > 0 && infoMan.getTargetUnits(targetLoc, magicNums.SQUAD_SEPARATION_THRESHOLD,false).size() == 0;
		case DEFEND_LOC: return infoMan.getTargetUnits(targetLoc, 100, false).size() == 0;
		default: return false;
		}
	}
	
	/*
	private void moveToSwarm(Nav nav){
		//TODO: micro more if you see enemies on the way
		for(int uid: separatedUnits){
			if(!gc.isMoveReaUtils.dy(uid))
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
		/*long last = System.nanoTime();
		long enemiesAccum = 0;
		long updateAccum = 0;
		long otherAccum = 0;*/
		int x,y,nx,ny;
		for(CombatUnit cu: combatUnits.values()){
			cu.update(gc,(int) cu.myLoc.distanceSquaredTo(targetLoc));
			combatUnits.put(cu.ID, cu);
			if(cu.notOnMap)
				continue;
			//updateAccum += System.nanoTime() - last;
			//last = System.nanoTime();
			x = cu.myLoc.getX();
			y = cu.myLoc.getY();
			//otherAccum += System.nanoTime() - last;
			//last = System.nanoTime();
			infoMan.tiles[x][y].updateEnemies(gc);
			//enemiesAccum += System.nanoTime() - last;
			//last = System.nanoTime();
			if(cu.type == UnitType.Ranger && infoMan.tiles[x][y].distFromNearestHostile < goalRangerDistance)
				goalRangerDistance = infoMan.tiles[x][y].distFromNearestHostile;
			cu.distFromNearestHostile = infoMan.tiles[x][y].distFromNearestHostile;
			if(cu.canMove){
				//System.out.println("microing unit " + cu.ID + " x = " + x + " y = " + y);
				for(int i = 0; i < 9; i++){
					if(i == 4)
						continue;
					nx = x + Utils.dx[i];
					ny = y + Utils.dy[i];
					if(nx >= infoMan.width || nx<0 || ny >= infoMan.height || ny<0 || !infoMan.tiles[nx][ny].isWalkable)
						continue;
					//otherAccum += System.nanoTime() - last;
					//last = System.nanoTime();
					infoMan.tiles[nx][ny].updateEnemies(gc);
					//enemiesAccum += System.nanoTime() - last;
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
		/*
		Utils.log("updateAccum = " + updateAccum);
		Utils.log("otherAccum = " + otherAccum);
		Utils.log("enemiesAccum = " + enemiesAccum);
		*/
		
		goalRangerDistance = (goalRangerDistance < 50 ? 50: goalRangerDistance);

		doKnightMicro(knights,retreat,nav);
		doMageMicro(mages,retreat,nav);
		doRangerMicro(rangers,retreat,nav);
		infoMan.logTimeCheckpoint("rangers microed");
		doHealerMicro(healers,retreat,nav);
		infoMan.logTimeCheckpoint("healers microed");
		//update each unit from heal, overcharge
		TreeSet<CombatUnit> updatedRangers = new TreeSet<CombatUnit>(new AscendingStepsComp());
		for(CombatUnit cu: rangers){
			updatedRangers.add(combatUnits.get(cu.ID));
		}
		doSnipes(updatedRangers,retreat,nav);
		infoMan.logTimeCheckpoint("sniped done");
		
	}

	private void doRangerMicro(TreeSet<CombatUnit> rangers, boolean retreat, Nav nav) {
		//first go through rangers which can attack alreaUtils.dy
		for(CombatUnit cu: rangers.descendingSet()){
			if(!cu.canAttack)
				continue;
			Tile myTile = infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()];
			//System.out.println("trying to attack somoeone.");
			//System.out.flush();
			if(myTile.enemiesWithinRangerRange.size() > 0){
				//System.out.println("type = " + myTile.enemiesWithinRangerRange.first().type + " priority = " + myTile.enemiesWithinRangerRange.first().priority);
				//System.out.flush();
				gc.attack(cu.ID, myTile.enemiesWithinRangerRange.first().ID);
				updateDamage(cu,myTile.enemiesWithinRangerRange.first());
				cu.canAttack = false;
				combatUnits.put(cu.ID, cu);
			}
		}

		//now if retreating, run away
		if(retreat){
			for(CombatUnit cu: rangers.descendingSet()){
				if(cu.canMove){
					cu = runAway(cu);
					combatUnits.put(cu.ID, cu);
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
				cu = rangerMoveAndAttack(cu,nav);
				combatUnits.put(cu.ID, cu);
			}
			else{
				cu = rangerMove(cu,nav);
				combatUnits.put(cu.ID, cu);
			}
		}
		
	}
	
	private void doHealerMicro(TreeSet<CombatUnit> healers, boolean retreat, Nav nav) {
		//go through healers which can heal and heal units which are low on health and close to combat
		for(CombatUnit cu: healers.descendingSet()){
			if(cu.canAttack){
				cu = healSomeone(cu);
				combatUnits.put(cu.ID, cu);
			}
		}
		
		//overcharge then retreat if retreating, otherwise move up then overcharge
		if(retreat){
			for(CombatUnit cu: healers.descendingSet()){
				if(cu.canOvercharge){
					cu = performOvercharge(cu,retreat,nav);
					combatUnits.put(cu.ID, cu);
				}
				if(cu.canMove){
					cu = runAway(cu);
					combatUnits.put(cu.ID, cu);
				}
			}
			return;
		}
		
		for(CombatUnit cu: healers){
			if(cu.canMove){
				cu = healerMove(cu,nav);
				combatUnits.put(cu.ID, cu);
			}
			if(cu.canOvercharge){
				cu = performOvercharge(cu,retreat,nav);
				combatUnits.put(cu.ID, cu);
			}
		}
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
		
		if(snipers.size()==0)
			return;
		
		TreeSet<TargetUnit> snipees = new TreeSet<TargetUnit>(new DescendingSnipePriorityComp());
		for(TargetUnit tu: infoMan.targetUnits.values()){
			tu.updateSnipePriority(swarmLoc);
			snipees.add(tu);
		}
		for(TargetUnit tu: snipees){
			if(tu.snipeDamageToDo <= snipers.size()*30){
				Utils.log("sniping " + tu.myLoc);
				//System.out.flush();
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
		CombatUnit tO = new CombatUnit();
		double bestScore = -10000;
		double score = 0;
		for(CombatUnit o: overchargees){
			switch(o.type){
			case Healer: continue;
			case Ranger: score = (gc.researchInfo().getLevel(UnitType.Ranger) == 3 ? gc.unit(o.ID).abilityHeat() * magicNums.ABILITY_HEAT_OVERCHARGE_FACTOR : 0) - o.distFromNearestHostile; break;
			case Knight: score = (gc.researchInfo().getLevel(UnitType.Knight) == 3 ? gc.unit(o.ID).abilityHeat() * magicNums.ABILITY_HEAT_OVERCHARGE_FACTOR : 0) - o.distFromNearestHostile; break;
			case Mage: score = (gc.researchInfo().getLevel(UnitType.Mage) == 4 ? gc.unit(o.ID).abilityHeat() * magicNums.ABILITY_HEAT_OVERCHARGE_FACTOR : 0) - o.distFromNearestHostile;
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
			//removeCombatUnit(toO);
			TreeSet<CombatUnit> temp = new TreeSet<CombatUnit>(new AscendingStepsComp());
			temp.add(tO);
			int x = tO.myLoc.getX();
			int y = tO.myLoc.getY();
			int nx,ny;
			for(int i = 0; i < 9; i++){
				nx = x + Utils.dx[i];
				ny = y + Utils.dy[i];
				if(nx >= infoMan.width || nx<0 || ny >= infoMan.height || ny<0 || !infoMan.tiles[nx][ny].isWalkable)
					continue;
				infoMan.tiles[nx][ny].updateEnemies(gc);
			}
			switch(tO.type){
			case Ranger: doRangerMicro(temp,retreat,nav); break;
			case Knight: doKnightMicro(temp,retreat,nav); break;
			case Mage: doMageMicro(temp,retreat,nav); break;
			default:
			}
			//tO = combatUnits.get(tO.ID);
			//tO.update(gc, nav.optimalStepsTo(tO.myLoc, targetLoc));
			combatUnits.put(tO.ID, tO);
			//System.out.println("adding " + temp.first().ID + " 3");
			//System.out.flush();
			//combatUnits.add(temp.first());
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
			double score = -h.distFromNearestHostile - h.health * magicNums.HEALER_HEALTH_FACTOR;
			if(score > bestScore){
				tH = h;
				toHeal = h.ID;
				bestScore = score;
			}
		}
		if(toHeal != -1){
			gc.heal(cu.ID, toHeal);
			cu.canAttack = false;
			//removeCombatUnit(toHeal);
			switch((int)(gc.researchInfo().getLevel(UnitType.Healer))){
			case 0: tH.health += 10; break;
			case 1: tH.health += 12; break;
			default: tH.health += 17;
			}
			combatUnits.put(tH.ID, tH);
			//System.out.println("adding " + toHeal + " 4");
			//System.out.flush();
			//combatUnits.add(tH);
		}
		return cu;
	}
	
	/* not sure if it's actually faster :(
	private static final int[] healUtils.dx = {0,0,0,0,0,1,1,1,1,1,1,2,2,2,2,2,2,3,3,3,3,3,4,4,4,4,5,5,5,
										 0,0,0,0,0,1,1,1,1,1,2,2,2,2,2,3,3,3,3,4,4,4,5,5,
										 -1,-1,-1,-1,-1,-1,-2,-2,-2,-2,-2,-2,-3,-3,-3,-3,-3,-4,-4,-4,-4,-5,-5,-5,
										 -1,-1,-1,-1,-1,-2,-2,-2,-2,-2,-3,-3,-3,-3,-4,-4,-4,-5,-5};
	private static final int[] healUtils.dy = {1,2,3,4,5,0,1,2,3,4,5,0,1,2,3,4,5,0,1,2,3,4,0,1,2,3,0,1,2,
										 -1,-2,-3,-4,-5,-1,-2,-3,-4,-5,-1,-2,-3,-4,-5,-1,-2,-3,-4,-1,-2,-3,-1,-2,
										 0,1,2,3,4,5,0,1,2,3,4,5,0,1,2,3,4,0,1,2,3,0,1,2,
										 -1,-2,-3,-4,-5,-1,-2,-3,-4,-5,-1,-2,-3,-4,-1,-2,-3,-1,-2};
	*/

	private TreeSet<CombatUnit> getUnitsToHeal(MapLocation ml){
    	TreeSet<CombatUnit> ret = new TreeSet<CombatUnit>(new AscendingStepsComp());
    	for(CombatUnit cu: combatUnits.values()){
    		int dist = (int) ml.distanceSquaredTo(cu.myLoc);
    		if(dist <= magicNums.HEALER_RANGE)
    			ret.add(cu);
    	}
    	return ret;
    }

	private CombatUnit rangerMoveAndAttack(CombatUnit cu, Nav nav) {
		Tile myTile = infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()];
		//if we're not near any enemies nav, otherwise move and attack
		if(myTile.distFromNearestHostile > magicNums.MAX_DIST_THEY_COULD_HIT_NEXT_TURN){
			//Utils.log("navving " + cu.myLoc.getX() + " " + cu.myLoc.getY());
			Direction d = nav.dirToMove(cu.myLoc, targetLoc);
			return moveAndUpdate(cu,d);
		}
		return (cu.health <= magicNums.RANGER_RUN_AWAY_HEALTH_THRESH ? runAway(cu) : rangerMoveAndAttack(cu));
	}
	
	private CombatUnit rangerMove(CombatUnit cu, Nav nav) {
		Tile myTile = infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()];
		//if we're not near any enemies nav, otherwise move
		if(myTile.distFromNearestHostile > magicNums.MAX_DIST_THEY_COULD_HIT_NEXT_TURN){
			//Utils.log("navving " + cu.myLoc.getX() + " " + cu.myLoc.getY());
			Direction d = nav.dirToMove(cu.myLoc, targetLoc);
			return moveAndUpdate(cu,d);
		}
		return (cu.health <= magicNums.RANGER_RUN_AWAY_HEALTH_THRESH ? runAway(cu) : rangerMove(cu));
	}
	
	private CombatUnit healerMove(CombatUnit cu, Nav nav) {
		Tile myTile = infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()];
		//if we're not near any enemies nav, otherwise run away
		if(numEnemyUnits == 0){
			Direction d = nav.dirToMoveSafely(cu.myLoc, targetLoc);
			cu = moveAndUpdate(cu,d);
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
			if(nx<0||nx>=infoMan.width||ny<0||ny>=infoMan.height)
				continue;
			Tile t = infoMan.tiles[nx][ny];
			if(!t.isWalkable || t.containsUnit)
				continue;
			score = t.distFromNearestHostile * magicNums.HOSTILE_FACTOR_HEALER_MOVE
					- (t.distFromNearestHostile - (goalRangerDistance+magicNums.HEALER_RANGE) > 0 ? t.distFromNearestHostile - (goalRangerDistance+magicNums.HEALER_RANGE) : 0) 
					* magicNums.DISTANCE_FACTOR_RANGER_MOVE
					- t.possibleDamage * magicNums.DAMAGE_FACTOR_HEALER_MOVE 
					- t.myLoc.distanceSquaredTo(swarmLoc) * magicNums.SWARM_FACTOR_HEALER_MOVE;
			if(score>bestScore){
				bestScore = score;
				bestIndex = i;
			}
		}
		Direction toMove = indexToDirection(bestIndex);
		cu = moveAndUpdate(cu,toMove);
		return cu;
	}
	
	private CombatUnit rangerMove(CombatUnit cu) {
		int x,y,nx,ny;
		x = cu.myLoc.getX();
		y = cu.myLoc.getY();
		int bestIndex = -1;
		double bestScore = -1000000;
		double score;
		for(int i = 0; i < 9; i++){
			nx = x + Utils.dx[i];
			ny = y + Utils.dy[i];
			if(nx<0||nx>=infoMan.width||ny<0||ny>=infoMan.height)
				continue;
			Tile t = infoMan.tiles[nx][ny];
			if(!t.isWalkable || t.containsUnit)
				continue;
			score = t.distFromNearestHostile * magicNums.HOSTILE_FACTOR_RANGER_MOVE
					- (t.distFromNearestHostile - goalRangerDistance > 0 ? t.distFromNearestHostile - goalRangerDistance : 0) * magicNums.DISTANCE_FACTOR_RANGER_MOVE
					- t.possibleDamage * magicNums.DAMAGE_FACTOR_RANGER_MOVE
					- t.myLoc.distanceSquaredTo(swarmLoc) * magicNums.SWARM_FACTOR_RANGER_MOVE
					- t.myLoc.distanceSquaredTo(targetLoc) * magicNums.TARGET_FACTOR_RANGER_MOVE;
			if(score>bestScore){
				bestScore = score;
				bestIndex = i;
			}
		}
		Direction toMove = indexToDirection(bestIndex);
		cu = moveAndUpdate(cu,toMove);
		return cu;
	}
	
	private CombatUnit rangerMoveAndAttack(CombatUnit cu) {
		if(cu.health < magicNums.RANGER_RUN_AWAY_HEALTH_THRESH)
			return runAway(cu);
		int x,y,nx,ny;
		x = cu.myLoc.getX();
		y = cu.myLoc.getY();
		int toAttack = -1;
		int bestIndex = -1;
		double bestScore = -1000000;
		double bestNormalScore = -1000000;
		int bestNormalIndex = -1;
		double score;
		for(int i = 0; i < 9; i++){
			nx = x + Utils.dx[i];
			ny = y + Utils.dy[i];
			if(nx<0||nx>=infoMan.width||ny<0||ny>=infoMan.height)
				continue;
			Tile t = infoMan.tiles[nx][ny];
			if(!t.isWalkable || t.containsUnit)
				continue;
			score = t.distFromNearestHostile * magicNums.HOSTILE_FACTOR_HEALER_MOVE
					- (t.distFromNearestHostile - goalRangerDistance > 0 ? t.distFromNearestHostile - goalRangerDistance : 0) * magicNums.DISTANCE_FACTOR_RANGER_MOVE
					- t.possibleDamage * magicNums.DAMAGE_FACTOR_RANGER_MOVE
					- t.myLoc.distanceSquaredTo(swarmLoc) * magicNums.SWARM_FACTOR_RANGER_MOVE
					- t.myLoc.distanceSquaredTo(targetLoc) * magicNums.TARGET_FACTOR_RANGER_MOVE;
			if(score>bestNormalScore){
				bestNormalScore = score;
				bestNormalIndex = i;
			}
			if(t.enemiesWithinRangerRange.size()==0)
				continue;
			score = t.distFromNearestHostile * magicNums.HOSTILE_FACTOR_RANGER_MOVE_ATTACK 
					- (t.distFromNearestHostile - goalRangerDistance > 0 ? t.distFromNearestHostile - goalRangerDistance : 0) * magicNums.DISTANCE_FACTOR_RANGER_MOVE_ATTACK
					- t.possibleDamage * magicNums.DAMAGE_FACTOR_RANGER_MOVE_ATTACK
					- t.myLoc.distanceSquaredTo(swarmLoc) * magicNums.SWARM_FACTOR_RANGER_MOVE_ATTACK;
			if(score>bestScore){
				bestScore = score;
				bestIndex = i;
				toAttack = t.enemiesWithinRangerRange.first().ID;
			}
		}
		if(toAttack != -1){
			//we found someone to attack
			Direction toMove = indexToDirection(bestIndex);
			cu = moveAndUpdate(cu,toMove);
			//System.out.println("moving in direction " + bestIndex + " to loc " + cu.myLoc + " and attacking loc " + infoMan.targetUnits.get(toAttack).myLoc);
			//System.out.flush();
			gc.attack(cu.ID, toAttack);
			updateDamage(cu,infoMan.targetUnits.get(toAttack));
			cu.canAttack = false;
			return cu;
		}
		//otherwise do normal move
		//Direction d = nav.dirToMove(cu.myLoc, targetLoc);
		//cu = moveAndUpdate(cu,d);
		//return cu;
		Direction d = indexToDirection(bestNormalIndex);
		return moveAndUpdate(cu,d);
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
			if(nx<0||nx>=infoMan.width||ny<0||ny>=infoMan.height)
				continue;
			Tile t = infoMan.tiles[nx][ny];
			if(!t.isWalkable || t.containsUnit)
				continue;
			score = t.distFromNearestHostile*magicNums.HOSTILE_FACTOR_RUN_AWAY 
					- t.myLoc.distanceSquaredTo(swarmLoc)*magicNums.SWARM_FACTOR_RUN_AWAY
					- t.possibleDamage * magicNums.DAMAGE_FACTOR_RUN_AWAY;
			if(score>bestScore){
				bestScore = score;
				bestIndex = i;
			}
		}
		Direction toMove = indexToDirection(bestIndex);
		cu = moveAndUpdate(cu,toMove);
		return cu;
	}

	private Direction indexToDirection(int i){
		switch(i){
		case 0: return Direction.Southwest;
		case 1: return Direction.West;
		case 2: return Direction.Northwest;
		case 3: return Direction.South;
		case 4: return Direction.Center;
		case 5: return Direction.North;
		case 6: return Direction.Southeast;
		case 7: return Direction.East;
		case 8: return Direction.Northeast;
		default: return Direction.Center;
		}
	}

	private void doMageMicro(TreeSet<CombatUnit> mages, boolean retreat, Nav nav) {
		// TODO Auto-generated method stub
	}

	private void doKnightMicro(TreeSet<CombatUnit> knights, boolean retreat, Nav nav) {
		// TODO Auto-generated method stub
	}

	private CombatUnit moveAndUpdate(CombatUnit cu, Direction d){
		if(d==Direction.Center)
			return cu;
		infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()].containsUnit = false;
		infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()].unitID = -1;
		System.out.flush();
		cu.canMove = false;
		cu.myLoc = cu.myLoc.add(d);
		//Utils.log(cu.ID + " moving to " + cu.myLoc.getX() + " " + cu.myLoc.getY());
		infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()].containsUnit = true;
		infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()].unitID = cu.ID;
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
		//TODO: make this better
		return numEnemyUnits > combatUnits.size() * magicNums.AGGRESION_FACTOR;
	}

}

/* Below lies too complicated micro
 * 
 *
 *- Once we've accounted for killing all enemy units that can be attacked or we run out of attack-reaUtils.dy units, all that's
 *  left is determining the moves of the remaining move-reaUtils.dy units.
 *- For each remaining unit, score each tile based on either taking as little damage as possible if we're retreating
 *  or getting as close as possible to the targetLoc if we're attacking (go from highest to least health order so that
 *  higher health units move toward the front if we're attacking, opposite if retreating) while minimizing damage
 *- Process attacks of units that are attacking before moving
 *- Process all moves
 *- Process attacks of remaining attack reaUtils.dy units
 *
 *Data Structures:
 *- Tile to store which enemy units can be hit by each unit type and whether or not the tile is claimed
 *- CombatUnit to store all the various shit we need to store
 *- TargetUnit to store all the various enemy shit we need to store
 *
 *- HashMap of friendly ID to list of enemy IDs for which units we can hit for each friendly unit which can attack
 *- HashMap of enemy ID to list of friendly IDs which can hit it
 *- TreeMap of enemy ID to health remaining taking into account planned attacks
 *- HashMap of friendly ID to friendly ID which it has dependency on
 *- HashMap for friendly ID to enemy ID of attacks that should be processed before moving
 *- HashMap for friendly ID to enemy ID of attacks that should be processed after moving
 *- HashMap of friendly ID to planned move direction
 *

//Create ArrayList of CombatUnits for each unit that is either move or attack reaUtils.dy
HashMap<Integer,CombatUnit> combatants = new HashMap<Integer,CombatUnit>();
TreeSet<CombatUnit> attackReaUtils.dyCombatants = new TreeSet<CombatUnit>(new AscendingOptionsComp());
for(int uid: swarmUnits){
	Unit u = gc.unit(uid);
	if(u.movementHeat()<10 || u.attackHeat()<10){
		CombatUnit cu = new CombatUnit(uid,u.damage(),u.health(),u.attackHeat()<10,u.movementHeat()<10,u.location().mapLocation(),u.unitType());
		combatants.put(uid,cu);
		if(u.attackHeat()<10)
			attackReaUtils.dyCombatants.add(cu);
	}
}

/* For each unit that is attack-reaUtils.dy, determine which units it can hit taking into account the fact that if it is
 * move-reaUtils.dy it can move first, keeping a list of all enemies that can be hit.
 *
TreeSet<TargetUnit> targets = new TreeSet<TargetUnit>(new ascendingHealthComp());
int[] Utils.dx = {-1,-1,-1,0,0,0,1,1,1};
int[] Utils.dy = {-1,0,1,-1,0,1,-1,0,1};
int x,y,nx,ny;
for(CombatUnit cu: attackReaUtils.dyCombatants){
	x = cu.myLoc.getX();
	y = cu.myLoc.getY();
	if(cu.canMove){
		for(int i=0; i<9; i++){
			nx = x + Utils.dx[i];
			ny = y + Utils.dy[i];
			infoMan.tiles[nx][ny].updateEnemies(gc);
			if(!infoMan.tiles[nx][ny].accessible)
				continue;
			HashSet<Integer> options = infoMan.tiles[nx][ny].getEnemiesWithinRange(cu.type);
			for(int opt: options){
				cu.addOption(opt,infoMan.tiles[nx][ny]);
				TargetUnit tu = infoMan.targetUnits.get(opt);
				tu.whoCanHitMe.add(cu);
				targets.add(tu);
			}
		}
	}
	else{
		HashSet<Integer> options = infoMan.tiles[x][y].getEnemiesWithinRange(cu.type);
		for(int opt: options){
			infoMan.tiles[x][y].updateEnemies(gc);
			cu.addOption(opt,infoMan.tiles[x][y]);
			TargetUnit tu = infoMan.targetUnits.get(opt);
			tu.whoCanHitMe.add(cu);
			targets.add(tu);
		}
	}
	combatants.put(cu.ID, cu);
}

 *  - Iterate through all enemies we can hit in order of ascending remaining health. For each one:
 *	- Iterate through all our units that can hit them this turn in order of ascending total enemies that one can hit.
 *	  If it needs to move to a tile, "claim" it as being occupied by it at the end of the turn. Update the "enemy hit list"
 *    of units next to the tile it claimed. If there is a unit on the tile it claimed, first make sure you're not
 *    creating an unresolvable circular dependency. Then update a "dependency" that you need them to move before you can.
 *  - Once you've claimed enough firepower to kill the unit, move on and update the enemy hit lists accordingly.*

for(TargetUnit target: targets){
	for(CombatUnit cu: target.whoCanHitMe){
		//determine move
		Tile moveTo = null;
		ArrayList<Tile> possibleTiles = cu.attackOptions.get(target.ID);
		//calc damage and remove target if necessary
		int damageDone = (int) (cu.damage - gc.unit(target.ID).knightDefense());
	}
}
 */

/* below lies dumb micro
for(int uid: swarmUnits){
	Unit u = gc.unit(uid);
	MapLocation myLoc = u.location().mapLocation();
	if(retreat){
		//attack then move
		if(gc.isAttackReaUtils.dy(uid)){
			VecUnit possibleUnitsToAttack = gc.senseNearbyUnitsByTeam(swarmLoc, u.attackRange(), Utils.enemyTeam(gc));
			long minHealth = 1000;
			int idToAttack = 0;
			for(int i = 0; i < possibleUnitsToAttack.size(); i++){
				Unit e = possibleUnitsToAttack.get(i);
				if(e.health() < minHealth){
					minHealth = e.health();
					idToAttack = e.id();
				}
			}
			if(gc.canAttack(uid, idToAttack))
				gc.attack(uid, idToAttack);
		}
		if(gc.isMoveReaUtils.dy(uid)){
			VecUnit nearbyEnemies = gc.senseNearbyUnitsByTeam(myLoc, 100, Utils.enemyTeam(gc));
			long minDist = 1000;
			MapLocation runAwayFrom = myLoc;
			for(int i = 0; i < nearbyEnemies.size(); i++){
				Unit e = nearbyEnemies.get(i);
				long dist = myLoc.distanceSquaredTo(e.location().mapLocation());
				if(dist < minDist){
					minDist = dist;
					runAwayFrom = e.location().mapLocation();
				}
			}
			MapLocation targetL = myLoc.addMultiple(runAwayFrom.directionTo(myLoc), 5);
			Direction moveDir = nav.dirToMove(myLoc,targetL);
			if(gc.canMove(uid, moveDir))
				gc.moveRobot(uid, moveDir);
		}
	}
	else{
		//move then attack
		if(gc.isMoveReaUtils.dy(uid)){
			VecUnit nearbyEnemies = gc.senseNearbyUnitsByTeam(myLoc, 100, Utils.enemyTeam(gc));
			long minDist = 1000;
			MapLocation runAwayFrom = myLoc;
			for(int i = 0; i < nearbyEnemies.size(); i++){
				Unit e = nearbyEnemies.get(i);
				long dist = myLoc.distanceSquaredTo(e.location().mapLocation());
				if(dist < minDist){
					minDist = dist;
					runAwayFrom = e.location().mapLocation();
				}
			}
			MapLocation targetL = myLoc.addMultiple(runAwayFrom.directionTo(myLoc), 5);
			if(minDist > 40)
				targetL = runAwayFrom;
			Direction moveDir = nav.dirToMove(myLoc,targetL);
			if(gc.canMove(uid, moveDir))
				gc.moveRobot(uid, moveDir);
		}
		if(gc.isAttackReaUtils.dy(uid)){
			VecUnit possibleUnitsToAttack = gc.senseNearbyUnitsByTeam(swarmLoc, u.attackRange(), Utils.enemyTeam(gc));
			long minHealth = 1000;
			int idToAttack = 0;
			for(int i = 0; i < possibleUnitsToAttack.size(); i++){
				Unit e = possibleUnitsToAttack.get(i);
				if(e.health() < minHealth){
					minHealth = e.health();
					idToAttack = e.id();
				}
			}
			if(gc.canAttack(uid, idToAttack))
				gc.attack(uid, idToAttack);
		}
	}
}*/
