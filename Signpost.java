import bc.*;

/*
data structure for Nav's BFS caching thing
*/
public class Signpost{
    Direction direction;
    short stepsToDest;
    // int roundLastUsed; // if we find it's getting too big and have to remove old ones

    public Signpost(Direction dir, short dist){
        direction = dir;
        stepsToDest = dist;
    }
}
