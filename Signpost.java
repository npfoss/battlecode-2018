import bc.*;

public class Signpost{
    Direction direction;
    int stepsToDest;
    // int roundLastUsed; // if we find it's getting too big and have to remove old ones

    public Signpost(Direction dir, int dist){
        direction = dir;
        stepsToDest = dist;
    }
}