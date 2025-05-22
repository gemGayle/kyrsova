package com.caw.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Enemy {
    private float stateTime;
    public Body body;
    private Animation<TextureRegion> patrolAnimation;
    public static final int SPRITESHEET_FRAME_WIDTH = 32;
    public static final int SPRITESHEET_FRAME_HEIGHT = 20;
    private boolean movingRight = true;

    private float speed = 1.0f;

    private float patrolCenterX_meters;
    private float patrolHalfDistanceMeters;

    private boolean isStomped = false;
    private boolean scheduledForRemoval = false;
    private GameScreen gameScreen;

    private float visualWidthPixels;
    private float visualHeightPixels;

    public Enemy(Body body, Texture patrolSheet, float initialX_pixels, float patrolDistance_pixels,
                 float visualWidth_pixels, float visualHeight_pixels, GameScreen gameScreen) {

        this.body = body;
        this.visualWidthPixels = visualWidth_pixels;
        this.visualHeightPixels = visualHeight_pixels;

        this.gameScreen = gameScreen;

        this.patrolCenterX_meters = initialX_pixels / GameScreen.PPM;
        this.patrolHalfDistanceMeters = (patrolDistance_pixels / 2f) / GameScreen.PPM;

        loadAnimations(patrolSheet);

        if (this.body != null) {
            if (!this.body.getFixtureList().isEmpty()) {
                Fixture mainFixture = this.body.getFixtureList().first();
                mainFixture.setFriction(0.2f);
            }
            this.movingRight = true;
            this.body.setLinearVelocity(speed, 0);
            this.body.setUserData(this);
        }
        stateTime = 0f;
    }
    private void loadAnimations(Texture patrolSheet) {
        if (patrolSheet != null) {
            int patrolFrameCount = 4;
            TextureRegion[][] tmpPatrol = TextureRegion.split(patrolSheet, SPRITESHEET_FRAME_WIDTH, SPRITESHEET_FRAME_HEIGHT);
            Array<TextureRegion> patrolFrames = new Array<>(patrolFrameCount);
            for (int i = 0; i < patrolFrameCount; i++) {
                patrolFrames.add(tmpPatrol[0][i]);
            }
            patrolAnimation = new Animation<>(0.15f, patrolFrames, Animation.PlayMode.LOOP);
        }
    }

    public void update(float deltaTime) {
        if (body == null || !body.isActive()) {
            return;
        }

        if (isStomped || scheduledForRemoval) {
            if (!scheduledForRemoval) {
                body.setLinearVelocity(0, 0);
                gameScreen.scheduleBodyForRemoval(this.body);
                scheduledForRemoval = true;
            }
            return;
        }

        stateTime += deltaTime; //update anim time
        Vector2 currentPosition = body.getPosition();

        if (patrolHalfDistanceMeters > 0.001f) {
            float leftBound = patrolCenterX_meters - patrolHalfDistanceMeters;
            float rightBound = patrolCenterX_meters + patrolHalfDistanceMeters;

            if (movingRight) {
                if (currentPosition.x >= rightBound) {
                    movingRight = false;
                    body.setTransform(rightBound, currentPosition.y, body.getAngle());
                    body.setLinearVelocity(-speed, body.getLinearVelocity().y);
                } else if (body.getLinearVelocity().x < speed * 0.9f) {
                    body.setLinearVelocity(speed, body.getLinearVelocity().y);
                }
            } else {
                if (currentPosition.x <= leftBound) {
                    movingRight = true;
                    body.setTransform(leftBound, currentPosition.y, body.getAngle());
                    body.setLinearVelocity(speed, body.getLinearVelocity().y);
                } else if (body.getLinearVelocity().x > -speed * 0.9f) {
                    body.setLinearVelocity(-speed, body.getLinearVelocity().y);
                }
            }
        } else {
            if (Math.abs(body.getLinearVelocity().x) > 0.01f) {
                body.setLinearVelocity(0, body.getLinearVelocity().y);
            }
        }
    }

    private TextureRegion getFrameToRender() {
        if (patrolAnimation != null) {
            return patrolAnimation.getKeyFrame(stateTime, true);
        }
        return null;
    }

    private void flipCurrentFrame(TextureRegion frame) {
        if (frame == null) return;

        if (!movingRight && !frame.isFlipX()) {
            frame.flip(true, false);
        } else if (movingRight && frame.isFlipX()) {
            frame.flip(true, false);
        }
    }

    public void draw(SpriteBatch batch) {
        if (body == null || !body.isActive() || isStomped) {
            return;
        }

        TextureRegion currentFrame = getFrameToRender();
        if (currentFrame == null) {
            return;
        }

        flipCurrentFrame(currentFrame);

        float drawX = body.getPosition().x * GameScreen.PPM - visualWidthPixels / 2f;
        float drawY = body.getPosition().y * GameScreen.PPM - visualHeightPixels / 2f;

        batch.draw(currentFrame, drawX, drawY, visualWidthPixels, visualHeightPixels);
    }

    public void onStomped() {
        if (isStomped) return;

        Gdx.app.log("ENEMY", "Enemy stomped!");
        isStomped = true;

        if (body != null && body.isActive()) {
            body.setLinearVelocity(0, 0);

            for (Fixture fixture : body.getFixtureList()) {
                fixture.setSensor(true);
            }

            if (gameScreen != null && !scheduledForRemoval) {
                gameScreen.scheduleBodyForRemoval(this.body);
                scheduledForRemoval = true;
            }
        }
        if (gameScreen != null) gameScreen.playEnemyDeathSound();
    }

    public boolean isStomped() {
        return isStomped;
    }

    public boolean isScheduledForRemoval() {
        return scheduledForRemoval;
    }

    public Body getBody() {
        return body;
    }

    public float getVisualHeight() {
        return visualHeightPixels;
    }

    public void dispose() {
    }
}
