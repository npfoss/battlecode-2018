import bc.*;

/*
for squads, so they know what to do with themselves
*/
public enum Objective {
    NONE("NONE"),
    EXPLORE("EXPLORE"),
    MINE("MINE"),
    BUILD("BUILD"),
    BOARD_ROCKET("BOARD_ROCKET"),
    DEFEND_LOC("DEFEND_LOC"),
    ATTACK_LOC("ATTACK_LOC");

    String printStr;

    Objective(){
        printStr = "no printStr set :(";
    }

    Objective(String str){
        printStr = str;
    }

    public String toString(){
        return printStr;
    }
}
