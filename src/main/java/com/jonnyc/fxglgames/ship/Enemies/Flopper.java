package com.jonnyc.fxglgames.ship.Enemies;

import com.jonnyc.fxglgames.ship.BoundaryManager;
import com.jonnyc.fxglgames.ship.*;
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
    SceneManager sceneManager;
    FlopperState state;
    Vector2 target;
    double groundedTimer; // flopper lies on the ground for a while before flopping again
    double groundedDuration = 3000; // the time it lies down for before flopping
    double flightDuration = 1;
    double timeSinceFlop = 0;
    Vector2 flopStart;
    double flightHeight = 50;
    public Flopper(int pID, int pX, int pY, int pHealth, SceneManager pSceneManager){
        ID = pID;
        x = pX;
        y = pY;
        health = pHealth;
        sceneManager = pSceneManager;
        state = FlopperState.SPAWNING;
        target = new Vector2(0, 0);
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
        switch(state){
            case GROUNDED:
                groundedTimer += deltaTime;
                if(groundedTimer >= groundedDuration){
                    Flop();
                    groundedTimer = 0;
                    //TODO randomise grounded duration here
                }
                break;
            case AIRBORNE:
                timeSinceFlop += deltaTime / 1000;
                double flightProgress = timeSinceFlop / flightDuration;
                Vector2 flightVector = target.Subtract(flopStart);
                Vector2 linearPosition = flopStart.Add(flightVector.Multiply(flightProgress));

                double quadraticX = ((timeSinceFlop / flightDuration) * 2) - 1;
                double flightOffset = (-flightHeight * (quadraticX * quadraticX)) + flightHeight;

                Vector2 finalPos = new Vector2(linearPosition.x, linearPosition.y - flightOffset);
                if(timeSinceFlop > flightDuration){
                    //if the distance to the target is less than the distance it would move in one frame
                    Land();
                }
                x = finalPos.x;
                y = finalPos.y;
                break;
            case SPAWNING:
                state = FlopperState.GROUNDED;
                break;
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


    private void Flop(){
        state = FlopperState.AIRBORNE;
        Player targetedPlayer = sceneManager.GetRandomPlayer();
        flopStart = GetLocation();
        timeSinceFlop = 0;
        if(targetedPlayer != null){
            target = new Vector2(targetedPlayer.x, targetedPlayer.y);
        }
        else{
            target = new Vector2(x, y); // if no players are present, flop onto itsself. this is fine
        }
    }
    private void Land(){
        state = FlopperState.GROUNDED;
    }

    @Override
    public Vector2 GetLocation() {
        return new Vector2(x, y);
    }
    public double GetDispersionWeight(){
        return 0;
    }
}
