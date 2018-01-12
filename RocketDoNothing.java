import bc.*;
import java.util.ArrayList;

/*
for Mars
*/
public class RocketDoNothing extends RocketManager{

    public RocketDoNothing(GameController g, InfoManager im){
        super(g, im);
    }

    public void update(){
        // does nothing, or even better, self destructs the rockets once empty
    	
    }
}