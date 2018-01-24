/****************/
/* REFACTOR ME! */
/****************/

import bc.*;

/*
Manages all communication between Earth and Mars
(manipulating/interpreting array) - for Mars,
continually sends updates to Earth based on what’s happening 
*/
public class Comms{
    GameController gc;

    public Comms(GameController g){
        gc = g;
    }

    public void update(){
        // check for new messages I guess?
    }
}