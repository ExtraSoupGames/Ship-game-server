package com.jonnyc.fxglgames.ship;

public class Vector2 {
    public double x;
    public double y;
    public Vector2(double pX, double pY){
        x = pX;
        y = pY;
    }
    public Vector2 Add(Vector2 other){
        return new Vector2(x + other.x, y + other.y);
    }
    //use pythagoras theorem to find distance to another vector
    public double FindDistance(Vector2 other){
        return Math.sqrt(Math.pow(other.x - x, 2) + Math.pow(other.y - y, 2));
    }
    public Vector2 Subtract(Vector2 other){
        return new Vector2(x - other.x, y - other.y);
    }
    public Vector2 Normalise(){
        double magnitude = this.FindDistance(new Vector2(0,0));
        return new Vector2(x / magnitude, y / magnitude);
    }
    public Vector2 Multiply(double factor){
        return new Vector2(x * factor, y * factor);
    }
}
