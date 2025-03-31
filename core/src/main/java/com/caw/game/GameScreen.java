package com.caw.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.caw.game.GameStart;
import com.badlogic.gdx.utils.Array;

public class GameScreen implements Screen {
    final GameStart game;
    private Body playerBody;
    private World world;
    private Box2DDebugRenderer debugRenderer;
    private WorldContactListener contactListener;
    OrthographicCamera camera;

    //animations
    private Animation<TextureRegion> idleAnimation;
    private Animation<TextureRegion> runAnimation;
    private TextureRegion jumpFrame;
    private TextureRegion fallFrame;
    private enum State {IDLE, RUNNING, JUMPING, FALLING}
    private State currentState;
    private State previousState;
    private  float stateTime;

    //is right
    private boolean facingRight = true;

    private static final int FRAME_WIDTH = 32;
    private static final int FRAME_HEIGHT = 32;

    //convert pixels -> meters
    public static final float PPM = 100;

    public GameScreen(final GameStart gam) {
        this.game = gam;

        //camera
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 480);


    }


    @Override
    public void render(float delta) {
        //input
        update(delta);
        handleInput(delta);
        world.step(1/60f,6,2); //physics

        //camera
        camera.update();
        Vector2 bodyPos = playerBody.getPosition();
        float lerp = 0.1f;
        camera.position.x += (bodyPos.x * PPM - camera.position.x) * lerp;
        camera.position.y += (bodyPos.y * PPM - camera.position.y) * lerp;


        //get texture
        TextureRegion currentFrame = null;
        switch (currentState){
            case JUMPING:
                currentFrame = jumpFrame;
                break;
            case FALLING:
                currentFrame = fallFrame;
                break;
            case RUNNING:
                currentFrame = runAnimation.getKeyFrame(stateTime, true);
                break;
            case IDLE:
                currentFrame = idleAnimation.getKeyFrame(stateTime, true);
                break;
        }


        //rozvorot kadry
        if (!facingRight && !currentFrame.isFlipX()){
            currentFrame.flip(true, false);
        } else if (facingRight && currentFrame.isFlipX()) {
            currentFrame.flip(true, false);
        }

        //draw
        Gdx.gl.glClearColor(0.5f, 0.6f, 1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        float playerDrawX = bodyPos.x * PPM - FRAME_WIDTH/2f;
        float playerDrawY = bodyPos.y * PPM - FRAME_HEIGHT/2f;

        game.batch.draw(currentFrame, playerDrawX, playerDrawY, FRAME_WIDTH, FRAME_HEIGHT);

        game.batch.end();

        //debug Box2D
        debugRenderer.render(world, camera.combined.cpy().scl(PPM));

    }

    private void handleInput(float dt){
        float maxVel = 3.0f; //max speed
        float targetVelX = 0; //speed


        if (Gdx.input.isKeyPressed(Keys.A)){
            targetVelX = -maxVel;
            facingRight = false;
        }

        if (Gdx.input.isKeyPressed(Keys.D)){
            targetVelX = maxVel;
            facingRight = true;
        }

        if(Gdx.input.isKeyPressed(Keys.A) && Gdx.input.isKeyPressed(Keys.D)){
            targetVelX = 0; //no move when A and D pressed
        }

        playerBody.setLinearVelocity(targetVelX, playerBody.getLinearVelocity().y);

        if(Gdx.input.isKeyJustPressed(Keys.SPACE) && contactListener.isPlayerOnGround()
            || Gdx.input.isKeyJustPressed(Keys.W) && contactListener.isPlayerOnGround()){
            float jumpForce = 0.4f;
            playerBody.applyLinearImpulse(0f, jumpForce, playerBody.getWorldCenter().x, playerBody.getWorldCenter().y, true);
        }

    }

    public void update(float dt){
        currentState = getState();

        stateTime = (currentState == previousState) ? stateTime + dt : 0;
        previousState = currentState;

    }
    private State getState() {
        if (!contactListener.isPlayerOnGround()) {
            if (playerBody.getLinearVelocity().y > 0.1f) {
                return State.JUMPING;
            } else if (playerBody.getLinearVelocity().y < 0.1f) {
                return State.FALLING;
            } else {
                if (Math.abs(playerBody.getLinearVelocity().x) > 0.1f) {
                    return State.RUNNING;
                } else {
                    return State.FALLING;
                }
            }

        } else {
            if (Math.abs(playerBody.getLinearVelocity().x) > 0.1f) {
                return State.RUNNING;
            } else {
                return State.IDLE;
            }
        }
    }


        private void createPlayerBody(){
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;

        //start pos in meters
        float startX = 800/2f;
        float startY = 100f;
        bodyDef.position.set(startX/PPM, startY/PPM);
        bodyDef.fixedRotation = true;
        bodyDef.linearDamping = 2.0f;

        playerBody = world.createBody(bodyDef);

        PolygonShape shape = new PolygonShape();
        //body size in meters
        float playerWidthPixels = 32;
        float playerHeightPixels = 32;
        shape.setAsBox(playerWidthPixels/2/PPM, playerHeightPixels/2/PPM);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 0.5f; //тіпа маса
        fixtureDef.friction = 0.5f; //тертя
        fixtureDef.restitution = 0.0f; //пружність

        playerBody.createFixture(fixtureDef);
        playerBody.setUserData("player"); //metka

        shape.dispose();
    }

    private void createGround(){
        BodyDef groundBodyDef = new BodyDef();
        groundBodyDef.position.set(camera.viewportWidth/2/PPM, 10/PPM);
        groundBodyDef.type = BodyDef.BodyType.StaticBody;

        Body groundBody = world.createBody(groundBodyDef);

        PolygonShape groundBox = new PolygonShape();
        groundBox.setAsBox(camera.viewportWidth/2/PPM, 10/2/PPM);

        FixtureDef groundFixtureDef = new FixtureDef();
        groundFixtureDef.shape = groundBox;
        groundFixtureDef.friction = 0.5f;

        groundBody.createFixture(groundFixtureDef);
        groundBody.setUserData("ground");

        groundBox.dispose();
    }

    @Override
    public void show() {
        world = new World(new Vector2(0, -10f), true);
        debugRenderer = new Box2DDebugRenderer();

        contactListener = new WorldContactListener();
        world.setContactListener(contactListener);

        //animation for character
        Texture idleSheet = new Texture(Gdx.files.internal("assets\\player_idle.png"));
        Texture runSheet = new Texture(Gdx.files.internal("assets\\player_run.png"));
        //jump - fall
        Texture jumpSheet = new Texture(Gdx.files.internal("assets\\player_jump.png"));
        Texture fallSheet = new Texture(Gdx.files.internal("assets\\player_fall.png"));

        //IDLE ANIM
        int idleFrameCount = 4;
        TextureRegion[][] tmpIdle = TextureRegion.split(idleSheet, FRAME_WIDTH, FRAME_HEIGHT);
        Array<TextureRegion> idleFrames = new Array<>(idleFrameCount);
        for(int i = 0; i < idleFrameCount; i++){
            idleFrames.add(tmpIdle[0][i]);
        }
        idleAnimation = new Animation<>(0.15f, idleFrames, Animation.PlayMode.LOOP);

        //RUN ANIM
        int runFrameCount = 8;
        TextureRegion[][] tmpRun = TextureRegion.split(runSheet, FRAME_WIDTH, FRAME_HEIGHT);
        Array<TextureRegion> rumFrames = new Array<>(runFrameCount);
        for (int i = 0; i < runFrameCount; i++){
            rumFrames.add(tmpRun[0][i]);
        }
        runAnimation = new Animation<>(0.1f, rumFrames, Animation.PlayMode.LOOP);

        //JUMP-FALL ANIM
        jumpFrame = new TextureRegion(jumpSheet, 0,0, FRAME_WIDTH, FRAME_HEIGHT);
        fallFrame = new TextureRegion(jumpSheet, 0,0, FRAME_WIDTH, FRAME_HEIGHT);

        stateTime = 0f;

        currentState = State.IDLE;
        createPlayerBody();
        createGround();
    }

    @Override
    public void resize(int width, int height) {
    }



    @Override
    public void hide() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        world.dispose();
        debugRenderer.dispose();

        if (idleAnimation != null && idleAnimation.getKeyFrames().length > 0){
            Texture texture = ((TextureRegion)idleAnimation.getKeyFrames()[0]).getTexture();
            if (texture != null){
                texture.dispose();
            }
        }
        if (runAnimation != null && runAnimation.getKeyFrames().length > 0){
            ((TextureRegion)runAnimation.getKeyFrames()[0]).getTexture().dispose();
        }
        if (jumpFrame != null) jumpFrame.getTexture().dispose();
        if (fallFrame != null) fallFrame.getTexture().dispose();
    }

}
