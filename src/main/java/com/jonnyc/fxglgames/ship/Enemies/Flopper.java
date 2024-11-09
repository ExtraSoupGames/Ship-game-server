package com.jonnyc.fxglgames.ship.Enemies;

import com.jonnyc.fxglgames.ship.BoundaryManager;
import com.jonnyc.fxglgames.ship.PlayerManager;
import com.jonnyc.fxglgames.ship.UDPServer;
enum FlopperState{
    GROUNDED,
    AIRBORNE,
    SPAWNING
}
public class Flopper implements Enemy{
    int ID;
    double x;
    double y;
    int health;
    PlayerManager playerManager;
    FlopperState state;
    double testingStateChangeCooldown;
    double stateChangeTime = 1000;
    public Flopper(int pID, int pX, int pY, int pHealth, PlayerManager pPlayerManager){
        ID = pID;
        x = pX;
        y = pY;
        health = pHealth;
        playerManager = pPlayerManager;
        state = FlopperState.SPAWNING;
    }
    @Override
    public boolean GetIsDead() {
        return health <= 0;
    }

    @Override
    public void TakeHit(int damage) {
        health -= damage;
    }

    @Override
    public void UpdateMove(BoundaryManager boundaryManager, double deltaTime) {
        testingStateChangeCooldown += deltaTime;
        if(testingStateChangeCooldown >= stateChangeTime){
            testingStateChangeCooldown -= stateChangeTime;
            if(state == FlopperState.SPAWNING){
                state = FlopperState.GROUNDED;
            }
            else if (state == FlopperState.GROUNDED){
                state = FlopperState.AIRBORNE;
            }
            else if (state == FlopperState.AIRBORNE){
                state = FlopperState.SPAWNING;
            }
        }
    }
    private String StateToBinary(){
        switch(state){
            case GROUNDED:
                return "00";
            case AIRBORNE:
                return "01";
            case SPAWNING:
                return "10";
        }
        return "11"; // error
    }
    @Override
    public String GetBroadcastData() {
        String out = "001" // enemy type code
                + UDPServer.CompressInt(ID, 32)
                + UDPServer.CompressPosition((int) x, (int) y)
                + StateToBinary(); // Flopper has 2 extra bits of data that determine the state
        return out;
    }
}
