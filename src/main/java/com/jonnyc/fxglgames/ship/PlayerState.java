package com.jonnyc.fxglgames.ship;

public class PlayerState {
    int direction; // 8 possible values
    int movementState; // 4 possible values
    int attackState; // 3 possible values
    public PlayerState(int pDirection, int pMovementState, int pAttackState) {
        if(pDirection > 7){
            System.out.println("direction should be between 0 and 7, inputted value: " + pDirection);
        }
        if(pMovementState > 3){
            System.out.println("movement state should be between 0 and 3, inputted value: " + pMovementState);
        }
        if(pAttackState > 2){
            System.out.println("attack state should be between 0 and 2, inputted value: " + pAttackState);
        }
        direction = pDirection;
        movementState = pMovementState;
        attackState = pAttackState;
    };
}
