package com.jonnyc.fxglgames.ship.Enemies;

import com.jonnyc.fxglgames.ship.*;

public class Bobleech implements Enemy {
    int ID;
    double x;
    double y;
    int health;
    boolean flipped;
    SceneManager sceneManager;
    double speed;
    double updateTargetTimer; // updating target doesnt need to happen every frame so we have a timer
    double updateTargetCooldown = 200; // time between target updates
    Vector2 targetPlayerPos;
    public Bobleech(int pID, int pX, int pY, int pHealth, SceneManager pSceneManager) {
        ID = pID;
        x = pX;
        y = pY;
        health = pHealth;
        flipped = false;
        sceneManager = pSceneManager;
        speed = 1.5;
        updateTargetTimer = 0;
        targetPlayerPos = new Vector2(x, y); // until it finds a player it will just stay still. this is fine
    }
    @Override
    public boolean GetIsDead(){
        return health <= 0;
    }
    @Override
    public void TakeHit(int damage) {
        health -= damage;
    }
    @Override
    public void UpdateMove(BoundaryManager boundaryManager, double deltaTime) {
        //store current position
        Vector2 currentLocation = GetLocation();
        //apply potential movement
        //find the position of the closest player if timer is up
        updateTargetTimer += deltaTime;
        if(updateTargetTimer > updateTargetCooldown) {
            updateTargetTimer -= updateTargetCooldown;
            targetPlayerPos = sceneManager.GetClosestPlayersPosition(currentLocation);
        }
        //find the difference
        Vector2 direction = targetPlayerPos.Subtract(currentLocation);
        if(direction.Magnitude() < 1){
            //if very close to the target then just stay still
            return;
        }
        Vector2 dispersionForce = sceneManager.GetDispersionForce(currentLocation).Multiply(-1);
        Vector2 adjustedDirection = direction.Add(dispersionForce);
        //normalise it
        adjustedDirection = adjustedDirection.Normalise();
        //multiply by speed
        Vector2 desiredLocation = currentLocation.Add(adjustedDirection.Multiply(speed));
        //check for collision
        Vector2 finalLocation = boundaryManager.ApplyCollision(currentLocation, desiredLocation);
        //apply finalised location
        x = finalLocation.x;
        y = finalLocation.y;

        //adjust if sprite needs to be flipped
        flipped = adjustedDirection.x > 0;
    }
    @Override
    public String GetBroadcastData(){
        String out = "000" // enemy type code
                + UDPServer.CompressInt(ID, 32)
                + UDPServer.CompressPosition((int) x, (int) y)
                + (flipped ? "01" : "00");
        return out;
    }
    public Vector2 GetLocation(){
        return new Vector2(x, y);
    }
    public double GetDispersionWeight(){
        return 0.5;
    }
}
