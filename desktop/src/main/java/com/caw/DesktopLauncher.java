package com.caw;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.caw.game.GameScreen;
import com.caw.game.GameStart;

public class DesktopLauncher {
    public static void main(String[] args){
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Game");
        config.setForegroundFPS(120);
        config.setWindowSizeLimits(800,600,2560,1600);
        config.setWindowedMode(800,600);
        config.useVsync(false);
        new Lwjgl3Application(new GameStart(), config);
    }
}
