/*
 * FXGL - JavaFX Game Library. The MIT License (MIT).
 * Copyright (c) AlmasB (almaslvl@gmail.com).
 * See LICENSE for details.
 */

package com.jonnyc.fxglgames.ship;

import com.almasb.fxgl.core.serialization.Bundle;
import com.almasb.fxgl.logging.ConsoleOutput;
import com.almasb.fxgl.logging.Logger;
import com.almasb.fxgl.logging.LoggerConfig;
import com.almasb.fxgl.logging.LoggerLevel;
import com.almasb.fxgl.net.*;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import static com.jonnyc.fxglgames.ship.ShipServer.serverName;

class ImportantMessage{
    String binaryContents;
    int messageID;
    ArrayList<Integer> clientIDs;
    ImportantMessage(String pBinaryContents, int pMessageID, ArrayList<Integer> pClientIDs){
        binaryContents = pBinaryContents;
        messageID = pMessageID;
        clientIDs = pClientIDs;
    }
    String GetMessage(){
        String header = binaryContents.substring(0, 4);
        String msgID = UDPServer.CompressInt(messageID, 32);
        String messageData = binaryContents.substring(4);
        return header + msgID + messageData;
    }
    boolean ReceiveConfirmation(Integer pClientID){
        clientIDs.remove(pClientID);
        return clientIDs.isEmpty();
    }
}
public class UDPServer implements Runnable{
    private PlayerManager playerManager;
    private EnemyManager enemyManager;
    private BoundaryManager boundaryManager;
    private SceneManager sceneManager;
    private GameState gameState;
    private StartingPad startPad;
    private PlayerColourChooser colourChooser;
    private ArrayList<Integer> clientsChosenColour;
    private PlayerPad newGamePad;
    double enemySpawnCooldown;
    double enemySpawnTimer;
    Random random;
    int currentEnemyID;
    Server<Bundle> server;
    long serverStartTime;
    double importantMessageCooldown;
    double importantMessageTimer;
    double heartbeatCooldown;
    double heartbeatTimer;
    int importantMessageID = 0;
    int port = 55555;
    double timeSurvived;
    ArrayList<ImportantMessage> importantMessages;
    public UDPServer(){
    }
    private void StartGame(){
        gameState = GameState.GameRunning;
        SendImportantMessage("0101"); // game start code
        timeSurvived = 0;
        enemyManager.ResetEnemies();
        sceneManager = new SceneManager(enemyManager, playerManager);
        boundaryManager = new BoundaryManager();
        currentEnemyID = 7;
        boundaryManager.AddBoundary(new Boundary(32, 0, 464, 0, 0, 1));
        boundaryManager.AddBoundary(new Boundary(464, 0, 562, 40, -1, 1));
        boundaryManager.AddBoundary(new Boundary(562, 40, 710, 240, -1, 0));
        boundaryManager.AddBoundary(new Boundary(710, 180, 562, 380, -1, 0));
        boundaryManager.AddBoundary(new Boundary(562, 380, 464, 420, -1, -1));
        boundaryManager.AddBoundary(new Boundary(464, 420, 32, 420, 0, -1));
        boundaryManager.AddBoundary(new Boundary(32, 420, 32, 0, 1, 0));
        random = new Random();
    }
    @Override
    public void run() {
        serverStartTime = System.currentTimeMillis();
        gameState = GameState.StartRoom;
        Logger.configure(new LoggerConfig());
        Logger.addOutput(new ConsoleOutput(), LoggerLevel.FATAL);
        playerManager = new PlayerManager();
        enemyManager = new EnemyManager();
        startPad = new StartingPad();
        colourChooser = new PlayerColourChooser();
        clientsChosenColour = new ArrayList<Integer>();
        newGamePad = new PlayerPad();
        importantMessages = new ArrayList<ImportantMessage>();
        importantMessageCooldown = 500;
        heartbeatCooldown = 500;
        enemySpawnCooldown = 5000;
        enemySpawnTimer = 0;
        //create server
        server = new NetService().newUDPServer(port);
        server.setOnConnected(connection -> {
            connection.addMessageHandler(new Handler(playerManager, enemyManager, this));
        });
        //create thread to broadcast data
        Thread t = new Thread(() -> {
            double lastFrame = System.currentTimeMillis();
            while(true){
                try {
                    Thread.sleep(30);
                    double frameDuration = System.currentTimeMillis() - lastFrame;
                    lastFrame = System.currentTimeMillis();
                    switch(gameState){
                        case StartRoom:
                            startPad.Update(playerManager, frameDuration, clientsChosenColour);
                            SendColoursInfo(colourChooser);
                            SendPlayerPadInfo(startPad);
                            if(startPad.starting){
                                StartGame();
                            }
                            if(playerManager.PlayersExist()) {
                                server.broadcast(new Bundle(playerManager.GetLocationData(serverStartTime)));
                            }
                            break;
                        case GameRunning:
                            timeSurvived += frameDuration;
                            SendTimeSurvived();
                            enemyManager.UpdateEnemies(boundaryManager, frameDuration);
                            server.broadcast(new Bundle(enemyManager.GetEnemyData(serverStartTime)));
                            if(!playerManager.AllPlayersDead()){
                                server.broadcast(new Bundle(playerManager.GetLocationData(serverStartTime)));
                            }
                            if(playerManager.AllPlayersDead()){
                                SendImportantMessage("0100" + CompressInt((int) timeSurvived / 100, 32));
                                //time survived is divided by 100 here, not 1000, so the time
                                //can be displayed to more precision on the final screen
                                gameState = GameState.GameOver;
                            }
                            enemySpawnTimer -= frameDuration;
                            if(enemySpawnTimer < 0){
                                enemySpawnTimer += enemySpawnCooldown;
                                SpawnRandomEnemy();
                                SpawnRandomEnemy();
                                SpawnRandomEnemy();
                                SpawnRandomEnemy();
                            }
                            break;
                        case GameOver:
                            //game over functionality
                            newGamePad.Update(playerManager, frameDuration);
                            SendPlayerPadInfo(newGamePad);
                            if(newGamePad.poweredState == 2){
                                StartGame();
                            }
                            if(playerManager.PlayersExist()){
                                server.broadcast(new Bundle(playerManager.GetLocationData(serverStartTime)));
                            }
                            break;
                    }
                    importantMessageTimer += frameDuration;
                    if(importantMessageTimer > importantMessageCooldown){
                        importantMessageTimer -= importantMessageCooldown;
                        SendImportantMessages();
                    }
                    heartbeatTimer += frameDuration;
                    if(heartbeatTimer > heartbeatCooldown){
                        heartbeatTimer -= heartbeatCooldown;
                        SendHeartbeat();
                    }
                    playerManager.CheckForKickedPlayers(this);
                    playerManager.UpdateHeartbeats(frameDuration);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        t.setDaemon(true);
        t.start();

        //start server which will listen for incoming data
        server.startTask().run();
    }
    //region enemySpawning
    private void SpawnRandomEnemy(){
        int enemyTypes = 3;
        int randomNumber = random.nextInt(enemyTypes) + 1;
        if(randomNumber == 1){
            enemyManager.AddBobleech(currentEnemyID, 50, 50, sceneManager);
        }
        else if (randomNumber == 2){
            enemyManager.AddFlopper(currentEnemyID, 50, 50, sceneManager);
        }
        else if (randomNumber == 3){
            enemyManager.AddClingabing(currentEnemyID, 50, 50, sceneManager);
        }
        currentEnemyID ++;
    }
    //endregion enemySpawning
    //region ImportantMessages
    private int GetNextMessageID(){
        importantMessageID++;
        return importantMessageID - 1;
    }
    public void SendImportantMessage(String binaryContents){
        if(!playerManager.PlayersExist()){
            System.out.println("Attempting to send an important message, but no players to send it to :( so lonely");
            return;
        }
        ImportantMessage messageToSend = new ImportantMessage(binaryContents, GetNextMessageID(), playerManager.GetClientIDs());
        importantMessages.add(messageToSend);
    }
    public void SendImportantMessageTo(String binaryContents, Integer clientID){
        System.out.println("sending important message to player ID: " + clientID);
        ImportantMessage messageToSend = new ImportantMessage(binaryContents,
                GetNextMessageID(),
                new ArrayList<>(Collections.singletonList(clientID)));
        importantMessages.add(messageToSend);
    }
    public void SendImportantMessages(){
        //called repeatedly in the main server loop
        ArrayList<ImportantMessage> messages = (ArrayList<ImportantMessage>) importantMessages.clone();
        for(ImportantMessage im : messages){
            server.broadcast(new Bundle(im.GetMessage()));
        }
    }
    public void SendImportantMessageConfirmation(String messageIn){
        String returnHeader = "1110"; // all confirmation messages have the same header as each important message
        //has as unique client id and message id so the header is only needed to signify that it is an important message confirmation
        String messageID = messageIn.substring(0, 32);
        String clientID = messageIn.substring(32, 64);
        server.broadcast(new Bundle(returnHeader.concat(messageID.concat(clientID))));
    }
    public void ReceiveImportantMessageConfirmation(String decompressedData) {
        int messageID = DecompressInt(decompressedData.substring(0, 32));
        int clientID = DecompressInt(decompressedData.substring(32, 64));
        for(ImportantMessage msg : importantMessages){
            if(msg.messageID == messageID){
                if(msg.ReceiveConfirmation(clientID)){
                    importantMessages.remove(msg);
                }
                return;
            }
        }
    }
    //endregion ImportantMessages
    //region UpdateFunctions
    public void SendHeartbeat(){
        String out = "1010";
        server.broadcast(new Bundle(out));
    }
    public void KickPlayer(int clientID){
        String out = "1011";
        out = out.concat(CompressInt(clientID, 32));
        server.broadcast(new Bundle(out));
    }
    public void HeartbeatResponse(String data){
        playerManager.HeartbeatResponse(data);
    }
    public void SendNetworkInfo(){
        //only send network info if on start screen
        if (gameState == GameState.StartRoom){
            String addressString;
            try {
                InetAddress address = InetAddress.getLocalHost();
                addressString = address.getHostAddress();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            if(addressString != null){
                server.broadcast(new Bundle("0000" + CompressAddress(addressString)
                        + CompressInt(port, 32)
                        + CompressString(serverName, 512)));
            }
        }
    }
    public void SendBoundaryData(){
        String out = boundaryManager.GetBoundaryData(serverStartTime);
        server.broadcast(new Bundle(out));
    }
    public void SendPlayerPadInfo(PlayerPad padToSend){
        String out = padToSend.GetPadInfo();
        server.broadcast(new Bundle(out));
    }
    public void SendColoursInfo(PlayerColourChooser chooser){
        String out = chooser.GetColourInfo();
        server.broadcast(new Bundle(out));
    }
    public void SendColourConfirm(String messageContents){
        String colourCode = messageContents.substring(64, 67);
        String clientBinary = messageContents.substring(67, 99);
        int clientID = DecompressInt(clientBinary);
        if(colourChooser.Use(colourCode)){
            if(!clientsChosenColour.contains(clientID)){
                clientsChosenColour.add(clientID);
            }
            SendImportantMessageTo("10001" + colourCode + clientBinary, clientID);
            // 1000 is the code for a colour confirmation, the final digit is the result of the request
            // the colour code is included so the client can display its colour
        }
        else{
            //SendImportantMessage("10000");
        }
    }
    public void FreeUpColour(String messageContents){
        String colourCode = messageContents.substring(64,67);
        colourChooser.FreeUp(colourCode);
    }
    public void StartLeverPulled(){
        startPad.LeverPulled();
    }
    public void SendTimeSurvived(){
        String out = "0111";
        out = out.concat(CompressInt((int)timeSurvived / 1000, 32)); // convert millis to seconds
        server.broadcast(new Bundle(out));
    }
    //endregion UpdateFunctions
    //region Compression
    public static String CompressPlayerState(PlayerState state){
        return CompressInt(state.direction, 3) +
                CompressInt(state.movementState, 2) +
                CompressInt(state.attackState, 2) +
                CompressInt(state.animationState, 2);
    }
    public static String CompressInt(int inInt, int maxBits){
        return PadBinary(Integer.toBinaryString(inInt), maxBits);
    }
    public static String CompressPosition(int x, int y){
        x = x / 4;
        y = y / 4;
        String out = PadBinary(Integer.toBinaryString(x), 8);
        out = out.concat(PadBinary(Integer.toBinaryString(y), 8));
        return out;
    }
    public static String LongToBinary(long inLong, int maxBits){
        return PadBinary(Long.toBinaryString(inLong), maxBits);
    }
    public static String PadBinary(String inBinary, int maxBits){
        if(maxBits < inBinary.length()){
            //if the string is longer that it's supposed to be we cut off the start
            int cutoff = inBinary.length() - maxBits;
            return inBinary.substring(cutoff);
        }
        int paddingAmount = maxBits - inBinary.length();
        String padding = new String(new char[paddingAmount]).replace("\0", "0");
        return padding.concat(inBinary);
    }
    public static PlayerState DecompressPlayerState(String binaryIn) {
        if(binaryIn.length() != 9){
            System.out.println("PlayerState Binary must be 9 bits exactly");
        }
        int direction = Integer.parseUnsignedInt(binaryIn.substring(0, 3), 2);
        int movementState = Integer.parseUnsignedInt(binaryIn.substring(3, 5), 2);
        int attackState = Integer.parseUnsignedInt(binaryIn.substring(5, 7), 2);
        int animationState = Integer.parseUnsignedInt(binaryIn.substring(7, 9), 2);
        return new PlayerState(direction, movementState, attackState, animationState);
    }
    public static int DecompressInt(String binaryIn){
        return Integer.parseUnsignedInt(binaryIn, 2);
    }
    public static int[] DecompressPosition(String binaryIn){
        int[] returnValues = new int[2];
        returnValues[0] = Integer.parseUnsignedInt(binaryIn.substring(0, 8), 2) * 4;
        returnValues[1] = Integer.parseUnsignedInt(binaryIn.substring(8, 16), 2) * 4;
        return returnValues;
    }
    public static String CompressAddress(String address){
        StringBuilder outBinary = new StringBuilder();
        byte[] stringBytes = address.getBytes();
        for (byte stringByte : stringBytes) {
            outBinary.append(PadBinary(Integer.toBinaryString(stringByte), 8));
        }
        return PadBinary(outBinary.toString(), 128);
    }
    public static String CompressString(String stringToCompress, int outLength){
        StringBuilder outBinary = new StringBuilder();
        byte[] stringBytes = stringToCompress.getBytes();
        for (byte stringByte : stringBytes) {
            outBinary.append(PadBinary(Integer.toBinaryString(stringByte), 8));
        }
        return PadBinary(outBinary.toString(), outLength);
    }
    //endregion Compression
}
class Handler implements MessageHandler<Bundle> {
    private PlayerManager playerManager;
    private EnemyManager enemyManager;
    private UDPServer server;
    Handler(PlayerManager pPlayerManager, EnemyManager pEnemyManager, UDPServer pServer){
        playerManager = pPlayerManager;
        enemyManager = pEnemyManager;
        server = pServer;
        Readers.INSTANCE.addUDPReader(Bundle.class, new BundleMessageReaderS());
        Writers.INSTANCE.addUDPWriter(Bundle.class, new BundleMessageWriterS());
    }

    static class BundleMessageReaderS implements UDPMessageReader<Bundle> {
        BundleMessageReaderS(){
        }
        public static int convertToInt(byte[] data) {
            if (data.length != 4) {
                throw new IllegalArgumentException("Input array must have exactly 4 bytes.");
            }

            // Convert the 4 bytes to a single 32-bit integer
            int result = ((data[0] & 0xFF) << 24)  // Shift the first byte 24 bits to the left
                    | ((data[1] & 0xFF) << 16)  // Shift the second byte 16 bits to the left
                    | ((data[2] & 0xFF) << 8)   // Shift the third byte 8 bits to the left
                    | (data[3] & 0xFF);         // No shift needed for the last byte

            return result;
        }
        @Override
        public Bundle read(@NotNull byte[] inData){
            //each packet starts with a 4 byte header that denotes the length of the packet
            byte[] headerBytes = new byte[]{inData[3], inData[2], inData[1], inData[0]};
            //we calculate the length from these 4 bytes, so that we can read the rest of the data
            int length = convertToInt(headerBytes);
            StringBuilder dataDecompressor = new StringBuilder();
            //iterate through the remaining bytes of the packet, skipping the first 4
            for(int i = 0; i <= length -4; i ++){
                byte thisByte = inData[i + 4];
                //extract the binary data from each byte, as a string of 1s and 0s
                String binaryString = String.format("%8s", Integer.toBinaryString(thisByte & 0xFF)).replace(' ', '0');
                //append this to our output
                dataDecompressor.append(binaryString, 0, 8);
            }
            //format and return our output
            String decompressedData = dataDecompressor.toString();
            return new Bundle(decompressedData);
        }
    }
    static class BundleMessageWriterS implements UDPMessageWriter<Bundle> {
        BundleMessageWriterS(){
        }
        @NotNull
        @Override
        public byte[] write(Bundle bundle) {
            StringBuilder data = new StringBuilder(bundle.getName());
            //pad data to the nearest byte
            while((data.length() % 8) != 0){
                data.append("0");
            }
            //create an empty array of bytes to hold our data
            byte[] packetData = new byte[data.length() / 8];
            //iterate through the string, byte at a time
            for(int i = 0; i < data.length(); i += 8){
                //extract a single byte of data from the input string
                String byteString = data.substring(i, i + 8);
                // Convert the binary string to a signed byte
                try {
                    //parse the string representing a byte into a byte
                    byte byteValue = (byte) Integer.parseInt(byteString, 2);
                    packetData[i / 8] = byteValue;
                } catch (NumberFormatException e) {
                    System.err.println("Invalid binary string: " + byteString);
                    return new byte[0]; // Return an empty byte array in case of error
                }
            }
            //return our formatted packet data
            return packetData;
        }
    }
    @Override
    public void onReceive(Connection<Bundle> connection, Bundle bundle) {
        if(bundle == null){
            return;
        }
        String decompressedData = bundle.getName();
        String messageType = decompressedData.substring(0, 4); // first 4 bits of message denote message data
        decompressedData = decompressedData.substring(4); // process only the rest of the data
        HandleMessage(messageType, decompressedData);
    }
    private void HandleMessage(String messageType, String decompressedData){
        //depending on the first 4 characters, representing the message type, different responses occur
        switch (messageType) {
            case "0000": // A client on a discovery screen scanning for servers is requesting the server data
                server.SendNetworkInfo();
                break;
            case "0001": // A client requesting the locations of the boundaries of the ship
                server.SendBoundaryData();
                break;
            case "0010": // A client sending data about its player: location, state, and if its alive
                playerManager.incomingData(decompressedData);
                break;
            case "0011": // A client sending data about enemies: if it's player has hit any
                enemyManager.incomingData(decompressedData);
                break;
            case "0100": // A client pulled the lever in the start room
                server.SendImportantMessageConfirmation(decompressedData);
                server.StartLeverPulled();
                break;
            case "0101": // A client selects a colour
                server.SendImportantMessageConfirmation(decompressedData);
                server.SendColourConfirm(decompressedData);
                break;
            case "0110":
                server.SendImportantMessageConfirmation(decompressedData);
                server.FreeUpColour(decompressedData);
                break;
            case "0111":
                server.HeartbeatResponse(decompressedData);
                break;
            case "1110": // Receiving confirmation of an important message sent by the server
                server.ReceiveImportantMessageConfirmation(decompressedData);
                break;
        }
    }
}
