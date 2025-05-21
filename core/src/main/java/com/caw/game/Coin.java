package com.caw.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Array;

public class Coin {
    private Body body;
    private Animation<TextureRegion> spinAnimation;
    private float stateTime;

    public static final int SPRITESHEET_FRAME_WIDTH = 16;
    public static final int SPRITESHEET_FRAME_HEIGHT = 16;
    public static final float VISUAL_COIN_SIZE = 16f;

    private boolean scheduledForRemoval = false;

    public Coin(Body body, Texture animationSheet) {
        this.body = body;
        if (this.body != null) {
            this.body.setUserData(this);
        }
        loadAnimation(animationSheet);
        this.stateTime = (float)(Math.random() * 100);
    }

    private void loadAnimation(Texture animationSheet) {
        if (animationSheet == null) {
            Gdx.app.error("CoinAnimation", "Animation sheet for coin is null!");
            return;
        }


        int frameCount = 8;
        TextureRegion[][] tmpFrames = TextureRegion.split(animationSheet, SPRITESHEET_FRAME_WIDTH, SPRITESHEET_FRAME_HEIGHT);
        Array<TextureRegion> spinFrames = new Array<>(frameCount);
        for (int i = 0; i < frameCount; i++) {
            spinFrames.add(tmpFrames[0][i]);
        }

        spinAnimation = new Animation<>(0.15f, spinFrames, Animation.PlayMode.LOOP);
    }

    public void update(float deltaTime) {
        if (scheduledForRemoval) return;
        stateTime += deltaTime;
    }

    public void render(SpriteBatch batch) {
        if (body == null || !body.isActive() || scheduledForRemoval || spinAnimation == null) {
            return;
        }

        TextureRegion currentFrame = spinAnimation.getKeyFrame(stateTime, true);

        float coinX = body.getPosition().x * GameScreen.PPM - VISUAL_COIN_SIZE / 2f;
        float coinY = body.getPosition().y * GameScreen.PPM - VISUAL_COIN_SIZE / 2f;

        batch.draw(currentFrame, coinX, coinY, VISUAL_COIN_SIZE, VISUAL_COIN_SIZE);
    }

    public Body getBody() {
        return body;
    }

    public void scheduleForRemoval() {
        this.scheduledForRemoval = true;
    }

    public boolean isScheduledForRemoval() {
        return scheduledForRemoval;
    }

    public void dispose() {
    }
}
