package com.caw.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public class GameStart extends Game {
    public SpriteBatch batch;
    public Preferences prefs;
    public BitmapFont defaultFont;
    public BitmapFont hudScoreFont;

    @Override
    public void create() {
        batch = new SpriteBatch();
        prefs = Gdx.app.getPreferences("MyGamePrefrences");
        FreeTypeFontGenerator generator = null;
        try {
            generator = new FreeTypeFontGenerator(Gdx.files.internal("assets/fonts/Hud_font.ttf"));
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();

            //standart font
            parameter.size = 16;
            parameter.color = Color.WHITE;
            defaultFont = generator.generateFont(parameter);

            //for HUD
            parameter.size = 16;
            hudScoreFont = generator.generateFont(parameter);

        } catch (Exception e) {
            Gdx.app.error("FontLoader", "Could not generate fonts from TTF", e);
            defaultFont = new BitmapFont();
            hudScoreFont = new BitmapFont();
        } finally {
            if (generator != null) {
                generator.dispose();
            }
        }
        this.setScreen(new MainMenuScreen(this));
    }


    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        batch.dispose();
        defaultFont.dispose();
        if (defaultFont != null) defaultFont.dispose();
        if (hudScoreFont != null) hudScoreFont.dispose();
    }
}
