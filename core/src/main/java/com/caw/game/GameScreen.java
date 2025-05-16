package com.caw.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class GameScreen implements Screen {
    final GameStart game;
    private World world;
    private Box2DDebugRenderer debugRenderer;
    private WorldContactListener contactListener;

    //camera
    public static final float WORLD_WIDTH_PIXELS = 800f;
    public static final float WORLD_HEIGHT_PIXELS = 480f;
    private OrthographicCamera gameCamera;
    private OrthographicCamera hudCamera;
    private OrthographicCamera debugCamera;
    private com.badlogic.gdx.utils.viewport.Viewport gameViewport;
    private com.badlogic.gdx.utils.viewport.Viewport hudViewport;

    //map
    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;

    // Player related
    private Player player;
    private Texture playerIdleSheetTexture;
    private Texture playerRunSheetTexture;
    private Texture playerJumpSheetTexture;
    private Texture playerFallSheetTexture;
    private Vector2 initialPlayerSpawnPointPixels;

    public static final float PPM = 100;

    //coin
    private Texture coinTexture;
    private static final float COIN_SIZE = 16f;
    private Array<Body> coinBodies;
    private Array<Body> bodiesToRemove;

    private int score = 0;
    private ShapeRenderer shapeRenderer;

    //enemy
    private Array<Enemy> enemies;
    private Array<ShootingEnemy> shootingEnemies;
    private Array<Projectile> projectiles;
    private Texture enemyTexture;
    private Texture shootingEnemyTexture;
    private Texture projectileTexture;
    private static final float ENEMY_VISUAL_WIDTH = 32f;
    private static final float ENEMY_VISUAL_HEIGHT = 32f;

    //shooting enemy
    private static final float SHOOTING_ENEMY_VISUAL_WIDTH = 32f;
    private static final float SHOOTING_ENEMY_VISUAL_HEIGHT = 32f;

    //key - door
    private Texture keyTexture;
    private Texture doorClosedTexture;
    public Body keyBody;
    public boolean playerHasKey = false;
    private Vector2 keyPositionPixels;
    private Array<DoorData> doors;
    private Texture doorOpenTexture;

    private boolean playerNeedsPositionReset = false; // reset pos WO death

    public GameScreen(final GameStart gam) {
        this.game = gam;
        gameCamera = new OrthographicCamera();

        hudCamera = new OrthographicCamera();
        hudCamera.setToOrtho(false, WORLD_WIDTH_PIXELS, WORLD_HEIGHT_PIXELS);
    }

    @Override
    public void show() {
        Gdx.app.log("GameScreen", "show() called");

        gameViewport = new com.badlogic.gdx.utils.viewport.ExtendViewport(WORLD_WIDTH_PIXELS, WORLD_HEIGHT_PIXELS, gameCamera);
        hudViewport = new com.badlogic.gdx.utils.viewport.ExtendViewport(WORLD_WIDTH_PIXELS, WORLD_HEIGHT_PIXELS, hudCamera);


        gameViewport.apply();
        hudViewport.apply();

//        gameCamera.setToOrtho(false, 800, 480);
//        hudCamera.setToOrtho(false, 800, 480);

        world = new World(new Vector2(0, -10f), true);
        debugRenderer = new Box2DDebugRenderer();
        contactListener = new WorldContactListener(this);
        world.setContactListener(contactListener);

        map = new TmxMapLoader().load("assets/test2_map.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map, 1); // map render

        // player textures
        playerIdleSheetTexture = new Texture(Gdx.files.internal("assets/player_idle.png"));
        playerRunSheetTexture = new Texture(Gdx.files.internal("assets/player_run.png"));
        playerJumpSheetTexture = new Texture(Gdx.files.internal("assets/player_jump.png"));
        playerFallSheetTexture = new Texture(Gdx.files.internal("assets/player_fall.png"));

        //collectibles
        coinTexture = new Texture(Gdx.files.internal("assets/coin.png"));
        keyTexture = new Texture(Gdx.files.internal("assets/key.png"));
        doorClosedTexture = new Texture(Gdx.files.internal("assets/door_closed.png"));
        doorOpenTexture = new Texture(Gdx.files.internal("assets/door_open.png"));

        //enemy
        enemyTexture = new Texture(Gdx.files.internal("assets/enemy.png"));
        shootingEnemyTexture = new Texture(Gdx.files.internal("assets/shooting_enemy.png"));
        projectileTexture = new Texture(Gdx.files.internal("assets/projectile.png"));

        shapeRenderer = new ShapeRenderer();
        coinBodies = new Array<>();
        bodiesToRemove = new Array<>();
        enemies = new Array<>();
        shootingEnemies = new Array<>();
        projectiles = new Array<>();
        doors = new Array<>();

        score = 0;
        playerHasKey = false;

        findInitialPlayerSpawnPoint();
        player = new Player(world, contactListener, initialPlayerSpawnPointPixels,
            playerIdleSheetTexture, playerRunSheetTexture, playerJumpSheetTexture, playerFallSheetTexture);
        contactListener.setPlayer(player); // player go to contactListener

        createPhysicsFromMap();
        createCollectibles();
        createDoorsFromMap();
        createEnemiesFromMap();

//        createShootingEnemies();

        Gdx.app.log("GameScreen", "show() finished initialization");
    }

    private void findInitialPlayerSpawnPoint() {
        initialPlayerSpawnPointPixels = new Vector2(gameCamera.viewportWidth / 2f, gameCamera.viewportHeight / 2f); // Default
        MapLayer spawnLayer = map.getLayers().get("SpawnPoints");
        if (spawnLayer != null) {
            for (MapObject object : spawnLayer.getObjects()) {
                if (object.getProperties().containsKey("playerStart") &&
                    object.getProperties().get("playerStart", Boolean.class)) {
                    if (object instanceof RectangleMapObject) {
                        Rectangle rect = ((RectangleMapObject) object).getRectangle();
                        initialPlayerSpawnPointPixels.set(rect.getX() + rect.getWidth() / 2f, rect.getY() + rect.getHeight() / 2f);
                    } else {
                        initialPlayerSpawnPointPixels.set(object.getProperties().get("x", Float.class),
                            object.getProperties().get("y", Float.class));
                    }
                    Gdx.app.log("PLAYER_SPAWN", "Player start pos found at (pixels): " + initialPlayerSpawnPointPixels);
                    return;
                }
            }
        }
        Gdx.app.log("PLAYER_SPAWN", "SpawnPoint 'playerStart' not found, using default: " + initialPlayerSpawnPointPixels);
    }


    @Override
    public void render(float delta) {
        // dealayed actions
        handlePendingActions();

        // game logic
        if (player != null && !player.isDead()) {
            player.handleInput(delta); // player input
            player.update(delta);      // update
        } else if (player != null && player.isDead()) {
            // "Game Over"
            if (Gdx.input.isKeyJustPressed(Keys.R)) {
                restartLevel();
            }
            if (Gdx.input.isKeyJustPressed(Keys.M)) {
                game.setScreen(new MainMenuScreen(game));
            }
        }

        for (Enemy enemy : enemies) {
            enemy.update(delta);
        }

        //shooting enemy
        for (ShootingEnemy sEnemy : shootingEnemies) {
            sEnemy.update(delta, player);
        }

        //projectile update
        for (int i = projectiles.size - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            p.update(delta);
            if (p.isScheduledForRemoval()) {
                if (p.getBody() != null && p.getBody().isActive()) {
                    scheduleBodyForRemoval(p.getBody());
                }
                projectiles.removeIndex(i);
            }
        }

        //doors
        for (DoorData door : doors) {
            if (door.isLocked && playerHasKey && !door.isOpen) {
                door.isOpen = true;
            }
        }

        world.step(1 / 60f, 6, 2);
        removeScheduledBodies(); // remove bodies after world step

        // screen clear (BG)
        Gdx.gl.glClearColor(0.5f, 0.6f, 1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // cam update
        if (player != null && player.getBody() != null) {
            Vector2 playerPosMeters = player.getPositionMeters();
            float lerp = 0.1f; // smooth camera
            gameCamera.position.x += (playerPosMeters.x * PPM - gameCamera.position.x) * lerp;
            gameCamera.position.y += (playerPosMeters.y * PPM - gameCamera.position.y) * lerp;
        }
        limitCameraView();
        gameCamera.update();

        // map render
        mapRenderer.setView(gameCamera);
        mapRenderer.render();

        // game obj render
        game.batch.setProjectionMatrix(gameCamera.combined);
        game.batch.begin();

        // coins
        if (coinTexture != null) {
            for (Body coinBody : coinBodies) {
                if (coinBody.isActive()) { // check if body still active
                    float coinX = coinBody.getPosition().x * PPM - COIN_SIZE / 2f;
                    float coinY = coinBody.getPosition().y * PPM - COIN_SIZE / 2f;
                    game.batch.draw(coinTexture, coinX, coinY, COIN_SIZE, COIN_SIZE);
                }
            }
        }
        //key
        if (keyBody != null && keyBody.isActive() && keyTexture != null) {
            float keyDrawWidth = 24f;
            float keyDrawHeight = 24f;
            game.batch.draw(keyTexture,
                keyBody.getPosition().x * PPM - keyDrawWidth / 2f,
                keyBody.getPosition().y * PPM - keyDrawHeight / 2f,
                keyDrawWidth, keyDrawHeight);
        }

        //doors
        for (DoorData door : doors) {
            Texture doorTextureToDraw = (door.isOpen || !door.isLocked) ? (doorOpenTexture != null ? doorOpenTexture : doorClosedTexture) : doorClosedTexture;
            if (doorTextureToDraw != null) {
                game.batch.draw(doorTextureToDraw,
                    door.boundsPixels.x, door.boundsPixels.y,
                    door.boundsPixels.width, door.boundsPixels.height);
            }
        }

        // enemies
        for (Enemy enemy : enemies) {
            enemy.draw(game.batch);
        }

        for (ShootingEnemy sEnemy : shootingEnemies) {
            sEnemy.draw(game.batch);
        }

        for (Projectile projectile : projectiles) {
            projectile.render(game.batch);
        }

        // player
        if (player != null) {
            player.render(game.batch);
        }
        game.batch.end();

        // debug info Box2d (HITBOXES)
        if (debugRenderer != null) {
            debugRenderer.render(world, gameCamera.combined.cpy().scl(PPM));
        }

        // HUD
        hudCamera.update();

        // Health Bar
        if (player != null && shapeRenderer != null) {
            float healthBarWidth = 200f;
            float healthBarHeight = 20f;
            float healthBarX = 20f;
            float healthBarY = hudCamera.viewportHeight - healthBarHeight - 20f;

            shapeRenderer.setProjectionMatrix(hudCamera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1f); // Фон
            shapeRenderer.rect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);

            float currentHealthWidth = healthBarWidth * (player.getCurrentHealth() / player.getMaxHealth());
            if (currentHealthWidth < 0) currentHealthWidth = 0;
            shapeRenderer.setColor(0f, 1f, 0f, 1f); // Сама смужка
            shapeRenderer.rect(healthBarX, healthBarY, currentHealthWidth, healthBarHeight);
            shapeRenderer.end();
        }

        //key hud
        if (playerHasKey && keyTexture != null) {
            game.batch.draw(keyTexture, hudViewport.getWorldWidth() - 50f, hudViewport.getWorldHeight() - 50f, 32f, 32f);
        }

        // Текст HUD (рахунок, Game Over)
        game.batch.setProjectionMatrix(hudCamera.combined);
        game.batch.begin();
        float scorePadding = 20f;
        float healthBarHeightForText = 20f;
        game.font.draw(game.batch, "Score: " + score, scorePadding, hudCamera.viewportHeight - scorePadding - healthBarHeightForText - 10f);

        if (player != null && player.isDead()) {
            game.font.getData().setScale(2);
            String gameOverText = "GAME OVER";
            com.badlogic.gdx.graphics.g2d.GlyphLayout layoutGO = new com.badlogic.gdx.graphics.g2d.GlyphLayout(game.font, gameOverText);
            game.font.draw(game.batch, gameOverText, (hudCamera.viewportWidth - layoutGO.width) / 2, hudCamera.viewportHeight / 2 + layoutGO.height + 30);

            game.font.getData().setScale(1);
            String restartText = "Press 'R' to Restart";
            com.badlogic.gdx.graphics.g2d.GlyphLayout layoutR = new com.badlogic.gdx.graphics.g2d.GlyphLayout(game.font, restartText);
            game.font.draw(game.batch, restartText, (hudCamera.viewportWidth - layoutR.width) / 2, hudCamera.viewportHeight / 2 - 10);

            String menuText = "Press 'M' for Main Menu";
            com.badlogic.gdx.graphics.g2d.GlyphLayout layoutM = new com.badlogic.gdx.graphics.g2d.GlyphLayout(game.font, menuText);
            game.font.draw(game.batch, menuText, (hudCamera.viewportWidth - layoutM.width) / 2, hudCamera.viewportHeight / 2 - 10 - layoutR.height - 5);
        }
        game.batch.end();
    }

    private void handlePendingActions() {
        if (playerNeedsPositionReset) {
            if (player != null) {
                player.resetPositionToSpawn();
            }
            playerNeedsPositionReset = false;
        }
    }

//    public void requestPlayerDamage(float damageAmount) {
//        if (player != null && !player.isDead()) {
//            player.takeDamage(damageAmount);
//        }
//    }

    public void requestPlayerPositionReset() {
        if (player != null && !player.isDead()) {
            playerNeedsPositionReset = true;
        }
    }

    public void collectCoin() {
        score++;
        Gdx.app.log("SCORE", "Score: " + score);
    }


    private void limitCameraView() {
        if (map == null) return;

        Integer mapTileWidthProp = map.getProperties().get("width", Integer.class);
        Integer mapTileHeightProp = map.getProperties().get("height", Integer.class);
        Integer tilePixelWidthProp = map.getProperties().get("tilewidth", Integer.class);
        Integer tilePixelHeightProp = map.getProperties().get("tileheight", Integer.class);

        if (mapTileWidthProp == null || mapTileHeightProp == null || tilePixelWidthProp == null || tilePixelHeightProp == null) {
            Gdx.app.error("LIMIT_CAM", "Map properties not found!");
            return;
        }

        float mapWidthPixels = (float)mapTileWidthProp * tilePixelWidthProp;
        float mapHeightPixels = (float)mapTileHeightProp * tilePixelHeightProp;

        float cameraEffectiveViewportWidth = gameCamera.viewportWidth;
        float cameraEffectiveViewportHeight = gameCamera.viewportHeight;


        float cameraX = gameCamera.position.x;
        float cameraY = gameCamera.position.y;

        if (mapWidthPixels >= cameraEffectiveViewportWidth) {
            cameraX = Math.max(cameraEffectiveViewportWidth / 2f, cameraX);
            cameraX = Math.min(mapWidthPixels - cameraEffectiveViewportWidth / 2f, cameraX);
        } else {
            cameraX = mapWidthPixels / 2f;
        }

        if (mapHeightPixels >= cameraEffectiveViewportHeight) {
            cameraY = Math.max(cameraEffectiveViewportHeight / 2f, cameraY);
            cameraY = Math.min(mapHeightPixels - cameraEffectiveViewportHeight / 2f, cameraY);
        } else {
            cameraY = mapHeightPixels / 2f;
        }
        gameCamera.position.set(cameraX, cameraY, 0);
    }


    public void scheduleBodyForRemoval(Body body) {
        if (body != null && !bodiesToRemove.contains(body, true)) {
            bodiesToRemove.add(body);
        }
    }

    private void removeScheduledBodies() {
        if (world == null || world.isLocked() || bodiesToRemove.isEmpty()) return;

        for (Body body : bodiesToRemove) {

            if (coinBodies.contains(body, true)) {
                coinBodies.removeValue(body, true);
            }

            if (body.getUserData() instanceof Enemy) {
                Enemy enemy = (Enemy) body.getUserData();
                enemies.removeValue(enemy, true);
            }

            if (body.getUserData() instanceof ShootingEnemy) {
                ShootingEnemy sEnemy = (ShootingEnemy) body.getUserData();
                //shootingEnemies.removeValue(sEnemy, true);

            }

            if (body.getUserData() instanceof Projectile) {
            }

            world.destroyBody(body);
        }
        bodiesToRemove.clear();
    }

    public void restartLevel() {
        Gdx.app.log("GAME_STATE", "Restarting level...");
        score = 0;
        if (player != null) {
            player.respawn();
        }
        playerNeedsPositionReset = false;

        Array<Body> bodiesToDestroy = new Array<>();

        for (Body coinBody : coinBodies) {
            bodiesToDestroy.add(coinBody);
        }
        for (Enemy enemy : enemies) {
            if (enemy.body != null) bodiesToDestroy.add(enemy.body);
        }

        for (Body b : bodiesToDestroy) {
            if (!world.isLocked()) world.destroyBody(b);
            else Gdx.app.error("RESTART", "World locked, cannot destroy body " + b.getUserData());
        }

        for (Projectile p : projectiles) {
            if (p.getBody() != null && p.getBody().isActive()) {
                if (!world.isLocked()) world.destroyBody(p.getBody());
            }
        }

        //key reset
        playerHasKey = false;
        if (keyBody != null && !keyBody.isActive()) {
        }

        doors.clear();
        projectiles.clear();
        coinBodies.clear();
        enemies.clear();
        shootingEnemies.clear();

        createCollectibles();
        createEnemiesFromMap();
        createDoorsFromMap();

        Gdx.app.log("GAME_STATE", "Level re-initialized.");
    }

    public void addProjectile(Projectile projectile) {
        projectiles.add(projectile);
    }

    private void createPhysicsFromMap(){
        if (world == null || map == null) return;
        MapLayer collisionLayer = map.getLayers().get("Collision");
        if (collisionLayer ==null){
            Gdx.app.error("MAP_LOADER","Object layer 'Collision' not found in Tiled map!");
            return;
        }

        BodyDef bodyDef = new BodyDef();
        FixtureDef fixtureDef = new FixtureDef();
        PolygonShape shape = new PolygonShape();

        for (MapObject object : collisionLayer.getObjects()){
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
        }
        shape.dispose();
    }

    public static class DoorData {
        public Body body;
        public Rectangle boundsPixels;
        public String nextLevelAsset;
        public boolean isLocked;
        public boolean isOpen = false;
        public Vector2 positionPixels;

        public DoorData(Body body, Rectangle boundsPixels, String nextLevelAsset, boolean initiallyLocked) {
            this.body = body;
            this.boundsPixels = boundsPixels;
            this.nextLevelAsset = nextLevelAsset;
            this.isLocked = initiallyLocked;
            if (body != null) {
                body.setUserData(this);
            }
            this.positionPixels = new Vector2(boundsPixels.x + boundsPixels.width / 2, boundsPixels.y + boundsPixels.height / 2);
        }
    }

    private void createCollectibles() {
        if (world == null || map == null) return;

        MapLayer collectiblesLayer = map.getLayers().get("Collectibles");
        if (collectiblesLayer == null) {
            Gdx.app.log("MAP_LOADER", "Object layer 'Collectibles' not found.");
            return;
        }

        BodyDef bodyDef = new BodyDef();
        FixtureDef fixtureDef = new FixtureDef();
        PolygonShape shape = new PolygonShape();

        for (MapObject object : collectiblesLayer.getObjects()) {
            if (object.getProperties().containsKey("type") && "coin".equals(object.getProperties().get("type", String.class))) {
                if (object instanceof RectangleMapObject) {
                    Rectangle rect = ((RectangleMapObject) object).getRectangle();
                    bodyDef.type = BodyDef.BodyType.StaticBody;
                    float centerX = (rect.getX() + rect.getWidth() / 2f) / PPM;
                    float centerY = (rect.getY() + rect.getHeight() / 2f) / PPM;
                    bodyDef.position.set(centerX, centerY);

                    Body coinBody = world.createBody(bodyDef);
                    shape.setAsBox(COIN_SIZE / 2f / PPM, COIN_SIZE / 2f / PPM);
                    fixtureDef.shape = shape;
                    fixtureDef.isSensor = true;
                    coinBody.createFixture(fixtureDef).setUserData("coin");
                    coinBody.setUserData("coin_body"); // Додатковий ідентифікатор для тіла
                    coinBodies.add(coinBody);
                }
            }
        }

        for (MapObject object : collectiblesLayer.getObjects()) {
            if ("key".equals(object.getProperties().get("type", String.class))) {
                if (object instanceof RectangleMapObject) {
                    Rectangle rect = ((RectangleMapObject) object).getRectangle();
                    keyPositionPixels = new Vector2(rect.getX(), rect.getY());

                    bodyDef.type = BodyDef.BodyType.StaticBody;
                    float centerX = (rect.getX() + rect.getWidth() / 2f) / PPM;
                    float centerY = (rect.getY() + rect.getHeight() / 2f) / PPM;
                    bodyDef.position.set(centerX, centerY);

                    keyBody = world.createBody(bodyDef);
                    float keyHitboxSize = 16f;
                    shape.setAsBox(keyHitboxSize / 2f / PPM, keyHitboxSize / 2f / PPM);
                    fixtureDef.shape = shape;
                    fixtureDef.isSensor = true;
                    keyBody.createFixture(fixtureDef).setUserData("key");
                    Gdx.app.log("GAME_SETUP", "Key created at: " + keyPositionPixels);
                    break; // cus key is 1
                }
            }
        }

        shape.dispose();
    }

//    private void createShootingEnemies() {
//        float x_pixels = 600f;
//        float y_pixels = 100f + Player.FRAME_HEIGHT / 2f;
//        float visualWidth_pixels = 32f;
//        float visualHeight_pixels = 32f;
//
//        ShootingEnemy sEnemy = new ShootingEnemy(world, this, shootingEnemyTexture, projectileTexture,
//            x_pixels, y_pixels, visualWidth_pixels, visualHeight_pixels);
//        shootingEnemies.add(sEnemy);
//        Gdx.app.log("GAME_SETUP", "Created shooting enemy at " + x_pixels + "," + y_pixels);
//    }

    private void createEnemiesFromMap(){
        if (world == null || map == null) return;
        MapLayer enemyLayer = map.getLayers().get("Enemies");
        if (enemyLayer == null){
            Gdx.app.log("MAP_LOADER", "Object layer 'Enemies' not found.");
            return;
        }

        //for regular enemy
        BodyDef enemyBodyDef = new BodyDef();
        enemyBodyDef.type = BodyDef.BodyType.DynamicBody;
        enemyBodyDef.fixedRotation = true;

        PolygonShape enemyShape = new PolygonShape();

        enemyShape.setAsBox(ENEMY_VISUAL_WIDTH / 2f / PPM, ENEMY_VISUAL_HEIGHT / 2f / PPM);

        FixtureDef enemyFixtureDef = new FixtureDef();
        enemyFixtureDef.shape = enemyShape;
        enemyFixtureDef.density = 0.8f;
        enemyFixtureDef.friction = 0.4f;
        enemyFixtureDef.restitution = 0.0f;


        for (MapObject object : enemyLayer.getObjects()) {
            String type = object.getProperties().get("type", String.class);
            if (type == null) continue; // Пропускаємо об'єкти без типу

            float xPixels = 0, yPixels = 0;

            if (object instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject) object).getRectangle();

                xPixels = rect.getX() + rect.getWidth() / 2f;
                yPixels = rect.getY() + rect.getHeight() / 2f;
            } else {
                xPixels = object.getProperties().get("x", 0f, Float.class);
                yPixels = object.getProperties().get("y", 0f, Float.class);

                Float objWidth = object.getProperties().get("width", Float.class);
                Float objHeight = object.getProperties().get("height", Float.class);
                if (objWidth != null) xPixels += objWidth / 2f;
                if (objHeight != null) yPixels += objHeight / 2f;
            }


            if ("enemy".equals(type)) {
                if (enemyTexture == null) {
                    Gdx.app.error("ENEMY_FACTORY", "Enemy texture is null, cannot create regular enemy.");
                    continue;
                }
                float patrolDistPixels = object.getProperties().get("patrolDistance", 64f, Float.class);

                enemyBodyDef.position.set(xPixels / PPM, yPixels / PPM);
                Body regEnemyBody = world.createBody(enemyBodyDef);

                regEnemyBody.createFixture(enemyFixtureDef).setUserData("enemy_fixture");

                Enemy enemy = new Enemy(regEnemyBody, enemyTexture, xPixels, patrolDistPixels,
                    ENEMY_VISUAL_WIDTH, ENEMY_VISUAL_HEIGHT, this);
                enemies.add(enemy);

            } else if ("shooting_enemy".equals(type)) {
                if (shootingEnemyTexture == null || projectileTexture == null) {
                    Gdx.app.error("ENEMY_FACTORY", "Shooting enemy or projectile texture is null.");
                    continue;
                }



                float detectionRadiusPixels = object.getProperties().get("detectionRadius", ShootingEnemy.DETECTION_RADIUS * PPM, Float.class);
                float shootCooldownSeconds = object.getProperties().get("shootCooldown", ShootingEnemy.SHOOT_COOLDOWN, Float.class);


                // ShootingEnemy creatin body inside staticBody
                ShootingEnemy sEnemy = new ShootingEnemy(world, this, shootingEnemyTexture, projectileTexture,
                    xPixels, yPixels,
                    SHOOTING_ENEMY_VISUAL_WIDTH, SHOOTING_ENEMY_VISUAL_HEIGHT,
                    detectionRadiusPixels, shootCooldownSeconds);
                shootingEnemies.add(sEnemy);
                Gdx.app.log("GAME_SETUP", "Created shooting enemy from map at " + xPixels + "," + yPixels);
            }
        }
        enemyShape.dispose();
    }

    private void createDoorsFromMap() {
        if (world == null || map == null) return;
        MapLayer interactablesLayer = map.getLayers().get("Interactables");
        if (interactablesLayer == null) {
            Gdx.app.log("MAP_LOADER", "Object layer 'Interactables' for doors not found.");
            return;
        }

        BodyDef bodyDef = new BodyDef();
        FixtureDef fixtureDef = new FixtureDef();
        PolygonShape shape = new PolygonShape();

        for (MapObject object : interactablesLayer.getObjects()) {
            if (!"door".equals(object.getProperties().get("type", String.class))) {
                continue;
            }
            if (object instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject) object).getRectangle();

                bodyDef.type = BodyDef.BodyType.StaticBody;
                float centerX = (rect.getX() + rect.getWidth() / 2f) / PPM;
                float centerY = (rect.getY() + rect.getHeight() / 2f) / PPM;
                bodyDef.position.set(centerX, centerY);

                Body doorBody = world.createBody(bodyDef);
                shape.setAsBox(rect.getWidth() / 2f / PPM, rect.getHeight() / 2f / PPM);
                fixtureDef.shape = shape;
                fixtureDef.isSensor = true;

                String nextLevel = object.getProperties().get("nextLevel", "main_menu", String.class); //na main menu
                boolean initiallyLocked = object.getProperties().get("initiallyLocked", true, Boolean.class);

                DoorData doorData = new DoorData(doorBody, new Rectangle(rect), nextLevel, initiallyLocked);
                doors.add(doorData);

                Gdx.app.log("GAME_SETUP", "Door created leading to: " + nextLevel + ", Locked: " + initiallyLocked);
            }
        }
        shape.dispose();
    }

    @Override
    public void resize(int width, int height) {
        Gdx.app.log("GameScreen", "Resizin to: " + width + "x" + height);
        if (gameViewport != null) {
            gameViewport.update(width, height, false);
        }

        if (hudCamera != null) {
            hudCamera.setToOrtho(false, width, height);
            hudCamera.update();
        }
//        camera.viewportWidth = 800;
//        camera.viewportHeight = 480;
//        camera.update();
//
//        hudCamera.viewportWidth = 800;
//        hudCamera.viewportHeight = 480;
//        hudCamera.update();
    }

    @Override
    public void hide() {
        Gdx.app.log("GameScreen", "hide() called");
    }

    @Override
    public void pause() { Gdx.app.log("GameScreen", "pause() called"); }

    @Override
    public void resume() { Gdx.app.log("GameScreen", "resume() called"); }

    @Override
    public void dispose() {
        Gdx.app.log("GameScreen", "dispose() called");

        if (playerIdleSheetTexture != null) playerIdleSheetTexture.dispose();
        if (playerRunSheetTexture != null) playerRunSheetTexture.dispose();
        if (playerJumpSheetTexture != null) playerJumpSheetTexture.dispose();
        if (playerFallSheetTexture != null) playerFallSheetTexture.dispose();
        if (coinTexture != null) coinTexture.dispose();
        if (enemyTexture != null) enemyTexture.dispose();
        if (shootingEnemyTexture != null) shootingEnemyTexture.dispose();
        if (projectileTexture != null) projectileTexture.dispose();
        if (keyTexture != null) keyTexture.dispose();
        if (doorClosedTexture != null) doorClosedTexture.dispose();
        if (doorOpenTexture != null) doorOpenTexture.dispose();

        playerIdleSheetTexture = null;
        playerRunSheetTexture = null;
        playerJumpSheetTexture = null;
        playerFallSheetTexture = null;
        coinTexture = null;
        enemyTexture = null;

        if (player != null) {
            player.dispose(); // Звільняємо тіло гравця, якщо воно ще існує
            player = null;
        }

        // before release world, release enemies and other obj
        if (world != null && !world.isLocked()) {
            // release bodies (coin & enemy)
            Array<Body> allBodies = new Array<>();
            world.getBodies(allBodies);
            for (Body body : allBodies) {
                // check if its not a player body, if it not null
                if (player != null && body == player.getBody()) continue;

                // delete from spiski, if not already
                if (coinBodies.contains(body, true)) coinBodies.removeValue(body, true);
                if (body.getUserData() instanceof Enemy) {
                    enemies.removeValue((Enemy)body.getUserData(), true);
                }
                world.destroyBody(body);
            }
        }
        coinBodies.clear();
        enemies.clear();


        if (world != null) world.dispose();
        if (debugRenderer != null) debugRenderer.dispose();
        if (map != null) map.dispose();
        if (mapRenderer != null) mapRenderer.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();

        world = null;
        debugRenderer = null;
        map = null;
        mapRenderer = null;
        shapeRenderer = null;
        contactListener = null;

        if (bodiesToRemove != null) bodiesToRemove.clear();

        Gdx.app.log("GameScreen", "dispose() finished");
    }
}
