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
    int MAX_DIST_THEY_COULD_HIT_NEXT_TURN = 72;
    int MAX_DIST_THEY_COULD_HIT = 50;
    int ABILITY_HEAT_OVERCHARGE_FACTOR = 50; //the bigger, the less we care about overcharging ability heats
    int HOSTILE_FACTOR_RUN_AWAY = 2; //bigger, more we care
    int SWARM_FACTOR_RUN_AWAY = 1; //^
    int HOSTILE_FACTOR_RANGER_MOVE = 1; //^
    int SWARM_FACTOR_RANGER_MOVE = 1; //^
    double g;
}