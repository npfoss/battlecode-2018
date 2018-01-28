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
    ArrayList<UnitType> requestedUnits;
    GameController gc;
    InfoManager infoMan;

    Objective objective;

    public Squad(InfoManager im){
        units = new ArrayList<Integer>();
        urgency = 0;
        targetLoc = null;
        requestedUnits = new ArrayList<UnitType>();
        objective = Objective.NONE;
        infoMan = im;
        gc = infoMan.gc;
    }

    public void update(){
        // managers will update squad needs
    }

    public void move(Nav nav){
        // actually move the units
    }
    
    public void removeUnit(int id){
    	units.remove(units.indexOf(id));
    }
    
    //not sure if i flipped the order lol REFACTOR: <-- double check and remove comment @Eli
	public static Comparator<Squad> byUrgency()
    {   
     Comparator<Squad> comp = new Comparator<Squad>(){
         @Override
         public int compare(Squad s1, Squad s2)
         {
             return Integer.compare(s2.urgency, s1.urgency);
         }        
     };
     return comp;
    }  
}