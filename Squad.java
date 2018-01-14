import bc.*;
import java.util.ArrayList;
import java.util.Comparator;

/*
Subclasses of these objects actually issue the move commands for their units
*/
public class Squad{
    ArrayList<Integer> units;
    int urgency;
    MapLocation targetLoc;
    UnitType toBuild;
    ArrayList<UnitType> requestedUnits;
    GameController gc;

    Objective objective;

    public Squad(GameController g){
        units = new ArrayList<Integer>();
        urgency = 0;
        targetLoc = null;
        toBuild = UnitType.Factory;
        requestedUnits = new ArrayList<UnitType>();
        objective = Objective.NONE;
        gc = g;
    }

    public void update(){
        // managers will update squad needs
    }

    public void move(Nav nav){
        // actually move the units
    }
    
    //not sure if i flipped the order lol
	public static Comparator<Squad> byUrgency()
    {   
     Comparator<Squad> comp = new Comparator<Squad>(){
         @Override
         public int compare(Squad s1, Squad s2)
         {
             return s1.urgency - s2.urgency;
         }        
     };
     return comp;
    }  
}