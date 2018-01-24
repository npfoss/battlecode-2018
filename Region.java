import bc.*;
import java.util.ArrayList;

/*
just a data structure for storing region data
*/
public class Region{
    ArrayList<Tile> tiles;
    long karbonite;

    public Region(){
        tiles = new ArrayList<Tile>();
        karbonite = 0;
    }
}
