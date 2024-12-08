package com.jonnyc.fxglgames.ship;

import java.util.ArrayList;
import java.util.HashMap;

public class PlayerManager{
    private final HashMap<Integer, Player> players;
    public PlayerManager(){
        players = new HashMap<>();
    }
    public void incomingData(String data){
        int clientID = UDPServer.DecompressInt(data.substring(0, 32));
        int[] xy = UDPServer.DecompressPosition(data.substring(32, 48));
        PlayerState state = UDPServer.DecompressPlayerState(data.substring(48, 55));
        boolean isAlive = data.charAt(55) == '1';
        if (players.get(clientID) != null) {
            Player player = players.get(clientID);
            player.x = xy[0];
            player.y = xy[1];
            player.state = state;
            player.isAlive = isAlive;
        } else {
            players.put(clientID, new Player(clientID, 0, 0, state, isAlive));
        }
    };
    public boolean PlayersExist(){
        return players.size() > 0;
    }
    public String GetLocationData(long serverStartTime){
        StringBuilder outData = new StringBuilder("0101");
        for(Integer i : players.keySet()){
            Player p = players.get(i);
            if(p.isAlive){
                outData.append(UDPServer.CompressInt(i, 32));
                outData.append(UDPServer.CompressPosition(p.x, p.y));
                outData.append(UDPServer.CompressPlayerState(p.state));
            }
        }
        outData.append(UDPServer.LongToBinary(System.currentTimeMillis() - serverStartTime, 64));
        return outData.toString();
    }

    public HashMap<Integer, Player> GetPlayers(){
        return new HashMap<>(players);
    }

    public ArrayList<Integer> GetClientIDs(){
        ArrayList<Integer> clientIDs = new ArrayList<>();
        for(Integer i : players.keySet()){
            clientIDs.add(i);
        }
        return clientIDs;
    }
    public boolean AllPlayersDead(){
        if(!PlayersExist()){
            return false; // if no players exist then there are no players to be dead
        }
        boolean allDead = true;
        for(Integer i : players.keySet()){
            if(players.get(i).isAlive){
                allDead = false;
            }
        }
        return allDead;
    }
}
