package com.jonnyc.fxglgames.ship.Enemies;

import com.jonnyc.fxglgames.ship.BoundaryManager;
import com.jonnyc.fxglgames.ship.UDPServer;
import com.jonnyc.fxglgames.ship.Vector2;

public class Leech implements Enemy {
    int ID;
    double x;
    double y;
    int health;

    public Leech(int pID, int pX, int pY, int pHealth) {
        ID = pID;
        x = pX;
        y = pY;
        health = pHealth;
    }
    public boolean GetIsDead(){
        return health <= 0;
    }
    public void TakeHit(int damage) {
        health -= damage;
    }

    public void UpdateMove(BoundaryManager boundaryManager) {
        //store current position
        Vector2 currentLocation = new Vector2(x, y);
        //apply potential movement
        x += 1;
        y += 1;

        //calculate potential new location and check against collisions from boundary manager
        Vector2 desiredLocation = new Vector2(x, y);
        Vector2 finalLocation = boundaryManager.ApplyCollision(currentLocation, desiredLocation);
        //apply finalised location
        x = finalLocation.x;
        y = finalLocation.y;
    }
    public String GetBroadcastData(){
        //TODO add enemy type to this
        return UDPServer.CompressInt(ID, 32) +
                UDPServer.CompressPosition((int) x, (int) y);
    }
}
