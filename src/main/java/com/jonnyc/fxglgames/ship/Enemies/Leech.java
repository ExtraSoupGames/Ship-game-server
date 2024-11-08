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
    public void UpdateMove(BoundaryManager boundaryManager) {
        //store current position
        Vector2 currentLocation = new Vector2(x, y);
        //apply potential movement
        Vector2 targetPlayerPos = playerManager.GetClosestPlayersPosition(currentLocation);
        Vector2 direction = targetPlayerPos.Subtract(currentLocation);
        direction = direction.Normalise();
        //calculate potential new location and check against collisions from boundary manager
        Vector2 desiredLocation = currentLocation.Add(direction.Multiply(speed));
        Vector2 finalLocation = boundaryManager.ApplyCollision(currentLocation, desiredLocation);
        //apply finalised location
        x = finalLocation.x;
        y = finalLocation.y;
        System.out.println("Enemy final location: " + targetPlayerPos.x + " - "+ targetPlayerPos.y);
    }
    @Override
    public String GetBroadcastData(){
        //TODO add enemy type to this
        return UDPServer.CompressInt(ID, 32) +
                UDPServer.CompressPosition((int) x, (int) y);
    }
}
