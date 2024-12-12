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
import java.util.ArrayList;
import java.util.Arrays;

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
    private PlayerPad newGamePad;
    Server<Bundle> server;
    long serverStartTime;
    double importantMessageCooldown;
    double importantMessageTimer;
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
        enemyManager.AddBobleech(1, 50, 50, sceneManager);
        enemyManager.AddBobleech(2, 70, 50, sceneManager);
        enemyManager.AddBobleech(3, 90, 50, sceneManager);
        enemyManager.AddBobleech(4, 110, 50, sceneManager);
        enemyManager.AddClingabing(5, 100, 100, sceneManager);
        enemyManager.AddFlopper(6, 200, 200, sceneManager);
        boundaryManager.AddBoundary(new Boundary(0, 0, 720, 0, 0, 1));
        boundaryManager.AddBoundary(new Boundary(720, 0, 720, 480, -1, 0));
        boundaryManager.AddBoundary(new Boundary(720, 480, 0, 480, 0, -1));
        boundaryManager.AddBoundary(new Boundary(0, 480, 0, 0, 1, 0));
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
        newGamePad = new PlayerPad();
        importantMessages = new ArrayList<ImportantMessage>();
        importantMessageCooldown = 500;
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
                            startPad.Update(playerManager, frameDuration);
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
                                server.broadcast(new Bundle("0100")); // TODO add report data here
                                gameState = GameState.GameOver;
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
    public void SendImportantMessages(){
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
    public void SendNetworkInfo(){
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
    public void SendBoundaryData(){
        String out = boundaryManager.GetBoundaryData(serverStartTime);
        server.broadcast(new Bundle(out));
    }
    public void SendPlayerPadInfo(PlayerPad padToSend){
        String out = padToSend.GetPadInfo();
        server.broadcast(new Bundle(out));
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
                CompressInt(state.attackState, 2);
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
        if(binaryIn.length() != 7){
            System.out.println("PlayerState Binary must be 7 bits exactly");
        }
        int direction = Integer.parseUnsignedInt(binaryIn.substring(0, 3), 2);
        int movementState = Integer.parseUnsignedInt(binaryIn.substring(3, 5), 2);
        int attackState = Integer.parseUnsignedInt(binaryIn.substring(5, 7), 2);
        return new PlayerState(direction, movementState, attackState);
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
            byte[] headerBytes = new byte[]{inData[3], inData[2], inData[1], inData[0]};
            int length = convertToInt(headerBytes);
            StringBuilder dataDecompressor = new StringBuilder();
            for(int i = 0; i <= length -4; i ++){
                byte thisByte = inData[i + 4];
                String binaryString = String.format("%8s", Integer.toBinaryString(thisByte & 0xFF)).replace(' ', '0');
                dataDecompressor.append(binaryString, 0, 8);
            }
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
            //compress data
            //pad data to a byte
            while((data.length() % 8) != 0){
                data.append("0");
            }

            byte[] packetData = new byte[data.length() / 8];
            for(int i = 0; i < data.length(); i += 8){
                String byteString = data.substring(i, i + 8);
                // Convert the binary string to a signed byte
                try {
                    byte byteValue = (byte) Integer.parseInt(byteString, 2);
                    packetData[i / 8] = byteValue;
                } catch (NumberFormatException e) {
                    System.err.println("Invalid binary string: " + byteString);
                    return new byte[0]; // Return an empty byte array in case of error
                }
            }
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
                server.StartLeverPulled();
                break;
            case "1011": // Receiving confirmation of an important message sent by the server
                server.ReceiveImportantMessageConfirmation(decompressedData);
                break;
        }
    }
}
