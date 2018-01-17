import bc.*;
import java.util.HashMap;
import java.util.TreeMap;

public class Tile{
    int x;
    int y;
    boolean isWalkable;
    long karbonite;
    Region region;

    int roundLastUpdated;
    int hittableBy;
    double possibleDamage;
    HashMap<MapLocation, Signpost> destToDir;
    
    //for combat: dist from tile to ID
    TreeMap<Integer, Integer> nearbyEnemies; 

    public Tile(int ex, int why, boolean walkable, long karb, Region reg){
        x = ex;
        y = why;
        isWalkable = walkable;
        karbonite = karb;
        region = reg;

        roundLastUpdated = 0;
        hittableBy = 0;
        possibleDamage = 0;
        destToDir = new HashMap<MapLocation, Signpost>();
    }

    public void updateKarbonite(long newKarb){
        if (newKarb != karbonite){
            region.karbonite += newKarb - karbonite;
            karbonite = newKarb;
        }
    }
}