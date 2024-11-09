package com.jonnyc.fxglgames.ship;

public class Player {
    public int ID;
    public int x;
    public int y;
    public PlayerState state;

    public Player(int ID, int x, int y, PlayerState state) {
        this.ID = ID;
        this.x = x;
        this.y = y;
        this.state = state;
    }

    public Player() {
        x = 0;
        y = 0;
    }
}
