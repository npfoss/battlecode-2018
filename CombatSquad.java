import bc.*;
import java.util.ArrayList;
import java.util.HashSet;

public class CombatSquad extends Squad{

	//keep track of units into two groups: those with the main swarm and those separated from it
	ArrayList<Integer> swarmUnits;
	ArrayList<Integer> separatedUnits;
	InfoManager infoMan;

	public CombatSquad(GameController g, InfoManager im) {
		super(g);
		swarmUnits = new ArrayList<Integer>();
		separatedUnits = new ArrayList<Integer>();
		infoMan = im;
	}

	public void update(){
		if(requestedUnits.isEmpty())
			requestedUnits.add(UnitType.Ranger);
	}

	public void move(Nav nav){
		//reassign separated units to swarm if appropriate
		if(units.size()==0)
			return;
		System.out.println("cs here");
		if(swarmUnits.size()==0){
			for(int id: separatedUnits){
				swarmUnits.add(id);
			}
			separatedUnits.clear();
		}
		MapLocation swarmLoc = Utils.averageMapLocation(gc, swarmUnits);
		//TODO: think about if this is actually a good threshold
		int swarmThreshold = swarmUnits.size() + 1;
		for(int i = separatedUnits.size()-1; i>=0; i--){
			Unit u = gc.unit(separatedUnits.get(i));
			if(!u.location().isOnMap())
				continue;
			if(u.location().mapLocation().distanceSquaredTo(swarmLoc) <= swarmThreshold){
				separatedUnits.remove(i);
				swarmUnits.add(u.id());
				swarmLoc = Utils.averageMapLocation(gc, swarmUnits);
				swarmThreshold++;
			}
		}
		System.out.println("swarm size = " + swarmUnits.size() + " swarmLoc = " + swarmLoc + " targetLoc = " + targetLoc);
		moveToSwarm(nav);
		boolean retreat = shouldWeRetreat(swarmLoc);
		switch(objective){
		case EXPLORE: explore(nav, swarmLoc);
		default: doSquadMicro(retreat,swarmLoc,nav);
		}
		//check if we're done with our objective
		boolean done = areWeDone(swarmLoc);
		if(done){
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
				Direction dirToMove = Utils.orderedDirections[(int) (8*Math.random())];
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

	private boolean areWeDone(MapLocation swarmLoc){
		switch(objective){
		case ATTACK_LOC: return swarmLoc.distanceSquaredTo(targetLoc) < 9 && gc.senseNearbyUnitsByTeam(swarmLoc, 25, Utils.enemyTeam(gc)).size() == 0;
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

	private void explore(Nav nav, MapLocation swarmLoc){
		Direction dirToMove = Utils.orderedDirections[(int) (8*Math.random())];
		MapLocation targetL = swarmLoc.addMultiple(dirToMove,5);
		for(int uid: swarmUnits){
			Unit u = gc.unit(uid);
			Direction moveDir = nav.dirToMove(u.location().mapLocation(),targetL);
			if(gc.canMove(uid, moveDir))
				gc.moveRobot(uid, moveDir);
		}
	}

	private void doSquadMicro(boolean retreat, MapLocation swarmLoc, Nav nav){
		/*
		 * General goals:
		 * 1. Maximize enemy units killed minus our units enemies can kill next turn
		 * 2. Maximize damage dealt by us this turn minus damage they can deal next turn
		 * 3. Minimize distance to targetLoc if not retreating and attacking
		 * 4. Minimize distance to line between targetLoc and nearest enemy if defending
		 * 5. Give special priority to minimizing damage taken by higher priority units (factories,etc.)
		 */
		
	     //TODO: deal with special abilities, healers in general if we build them , take into acount mages
		 /*
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
		 */
		
		//Create ArrayList of CombatUnits for each unit that is either move or attack ready
		ArrayList<CombatUnit> combatants = new ArrayList<CombatUnit>();
		for(int uid: swarmUnits){
			Unit u = gc.unit(uid);
			if(u.movementHeat()<10 || u.attackHeat()<10){
				CombatUnit cu = new CombatUnit(uid,u.damage(),u.health(),u.attackHeat()<10,u.movementHeat()<10,u.location().mapLocation(),u.unitType());
				combatants.add(cu);
			}
		}
		
		/* For each unit that is attack-ready, determine which units it can hit taking into account the fact that if it is
		 * move-ready it can move first, keeping a list of all enemies that can be hit.
		 */
		HashSet<Integer> targets = new HashSet<Integer>();
		int[] dx = {-1,-1,-1,0,0,0,1,1,1};
		int[] dy = {-1,0,1,-1,0,1,-1,0,1};
		int x,y,nx,ny;
		for(CombatUnit cu: combatants){
			if(cu.canAttack){
				x = cu.myLoc.getX();
				y = cu.myLoc.getY();
				if(cu.canMove){
					for(int i=0; i<9; i++){
						nx = x + dx[i];
						ny = y + dy[i];
						if(!infoMan.tiles[nx][ny].accessible)
							continue;
						HashSet<Integer> options = infoMan.tiles[nx][ny].getEnemiesWithinRange(cu.type);
						for(int opt: options){
							cu.addOption(opt,infoMan.tiles[nx][ny]);
							targets.add(opt);
							TargetUnit tu = infoMan.targetUnits.get(opt);
							tu.whoCanHitMe.add(cu.ID);
							infoMan.targetUnits.put(opt, tu);
						}
					}
				}
				else{
					HashSet<Integer> options = infoMan.tiles[x][y].getEnemiesWithinRange(cu.type);
					for(int opt: options){
						cu.addOption(opt,infoMan.tiles[x][y]);
						targets.add(opt);
						TargetUnit tu = infoMan.targetUnits.get(opt);
						tu.whoCanHitMe.add(cu.ID);
						infoMan.targetUnits.put(opt, tu);
					}
				}
			}
		}
		
		/*  - Iterate through all enemies we can hit in order of ascending remaining health. For each one:
		 *	- Iterate through all our units that can hit them this turn in order of ascending total enemies that one can hit.
		 *	  If it needs to move to a tile, "claim" it as being occupied by it at the end of the turn. Update the "enemy hit list"
		 *    of units next to the tile it claimed. If there is a unit on the tile it claimed, first make sure you're not
		 *    creating an unresolvable circular dependency. Then update a "dependency" that you need them to move before you can.
		 *  - Once you've claimed enough firepower to kill the unit, move on and update the enemy hit lists accordingly.*/
		
		
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

	private boolean shouldWeRetreat(MapLocation swarmLoc){
		//TODO: make this better
		int ourUnitCount = swarmUnits.size();
		int theirUnitCount = (int) gc.senseNearbyUnitsByTeam(swarmLoc, 100, Utils.enemyTeam(gc)).size();
		return theirUnitCount > ourUnitCount;
	}



}