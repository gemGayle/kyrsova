package com.caw.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;

public class ShootingEnemy {
    public Body body;
    // private TextureRegion textureRegion;
    private Animation<TextureRegion> activeAnimation;
    private float stateTime;
    private boolean facingRight = true;

    private World world;
    private GameScreen gameScreen;

    private float visualWidth, visualHeight;

    private float shootCooldownTimer = 0f;
    public static final float SHOOT_COOLDOWN = 3.0f;
    public static final float DETECTION_RADIUS = 5f;

    private float instanceShootCooldown;
    private float instanceDetectionRadiusMeters;
    private Texture projectileTexture;

    public static final int SPRITESHEET_FRAME_WIDTH = 16;
    public static final int SPRITESHEET_FRAME_HEIGHT = 16;

    public float PROJECTILE_WIDTH_PIXELS = 6f;
    public float PROJECTILE_HEIGHT_PIXELS = 6f;


    public ShootingEnemy(World world, GameScreen gameScreen,
                         Texture shootingEnemySheetTexture,
                         Texture projectileTexture,
                         float x_pixels,
                         float y_pixels,
                         float visualWidth_pixels,
                         float visualHeight_pixels,
                         float detectionRadius_pixels,
                         float shootCooldown_seconds) {

        this.world = world;
        this.gameScreen = gameScreen;
        this.projectileTexture = projectileTexture;
        this.visualWidth = visualWidth_pixels;
        this.visualHeight = visualHeight_pixels;

        this.instanceDetectionRadiusMeters = detectionRadius_pixels / GameScreen.PPM;
        this.instanceShootCooldown = shootCooldown_seconds;
        this.shootCooldownTimer = this.instanceShootCooldown * (float) Math.random(); // a bit random

        loadAnimation(shootingEnemySheetTexture);

        if (activeAnimation != null) {
            this.stateTime = (float)(Math.random() * activeAnimation.getAnimationDuration());
        } else {
            this.stateTime = 0f; // if nema animations
            Gdx.app.error("ShootingEnemyConstruct", "activeAnimation is null after loadAnimation, cannot set random stateTime based on duration.");
        }

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(x_pixels / GameScreen.PPM, y_pixels / GameScreen.PPM);
        bodyDef.fixedRotation = true;

        body = world.createBody(bodyDef);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(visualWidth_pixels / 2f / GameScreen.PPM, visualHeight_pixels / 2f / GameScreen.PPM);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        body.createFixture(fixtureDef).setUserData(this);
        shape.dispose();
    }

    private void loadAnimation(Texture animationSheet) {
        if (animationSheet == null) {
            Gdx.app.error("ShootingEnemyAnim", "Animation sheet is null!");
            Texture dummyTexture = new Texture(Gdx.files.internal("assets/shooting_enemy.png")); // old texture (zaglushlka)
            activeAnimation = new Animation<>(1f, new TextureRegion(dummyTexture));
            return;
        }

        int frameCount = 4;
        TextureRegion[][] tmpFrames = TextureRegion.split(animationSheet, SPRITESHEET_FRAME_WIDTH, SPRITESHEET_FRAME_HEIGHT);
        Array<TextureRegion> frames = new Array<>(frameCount);
        for (int i = 0; i < frameCount; i++) {
            frames.add(tmpFrames[0][i]);
        }
        activeAnimation = new Animation<>(0.2f, frames, Animation.PlayMode.LOOP);
    }

    public Animation<TextureRegion> getActiveAnimation() {
        return activeAnimation;
    }

    public void update(float dt, Player player) {
        stateTime += dt; // update animate time

        // shootin logic
        shootCooldownTimer -= dt;
        if (shootCooldownTimer <= 0 && player != null && !player.isDead() && body != null && body.isActive()) {
            Vector2 enemyPos = body.getPosition();
            Vector2 playerPos = player.getPositionMeters();
            float distanceToPlayer = enemyPos.dst(playerPos);

            if (distanceToPlayer <= this.instanceDetectionRadiusMeters) {
                if (hasLineOfSight(player)) {
                    shoot(playerPos);
                    shootCooldownTimer = this.instanceShootCooldown;
                }
            }
        }

        // right left for canon
        if (player != null && !player.isDead() && body != null && body.isActive()) {
            Vector2 enemyPos = body.getPosition();
            if (enemyPos.dst(player.getPositionMeters()) <= instanceDetectionRadiusMeters * 1.2f) {
                boolean shouldFaceRight = player.getPositionMeters().x > enemyPos.x;
                facingRight = shouldFaceRight;
            }
        }
    }

    private void flipCurrentFrame(TextureRegion frame) {
        if (frame == null) return;
        if (!facingRight && !frame.isFlipX()) {
            frame.flip(true, false);
        } else if (facingRight && frame.isFlipX()) {
            frame.flip(true, false);
        }
    }

    public void draw(SpriteBatch batch) {
        if (body == null || !body.isActive() || activeAnimation == null) return;

        TextureRegion currentFrame = activeAnimation.getKeyFrame(stateTime, true);
        flipCurrentFrame(currentFrame);

        batch.draw(currentFrame,
            body.getPosition().x * GameScreen.PPM - visualWidth / 2f,
            body.getPosition().y * GameScreen.PPM - visualHeight / 2f,
            visualWidth,
            visualHeight);
    }

    private void shoot(Vector2 targetPosition) {
        Gdx.app.log("SHOOTING_ENEMY", "Shooting towards " + targetPosition);

        // set point of aim, with direction
        float firePointOffsetX = visualWidth / GameScreen.PPM / 2f + 0.1f; // from center
        if (!facingRight) { // if lookin left
            firePointOffsetX *= -1;
        }
        Vector2 firePoint = body.getPosition().cpy().add(firePointOffsetX, 0); // zsuv po X

        Vector2 direction = targetPosition.cpy().sub(firePoint).nor();

        if (gameScreen != null) gameScreen.playShootSound();

        Projectile projectile = new Projectile(world, projectileTexture,
            firePoint.x, firePoint.y,
            PROJECTILE_WIDTH_PIXELS, PROJECTILE_HEIGHT_PIXELS,
            direction);
        gameScreen.addProjectile(projectile);



    }

    private boolean hasLineOfSight(Player player) {
        if (world == null || body == null || player == null || player.getBody() == null) {
            return false;
        }
        Vector2 enemyPos = body.getPosition();
        Vector2 playerPos = player.getPositionMeters();
        final boolean[] hitPlayer = {false};
        final Fixture[] hitFixture = {null};
        RayCastCallback callback = new RayCastCallback() {
            @Override
            public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
                if (fixture.getBody() == ShootingEnemy.this.body) return 1;
                hitFixture[0] = fixture;
                if (fixture.getUserData() instanceof Player || (fixture.getUserData() instanceof String && "player".equals(fixture.getUserData())) ) {
                    hitPlayer[0] = true;
                    return 0;
                } else if (fixture.getUserData() instanceof String && "ground".equals(fixture.getUserData())) {
                    return 0;
                }
                return 1;
            }
        };
        world.rayCast(callback, enemyPos, playerPos);
        return hitPlayer[0] && hitFixture[0] != null && (hitFixture[0].getUserData() instanceof Player || "player".equals(hitFixture[0].getUserData()));
    }

    public Body getBody() {
        return body;
    }

    public void dispose() {
    }
}
