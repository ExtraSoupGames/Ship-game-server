/*
 * The MIT License (MIT)
 *
 * FXGL - JavaFX Game Library
 *
 * Copyright (c) 2015-2017 AlmasB (almaslvl@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.jonnyc.fxglgames.ship;

import com.almasb.fxgl.app.ApplicationMode;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;

import static com.almasb.fxgl.dsl.FXGL.*;


import java.util.ArrayList;
import java.util.Map;

public class ShipServer extends GameApplication{
    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle(serverName);
        settings.setVersion("1.0");
        settings.setFontUI("pong.ttf");
        settings.setApplicationMode(ApplicationMode.RELEASE);
    }
    UDPServer server;
    static String serverName = "DefaultServerName";

    @Override
    protected void initPhysics(){
        getPhysicsWorld().setGravity(0,0);
    }

    @Override
    protected void initInput() {
    //server input - not necessary

    }


    @Override
    protected void initGameVars(Map<String, Object> vars) {}

    @Override
    protected void initGame() {
        getGameWorld().addEntityFactory(new ShipFactory());
        server = new UDPServer();
        Thread serverThread = new Thread(server);
        serverThread.start();
    }


    @Override
    protected void onUpdate(double tpf){

    }

    public static void main(String[] args) {
        if(args.length > 0){
            serverName = args[0];
        }
        launch(args);
    }
}
