package com.jonnyc.fxglgames.ship;

public class StartingPad {
    int x;
    int y;
    int width;
    int height;
    StartButton startButton;
    boolean starting;
    double startTimer;
    public StartingPad(){
        x = 50;
        y = 50;
        width = 50;
        height = 50;
        startTimer = 0;
        startButton = new StartButton();
    }
    void Update(PlayerManager playerManager, double deltaTime){
        boolean allPlayersIn = true;
        for(Player p : playerManager.GetPlayers().values())
        {
            System.out.println("Checking player: " + p.ID);
            if(!IsInBounds(p.x, p.y, 10, 10)){
                allPlayersIn = false;
                System.out.println("Player " + p.ID + " is out of bounds");
            }
        }
        if(playerManager.PlayersExist() && allPlayersIn && startButton.active){ // TODO remove players exist check once start button functionality added
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
        return "1111";//TODO decide what data needs to be sent at start for client to render start room, boundaries?
    }
    String GetPadInfo(){
        return "1111"; // TODO add live transmission of render requirement data, players needed, players on, start timer, lever active
    }
}
