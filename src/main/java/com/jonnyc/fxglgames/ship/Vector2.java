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
        return this.Subtract(other).Magnitude();
    }
    //subtract 2 vector values from each other
    public Vector2 Subtract(Vector2 other){
        return new Vector2(x - other.x, y - other.y);
    }
    //scale the vector to have a magnitude of 1
    public Vector2 Normalise(){
        double magnitude = this.FindDistance(new Vector2(0,0));
        return new Vector2(x / magnitude, y / magnitude);
    }
    //multiply a vector by a factor
    public Vector2 Multiply(double factor){
        return new Vector2(x * factor, y * factor);
    }
    //find the magnitude of a vector
    public double Magnitude(){
        return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
    }
}
