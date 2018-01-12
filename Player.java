// import the API.
// See xxx for the javadocs.
import bc.*;
// You can use other files in this directory, and in subdirectories.

public class Player {
    public static void main(String[] args) {
        Overseer overseer = new Overseer();

        while (true) {
            try{
                overseer.takeTurn();
            } catch(Exception e) {}
            // Submit the actions we've done, and wait for our next turn.
            gc.nextTurn();
        }
    }
}