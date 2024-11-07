package com.jonnyc.fxglgames.ship;
public class Boundary {
    Vector2 point1;
    Vector2 point2;
    Vector2 offset;
    Boundary(int x1, int y1, int x2, int y2, int offsetX, int offsetY){
        point1 = new Vector2(x1, y1);
        point2 = new Vector2(x2, y2);
        offset = new Vector2(offsetX, offsetY);
    }
    public String GetDataForBroadcast(){
        StringBuilder data = new StringBuilder();
        data.append(UDPServer.CompressInt((int)point1.x, 32));
        data.append(UDPServer.CompressInt((int)point1.y, 32));
        data.append(UDPServer.CompressInt((int)point2.x, 32));
        data.append(UDPServer.CompressInt((int)point2.y, 32));
        data.append(UDPServer.CompressInt((int)offset.x, 32));
        data.append(UDPServer.CompressInt((int)offset.y, 32));
        return data.toString();
    }

    int FindOrientation(Vector2 P1, Vector2 P2, Vector2 P3) {
        double val = (((P2.y - P1.y) * (P3.x - P2.x)) - ((P2.x - P1.x) * (P3.y - P2.y)));
        if (val == 0) return 0;
        return (val > 0) ? 1 : 2;
    }
    public Vector2 InterruptMovement(Vector2 start, Vector2 desiredEnd){
        double A1 = desiredEnd.y - start.y;
        double B1 = start.x - desiredEnd.x;
        double C1 = A1 * start.x + B1 * start.y;
        double A2 = point2.y - point1.y;
        double B2 = point1.x - point2.x;
        double C2 = A2 * point1.x + B2 * point1.y;
        //Cramer's Rule
        double outX = ((C1 * B2) - (C2 * B1)) / ((A1 * B2) - (A2 * B1));
        double outY = ((A1 * C2) - (A2 * C1)) / ((A1 * B2) - (A2 * B1));
        double finalX = outX + offset.x;
        double finalY = outY + offset.y;
        return new Vector2(finalX, finalY);
    }
    public boolean IsRelevant(Vector2 start, Vector2 desiredEnd){
        double orientation1 = FindOrientation(start, desiredEnd, point1);
        double orientation2 = FindOrientation(start, desiredEnd, point2);
        double orientation3 = FindOrientation(point1, point2, start);
        double orientation4 = FindOrientation(point1, point2, desiredEnd);
        return ((orientation1 != orientation2) && (orientation3 != orientation4));
    }
}
