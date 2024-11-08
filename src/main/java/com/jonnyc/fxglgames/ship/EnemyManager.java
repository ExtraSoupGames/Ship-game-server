package com.jonnyc.fxglgames.ship;
import com.jonnyc.fxglgames.ship.Enemies.*;

import java.util.HashMap;
import java.util.Set;

public class EnemyManager {
    private final HashMap<Integer, Enemy> enemies;
    public EnemyManager(){
        enemies = new HashMap<>();
    }
    public void AddEnemy(int ID, PlayerManager pPlayerManager){
        enemies.put(ID, new Leech(ID, 50, 50, 100, pPlayerManager));
    }
    public void AddEnemy(int ID, int Y, PlayerManager pPlayerManager){
        enemies.put(ID, new Leech(ID, 50, Y, 100, pPlayerManager));
    }
    public void incomingData(String data) {
        data = data.substring(3);
        int enemyID = UDPServer.DecompressInt(data.substring(0, 32));
        int enemyDamage = UDPServer.DecompressInt(data.substring(32, 64));
        int enemyKnockback = UDPServer.DecompressInt(data.substring(64, 96));
        //knockback ignored for now
        if(enemies.get(enemyID) != null){
            Enemy e = enemies.get(enemyID);
            e.TakeHit(enemyDamage);
        }


        Integer IDToRemove = -1;
        for(Integer ID : enemies.keySet()){
            Enemy e = enemies.get(ID);
            if(e.GetIsDead()){
                IDToRemove = ID;
            }
        }
        if(IDToRemove != -1){
            enemies.remove(IDToRemove);
        }
    }
    public void UpdateEnemies(BoundaryManager boundaryManager){
        for(Integer ID : enemies.keySet()){
            enemies.get(ID).UpdateMove(boundaryManager);
        }
    }
    public String GetEnemyData(long serverStartTime){
        StringBuilder outData = new StringBuilder("100");
        //add data of enemies
        Set<Integer> IDs = enemies.keySet();
        for(Integer ID : IDs){
            Enemy e = enemies.get(ID);
            outData.append(e.GetBroadcastData());
        }
        outData.append(UDPServer.LongToBinary(System.currentTimeMillis() - serverStartTime, 64));
        return outData.toString();
    }
}
