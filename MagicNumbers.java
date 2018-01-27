import bc.*;

public class MagicNumbers{
	/* Options - global constants/adjustment stuff
    */
    int SQUAD_SEPARATION_THRESHOLD = 50;
    int RANGER_MIN_RANGE = 11;
    int RANGER_RANGE = 50;
    int KNIGHT_RANGE = 2;
    int MAGE_RANGE = 30;
    int HEALER_RANGE = 30;
    int MAX_DIST_TO_CHECK = 100;
    int MAX_DIST_THEY_COULD_HIT_NEXT_TURN = 72;
    int MAX_DIST_THEY_COULD_HIT = 50;
    int ENEMY_UNIT_DIST_THRESHOLD = 200;
    static int MAX_FIGHTER_COUNT = 1000;
	static long ROUND_TO_STOP_PRODUCTION = 650;
    int RANGER_RUN_AWAY_HEALTH_THRESH = 0;
    double AGGRESION_FACTOR = 1.0; //bigger = more aggressive
    //the bigger, the more we care:
    double HEALER_HEALTH_FACTOR = 0.1;
    double ABILITY_HEAT_OVERCHARGE_FACTOR = 0.02; 
    double HOSTILE_FACTOR_RUN_AWAY = 2.0; 
    double SWARM_FACTOR_RUN_AWAY = 1.0; 
    double DAMAGE_FACTOR_RUN_AWAY = 100.0;
    double TARGET_FACTOR_RANGER_MOVE = 1.0;
    double HOSTILE_FACTOR_RANGER_MOVE = 2.0;
    double SWARM_FACTOR_RANGER_MOVE = 1.5; 
    double DAMAGE_FACTOR_RANGER_MOVE = 100.0;
    double DISTANCE_FACTOR_RANGER_MOVE = 10.0; 
    double HOSTILE_FACTOR_HEALER_MOVE = 1.0; 
    double SWARM_FACTOR_HEALER_MOVE = 1.5; 
    double DAMAGE_FACTOR_HEALER_MOVE = 200.0;
    double DISTANCE_FACTOR_HEALER_MOVE = 10.0; 
    double HOSTILE_FACTOR_RANGER_MOVE_ATTACK = 1.0; 
    double DAMAGE_FACTOR_RANGER_MOVE_ATTACK = 100.0;
    double DISTANCE_FACTOR_RANGER_MOVE_ATTACK = 10.0;
    double SWARM_FACTOR_RANGER_MOVE_ATTACK = 1.5; 
	double g;

    int EARTH_FLOOD_ROUND = 750;

    // for rockets
    // how many turns to wait before launch
    int ROCKET_COUNTDOWN = 5;
    
    //for workers
    static int FACTORY_COST = 200;
	static int MAX_WORKERS_PER_BUILDING = 6;
	static double MINIMUM_SCORE_TO_STEAL = 0;
	static int MAX_DIST_TO_STEAL = 20;
	static long KARB_SEPARATION_DISTANCE = 10;
	static Double MIN_SCORE_TO_REPLICATE = 50.0;
}