package com.jonnyc.fxglgames.ship;

public class Player {
    public int ID;
    public int x;
    public int y;
    public PlayerState state;
    boolean isAlive;
    double timeSinceHeartbeatResponse = 0;

    public Player(int ID, int x, int y, PlayerState state, boolean isAlive) {
        this.ID = ID;
        this.x = x;
        this.y = y;
        this.state = state;
        this.isAlive = isAlive;
    }

    public Player() {
        x = 0;
        y = 0;
    }
    public Vector2 GetLocation(){
        return new Vector2(x, y);
    }
    public void ResetHeartbeatTimer(){
        timeSinceHeartbeatResponse = 0;
    }
    public void UpdateHeartbeatTimer(double deltaTime){
        timeSinceHeartbeatResponse += deltaTime;
    }
    public boolean MarkedForDeletion(){
        return timeSinceHeartbeatResponse > 5000;
    }
}
