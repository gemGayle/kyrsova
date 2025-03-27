package com.caw.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.math.Rectangle;

public class MainApp extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture playerSprite;
    Rectangle player;
    OrthographicCamera camera;

    @Override
    public void create() {
        batch = new SpriteBatch();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);

        // textures
        playerSprite = new Texture("C:/Users/User/Desktop/study/kyrsova/assets/PlayerIdle.png");

        // player rect
        player = new Rectangle();
        player.x = 800/2 - 32/2;
        player.y = 20;
        player.width = 32;
        player.height = 32;

    }

    @Override
    public void render() {
        ScreenUtils.clear(0.5f, 0.7f, 1f, 1f);

        camera.update();

        batch.begin();
        batch.draw(playerSprite, player.x, player.y);
        batch.end();

        //controls
        if(Gdx.input.isKeyPressed(Input.Keys.A)) player.x -= 400 * Gdx.graphics.getDeltaTime();
        if(Gdx.input.isKeyPressed(Input.Keys.D)) player.x += 400 * Gdx.graphics.getDeltaTime();

        if(player.x < 0) player.x = 0;
        if(player.x > 800 - 32) player.x = 800 - 32;
    }

    @Override
    public void dispose() {
        batch.dispose();
        playerSprite.dispose();
    }
}
