package com.jonnyc.fxglgames.ship;

import java.util.HashMap;

public class PlayerManager{
    private final HashMap<Integer, Player> players;
    public PlayerManager(){
        players = new HashMap<>();
    }
    public void incomingData(String data){
        data = data.substring(3); // remove the header
        int clientID = UDPServer.DecompressInt(data.substring(0, 32));
        int[] xy = UDPServer.DecompressPosition(data.substring(32, 48));
        PlayerState state = UDPServer.DecompressPlayerState(data.substring(48, 55));
        if (players.get(clientID) != null) {
            Player player = players.get(clientID);
            player.x = xy[0];
            player.y = xy[1];
            player.state = state;
        } else {
            players.put(clientID, new Player(clientID, 0, 0, state));
        }
    };
    public boolean PlayersExist(){
        return players.size() > 0;
    }
    public String GetLocationData(long serverStartTime){
        StringBuilder outData = new StringBuilder("011");
        for(Integer i : players.keySet()){
            outData.append(UDPServer.CompressInt(i, 32));
            Player p = players.get(i);
            outData.append(UDPServer.CompressPosition(p.x, p.y));
            outData.append(UDPServer.CompressPlayerState(p.state));
        }
        outData.append(UDPServer.LongToBinary(System.currentTimeMillis() - serverStartTime, 64));
        return outData.toString();
    }
}
class Player{
    public int ID;
    public int x;
    public int y;
    public PlayerState state;
    public Player(int ID, int x, int y, PlayerState state){
        this.ID = ID;
        this.x = x;
        this.y = y;
        this.state = state;
    }
    public Player() {
        x= 0;
        y = 0;
    }
}