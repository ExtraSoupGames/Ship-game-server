package com.jonnyc.fxglgames.ship.Enemies;

import com.jonnyc.fxglgames.ship.BoundaryManager;
import com.jonnyc.fxglgames.ship.PlayerManager;
import com.jonnyc.fxglgames.ship.UDPServer;
import com.jonnyc.fxglgames.ship.Vector2;

public class Leech implements Enemy {
    int ID;
    double x;
    double y;
    int health;
    PlayerManager playerManager;
    double speed;

    public Leech(int pID, int pX, int pY, int pHealth, PlayerManager pPlayerManager) {
        ID = pID;
        x = pX;
        y = pY;
        health = pHealth;
        playerManager = pPlayerManager;
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
        Vector2 targetPlayerPos = playerManager.GetClosestPlayersPosition(currentLocation);
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
