package com.jonnyc.fxglgames.ship;

public class StartingPad {
    int x;
    int y;
    int width;
    int height;
    StartButton startButton;
    boolean starting;
    double startTimer;
    int poweredState;
    public StartingPad(){
        x = 50;
        y = 50;
        width = 64;
        height = 64;
        startTimer = 0;
        startButton = new StartButton();
    }
    void Update(PlayerManager playerManager, double deltaTime){
        boolean allPlayersIn = true;
        boolean somePlayersIn = false;
        for(Player p : playerManager.GetPlayers().values())
        {
            System.out.println("Checking player: " + p.ID);
            if(!IsInBounds(p.x, p.y, 10, 10)){
                allPlayersIn = false;
                System.out.println("Player " + p.ID + " is out of bounds");
            }
            else{
                somePlayersIn = true;
            }
        }
        if(allPlayersIn){
            poweredState = 2;
        }
        else if(somePlayersIn){
            poweredState = 1;
        }
        else{
            poweredState = 0;
        }
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
    boolean IsInBounds(int pX, int pY, int pWidth, int pHeight){
        if(pX > x + width || x > pX + pWidth){
            return false;
        }
        else if(pY > y + height || y > pY + pHeight){
            return false;
        }
        return true;
    }
    String GetStartRoomInfo(){
        return "1111";
    }
    String GetPadInfo(){
        String out = "1110";
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
