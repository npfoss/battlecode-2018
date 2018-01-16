import bc.*;
import java.util.ArrayList;

public class CombatSquad extends Squad{

	//keep track of units into two groups: those with the main swarm and those separated from it
	ArrayList<Integer> swarmUnits;
	ArrayList<Integer> separatedUnits;

	public CombatSquad(GameController g) {
		super(g);
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
			if(u.location().mapLocation().distanceSquaredTo(swarmLoc) <= swarmThreshold){
				separatedUnits.remove(i);
				swarmUnits.add(u.id());
				swarmLoc = Utils.averageMapLocation(gc, swarmUnits);
				swarmThreshold++;
			}
		}
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
		//for now, loop through units individually TODO: make this smarter
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
		}
	}

	private boolean shouldWeRetreat(MapLocation swarmLoc){
		//TODO: make this better
		int ourUnitCount = swarmUnits.size();
		int theirUnitCount = (int) gc.senseNearbyUnitsByTeam(swarmLoc, 100, Utils.enemyTeam(gc)).size();
		return theirUnitCount > ourUnitCount;
	}



}