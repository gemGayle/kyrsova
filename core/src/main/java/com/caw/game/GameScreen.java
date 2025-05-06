package com.caw.game;

import com.badlogic.gdx.Gdx;
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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.maps.MapLayer;

public class GameScreen implements Screen {
    final GameStart game;
    private Body playerBody;
    private World world;
    private Box2DDebugRenderer debugRenderer;
    private WorldContactListener contactListener;
    OrthographicCamera camera;
    private OrthographicCamera hudCamera;

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
    private float stateTime;

    //is right
    private boolean facingRight = true;

    private static final int FRAME_WIDTH = 32;
    private static final int FRAME_HEIGHT = 32;

    //convert pixels -> meters
    public static final float PPM = 100;

    // Coins
    private Texture coinTexture;
    private static final float COIN_SIZE = 16f;
    private Array<Body> coinBodies;
    private Array<Body> bodiesToRemove;

    // Score
    private int score = 0;

    //Enemy
    private Array<Enemy> enemies;
    private Texture enemyTexture;
    private static final float ENEMY_VISUAL_WIDTH = 32f;
    private static final float ENEMY_VISUAL_HEIGHT = 32f;
    private boolean playerNeedsReset = false;
    private Vector2 playerSpawnPoint;

    public GameScreen(final GameStart gam) {
        this.game = gam;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 480);

        hudCamera = new OrthographicCamera();
        hudCamera.setToOrtho(false, 800, 480);

        coinBodies = new Array<>();
        bodiesToRemove = new Array<>();
        enemies = new Array<>();
    }


    @Override
    public void render(float delta) {
        handlePendingActions();

        update(delta);
        handleInput(delta);
        world.step(1/60f,6,2); // Фізика

        // Видалення тіл, позначених для видалення
        removeScheduledBodies();

        Gdx.gl.glClearColor(0.5f, 0.6f, 1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Vector2 bodyPos = playerBody.getPosition();
        float lerp = 0.1f;
        camera.position.x += (bodyPos.x * PPM - camera.position.x) * lerp;
        camera.position.y += (bodyPos.y * PPM - camera.position.y) * lerp;

        limitCameraView();
        camera.update();

        TextureRegion currentFrame = getFrame(delta);
        flipFrame(currentFrame);

        mapRenderer.setView(camera);
        mapRenderer.render();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // Рендер монет
        if (coinTexture != null) {
            for (Body coinBody : coinBodies) {
                if (coinBody.isActive()) { // Перевіряємо, чи тіло ще активне
                    float coinX = coinBody.getPosition().x * PPM - COIN_SIZE / 2f;
                    float coinY = coinBody.getPosition().y * PPM - COIN_SIZE / 2f;
                    game.batch.draw(coinTexture, coinX, coinY, COIN_SIZE, COIN_SIZE);
                }
            }
        }

        // Рендер ворогів
        for (Enemy enemy : enemies){
            enemy.draw(game.batch);
        }

        // Рендер гравця
        float playerDrawX = bodyPos.x * PPM - FRAME_WIDTH/2f;
        float playerDrawY = bodyPos.y * PPM - FRAME_HEIGHT/2f;
        if(currentFrame != null){
            game.batch.draw(currentFrame, playerDrawX, playerDrawY, FRAME_WIDTH, FRAME_HEIGHT);
        }
        game.batch.end();

        debugRenderer.render(world, camera.combined.cpy().scl(PPM));

        game.batch.setProjectionMatrix(hudCamera.combined);
        game.batch.begin();

        float scorePadding = 20f;
        game.font.draw(game.batch, "Score " + score, scorePadding, hudCamera.viewportHeight - scorePadding);
        game.batch.end();
    }

    private void removeScheduledBodies() {
        if (!bodiesToRemove.isEmpty()) {
            for (Body body : bodiesToRemove) {
                if (coinBodies.contains(body, true)) {
                    coinBodies.removeValue(body, true);
                }
                // Якщо будуть інші типи тіл для видалення, тут можна додати перевірки
                world.destroyBody(body);
            }
            bodiesToRemove.clear();
        }
    }


    // Метод для додавання тіла до черги на видалення (буде викликатися з ContactListener)
    public void scheduleBodyForRemoval(Body body) {
        if (body != null && !bodiesToRemove.contains(body, true)) {
            bodiesToRemove.add(body);
        }
    }


    private TextureRegion getFrame(float dt){
        //currentState = getState(); // перенесено в update
        //stateTime = (currentState == previousState) ? stateTime + dt : 0; // перенесено в update
        //previousState = currentState; // перенесено в update

        TextureRegion frame = null;
        switch (currentState){
            case JUMPING:frame = jumpFrame; break;
            case FALLING:frame = fallFrame; break;
            case RUNNING:frame = runAnimation.getKeyFrame(stateTime,true); break;
            case IDLE: default:frame = idleAnimation.getKeyFrame(stateTime, true); break;
        }
        return frame;
    }

    private void flipFrame(TextureRegion frame){
        if (frame == null) return;
        if (!facingRight &&  !frame.isFlipX()){
            frame.flip(true, false);
        }else if (facingRight && frame.isFlipX()) {
            frame.flip(true, false);
        }
    }

    private void limitCameraView(){
        Integer mapTileWidthProp = map.getProperties().get("width", Integer.class);
        Integer mapTileHeightProp = map.getProperties().get("height", Integer.class);
        Integer tilePixelWidthProp = map.getProperties().get("tilewidth", Integer.class);
        Integer tilePixelHeightProp = map.getProperties().get("tileheight", Integer.class);

        if (mapTileWidthProp == null || mapTileHeightProp == null || tilePixelWidthProp == null || tilePixelHeightProp == null) {
            Gdx.app.error("LIMIT_CAM", "Map properties not found!");
            return;
        }

        float mapTileWidth = mapTileWidthProp;
        float mapTileHeight = mapTileHeightProp;
        float tilePixelWidth = tilePixelWidthProp;
        float tilePixelHeight = tilePixelHeightProp;

        float mapWidthPixels = mapTileWidth * tilePixelWidth;
        float mapHeightPixels = mapTileHeight * tilePixelHeight;

        float cameraViewportWidth = camera.viewportWidth;
        float cameraViewportHeight = camera.viewportHeight;

        float cameraX = camera.position.x;
        float cameraY = camera.position.y;

        if (mapWidthPixels >= cameraViewportWidth) {
            cameraX = Math.max(cameraViewportWidth / 2f, cameraX);
            cameraX = Math.min(mapWidthPixels - cameraViewportWidth / 2f, cameraX);
        } else {
            cameraX = mapWidthPixels / 2f;
        }

        if (mapHeightPixels >= cameraViewportHeight) {
            cameraY = Math.max(cameraViewportHeight / 2f, cameraY);
            cameraY = Math.min(mapHeightPixels - cameraViewportHeight / 2f, cameraY);
        } else {
            cameraY = mapHeightPixels / 2f;
        }
        camera.position.set(cameraX, cameraY, 0);
    }

    private void handleInput(float dt){
        float maxVel = 2f;
        float targetVelX = 0;

        if (Gdx.input.isKeyPressed(Keys.A)){
            targetVelX = -maxVel;
            facingRight = false;
        }

        if (Gdx.input.isKeyPressed(Keys.D)){
            targetVelX = maxVel;
            facingRight = true;
        }

        if(Gdx.input.isKeyPressed(Keys.A) && Gdx.input.isKeyPressed(Keys.D)){
            targetVelX = 0;
        }

        playerBody.setLinearVelocity(targetVelX, playerBody.getLinearVelocity().y);

        if((Gdx.input.isKeyJustPressed(Keys.SPACE) && contactListener.isPlayerOnGround())
            || (Gdx.input.isKeyJustPressed(Keys.W) && contactListener.isPlayerOnGround())){
            // Збільшена сила стрибка для кращого відчуття
            float jumpForce = 5f; // Було 0.5f, що дуже мало. Box2D працює з масами та імпульсами.
            // Для тіла з density 0.5f та розміром 32x32px (0.32x0.32m),
            // маса буде приблизно 0.5 * (0.32*0.32) = 0.0512 кг.
            // Імпульс = маса * зміна швидкості. Зміна швидкості = Імпульс / маса.
            // Якщо імпульс 0.5, то зміна швидкості ~ 0.5 / 0.0512 ~ 9.7 м/с.
            // Якщо імпульс 5, то зміна швидкості ~ 5 / 0.0512 ~ 97 м/с (це забагато, але Box2D має gravity)
            // Давайте спробуємо імпульс, що дасть початкову швидкість близько 5-6 м/с.
            // playerBody.getMass() * desired_vy_change
            // Наприклад, якщо хочемо vy = 6 m/s, то імпульс = playerBody.getMass() * 6
            playerBody.applyLinearImpulse(0f, playerBody.getMass() * 6f, playerBody.getWorldCenter().x, playerBody.getWorldCenter().y, true);
        }
    }

    public void update(float dt){
        // Оновлення стану гравця
        currentState = getState();
        stateTime = (currentState == previousState) ? stateTime + dt : 0;
        previousState = currentState;

        for (Enemy enemy : enemies){
            enemy.update(dt);
        }
    }

    private State getState() {
        if (!contactListener.isPlayerOnGround()) {
            if (playerBody.getLinearVelocity().y > 0.1f) {
                return State.JUMPING;
            } else if (playerBody.getLinearVelocity().y < -0.1f) {
                return State.FALLING;
            } else {
                if (Math.abs(playerBody.getLinearVelocity().x) > 0.1f) {
                    return State.FALLING;
                } else {
                    return State.FALLING;
                }
            }
        } else { // На землі
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

        float startX = camera.viewportWidth / 2f;
        float startY = camera.viewportHeight / 2f;

        if (map.getLayers().get("SpawnPoints") != null){
            for (MapObject object : map.getLayers().get("SpawnPoints").getObjects()){
                if (object.getProperties().containsKey("playerStart") &&
                    object.getProperties().get("playerStart", Boolean.class)){
                    if (object instanceof RectangleMapObject){
                        Rectangle rect = ((RectangleMapObject) object).getRectangle();
                        startX = rect.getX() + rect.getWidth() / 2f;
                        startY = rect.getY() + rect.getHeight() / 2f;
                    } else {
                        startX = object.getProperties().get("x", startX, Float.class); // Додано значення за замовчуванням
                        startY = object.getProperties().get("y", startY, Float.class); // Додано значення за замовчуванням
                    }
                    Gdx.app.log("PLAYER_SPAWN", "Player start pos found at: " + startX + ", " + startY);
                    break;
                }
            }
        } else {
            Gdx.app.log("PLAYER_SPAWN", "SpawnPoint layer not found, using default start pos");
        }

        playerSpawnPoint = new Vector2(startX / PPM, startY / PPM);

        bodyDef.position.set(playerSpawnPoint.x, playerSpawnPoint.y);
        bodyDef.fixedRotation = true;
        bodyDef.linearDamping = 1.5f;

        playerBody = world.createBody(bodyDef);

        PolygonShape shape = new PolygonShape();
        float playerWidthPixels = 32; // FRAME_WIDTH
        float playerHeightPixels = 32; // FRAME_HEIGHT
        shape.setAsBox(playerWidthPixels/2/PPM, playerHeightPixels/2/PPM);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 0.5f;
        fixtureDef.friction = 0.5f; // Тертя з землею
        fixtureDef.restitution = 0.0f;

        playerBody.createFixture(fixtureDef).setUserData("player"); // Основне тіло гравця

        // Сенсор ніг
        shape.setAsBox(playerWidthPixels / 2.5f / PPM, 5 / PPM,
            new Vector2(0, -playerHeightPixels / 2f / PPM - (3 / PPM)),0); // Трохи нижче основи
        fixtureDef.shape = shape;
        fixtureDef.isSensor = true;
        playerBody.createFixture(fixtureDef).setUserData("playerFeet");

        shape.dispose();
    }

    private void createEnemiesFromMap(){
        MapLayer enemyLayer = map.getLayers().get("Enemies");
        if (enemyLayer == null){
            Gdx.app.log("MAP_LOADER", "Object layer 'Enemies' not found in Tiled map.");
            return;
        }

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.fixedRotation = true;

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(ENEMY_VISUAL_WIDTH / 2f / PPM, ENEMY_VISUAL_HEIGHT / 2f / PPM);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 0.8f;
        fixtureDef.friction = 0.5f;
        fixtureDef.restitution = 0.1f;

        for (MapObject object : enemyLayer.getObjects()) {
            if (object.getProperties().containsKey("type") && "enemy".equals(object.getProperties().get("type", String.class))) {
                float x = 0, y = 0;
                float patrolDist = 64f; // Значення за замовчуванням, якщо не вказано на карті

                if (object instanceof RectangleMapObject) {
                    Rectangle rect = ((RectangleMapObject) object).getRectangle();
                    x = (rect.getX() + rect.getWidth() / 2f);
                    y = (rect.getY() + rect.getHeight() / 2f);
                } else { // Якщо це точка
                    x = object.getProperties().get("x", Float.class);
                    y = object.getProperties().get("y", Float.class);
                }
                patrolDist = object.getProperties().get("patrolDistance", 64f, Float.class);


                bodyDef.position.set(x / PPM, y / PPM);
                Body enemyBody = world.createBody(bodyDef);
                enemyBody.createFixture(fixtureDef).setUserData("enemy"); // Позначаємо тіло як "enemy"

                Enemy enemy = new Enemy(enemyBody, enemyTexture, x / PPM, patrolDist, ENEMY_VISUAL_WIDTH, ENEMY_VISUAL_HEIGHT);
                enemies.add(enemy);
                Gdx.app.log("ENEMY_CREATE", "Enemy created at: " + (x/PPM) + ", " + (y/PPM) + " with patrol " + patrolDist);
            }
        }
        shape.dispose();

    }

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
                fixtureDef.friction = 0.8f; // Тертя землі

                body.createFixture(fixtureDef).setUserData("ground");
            }
            else {
                Gdx.app.log("MAP_LOADER","Skipping non-rectangle objects in Collision layer; " + object.getName());
            }
        }
        shape.dispose();
    }

    private void createCollectibles() {
        MapLayer collectiblesLayer = map.getLayers().get("Collectibles");
        if (collectiblesLayer == null) {
            Gdx.app.log("MAP_LOADER", "Object layer 'Collectibles' not found in Tiled map.");
            return;
        }

        BodyDef bodyDef = new BodyDef();
        FixtureDef fixtureDef = new FixtureDef();
        PolygonShape shape = new PolygonShape();

        for (MapObject object : collectiblesLayer.getObjects()) {
            if (object.getProperties().containsKey("type") && "coin".equals(object.getProperties().get("type", String.class))) {
                if (object instanceof RectangleMapObject) {
                    Rectangle rect = ((RectangleMapObject) object).getRectangle();

                    bodyDef.type = BodyDef.BodyType.StaticBody; // Монети не рухаються
                    float centerX = (rect.getX() + rect.getWidth() / 2f) / PPM;
                    float centerY = (rect.getY() + rect.getHeight() / 2f) / PPM;
                    bodyDef.position.set(centerX, centerY);

                    Body coinBody = world.createBody(bodyDef);

                    shape.setAsBox(COIN_SIZE / 2f / PPM, COIN_SIZE / 2f / PPM);

                    fixtureDef.shape = shape;
                    fixtureDef.isSensor = true;
                    fixtureDef.friction = 0;
                    fixtureDef.restitution = 0;

                    coinBody.createFixture(fixtureDef).setUserData("coin");
                    coinBodies.add(coinBody);
                    Gdx.app.log("COIN_CREATE", "Coin created at: " + centerX*PPM + ", " + centerY*PPM);
                }
            }
        }
        shape.dispose();
    }

    @Override
    public void show() {
        world = new World(new Vector2(0, -10f), true); // Гравітація -10f хороша стандартна
        debugRenderer = new Box2DDebugRenderer();
        contactListener = new WorldContactListener(this); // Передаємо GameScreen в конструктор
        world.setContactListener(contactListener);

        map = new TmxMapLoader().load("assets/test2_map.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map, 1f / PPM);

        mapRenderer = new OrthogonalTiledMapRenderer(map, 1f);


        // Завантаження текстур
        Texture idleSheet = new Texture(Gdx.files.internal("assets/player_idle.png"));
        Texture runSheet = new Texture(Gdx.files.internal("assets/player_run.png"));
        Texture jumpSheet = new Texture(Gdx.files.internal("assets/player_jump.png"));
        Texture fallSheet = new Texture(Gdx.files.internal("assets/player_fall.png"));
        coinTexture = new Texture(Gdx.files.internal("assets/coin.png"));
        enemyTexture = new Texture(Gdx.files.internal("assets/enemy.png"));

        // Анімації гравця
        int idleFrameCount = 4;
        TextureRegion[][] tmpIdle = TextureRegion.split(idleSheet, FRAME_WIDTH, FRAME_HEIGHT);
        Array<TextureRegion> idleFrames = new Array<>(idleFrameCount);
        for(int i = 0; i < idleFrameCount; i++){
            idleFrames.add(tmpIdle[0][i]);
        }
        idleAnimation = new Animation<>(0.15f, idleFrames, Animation.PlayMode.LOOP);

        int runFrameCount = 8;
        TextureRegion[][] tmpRun = TextureRegion.split(runSheet, FRAME_WIDTH, FRAME_HEIGHT);
        Array<TextureRegion> runFrames = new Array<>(runFrameCount);
        for (int i = 0; i < runFrameCount; i++){
            runFrames.add(tmpRun[0][i]);
        }
        runAnimation = new Animation<>(0.1f, runFrames, Animation.PlayMode.LOOP);

        jumpFrame = new TextureRegion(jumpSheet, 0,0, FRAME_WIDTH, FRAME_HEIGHT);
        fallFrame = new TextureRegion(fallSheet, 0,0, FRAME_WIDTH, FRAME_HEIGHT);

        stateTime = 0f;
        currentState = State.IDLE;

        createPlayerBody();
        createPhysicsFromMap();
        createCollectibles();
        createEnemiesFromMap();
    }

    private void handlePendingActions(){
        if(playerNeedsReset) {
            actuallyResetPlayer();
            playerNeedsReset = false;
        }
    }

    public void requestPlayerReset() {
        playerNeedsReset = true;
    }

    private void actuallyResetPlayer() {
        if (playerBody != null && playerSpawnPoint != null) {
            playerBody.setTransform(playerSpawnPoint, 0);
            playerBody.setLinearVelocity(0, 0);
            score = 0;
            Gdx.app.log("PLAYER", "Player actually reset to spawn point.");
        } else {
            Gdx.app.error("PLAYER_RESET", "Cannot reset player. Body or spawn point is null for actual reset.");
        }
    }

    // Метод для збільшення рахунку (буде викликатися з ContactListener)
    public void collectCoin() {
        score++;
        Gdx.app.log("SCORE", "Score: " + score);
        // додати звук збору монети
    }


    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height); // update viewport cameru when window changed
        camera.viewportWidth = 800;
        camera.viewportHeight = 480;
        camera.update();

        hudCamera.setToOrtho(false,800,480);
        hudCamera.update();
    }

    @Override
    public void hide() {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {
        world.dispose();
        debugRenderer.dispose();
        map.dispose();
        mapRenderer.dispose();

        // Вивільнення текстур анімацій
        if (idleAnimation != null && idleAnimation.getKeyFrames().length > 0 && idleAnimation.getKeyFrames()[0] != null){
            Texture texture = idleAnimation.getKeyFrames()[0].getTexture();
            if (texture != null) texture.dispose();
        }
        if (runAnimation != null && runAnimation.getKeyFrames().length > 0 && runAnimation.getKeyFrames()[0] != null){
            runAnimation.getKeyFrames()[0].getTexture().dispose();
        }
        if (jumpFrame != null && jumpFrame.getTexture() != null) jumpFrame.getTexture().dispose();
        if (fallFrame != null && fallFrame.getTexture() != null) fallFrame.getTexture().dispose();

        if (coinTexture != null) coinTexture.dispose();

        if (enemyTexture != null) enemyTexture.dispose();

        for (Enemy enemy : enemies) {
            enemy.dispose();
        }
        enemies.clear();

        // Очищення масивів, хоча вони будуть зібрані GC, але для певності
        coinBodies.clear();
        bodiesToRemove.clear();
    }
}
