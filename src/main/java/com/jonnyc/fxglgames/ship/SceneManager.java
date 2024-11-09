package com.jonnyc.fxglgames.ship;

import java.util.HashMap;

//allows all scene info to be accessed through one class instance
//rather than passing around enemy maager and player manager seperately
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
            if(newDistance < closestDistance){
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
}
