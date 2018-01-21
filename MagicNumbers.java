import bc.*;

public class MagicNumbers{
	/* Options - global constants/adjustment stuff
    */
    int PI = 3;
    int SQUAD_SEPARATION_THRESHOLD = 50;
    int RANGER_MIN_RANGE = 11;
    int RANGER_RANGE = 50;
    int KNIGHT_RANGE = 2;
    int MAGE_RANGE = 30;
    int HEALER_RANGE = 30;
    int MAX_DIST_TO_CHECK = 100;
    int MAX_DIST_THEY_COULD_HIT_NEXT_TURN = 72;
    int MAX_DIST_THEY_COULD_HIT = 50;
    //the bigger, the less we care:
    double ABILITY_HEAT_OVERCHARGE_FACTOR = 50.0; 
    double HEALER_HEALTH_FACTOR = 10.0;
    //the bigger, the more we care:
    double HOSTILE_FACTOR_RUN_AWAY = 2.0; 
    double SWARM_FACTOR_RUN_AWAY = 1.0; 
    double HOSTILE_FACTOR_RANGER_MOVE = 1.0; 
    double SWARM_FACTOR_RANGER_MOVE = 1.0; 
    double DAMAGE_FACTOR_RANGER_MOVE = 100.0;
    double HOSTILE_FACTOR_HEALER_MOVE = 1.0; 
    double SWARM_FACTOR_HEALER_MOVE = 2.0; 
    double DAMAGE_FACTOR_HEALER_MOVE = 200.0;
    double HOSTILE_FACTOR_RANGER_MOVE_ATTACK = 1.0; 
    double SWARM_FACTOR_RANGER_MOVE_ATTACK = 1.0; 
    double g;
}