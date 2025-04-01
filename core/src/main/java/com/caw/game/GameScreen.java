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
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.caw.game.GameStart;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

public class GameScreen implements Screen {
    final GameStart game;
    private Body playerBody;
    private World world;
    private Box2DDebugRenderer debugRenderer;
    private WorldContactListener contactListener;
    OrthographicCamera camera;

    //map
    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;

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

        Gdx.gl.glClearColor(0.5f, 0.6f, 1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);


        //camera
        Vector2 bodyPos = playerBody.getPosition();
        //plavna camera
        float lerp = 0.1f;
        camera.position.x += (bodyPos.x * PPM - camera.position.x) * lerp;
        camera.position.y += (bodyPos.y * PPM - camera.position.y) * lerp;

        limitCameraView();
        camera.update();

        //get texture & flip frame
        TextureRegion currentFrame = getFrame(delta);
        flipFrame(currentFrame);

        //map render
        mapRenderer.setView(camera);
        mapRenderer.render();
        game.batch.setProjectionMatrix(camera.combined);

        //player render
        game.batch.begin();
        float playerDrawX = bodyPos.x * PPM - FRAME_WIDTH/2f;
        float playerDrawY = bodyPos.y * PPM - FRAME_HEIGHT/2f;
        if(currentFrame != null){
            game.batch.draw(currentFrame, playerDrawX, playerDrawY, FRAME_WIDTH, FRAME_HEIGHT);
        }
        game.batch.end();

        //debug(hitbox) Box2D
        debugRenderer.render(world, camera.combined.cpy().scl(PPM));
    }

    //anim player
    private TextureRegion getFrame(float dt){
//        currentState = getState();
//        stateTime = (currentState == previousState) ? stateTime + dt : 0;
//        previousState = currentState;

        TextureRegion frame = null;
        switch (currentState){
            case JUMPING:frame = jumpFrame; break;
            case FALLING:frame = fallFrame; break;
            case RUNNING:frame = runAnimation.getKeyFrame(stateTime,true); break;
            case IDLE: default:frame = idleAnimation.getKeyFrame(stateTime, true); break;
        }
        return frame;
    }

    //rozvorot player-a
    private void flipFrame(TextureRegion frame){
        if (frame == null) return;
        if (!facingRight &&  !frame.isFlipX()){
            frame.flip(true, false);
        }else if (facingRight && frame.isFlipX()) {
            frame.flip(true, false);
        }
    }

    private void limitCameraView(){
        int mapTileWidth = map.getProperties().get("width", 0, Integer.class);
        int mapTileHeight = map.getProperties().get("height", 0, Integer.class);
        int tilePixelWidth = map.getProperties().get("tilewidth", 0, Integer.class);
        int tilePixelHeight = map.getProperties().get("tileheight", 0, Integer.class);

        if (mapTileWidth == 0 || mapTileHeight == 0 || tilePixelWidth == 0 || tilePixelHeight == 0) {
            return;
        }

        //map size
        float mapWidthPixels = mapTileWidth * tilePixelWidth;
        float mapHeightPixels = mapTileHeight * tilePixelHeight;

        //window size
        float cameraViewportWidth = camera.viewportWidth;
        float cameraViewportHeight = camera.viewportHeight;

//        Gdx.app.log("LIMIT_CAM", "Map dimensions (pixels): " + mapWidthPixels + " x " + mapHeightPixels);
//        Gdx.app.log("LIMIT_CAM", "Viewport dimensions: " + camera.viewportWidth + " x " + camera.viewportHeight);

//        float mapWidthPixels = map.getProperties().get("width", Integer.class) * map.getProperties().get("tilewidth", Integer.class);
//        float mapHeightPixels = map.getProperties().get("height", Integer.class) * map.getProperties().get("tileheight", Integer.class);

        float cameraX = camera.position.x;
        float cameraY = camera.position.y;

        // X
        if (mapWidthPixels >= cameraViewportWidth) {
            // if map wider or same
            cameraX = Math.max(cameraViewportWidth / 2f, cameraX); // Обмеження зліва
            cameraX = Math.min(mapWidthPixels - cameraViewportWidth / 2f, cameraX); // Обмеження справа
        } else {
            // if map smaller
            cameraX = mapWidthPixels / 2f;
        }

        // Y
        if (mapHeightPixels >= cameraViewportHeight) {
            // if map higher of same height
            cameraY = Math.max(cameraViewportHeight / 2f, cameraY); // Обмеження знизу
            cameraY = Math.min(mapHeightPixels - cameraViewportHeight / 2f, cameraY); // Обмеження зверху
        } else {
            // if max shoter than screen
            cameraY = mapHeightPixels / 2f;
        }

        //camera set
        camera.position.set(cameraX, cameraY, 0);

//        //limit for X
//        camera.position.x = Math.max(cameraViewportWidth / 2f, camera.position.x);
//        camera.position.x = Math.min(mapWidthPixels - cameraViewportWidth / 2f, camera.position.x);
//
//        //limit for Y
//        camera.position.y = Math.max(cameraViewportHeight / 2f, camera.position.y);
//        camera.position.y = Math.min(mapHeightPixels - cameraViewportHeight / 2f, camera.position.y);
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

//        //start pos in meters
//        float startX = 800/2f;
//        float startY = 100f;
//        bodyDef.position.set(startX/PPM, startY/PPM);
//        bodyDef.fixedRotation = true;
//        bodyDef.linearDamping = 2.0f;

        //if spawn point is not found
        float startX = camera.viewportWidth / 2f;
        float startY = camera.viewportHeight / 2f;

        if (map.getLayers().get("SpawnPoints") != null){
            for (MapObject object : map.getLayers().get("SpawnPoints").getObjects()){
                //obj with "playerStart" == true
                if (object.getProperties().containsKey("playerStart") &&
                    object.getProperties().get("playerStart", Boolean.class)){
                    //if start is rect
                    if (object instanceof RectangleMapObject){
                        Rectangle rect = ((RectangleMapObject) object).getRectangle();
                        startX = rect.getX() + rect.getWidth() / 2f;
                        startY = rect.getY() + rect.getHeight() / 2f;
                    }else { //if start is point
                        startX = object.getProperties().get("x", Float.class);
                        startY = object.getProperties().get("y", Float.class);
                    }
                    Gdx.app.log("PLAYER_SPAWN", "Player start pos found at: " + startX + ", " + startY);
                    break;
                }
            }
        } else {
            Gdx.app.log("PLAYER_SPAWN", "SpawnPoint layer not found, using default start pos");
        }

        bodyDef.position.set(startX / PPM, startY / PPM);

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

        playerBody.createFixture(fixtureDef).setUserData("player");

        //сенсор доторкання землі через окремий хітбокс ніг
        shape.setAsBox(playerWidthPixels / 2.5f / PPM, 5 / PPM,
            new Vector2(0, -playerHeightPixels / 2f / PPM - 3 / PPM),0);
        fixtureDef.shape = shape;
        fixtureDef.isSensor = true;
        playerBody.createFixture(fixtureDef).setUserData("playerFeet");

        shape.dispose();
    }

//    private void createGround(){
//        BodyDef groundBodyDef = new BodyDef();
//        groundBodyDef.position.set(camera.viewportWidth/2/PPM, 10/PPM);
//        groundBodyDef.type = BodyDef.BodyType.StaticBody;
//
//        Body groundBody = world.createBody(groundBodyDef);
//
//        PolygonShape groundBox = new PolygonShape();
//        groundBox.setAsBox(camera.viewportWidth/2/PPM, 10/2/PPM);
//
//        FixtureDef groundFixtureDef = new FixtureDef();
//        groundFixtureDef.shape = groundBox;
//        groundFixtureDef.friction = 0.5f;
//
//        groundBody.createFixture(groundFixtureDef);
//        groundBody.setUserData("ground");
//
//        groundBox.dispose();
//    }

    private void createPhysicsFromMap(){
        if (map.getLayers().get("Collision")==null){
            Gdx.app.error("MAP_LOADER","Object layer 'Collision' not found in Tiled map!");
            return;
        }

        BodyDef bodyDef = new BodyDef();
        FixtureDef fixtureDef = new FixtureDef();
        PolygonShape shape = new PolygonShape();

        for (MapObject object : map.getLayers().get("Collision").getObjects()){
            if (object instanceof RectangleMapObject){
                Rectangle rect = ((RectangleMapObject) object).getRectangle();
                bodyDef.type = BodyDef.BodyType.StaticBody;

                float centerX = (rect.getX() + rect.getWidth() / 2f)/PPM;
                float centerY = (rect.getY() + rect.getHeight() / 2f)/PPM;
                bodyDef.position.set(centerX,centerY);

                Body body = world.createBody(bodyDef);

                shape.setAsBox(rect.getWidth() /2f / PPM, rect.getHeight() / 2f / PPM);
                fixtureDef.shape = shape;
                fixtureDef.friction = 0.8f;

                body.createFixture(fixtureDef).setUserData("ground");
            }
            else {
                Gdx.app.log("MAP_LOADER","Skipping non-rectangle objects in Collision layer; " + object.getName());
            }
        }
        shape.dispose();

    }

    @Override
    public void show() {
        world = new World(new Vector2(0, -10f), true);
        debugRenderer = new Box2DDebugRenderer();
        contactListener = new WorldContactListener();
        world.setContactListener(contactListener);

        //MAP
        map = new TmxMapLoader().load("assets/test_map.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map, 1f);

        //animation for character
        Texture idleSheet = new Texture(Gdx.files.internal("assets/player_idle.png"));
        Texture runSheet = new Texture(Gdx.files.internal("assets/player_run.png"));
        //jump - fall
        Texture jumpSheet = new Texture(Gdx.files.internal("assets/player_jump.png"));
        Texture fallSheet = new Texture(Gdx.files.internal("assets/player_fall.png"));

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
        Array<TextureRegion> runFrames = new Array<>(runFrameCount);
        for (int i = 0; i < runFrameCount; i++){
            runFrames.add(tmpRun[0][i]);
        }
        runAnimation = new Animation<>(0.1f, runFrames, Animation.PlayMode.LOOP);

        //JUMP-FALL ANIM
        jumpFrame = new TextureRegion(jumpSheet, 0,0, FRAME_WIDTH, FRAME_HEIGHT);
        fallFrame = new TextureRegion(fallSheet, 0,0, FRAME_WIDTH, FRAME_HEIGHT);

        stateTime = 0f;

        currentState = State.IDLE;
        createPlayerBody();
        createPhysicsFromMap();
//        createGround();

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
        map.dispose();
        mapRenderer.dispose();

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
