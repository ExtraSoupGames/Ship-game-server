package com.jonnyc.fxglgames.ship.Enemies;

import com.jonnyc.fxglgames.ship.BoundaryManager;

public interface Enemy {
    boolean GetIsDead();
    void TakeHit(int damage);
    void UpdateMove(BoundaryManager boundaryManager, double deltaTime);
    public String GetBroadcastData();
}
