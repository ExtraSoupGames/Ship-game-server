package com.jonnyc.fxglgames.ship;

public class StartingPad extends PlayerPad{
    StartButton startButton;
    double startTimer;
    boolean starting;
    public StartingPad(){
        super();
        startTimer = 0;
        starting = false;
        startButton = new StartButton();
    }
    void Update(PlayerManager playerManager, double deltaTime){
        super.Update(playerManager, deltaTime);
        if(allPlayersIn && startButton.active){
            startTimer += deltaTime;
        }
        else{
            startTimer = 0;
        }
        if(startTimer > 3000){ // 3 seconds of all players on pad and lever active = start game
            starting = true;
        }
    }
    String GetPadInfo(){
        String out = "0110";
        out = out.concat(UDPServer.CompressInt(poweredState, 2));
        out = out.concat(UDPServer.CompressInt(startButton.active? 1 : 0, 1));
        int timerToBroadcast = 3000 - (int)startTimer;
        timerToBroadcast = timerToBroadcast / 500;
        out = out.concat(UDPServer.CompressInt(timerToBroadcast, 3));
        return out;
    }
    public void LeverPulled(){
        startButton.active = !startButton.active;
    }
}
