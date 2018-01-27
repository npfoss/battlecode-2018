// import the API.
// See xxx for the javadocs.
import bc.*;
import java.lang.Thread;
// You can use other files in this directory, and in subdirectories.

public class Player {
    public static void main(String[] args) {
        // Connect to the manager, starting the game
        GameController gc = new GameController();

        Overseer overseer = new Overseer(gc);

        while (true) {
            try{
                overseer.takeTurn();
                System.gc();
            } catch(Exception e) {
                System.out.println("***ERROR AHHHHHHHHHHHHHHHHHHHHHHHHH***");
                e.printStackTrace(System.out);
                gc.nextTurn();
            }
            // try{Thread.sleep(25);}catch(Exception e){}
        }
    }
}