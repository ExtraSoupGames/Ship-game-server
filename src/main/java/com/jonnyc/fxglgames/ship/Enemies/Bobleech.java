package com.jonnyc.fxglgames.ship.Enemies;

import com.jonnyc.fxglgames.ship.*;

public class Bobleech implements Enemy {
    int ID;
    double x;
    double y;
    int health;
    SceneManager sceneManager;
    double speed;

    public Bobleech(int pID, int pX, int pY, int pHealth, SceneManager pSceneManager) {
        ID = pID;
        x = pX;
        y = pY;
        health = pHealth;
        sceneManager = pSceneManager;
        speed = 3;
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
        //find the position of the closest player
        Vector2 targetPlayerPos = sceneManager.GetClosestPlayersPosition(currentLocation);
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
