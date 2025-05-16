package com.caw.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.math.Vector2;

public class Enemy {
    public Body body;
    private TextureRegion textureRegion;

    private float speed = 1.0f;
    private boolean movingRight = true;
    private float startX_meters;
    private float patrolDistanceMeters;

    private boolean isStomped = false;
    private boolean scheduledForRemoval = false;
    private GameScreen gameScreen;

    private float visualWidthPixels;
    private float visualHeightPixels;

    public Enemy(Body body, Texture enemyTexture, float initialX_pixels, float patrolDistance_pixels,
                 float visualWidth_pixels, float visualHeight_pixels, GameScreen gameScreen) {
        this.body = body;
        this.textureRegion = new TextureRegion(enemyTexture);
        this.visualWidthPixels = visualWidth_pixels;
        this.visualHeightPixels = visualHeight_pixels;

        // this.width = visualWidth_pixels / GameScreen.PPM;
        // this.height = visualHeight_pixels / GameScreen.PPM;

        this.startX_meters = initialX_pixels / GameScreen.PPM;
        this.patrolDistanceMeters = patrolDistance_pixels / GameScreen.PPM;

        this.gameScreen = gameScreen;

        if (this.body != null) {
            if (!this.body.getFixtureList().isEmpty()) {
                Fixture mainFixture = this.body.getFixtureList().first();
                mainFixture.setFriction(0.2f);
            }
            this.body.setLinearVelocity(speed, 0);
            this.body.setUserData(this);
        }
    }

    public void update(float deltaTime) {
        if (isStomped || scheduledForRemoval || body == null || !body.isActive()){
            if (body != null && body.isActive()) {
                body.setLinearVelocity(0, body.getLinearVelocity().y);
            }
            return;
        }

        Vector2 currentPosition = body.getPosition();
        float currentSpeed = speed;

        if (movingRight) {
            if (currentPosition.x > startX_meters + patrolDistanceMeters) {
                movingRight = false;
                currentSpeed = -speed;
            } else {
                currentSpeed = speed;
            }
        } else {
            if (currentPosition.x < startX_meters - patrolDistanceMeters) {
                movingRight = true;
                currentSpeed = speed;
            } else {
                currentSpeed = -speed;
            }
        }
        body.setLinearVelocity(currentSpeed, body.getLinearVelocity().y);

        if ((currentSpeed > 0 && textureRegion.isFlipX()) || (currentSpeed < 0 && !textureRegion.isFlipX())) {
            textureRegion.flip(true, false);
        }
    }

    public void draw(SpriteBatch batch) {
        if (body == null || !body.isActive() || (isStomped && scheduledForRemoval)) {
            return;
        }

        float drawX = body.getPosition().x * GameScreen.PPM - visualWidthPixels / 2f;
        float drawY = body.getPosition().y * GameScreen.PPM - visualHeightPixels / 2f;

        batch.draw(textureRegion, drawX, drawY, visualWidthPixels, visualHeightPixels);
    }

    public void onStomped() {
        if (isStomped || scheduledForRemoval) return;

        Gdx.app.log("ENEMY", "Enemy stomped!");
        isStomped = true;

        if (body != null && body.isActive()) {
            body.setLinearVelocity(0,0);

            for (Fixture fixture : body.getFixtureList()) {
                fixture.setSensor(true);
            }

            if (gameScreen != null) {
                gameScreen.scheduleBodyForRemoval(this.body);
                scheduledForRemoval = true;
            }
        }
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
