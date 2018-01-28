import bc.*;

/*
Options - global constants/adjustment stuff
*/
public class MagicNumbers{

	// example
    static double g;

    // general purpose
    static int EARTH_FLOOD_ROUND = 750;
    static int RANGER_MIN_RANGE = 11;
    static int RANGER_RANGE = 50;
    static int KNIGHT_RANGE = 2; // what about javelin?
    static int MAGE_RANGE = 30;
    static int HEALER_RANGE = 30;
    static int MAX_DIST_THEY_COULD_HIT_NEXT_TURN = 72;
    static int MAX_DIST_THEY_COULD_HIT = 50; 
    static int MAX_DIST_RUSH = 20;

    // for tweaking CombatManager
    static int SQUAD_SEPARATION_THRESHOLD = 50;
    static int SWARM_SEPARATION_THRESHOLD = 75;
	static int DEFEND_RANGE = 200;

    // for tweaking CombatSquad
    static int ENEMY_UNIT_DIST_THRESHOLD = 200;
    static int RANGER_RUN_AWAY_HEALTH_THRESH = 0;
    static int KNIGHT_RUN_AWAY_HEALTH_THRESH = 0;
    static int MIN_RANGER_GOAL_DIST = 50;
    static double AGGRESION_FACTOR = 1.0; //bigger = more aggressive
    //the bigger, the more we care:
    static double HEALER_HEALTH_FACTOR = 0.1;
    static double ABILITY_HEAT_OVERCHARGE_FACTOR = 0.02; 
    static double HOSTILE_FACTOR_RUN_AWAY = 10.0; 
    static double SWARM_FACTOR_RUN_AWAY = 1.0; 
    static double DAMAGE_FACTOR_RUN_AWAY = 100.0;
    static double TARGET_FACTOR_RANGER_MOVE = 1.0;
    static double HOSTILE_FACTOR_RANGER_MOVE = 10.0;
    static double SWARM_FACTOR_RANGER_MOVE = 1.5; 
    static double DAMAGE_FACTOR_RANGER_MOVE = 100.0;
    static double DISTANCE_FACTOR_RANGER_MOVE = 50.0; 
    static double TARGET_FACTOR_KNIGHT_MOVE = 1.0;
    static double HOSTILE_FACTOR_KNIGHT_MOVE = 100.0; // get closer to hostiles
    static double SWARM_FACTOR_KNIGHT_MOVE = 1.5; 
    static double DAMAGE_FACTOR_KNIGHT_MOVE = 10.0;
	static double ATTACK_FACTOR = 3.0;
    static double HOSTILE_FACTOR_HEALER_MOVE = 10.0; 
    static double SWARM_FACTOR_HEALER_MOVE = 2.5; 
    static double DAMAGE_FACTOR_HEALER_MOVE = 200.0;
    static double DISTANCE_FACTOR_HEALER_MOVE = 30.0; 

    // Tile stuff
    static int MAX_DIST_TO_CHECK = 100; // also in WorkerManager

    // ProductionManager
    static int MAX_FIGHTER_COUNT = 100;
    static long ROUND_TO_STOP_PRODUCTION = 675;

    //for workers
    static int FACTORY_COST = 200;
    static int MAX_WORKERS_PER_BUILDING = 6;
    static double MINIMUM_SCORE_TO_STEAL = 10;
    static int MAX_DIST_TO_STEAL = 20;
    static long KARB_SEPARATION_DISTANCE = 10;
    static Double MIN_SCORE_TO_REPLICATE = 50.0;
    static int FACTORY_SCARED_RADIUS = 150;

    // rockets
    static long SEND_EVERYTHING = 600;
    static long BUILD_UP_WORKERS = 400;
    static double FIGHTERS_PER_WORKER = 3.5;

    // unsorted
}