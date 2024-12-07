package com.jonnyc.fxglgames.ship.Enemies;

import com.jonnyc.fxglgames.ship.*;

enum ClingabingState{
    IDLE,
    DASHING,
    ATTACHED
}
public class Clingabing implements Enemy{
    int ID;
    double x;
    double y;
    int health;
    SceneManager sceneManager;
    ClingabingState state;
    Player attachedPlayer;
    Player targetedPlayer;
    double idleTimer = 0; // timer to track how long clingabing has been idle
    double idleDuration = 3000; // duration to stay idle for
    double speed;
    public Clingabing(int pID, int pX, int pY, int pHealth, SceneManager pSceneManager) {
        ID = pID;
        x = pX;
        y = pY;
        health = pHealth;
        sceneManager = pSceneManager;
        speed =3;
        state = ClingabingState.IDLE;
        targetedPlayer = new Player();
    }
    @Override
    public boolean GetIsDead() {
        return health <= 0;
    }

    @Override
    public void TakeHit(int damage) {health -= damage;}

    @Override
    public void UpdateMove(BoundaryManager boundaryManager, double deltaTime) {
        switch(state){
            case IDLE:
                //wait for a while then pick a player to dash to
                idleTimer += deltaTime;
                if(idleTimer > idleDuration){
                    idleTimer = 0;
                    Dash();
                }
                break;
            case DASHING:
                //move towards player until in range
                Vector2 currentLocation = GetLocation();
                Vector2 targetLocation = targetedPlayer.GetLocation();
                Vector2 difference = targetLocation.Subtract(currentLocation);
                if(difference.Magnitude() < 1){
                    //attach to the target player if very close
                    Attach(targetedPlayer);
                }
                Vector2 movement = difference.Normalise().Multiply(speed);
                Vector2 desiredLocation = currentLocation.Add(movement);
                Vector2 finalMovement = boundaryManager.ApplyCollision(currentLocation, desiredLocation);
                x = finalMovement.x;
                y = finalMovement.y;
                break;
            case ATTACHED:
                //TODO deal damage to the player periodically
                //changing position here is kind of unnecessary as clingabing will be rendered on top of player anyway
                x = attachedPlayer.x;
                y = attachedPlayer.y;
                break;
        }
    }
    private String StateToBinary(){
        switch(state){
            case IDLE:
                return "00";
            case DASHING:
                return "01";
            case ATTACHED:
                return "10";
        }
        return "11"; //error
    }
    @Override
    public String GetBroadcastData() {
        String out = "010" // enemy type code
                + UDPServer.CompressInt(ID, 32)
                + UDPServer.CompressPosition((int) x, (int) y)
                + StateToBinary();
        return out;
    }

    @Override
    public Vector2 GetLocation() {
        return new Vector2(x, y);
    }

    @Override
    public double GetDispersionWeight() {
        return 0.5;
    }
    private void Dash(){
        targetedPlayer = sceneManager.GetRandomPlayer();
        state = ClingabingState.DASHING;
    }
    private void Attach(Player attachTo){
        attachedPlayer = attachTo;
        state = ClingabingState.ATTACHED;
    }
}
