import bc.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Comparator;
import java.util.HashMap;

public class CombatSquad extends Squad{

	//keep track of units into two groups: those with the main swarm and those separated from it
	TreeSet<CombatUnit> combatUnits;
	ArrayList<Integer> separatedUnits;
	InfoManager infoMan;
	MapLocation swarmLoc;
	int numEnemyUnits;
	MagicNumbers magicNums;
	int[] unitCounts;
	final int[] dx = {-1,-1,-1,0,0,0,1,1,1};
	final int[] dy = {-1,0,1,-1,0,1,-1,0,1};

	public CombatSquad(GameController g, InfoManager im, MagicNumbers mn) {
		super(g);
		combatUnits = new TreeSet<CombatUnit>(new AscendingStepsComp());
		separatedUnits = new ArrayList<Integer>();
		infoMan = im;
		magicNums = mn;
		unitCounts = new int[]{0,0,0,0}; //knight,mage,ranger,healer
	}
	
	public void addUnit(Unit u){
		requestedUnits.remove(u);
		units.add(u.id());
		separatedUnits.add(u.id());
		infoMan.unassignedUnits.remove(u);
		switch(u.unitType()){
		case Knight:unitCounts[0]++; break;
		case Mage:unitCounts[1]++; break;
		case Ranger:unitCounts[2]++; break;
		case Healer:unitCounts[3]++; break;
		default: break;
		}
		update();
	}
	
	public void removeUnit(int index, int id){
		units.remove(index);
		if(separatedUnits.contains(id))
			separatedUnits.remove(separatedUnits.indexOf(id));
		else{
			CombatUnit toRemove  = new CombatUnit();
			boolean remove = false;
			for(CombatUnit cu: combatUnits){
				if(cu.ID == id){
					toRemove = cu;
					remove = true;
					break;
				}
			}
			if(remove){
				combatUnits.remove(toRemove);
				switch(toRemove.type){
				case Knight:unitCounts[0]--; break;
				case Mage:unitCounts[1]--; break;
				case Ranger:unitCounts[2]--; break;
				case Healer:unitCounts[3]--; break;
				default: break;
				}
			}
		}
	}

	public void update(){
		if(requestedUnits.isEmpty())
			requestedUnits.add(UnitType.Ranger);
		swarmLoc = targetLoc;
		if(units.size() > 0)
			swarmLoc = Utils.averageMapLocation(gc, combatUnits);
		numEnemyUnits = Utils.getTargetUnits(swarmLoc, 100, false, infoMan).size();
		if(units.size() == 0)
			urgency = 100;
		else
			urgency = (numEnemyUnits * 2 - units.size() + 5) * 10;
		if(urgency < 0)
			urgency = 0;
		if(urgency>100)
			urgency = 100;
	}

	public void move(Nav nav){
		//reassign separated units to swarm if appropriate
		if(units.size()==0)
			return;
		//System.out.println("cs here");
		if(objective == Objective.EXPLORE){
			explore(nav);
			objective = objective.NONE;
			return;
		}
		if(combatUnits.size()==0){
			for(int id: separatedUnits){
				Unit u = gc.unit(id);
				if(!u.location().isOnMap())
					continue;
				CombatUnit cu = new CombatUnit(id,u.damage(),u.health(),u.movementHeat()<10,u.attackHeat()<10,
						u.location().mapLocation(),u.unitType(),nav.optimalStepsTo(u.location().mapLocation(), targetLoc));
				combatUnits.add(cu);
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
				Utils.getTargetUnits(ml, 50, false, infoMan).size() > 0){
				separatedUnits.remove(i);
				CombatUnit cu = new CombatUnit(u.id(),u.damage(),u.health(),u.movementHeat()<10,u.attackHeat()<10,
						ml,u.unitType(),nav.optimalStepsTo(ml, targetLoc));
				combatUnits.add(cu);
				swarmThreshold++;
			}
		}
		System.out.println("swarm size = " + combatUnits.size() + " obj = " + objective + " swarmLoc = " + swarmLoc + " targetLoc = " + targetLoc);
		moveToSwarm(nav);
		boolean retreat = shouldWeRetreat();
		doSquadMicro(retreat,nav);
		//check if we're done with our objective
		boolean done = areWeDone();
		if(done){
			System.out.println("setting obj to none");
			System.out.flush();
			objective = Objective.NONE;
		}
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
					if(other.team() != gc.team() && gc.isAttackReady(fighter.id()) && gc.canAttack(fighter.id(), other.id())) {
						gc.attack(fighter.id(),other.id());
					}
				}
				Direction dirToMove = nav.dirToExplore(fighter.location().mapLocation());
				if(gc.isMoveReady(id) && gc.canMove(id, dirToMove)&&!gc.unit(id).location().isInGarrison())
					gc.moveRobot(id, dirToMove);
				fighter = gc.unit(id);
				nearby = gc.senseNearbyUnits(fighter.location().mapLocation(),50);
				for(int i=0;i<nearby.size();i++) {
					Unit other = nearby.get(i);
					if(other.team() != gc.team() && gc.isAttackReady(fighter.id()) && gc.canAttack(fighter.id(), other.id())) {
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
		case ATTACK_LOC: return gc.senseNearbyUnitsByTeam(targetLoc, 5, gc.team()).size() > 0 && Utils.getTargetUnits(targetLoc, magicNums.SQUAD_SEPARATION_THRESHOLD,false,infoMan).size() == 0;
		case DEFEND_LOC: return Utils.getTargetUnits(targetLoc, 100,false,infoMan).size() == 0;
		default: return false;
		}
	}

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
	}

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
		int x,y,nx,ny;
		for(CombatUnit cu: combatUnits){
			cu.update(gc);
			x = cu.myLoc.getX();
			y = cu.myLoc.getY();
			if(cu.canMove){
				//System.out.println("microing unit " + cu.ID + " x = " + x + " y = " + y);
				for(int i = 0; i < 9; i++){
					nx = x + dx[i];
					ny = y + dy[i];
					if(nx >= infoMan.width || nx<0 || ny >= infoMan.height || ny<0 || !infoMan.tiles[nx][ny].isWalkable)
						continue;
					infoMan.tiles[nx][ny].updateContains(gc);
					infoMan.tiles[nx][ny].updateEnemies(gc);
				}
			}
			else if(cu.canAttack){
				infoMan.tiles[x][y].updateContains(gc);
				infoMan.tiles[x][y].updateEnemies(gc);
			}
			switch(cu.type){
			case Ranger: rangers.add(cu); break;
			case Knight: knights.add(cu); break;
			case Healer: healers.add(cu); break;
			default: mages.add(cu); 
			}
		}

		doKnightMicro(knights,retreat,nav);
		doMageMicro(mages,retreat,nav);
		doRangerMicro(rangers,retreat,nav);
		doHealerMicro(healers,retreat,nav);


		/* Below lies too complicated micro
		 * 
		 *
		 *- Once we've accounted for killing all enemy units that can be attacked or we run out of attack-ready units, all that's
		 *  left is determining the moves of the remaining move-ready units.
		 *- For each remaining unit, score each tile based on either taking as little damage as possible if we're retreating
		 *  or getting as close as possible to the targetLoc if we're attacking (go from highest to least health order so that
		 *  higher health units move toward the front if we're attacking, opposite if retreating) while minimizing damage
		 *- Process attacks of units that are attacking before moving
		 *- Process all moves
		 *- Process attacks of remaining attack ready units
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

		//Create ArrayList of CombatUnits for each unit that is either move or attack ready
		HashMap<Integer,CombatUnit> combatants = new HashMap<Integer,CombatUnit>();
		TreeSet<CombatUnit> attackReadyCombatants = new TreeSet<CombatUnit>(new AscendingOptionsComp());
		for(int uid: swarmUnits){
			Unit u = gc.unit(uid);
			if(u.movementHeat()<10 || u.attackHeat()<10){
				CombatUnit cu = new CombatUnit(uid,u.damage(),u.health(),u.attackHeat()<10,u.movementHeat()<10,u.location().mapLocation(),u.unitType());
				combatants.put(uid,cu);
				if(u.attackHeat()<10)
					attackReadyCombatants.add(cu);
			}
		}

		/* For each unit that is attack-ready, determine which units it can hit taking into account the fact that if it is
		 * move-ready it can move first, keeping a list of all enemies that can be hit.
		 *
		TreeSet<TargetUnit> targets = new TreeSet<TargetUnit>(new ascendingHealthComp());
		int[] dx = {-1,-1,-1,0,0,0,1,1,1};
		int[] dy = {-1,0,1,-1,0,1,-1,0,1};
		int x,y,nx,ny;
		for(CombatUnit cu: attackReadyCombatants){
			x = cu.myLoc.getX();
			y = cu.myLoc.getY();
			if(cu.canMove){
				for(int i=0; i<9; i++){
					nx = x + dx[i];
					ny = y + dy[i];
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
				if(gc.isAttackReady(uid)){
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
				if(gc.isMoveReady(uid)){
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
				if(gc.isMoveReady(uid)){
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
				if(gc.isAttackReady(uid)){
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
	}

	private void doHealerMicro(TreeSet<CombatUnit> healers, boolean retreat, Nav nav) {
		// TODO Auto-generated method stub
	}

	private void doRangerMicro(TreeSet<CombatUnit> rangers, boolean retreat, Nav nav) {
		//first go through rangers which can attack already
		for(CombatUnit cu: rangers.descendingSet()){
			if(!cu.canAttack)
				continue;
			Tile myTile = infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()];
			//System.out.println("trying to atack somoeone.");
			//System.out.flush();
			if(myTile.enemiesWithinRangerRange.size() > 0){
				gc.attack(cu.ID, myTile.enemiesWithinRangerRange.first().ID);
				updateDamage(cu,myTile.enemiesWithinRangerRange.first());
				cu.canAttack = false;
			}
		}

		//now if retreating, run away
		if(retreat){
			for(CombatUnit cu: rangers.descendingSet()){
				if(cu.canMove)
					cu = runAway(cu);
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
			}
			else{
				cu = rangerMove(cu,nav);
			}
		}
	}

	private CombatUnit rangerMove(CombatUnit cu, Nav nav) {
		Tile myTile = infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()];
		//if we're not near any enemies nav, otherwise run away
		if(myTile.distFromNearestHostile == 100){
			Direction d = nav.dirToMove(cu.myLoc, targetLoc);
			cu = moveAndUpdate(cu,d);
			return cu;
		}
		return runAway(cu);
	}

	private CombatUnit rangerMoveAndAttack(CombatUnit cu, Nav nav) {
		int x,y,nx,ny;
		x = cu.myLoc.getX();
		y = cu.myLoc.getY();
		int toAttack = -1;
		int bestIndex = -1;
		int bestScore = -10000;
		int score;
		for(int i = 0; i < 9; i++){
			nx = x + dx[i];
			ny = y + dy[i];
			if(nx<0||nx>=infoMan.width||ny<0||ny>=infoMan.height)
				continue;
			Tile t = infoMan.tiles[nx][ny];
			if(!t.isWalkable || t.containsUnit || t.enemiesWithinRangerRange.size()==0)
				continue;
			score = (int) (t.distFromNearestHostile - t.myLoc.distanceSquaredTo(swarmLoc));
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
			gc.attack(cu.ID, toAttack);
			updateDamage(cu,infoMan.targetUnits.get(toAttack));
			cu.canAttack = false;
			return cu;
		}
		//otherwise just nav there
		Direction d = nav.dirToMove(cu.myLoc, targetLoc);
		cu = moveAndUpdate(cu,d);
		return cu;
	}

	private CombatUnit runAway(CombatUnit cu) {
		int x,y,nx,ny;
		x = cu.myLoc.getX();
		y = cu.myLoc.getY();
		int bestIndex = -1;
		int bestScore = -10000;
		int score;
		for(int i = 0; i < 9; i++){
			nx = x + dx[i];
			ny = y + dy[i];
			if(nx<0||nx>=infoMan.width||ny<0||ny>=infoMan.height)
				continue;
			Tile t = infoMan.tiles[nx][ny];
			if(!t.isWalkable || t.containsUnit)
				continue;
			score = (int) (t.distFromNearestHostile*2 - t.myLoc.distanceSquaredTo(swarmLoc));
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
		//System.out.println("moving to " + cu.myLoc.getX() + " " + cu.myLoc.getY());
		//System.out.flush();
		cu.canMove = false;
		cu.myLoc = cu.myLoc.add(d);
		infoMan.tiles[cu.myLoc.getX()][cu.myLoc.getY()].containsUnit = true;
		gc.moveRobot(cu.ID, d);
		return cu;
	}
	
	private void updateDamage(CombatUnit cu, TargetUnit tu){
		int damageDone = (int) (cu.damage - tu.defense);
		if(damageDone >= tu.health){
			infoMan.targetUnits.remove(tu.ID);
			infoMan.removeEnemyUnit(tu.ID, tu.type);
			for(Tile t: tu.tilesWhichHitMe){
				t.removeEnemy(tu);
			}
			return;
		}
		tu.health -= damageDone;
		for(Tile t: tu.tilesWhichHitMe){
			t.updateTarget(tu);
		}
		infoMan.targetUnits.put(tu.ID, tu);
	}

	private boolean shouldWeRetreat(){
		//TODO: make this better
		int ourUnitCount = combatUnits.size();
		int theirUnitCount = (int) gc.senseNearbyUnitsByTeam(swarmLoc, 100, Utils.enemyTeam(gc)).size();
		return theirUnitCount > ourUnitCount * 1.1;
	}

}
