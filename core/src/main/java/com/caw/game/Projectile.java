package com.caw.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

public class Projectile {
    public Body body;
    private Texture texture;
    private float width, height;
    private boolean scheduledForRemoval = false;
    private float lifeTime = 1f;

    public static final float PROJECTILE_SPEED = 2f;

    public Projectile(World world, Texture texture, float x, float y, float widthPixels, float heightPixels, Vector2 direction) {
        this.texture = texture;
        this.width = widthPixels / GameScreen.PPM;
        this.height = heightPixels / GameScreen.PPM;

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);
        bodyDef.bullet = true;

        body = world.createBody(bodyDef);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(this.width / 2f, this.height / 2f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 0.1f;
        fixtureDef.friction = 0.5f;
        fixtureDef.restitution = 0.1f;

        body.createFixture(fixtureDef).setUserData(this);
        shape.dispose();

        // start speed
        Vector2 velocity = direction.nor().scl(PROJECTILE_SPEED); // set direction and * to speed
        body.setLinearVelocity(velocity);
        body.setGravityScale(0);
    }

    public void update(float dt) {
        lifeTime -= dt;
        if (lifeTime <= 0) {
            scheduleForRemoval();
        }
    }

    public void render(SpriteBatch batch) {
        if (body == null || !body.isActive() || scheduledForRemoval) return;

        batch.draw(texture,
            body.getPosition().x * GameScreen.PPM - (width * GameScreen.PPM) / 2f,
            body.getPosition().y * GameScreen.PPM - (height * GameScreen.PPM) / 2f,
            width * GameScreen.PPM,
            height * GameScreen.PPM);
    }

    public void scheduleForRemoval() {
        this.scheduledForRemoval = true;
    }

    public boolean isScheduledForRemoval() {
        return scheduledForRemoval;
    }

    public Body getBody() {
        return body;
    }
}
