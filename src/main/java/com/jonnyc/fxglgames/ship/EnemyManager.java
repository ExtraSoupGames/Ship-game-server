package com.jonnyc.fxglgames.ship;
import com.jonnyc.fxglgames.ship.Enemies.*;

import java.util.HashMap;
import java.util.Set;

public class EnemyManager {
    private final HashMap<Integer, Enemy> enemies;
    public EnemyManager(){
        enemies = new HashMap<>();
    }
    public void AddBobleech(int ID, int x, int y, SceneManager pSceneManager){
        enemies.put(ID, new Bobleech(ID, x, y, 100, pSceneManager));
    }
    public void AddFlopper(int ID, int x, int Y, SceneManager pSceneManager){
        enemies.put(ID, new Flopper(ID, x, Y, 100, pSceneManager));
    }
    public void AddClingabing(int ID, int x, int y, SceneManager pSceneManager){
        enemies.put(ID, new Clingabing(ID, x, y, 100, pSceneManager));
    }
    public void incomingData(String data) {
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
    public void UpdateEnemies(BoundaryManager boundaryManager, double deltaTime){
        for(Integer ID : enemies.keySet()){
            enemies.get(ID).UpdateMove(boundaryManager, deltaTime);
        }
    }
    public String GetEnemyData(long serverStartTime){
        StringBuilder outData = new StringBuilder("0011");
        //add data of enemies
        Set<Integer> IDs = enemies.keySet();
        for(Integer ID : IDs){
            Enemy e = enemies.get(ID);
            outData.append(e.GetBroadcastData());
        }
        outData.append(UDPServer.LongToBinary(System.currentTimeMillis() - serverStartTime, 64));
        return outData.toString();
    }

    public HashMap<Integer, Enemy> GetEnemies() {
        return new HashMap<>(enemies);
    }
    public void ResetEnemies(){
        enemies.clear();
    }
}
