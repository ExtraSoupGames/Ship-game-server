package com.jonnyc.fxglgames.ship;

import java.util.ArrayList;

public class BoundaryManager {
    ArrayList<Boundary> boundaries;
    public BoundaryManager(){
        boundaries = new ArrayList<Boundary>();
    }
    public Vector2 ApplyCollision(Vector2 start, Vector2 desiredEnd){
        for (Boundary b : boundaries){
            if(b.IsRelevant(start, desiredEnd)){
                return b.InterruptMovement(start, desiredEnd);
            }
        }
        return desiredEnd;
    //TODO copy cpp logic to allow for multiple collisions in one frame in future
    }
    public void AddBoundary(Boundary b){
        boundaries.add(b);
    }
    public String GetBoundaryData(long serverStartTime){
        //TODO Write Boundary broadcasting into client joining
        StringBuilder out = new StringBuilder("001");
        for (Boundary b : boundaries){
            out.append(b.GetDataForBroadcast());
        }
        out.append(UDPServer.LongToBinary(serverStartTime, 64));
        return out.toString();
    }
}
