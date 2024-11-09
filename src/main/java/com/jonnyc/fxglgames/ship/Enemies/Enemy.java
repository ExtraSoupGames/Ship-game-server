package com.jonnyc.fxglgames.ship.Enemies;

import com.jonnyc.fxglgames.ship.*;

public interface Enemy {
    boolean GetIsDead();
    void TakeHit(int damage);
    void UpdateMove(BoundaryManager boundaryManager, double deltaTime);
    String GetBroadcastData();
    Vector2 GetLocation();
    double GetDispersionWeight();
}
