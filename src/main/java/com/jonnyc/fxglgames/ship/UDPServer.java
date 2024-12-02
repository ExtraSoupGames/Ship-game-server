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
import java.util.Collection;
import java.util.Collections;

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
    Server<Bundle> server;
    long serverStartTime;
    double importantMessageCooldown;
    double importantMessageTimer;
    int importantMessageID = 0;
    ArrayList<ImportantMessage> importantMessages;
    public UDPServer(){
    }
    @Override
    public void run() {
        serverStartTime = System.currentTimeMillis();
        Logger.configure(new LoggerConfig());
        Logger.addOutput(new ConsoleOutput(), LoggerLevel.FATAL);
        playerManager = new PlayerManager();
        enemyManager = new EnemyManager();
        sceneManager = new SceneManager(enemyManager, playerManager);
        boundaryManager = new BoundaryManager();
        enemyManager.AddBobleech(1, 50, 50, sceneManager);
        enemyManager.AddBobleech(2, 70, 50, sceneManager);
        enemyManager.AddBobleech(3, 90, 50, sceneManager);
        enemyManager.AddBobleech(4, 110, 50, sceneManager);
        enemyManager.AddClingabing(5, 100, 100, sceneManager);
        enemyManager.AddFlopper(6, 200, 200, sceneManager);
        boundaryManager.AddBoundary(new Boundary(50, 50, 550, 50, 0,1));
        boundaryManager.AddBoundary(new Boundary(550, 50, 750, 250, -1,1));
        boundaryManager.AddBoundary(new Boundary(750, 250, 550, 450, -1,-1));
        boundaryManager.AddBoundary(new Boundary(550, 450, 50, 450, 0,-1));
        boundaryManager.AddBoundary(new Boundary(50, 450, 50, 50, 1,0));
        importantMessages = new ArrayList<ImportantMessage>();
        importantMessageCooldown = 500;
        //create server
        server = new NetService().newUDPServer(55555);
        server.setOnConnected(connection -> {
            connection.addMessageHandler(new Handler(playerManager, enemyManager, this));
        });
        //create thread to broadcast data
        var t = new Thread(() -> {
            double lastFrame = System.currentTimeMillis();
            while(true){
                try {
                    Thread.sleep(30);
                    double frameDuration = System.currentTimeMillis() - lastFrame;
                    lastFrame = System.currentTimeMillis();
                    if(playerManager.PlayersExist()){
                        server.broadcast(new Bundle(playerManager.GetLocationData(serverStartTime)));
                    }
                    enemyManager.UpdateEnemies(boundaryManager, frameDuration);
                    server.broadcast(new Bundle(enemyManager.GetEnemyData(serverStartTime)));
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
        String returnHeader = "1010"; // all confirmation messages have the same header as each important message
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
            server.broadcast(new Bundle("0001" + CompressAddress(addressString)
                    + CompressInt(55555, 32)
                    + CompressString(serverName, 512)));
        }
    }
    public void SendBoundaryData(){
        String out = boundaryManager.GetBoundaryData(serverStartTime);
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
        for(int i = 0; i < address.length(); i++){
            char addressChar = address.charAt(i);
            outBinary.append(Integer.toBinaryString((byte) addressChar));
        }
        return PadBinary(outBinary.toString(), 512);
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
        @Override
        public Bundle read(@NotNull byte[] inData){
            byte b = 0;
            int index = inData.length;
            while(b==0){
                index--;
                b = inData[index];
            }
            StringBuilder dataDecompressor = new StringBuilder();
            for(int i = 0; i <= index; i ++){
                byte thisByte = inData[i];
                String binaryString = String.format("%8s", Integer.toBinaryString(thisByte & 0xFF)).replace(' ', '0');
                dataDecompressor.append(binaryString.substring(0, 7));
            }
            String decompressedData = dataDecompressor.toString();
            return new Bundle(decompressedData);
        }
    }
    static class BundleMessageWriterS implements UDPMessageWriter<Bundle> {
        BundleMessageWriterS(){
            //System.out.println("Constructing UDP BUNDLE MESSAGE WRITER");
        }
        @NotNull
        @Override
        public byte[] write(Bundle bundle) {
            StringBuilder data = new StringBuilder(bundle.getName());
            //compress data
            //pad data to a byte (only first 7 bits used of each byte)
            while((data.length() % 7) != 0){
                data.append("0");
            }

            byte[] packetData = new byte[data.length() / 7];
            for(int i = 0; i < data.toString().length(); i += 7){
                if(data.charAt(i) == '1'){
                    data.replace(i, i + 1, "-"); //convert to twos complement representation
                    packetData[i / 7] = (byte) (-128 - Byte.parseByte(data.substring(i, i + 7) + "1", 2));
                }
                else{
                    packetData[i / 7] = Byte.parseByte(data.substring(i, i + 7) + "1", 2);
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
        switch (messageType){
            case "0000":
                server.SendNetworkInfo();
                break;
            case "0100":
                playerManager.incomingData(decompressedData);
                break;
            case "0110":
                enemyManager.incomingData(decompressedData);
                break;
            case "0010":
                server.SendBoundaryData();
                break;
            case "1001":
                server.SendImportantMessageConfirmation(decompressedData);
                break;
            case "1011":
                server.ReceiveImportantMessageConfirmation(decompressedData);
                break;
        }
    }
}
