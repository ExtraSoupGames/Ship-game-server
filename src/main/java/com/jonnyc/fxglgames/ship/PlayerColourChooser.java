package com.jonnyc.fxglgames.ship;

class PlayerColour{
    boolean inUse;
    String code;
    PlayerColour(String pCode){
        code = pCode;
        inUse = false;
    }
}
public class PlayerColourChooser {
    /*
    Colours
    000 Fully white
    001 Fully black
    010 Fully light blue
    011 White + Black
    100 Orange + White
    101 Cream + Black
     */
    PlayerColour[] availableColours;
    public PlayerColourChooser(){
        availableColours = new PlayerColour[]{
                new PlayerColour("000"),
                new PlayerColour("001"),
                new PlayerColour("010"),
                new PlayerColour("011"),
                new PlayerColour("100"),
                new PlayerColour("101"),
        };
    }
    public boolean Use(String code){
        for(PlayerColour colour : availableColours){
            if(colour.code.equals(code)){
                if(!colour.inUse){
                    colour.inUse = true;
                    return true;
                }
                else{
                    System.out.println("client requested to use colour that was unavailable");
                    return false;
                }
            }
        }
        System.out.println("unexpected colour code being used by client");
        return false;
    }
    public String GetColourInfo(){
        StringBuilder out = new StringBuilder("1001");
        for(PlayerColour colour : availableColours){
            if(colour.inUse){
                out.append("0");
            }
            else{
                out.append("1");
            }
        }
        return out.toString();
    }
    public void FreeUp(String colourCode){
        for(PlayerColour colour : availableColours){
            if(colour.code.equals(colourCode)){
                if(colour.inUse){
                    colour.inUse = false;
                    System.out.println("Freed up a colour with code" + colour.code);
                }
                else{
                    System.out.println("Trying to free up a colour that isnt in use, slightly odd");
                }
            }
        }
    }
}
