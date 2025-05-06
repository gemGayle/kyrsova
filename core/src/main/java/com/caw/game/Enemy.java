package com.caw.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.math.Vector2;

public class Enemy {
    public Body body;
    private TextureRegion textureRegion;
    private float width, height; //meters
    private float visualWidth, visualHeight; //pix

    private float speed = 1.0f;
    private boolean movingRight = true;
    private float startX;
    private float patrolDistanceMeters;

    public Enemy(Body body, Texture enemyTexture, float initialX_meters, float partolDistance_pixels, float visualWidth_pixels, float visualHeight_pixels) {
        this.body = body;
        this.textureRegion = new TextureRegion(enemyTexture);
        this.visualWidth = visualWidth_pixels;
        this.visualHeight = visualHeight_pixels;

        this.width = visualWidth_pixels / GameScreen.PPM;
        this.height = visualHeight_pixels / GameScreen.PPM;

        this.startX = initialX_meters;
        this.patrolDistanceMeters = partolDistance_pixels / GameScreen.PPM;

        this.body.setLinearVelocity(speed, 0);
    }

    public void update(float deltaTime) {
        Vector2 currentPosition = body.getPosition();
        float currentSpeed = speed;

        if (movingRight) {
            if (currentPosition.x > startX + patrolDistanceMeters) {
                movingRight = false;
                currentSpeed = -speed; //vlivo
            } else {
                currentSpeed = speed; //vpravo
            }
        } else { // movingLeft
            if (currentPosition.x < startX - patrolDistanceMeters) {
                movingRight = true;
                currentSpeed = speed; //vpravo
            } else {
                currentSpeed = -speed; //vlivo
            }
        }
        body.setLinearVelocity(currentSpeed, body.getLinearVelocity().y);

        if ((currentSpeed > 0 && textureRegion.isFlipX()) || (currentSpeed < 0 && !textureRegion.isFlipX())) {
            textureRegion.flip(true, false);
        }
    }

    public void draw(SpriteBatch batch) {
        float drawX = body.getPosition().x * GameScreen.PPM - visualWidth / 2f;
        float drawY = body.getPosition().y * GameScreen.PPM - visualHeight / 2f;
        batch.draw(textureRegion, drawX, drawY, visualWidth, visualHeight);
    }

    public void dispose() {
    }
}
