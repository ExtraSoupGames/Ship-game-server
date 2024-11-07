package com.jonnyc.fxglgames.ship;

import com.almasb.fxgl.entity.Entity;

import java.util.HashMap;
import java.util.Set;

public class EnemyManager {
    private final HashMap<Integer, Enemy> enemies;
    public EnemyManager(){
        enemies = new HashMap<>();
    }
    public void AddEnemy(int ID){
        enemies.put(ID, new Enemy(ID, 50, 50, 100));
    }
    public void AddEnemy(int ID, int Y){
        enemies.put(ID, new Enemy(ID, 50, Y, 100));
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
            if(enemies.get(ID).health <= 0){
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
            outData.append(UDPServer.CompressInt(e.ID, 32));
            outData.append(UDPServer.CompressPosition((int)e.x, (int)e.y));
        }
        outData.append(UDPServer.LongToBinary(System.currentTimeMillis() - serverStartTime, 64));
        return outData.toString();
    }
}
class Enemy{
    int ID;
    double x;
    double y;
    int health;
    Enemy(int pID, int pX, int pY, int pHealth){
        ID = pID;
        x = pX;
        y = pY;
        health = pHealth;
    }
    void TakeHit(int damage){
        health -= damage;
    }
    void UpdateMove(BoundaryManager boundaryManager){
        //store current position
        Vector2 currentLocation = new Vector2(x, y);
        //apply potential movement
        x+=1;
        y+=1;

        //calculate potential new location and check against collisions from boundary manager
        Vector2 desiredLocation = new Vector2(x, y);
        Vector2 finalLocation = boundaryManager.ApplyCollision(currentLocation, desiredLocation);
        //apply finalised location
        x = finalLocation.x;
        y = finalLocation.y;
    }
}
