package com.jonnyc.fxglgames.ship;

public class PlayerPad {
    int x;
    int y;
    int width;
    int height;
    int poweredState;
    protected boolean allPlayersIn;
    public PlayerPad(){
        x = 50;
        y = 50;
        width = 64;
        height = 64;
    }
    void Update(PlayerManager playerManager, double deltaTime){
        allPlayersIn = true;
        boolean somePlayersIn = false;
        for(Player p : playerManager.GetPlayers().values())
        {
            if(!IsInBounds(p.x, p.y, 10, 10)){
                allPlayersIn = false;
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
        return "1111"; // TODO use this for other stuff
    }
}
