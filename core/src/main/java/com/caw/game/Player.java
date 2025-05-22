package com.caw.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;

public class Player {
    final GameScreen gameScreen;
    public enum State {IDLE, RUNNING, JUMPING, FALLING, DEAD}
    private State currentState;
    private State previousState;
    private float stateTime;
    private boolean facingRight = true;

    public Body body;
    final World world;

    private Animation<TextureRegion> idleAnimation;
    private Animation<TextureRegion> runAnimation;
    private TextureRegion jumpFrame;
    private TextureRegion fallFrame;

    public static final int SPRITESHEET_FRAME_WIDTH = 32;
    public static final int SPRITESHEET_FRAME_HEIGHT = 24;

    public static final int VISUAL_PLAYER_WIDTH = 16;
    public static final int VISUAL_PLAYER_HEIGHT = 16;

    public static final int FRAME_WIDTH = 18;
    public static final int FRAME_HEIGHT = 16;
    public static final float PPM = GameScreen.PPM;

    final Vector2 lastSafePositionMeters;
    public static final float FALL_DAMAGE = 25f;
    public static final float MIN_Y_DEATH_LEVEL_PIXELS = -100f;
    private float timeSinceLastSafePositionUpdate = 0f;
    private static final float SAFE_POSITION_UPDATE_INTERVAL = 0.25f;

    final float maxHealth = 100f;
    private float currentHealth;
    private boolean isDead = false;

    final Vector2 spawnPointMeters;
    final WorldContactListener contactListener;

    // KNOCKBACK FORCE
    public static final float KNOCKBACK_HORIZONTAL_BASE_FORCE = 0.2f;
    public static final float KNOCKBACK_VERTICAL_BASE_FORCE = 0.2f;
    private float invulnerabilityTimer = 0f;
    public static final float INVULNERABILITY_DURATION = 0.85f;

    // sounds
    private float walkSoundTimer = 0f;
    private static final float WALK_SOUND_INTERVAL = 0.25f;

    public Player(World world,
                  WorldContactListener contactListener,
                  Vector2 spawnPointPixels,
                  Texture idleSheet,
                  Texture runSheet,
                  GameScreen gameScreenRef) {

        this.gameScreen = gameScreenRef;
        this.world = world;
        this.contactListener = contactListener;
        this.spawnPointMeters = new Vector2(spawnPointPixels.x / PPM, spawnPointPixels.y / PPM);
        this.currentHealth = maxHealth;
        this.lastSafePositionMeters = new Vector2(spawnPointMeters);

        loadAnimations(idleSheet, runSheet);
        createBody();

        currentState = State.IDLE;
        previousState = State.IDLE;
        stateTime = 0f;
    }

    private void loadAnimations(Texture idleSheet, Texture runSheet) {

        //idle anim
        int idleFrameCount = 8;
        TextureRegion[][] tmpIdle = TextureRegion.split(idleSheet, SPRITESHEET_FRAME_WIDTH, SPRITESHEET_FRAME_HEIGHT);
        Array<TextureRegion> idleFrames = new Array<>(idleFrameCount);
        for (int i = 0; i < idleFrameCount; i++) {
            idleFrames.add(tmpIdle[0][i]);
        }
        idleAnimation = new Animation<>(0.15f, idleFrames, Animation.PlayMode.LOOP);

        //run anim
        int runFrameCount = 6;
        TextureRegion[][] tmpRun = TextureRegion.split(runSheet, SPRITESHEET_FRAME_WIDTH, SPRITESHEET_FRAME_HEIGHT);
        Array<TextureRegion> runFrames = new Array<>(runFrameCount);
        for (int i = 0; i < runFrameCount; i++) {
            runFrames.add(tmpRun[0][i]);
        }
        runAnimation = new Animation<>(0.1f, runFrames, Animation.PlayMode.LOOP);

        jumpFrame = runFrames.get(1);
        fallFrame = runFrames.get(0);

    }

    private void createBody() {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(spawnPointMeters);
        bodyDef.fixedRotation = true;
        bodyDef.linearDamping = 1.5f;

        body = world.createBody(bodyDef);

        PolygonShape shape = new PolygonShape();
        float playerHitboxHalfWidth = (VISUAL_PLAYER_WIDTH - 2f) / 2f / PPM;
        shape.setAsBox(playerHitboxHalfWidth, VISUAL_PLAYER_HEIGHT / 2f / PPM);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 0.5f;
        fixtureDef.friction = 0.2f;
        fixtureDef.restitution = 0.0f;
        body.createFixture(fixtureDef).setUserData("player");

        shape.setAsBox(6.7f/PPM, 2.5f/PPM, new Vector2(0, -9/PPM), 0);
        fixtureDef.shape = shape;
        fixtureDef.isSensor = true;
        body.createFixture(fixtureDef).setUserData("playerFeet");

        shape.dispose();
        body.setUserData(this);
    }

    public void handleInput() {
        if (isDead || body == null) return;
        if (invulnerabilityTimer > 0 && (Math.abs(body.getLinearVelocity().x) > 0.5f || Math.abs(body.getLinearVelocity().y) > 0.5f) ) {
             return;
        }

        float maxVel = 2f;
        float targetVelX = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            targetVelX = -maxVel;
            facingRight = false;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            targetVelX = maxVel;
            facingRight = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A) && Gdx.input.isKeyPressed(Input.Keys.D)) {
            targetVelX = 0;
        }

        body.setLinearVelocity(targetVelX, body.getLinearVelocity().y);

        if ((Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && contactListener.isPlayerOnGround())
            || (Gdx.input.isKeyJustPressed(Input.Keys.W) && contactListener.isPlayerOnGround())) {
            body.applyLinearImpulse(0f, body.getMass() * 6f, body.getWorldCenter().x, body.getWorldCenter().y, true);
            if (gameScreen != null) gameScreen.playPlayerJumpSound();
        }
    }

    public void update(float dt) {
        if (body == null) return;

        //last safe position
        timeSinceLastSafePositionUpdate += dt;
        if (contactListener.isPlayerOnGround() && timeSinceLastSafePositionUpdate >= SAFE_POSITION_UPDATE_INTERVAL) {
            if (body.getLinearVelocity().y > -0.1f && body.getLinearVelocity().y < 0.1f) {
                lastSafePositionMeters.set(body.getPosition());
                timeSinceLastSafePositionUpdate = 0f;

            }
        }

        float minYDeathLevelMeters = MIN_Y_DEATH_LEVEL_PIXELS / PPM;
        if (body.getPosition().y < minYDeathLevelMeters && !isDead) {
            Gdx.app.log("PlayerFall", "Player fell into a pit!");
            handleFallIntoPit();
        }

        if (invulnerabilityTimer > 0) {
            invulnerabilityTimer -= dt;
        }

        if (isDead && currentState != State.DEAD) {
            currentState = State.DEAD;
            if (body.isActive()) body.setLinearVelocity(0, 0); // CHECK isActive
            stateTime = 0;
        } else if (!isDead) {
            previousState = currentState;
            currentState = determineCurrentState();
        }

        if (currentState == previousState) {
            stateTime += dt;
        } else if (currentState == State.DEAD) {
            stateTime += dt;
        }

        if (currentState == State.RUNNING && contactListener.isPlayerOnGround()) {
            walkSoundTimer += dt;
            if (walkSoundTimer >= WALK_SOUND_INTERVAL) {
                if (gameScreen != null) {
                    gameScreen.playPlayerWalkSound();
                }
                walkSoundTimer = 0f;
            }
        } else {
            walkSoundTimer = 0f;
        }

    }

    public boolean isVisibleDuringInvulnerability() {
        if (invulnerabilityTimer > 0){
            return (int)(invulnerabilityTimer * 10) % 2 == 0;
        }
        return true;
    }

    private void handleFallIntoPit() {
        if (isDead) return;

        Gdx.app.log("PLAYER_HEALTH", "Player fell. Taking fall damage.");
        takeDamage(FALL_DAMAGE, null);

        if (!isDead) {
            resetPositionToLastSafe();
        }
    }

    public void resetPositionToLastSafe() {
        if (body != null && !isDead) {
            body.setTransform(lastSafePositionMeters, 0);
            body.setLinearVelocity(0, 0);
            body.setAwake(true);
            Gdx.app.log("PlayerPosition", "Player position reset to last safe: " + lastSafePositionMeters);
            invulnerabilityTimer = 0.5f;
            currentState = State.IDLE;
            stateTime = 0;
        }
    }

    private State determineCurrentState() {
        if (isDead) return State.DEAD;
        if (body == null || !body.isActive() || contactListener == null) return State.IDLE;

        if (!contactListener.isPlayerOnGround()) {
            if (body.getLinearVelocity().y > 0.1f) return State.JUMPING;
            if (body.getLinearVelocity().y < -0.1f) return State.FALLING;
            return State.FALLING;
        } else {
            if (Math.abs(body.getLinearVelocity().x) > 0.1f) return State.RUNNING;
            return State.IDLE;
        }
    }

    public void render(SpriteBatch batch) {
        if (body == null || !body.isActive()) return;
        if (!isVisibleDuringInvulnerability()) return;

        TextureRegion currentFrame = getFrameToRender();
        if (currentFrame == null) return;

        flipCurrentFrame(currentFrame);

        float playerDrawX = body.getPosition().x * PPM - FRAME_WIDTH / 2f;
        float playerDrawY = body.getPosition().y * PPM - FRAME_HEIGHT / 2f;

        batch.draw(currentFrame, playerDrawX, playerDrawY, VISUAL_PLAYER_WIDTH, VISUAL_PLAYER_HEIGHT);
    }

    private TextureRegion getFrameToRender() {
        switch (currentState) {
            case JUMPING: return jumpFrame;
            case FALLING: return fallFrame;
            case RUNNING: return runAnimation.getKeyFrame(stateTime, true);
//            case DEAD:    return fallFrame;
            case IDLE:
            default:      return idleAnimation.getKeyFrame(stateTime, true);
        }
    }

    private void flipCurrentFrame(TextureRegion frame) {
        if (!facingRight && !frame.isFlipX()) {
            frame.flip(true, false);
        } else if (facingRight && frame.isFlipX()) {
            frame.flip(true, false);
        }
    }

    //when take damage
    public void takeDamage(float amount, Body damageSourceBody) {
        if (isDead || invulnerabilityTimer > 0) return;

        currentHealth -= amount;

        invulnerabilityTimer = INVULNERABILITY_DURATION;
        Gdx.app.log("PLAYER_HEALTH", "Player took " + amount + " damage. Current health: " + currentHealth);

        if (body != null && body.isActive() && damageSourceBody != null && damageSourceBody.isActive()) {
            Vector2 playerPosition = body.getPosition();
            Vector2 sourcePosition = damageSourceBody.getPosition();
            Vector2 worldCenter = body.getWorldCenter();

            float pushDirectionX = Math.signum(playerPosition.x - sourcePosition.x);
            if (pushDirectionX == 0) {
                if (damageSourceBody.getLinearVelocity().x != 0) {
                    pushDirectionX = -Math.signum(damageSourceBody.getLinearVelocity().x);
                } else {
                    pushDirectionX = facingRight ? -1f : 1f;
                }
                if (pushDirectionX == 0) pushDirectionX = (Math.random() > 0.5 ? 1f : -1f); // direction
            }

            float impulseX = pushDirectionX * KNOCKBACK_HORIZONTAL_BASE_FORCE;
            float impulseY = KNOCKBACK_VERTICAL_BASE_FORCE;

            body.setLinearVelocity(0f, 0f);
            body.applyLinearImpulse(impulseX, impulseY, worldCenter.x, worldCenter.y, true);

            Gdx.app.log("PLAYER_KNOCKBACK", "Applied impulse: X=" + impulseX + ", Y=" + impulseY + " from source at " + sourcePosition);
        }

        //hurt sound
        if (gameScreen != null) gameScreen.playPlayerHurtSound();

        if (currentHealth <= 0) {
            currentHealth = 0;
            die();
        }
    }

    private void die() {
        if (isDead) return;
        isDead = true;
        Gdx.app.log("PLAYER_STATE", "Player has died.");
        // game over - dead sound
        if (gameScreen != null) gameScreen.playPlayerDeathSound();
    }

    public void respawn() {
        isDead = false;
        currentHealth = maxHealth;
        invulnerabilityTimer = 0f; // reset
        if (body != null) {
            body.setTransform(spawnPointMeters, 0);
            body.setLinearVelocity(0, 0);
            body.setAwake(true);
        }
        currentState = State.IDLE;
        previousState = State.IDLE;
        stateTime = 0f;
        facingRight = true;
        Gdx.app.log("PLAYER_STATE", "Player respawned.");
    }

    public void resetPositionToSpawn() {
        if (body != null && !isDead) {
            body.setTransform(spawnPointMeters, 0);
            body.setLinearVelocity(0, 0);
            body.setAwake(true);
            Gdx.app.log("PLAYER_POSITION", "Player position reset to spawn.");
        }
    }

    public Vector2 getPositionMeters() {
        return body != null && body.isActive() ? body.getPosition() : new Vector2(spawnPointMeters);
    }

    public boolean isDead() {
        return isDead;
    }

    public float getCurrentHealth() {
        return currentHealth;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public Body getBody() {
        return body;
    }

    public float getInvulnerabilityTimer() {
        return invulnerabilityTimer;
    }

    public void dispose() {
        if (body != null && world != null && !world.isLocked()) {
            world.destroyBody(body);
            body = null;
        }
    }
}
