package com.jonnyc.fxglgames.ship;

import com.jonnyc.fxglgames.ship.Enemies.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

//allows all scene info to be accessed through one class instance
//rather than passing around enemy manager and player manager separately
public class SceneManager {
    EnemyManager enemyManager;
    PlayerManager playerManager;

    public SceneManager(EnemyManager pEnemyManager, PlayerManager pPlayerManager){
        enemyManager = pEnemyManager;
        playerManager = pPlayerManager;
    }

    //lets enemies find their closest player to target it or run away ect
    public Vector2 GetClosestPlayersPosition(Vector2 enemyPos){
        double closestDistance = Double.MAX_VALUE;
        int closestID = -1;
        HashMap<Integer, Player> players = playerManager.GetPlayers();
        for(Integer i : players.keySet()){
            Player p = players.get(i);
            double newDistance = enemyPos.FindDistance(new Vector2(p.x, p.y));
            if(newDistance < closestDistance) {
                closestDistance = newDistance;
                closestID = p.ID;
            }
        }
        if(closestID != -1){
            return new Vector2(players.get(closestID).x, players.get(closestID).y);
        }
        else{
            //if a player cant be found then the enemy can just stay still
            return new Vector2(200, 200);
        }
    }

    public Player GetRandomPlayer() {
        HashMap<Integer, Player> players = playerManager.GetPlayers();
        if(players.isEmpty()){
            return null;
        }
        List<Player> playersList = new ArrayList<Player>(players.values());
        int randomIndex = new Random().nextInt(playersList.size());
        return playersList.get(randomIndex);
    }

    public Vector2 GetDispersionForce(Vector2 currentLocation) {
        int rangeToDisperseFrom = 20;
        Vector2 directionSum = new Vector2(0.,0);
        HashMap<Integer, Enemy> enemies = enemyManager.GetEnemies();
        for(Integer ID : enemies.keySet()){

            //TODO change this code to use a virtual function in Enemy to let enemy calculate its own dispersion forces
            try{
                Bobleech thisEnemy = (Bobleech)enemies.get(ID);
                //calculate the difference between the enemy dispersing and the enemy close to it
                Vector2 difference = thisEnemy.GetLocation().Subtract(currentLocation);
                //only for enemies that are close (within 5)
                if(difference.Magnitude() < rangeToDisperseFrom){
                    //invert the magnitude of the difference(further away enemies have less of an effect)
                    difference.Multiply(rangeToDisperseFrom / difference.Magnitude());
                    //add this to the sum
                    directionSum = directionSum.Add(thisEnemy.GetLocation().Subtract(currentLocation));
                }
            }
            catch(ClassCastException ignored){

            }
        }
        return directionSum;
    }
}
