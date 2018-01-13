import bc.*;

/* Enum - choose general strategy mode
didn't call it StrategyManager because it's an enum so this makes more sense
*/
public enum Strategy{
    UNSURE(), // initial strategy. not a very good one
    RUSH(), // focus production on military, charge them agressively
    ECON(); // focus production more on workers and factories

    public Strategy update(InfoManager infoMan){
        if(this == Strategy.ECON){
            return Strategy.RUSH;
        }
        return this;
    }
}