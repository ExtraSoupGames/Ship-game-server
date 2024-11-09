package com.jonnyc.fxglgames.ship.Enemies;

import com.jonnyc.fxglgames.ship.*;

public class Bobleech implements Enemy {
    int ID;
    double x;
    double y;
    int health;
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
        sceneManager = pSceneManager;
        speed = 3;
        updateTargetTimer = 0;
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
        Vector2 currentLocation = new Vector2(x, y);
        //apply potential movement
        //find the position of the closest player if timer is up
        updateTargetTimer += deltaTime;
        if(updateTargetTimer > updateTargetCooldown) {
            updateTargetTimer -= updateTargetCooldown;
            targetPlayerPos = sceneManager.GetClosestPlayersPosition(currentLocation);
        }
        //find the difference
        Vector2 direction = targetPlayerPos.Subtract(currentLocation);
        //normalise it
        direction = direction.Normalise();
        //multiply by speed
        Vector2 desiredLocation = currentLocation.Add(direction.Multiply(speed));
        //check for collision
        Vector2 finalLocation = boundaryManager.ApplyCollision(currentLocation, desiredLocation);
        //apply finalised location
        x = finalLocation.x;
        y = finalLocation.y;
    }
    @Override
    public String GetBroadcastData(){
        String out = "000" // enemy type code
                + UDPServer.CompressInt(ID, 32)
                + UDPServer.CompressPosition((int) x, (int) y)
        // leech has no additional data so 2 bits of padding is needed
                + "00";
        return out;
    }
}
