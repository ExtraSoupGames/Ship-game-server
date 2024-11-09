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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class UDPServer implements Runnable{
    private PlayerManager playerManager;
    private EnemyManager enemyManager;
    private BoundaryManager boundaryManager;
    private SceneManager sceneManager;
    Server<Bundle> server;
    long serverStartTime;
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
        enemyManager.AddEnemy(1, playerManager);
        enemyManager.AddEnemy(2, 200, sceneManager);
        enemyManager.AddEnemy(3, 100, sceneManager);
        boundaryManager.AddBoundary(new Boundary(0, 500, 500, 0, -1,- 1));
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

    //region UpdateFunctions
    public void SendBoundaryData(){
        String out = boundaryManager.GetBoundaryData(serverStartTime);
        server.broadcast(new Bundle(out));
    }

    //endregion
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
        String messageType = decompressedData.substring(0, 3); // first 3 bits of message denote message data
        switch (messageType){
            case "010":
                playerManager.incomingData(decompressedData);
                break;
            case "101":
                enemyManager.incomingData(decompressedData);
                break;
            case "000":
                server.SendBoundaryData();
                break;

        }
    }
}
