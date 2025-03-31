package com.caw;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.caw.game.GameScreen;
import com.caw.game.GameStart;

public class DesktopLauncher {
    public static void main(String[] args){
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Game");
        config.setWindowSizeLimits(800,600,1920,1080);
        config.setWindowedMode(800,600);
        new Lwjgl3Application(new GameStart(), config);
    }
}
