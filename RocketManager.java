import bc.*;
import java.util.ArrayList;

/*
creates and assigns empty RocketSquads to rockets
launches rockets when it's time

Schedules rocket launches.
As the scheduled launch nears, sets up rocket squads to fill each rocket
*/
public class RocketManager{
    InfoManager infoMan;
    public static GameController gc;

    public RocketManager(GameController g, InfoManager im){
        gc = g;
        infoMan = im;
    }

    public void update(){
        // modifies rocketSquads as necessary and launches rockets
        // remember, those are in infoMan

    	
    	
        // also update() on each squad after
        
    }
}