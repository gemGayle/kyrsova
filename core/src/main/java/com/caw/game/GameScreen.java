package com.caw.game;

import java.security.Key;
import java.util.Iterator;
import java.util.Vector;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.caw.game.GameStart;

public class GameScreen implements Screen {
    final GameStart game;
    private Texture playerSprite;
    private Body playerBody;
    private World world;
    private Box2DDebugRenderer debugRenderer;
    private WorldContactListener contactListener;
    OrthographicCamera camera;




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
        Gdx.gl.glClearColor(0.5f, 0.6f, 1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        //input
        handleInput(delta);

        //simulation Box2d
        world.step(1/60f,6,2);

        //player sprite updatin
        Vector2 bodyPos = playerBody.getPosition();
        float playerSpriteX = bodyPos.x * PPM - playerSprite.getWidth()/2;
        float playerSpriteY = bodyPos.y * PPM - playerSprite.getWidth()/2;

        //updatin camera
        camera.update();

        //plavno z linear interpolation (lerp)
        float lerp = 0.1f;
        camera.position.x += (bodyPos.x * PPM - camera.position.x) * lerp;
        camera.position.y += (bodyPos.y * PPM - camera.position.y) * lerp;


        //coordinate system
        game.batch.setProjectionMatrix(camera.combined);

        game.batch.begin();
        game.batch.draw(playerSprite, playerSpriteX, playerSpriteY,
            playerSprite.getWidth(), playerSprite.getHeight());
        game.batch.end();

        //debug Box2D
        debugRenderer.render(world, camera.combined.cpy().scl(PPM));

    }

    private void handleInput(float dt){
        float desiredVel = 0;
        float moveForce = 10f;
        float maxVel = 1.7f;

        if (Gdx.input.isKeyPressed(Keys.A) && playerBody.getLinearVelocity().x > -maxVel) {
            playerBody.applyLinearImpulse(-moveForce * dt, 0, playerBody.getPosition().x, playerBody.getPosition().y, true);
        }
        else if (Gdx.input.isKeyPressed(Keys.D) && playerBody.getLinearVelocity().x < maxVel){
            playerBody.applyLinearImpulse(moveForce * dt, 0, playerBody.getPosition().x, playerBody.getPosition().y, true);
        } else {

        }

        if(Gdx.input.isKeyJustPressed(Keys.SPACE) && contactListener.isPlayerOnGround()
            || Gdx.input.isKeyJustPressed(Keys.W) && contactListener.isPlayerOnGround()){
            float jumpForce = 0.3f;
            playerBody.applyLinearImpulse(0f, jumpForce, playerBody.getWorldCenter().x, playerBody.getWorldCenter().y, true);
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


        playerSprite = new Texture(Gdx.files.internal("C:\\Users\\User\\Desktop\\study\\chornovik\\assets\\PlayerIdle.png"));

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

        playerSprite.dispose();
    }

}
