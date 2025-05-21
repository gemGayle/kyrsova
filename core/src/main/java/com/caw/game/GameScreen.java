package com.caw.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
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
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.Input;

public class GameScreen implements Screen {
    final GameStart game;
    private AssetManager assetManager;
    private World world;
    private Box2DDebugRenderer debugRenderer;
    private WorldContactListener contactListener;

    //camera
    public static final float WORLD_WIDTH_PIXELS = 320f;
    public static final float WORLD_HEIGHT_PIXELS = 240f;
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

    public static final float PPM = 40;

    //coin
    private Texture coinTexture;
    private static final float COIN_SIZE = 8f;
    private Array<Body> coinBodies;
    private Array<Body> bodiesToRemove;
    private Array<Fixture> fixturesToMakeSensor;

    //anim coin
    private Texture coinAnimationSheetTexture;
    private Array<Coin> animatedCoins;
    private Texture enemyPatrolSheetTexture;
    private Texture shootingEnemySheetTexture;


    private int score = 0;
    private ShapeRenderer shapeRenderer;

    //enemy
    private Array<Enemy> enemies;
    private Array<ShootingEnemy> shootingEnemies;
    private Array<Projectile> projectiles;
    private Texture enemyTexture;
    private Texture shootingEnemyTexture;
    private Texture projectileTexture;
    private static final float ENEMY_VISUAL_WIDTH = 16f;
    private static final float ENEMY_VISUAL_HEIGHT = 16f;

    //shooting enemy
    private static final float SHOOTING_ENEMY_VISUAL_WIDTH = 16f;
    private static final float SHOOTING_ENEMY_VISUAL_HEIGHT = 16f;

    //key - door
    private Texture keyTexture;
    private Texture doorClosedTexture;
    private Texture doorOpenTexture;
    public Body keyBody;
    public boolean playerHasKey = false;
    private Vector2 keyPositionPixels;
    private Array<DoorData> doors;

    private boolean playerNeedsPositionReset = false; // reset pos WO death

    //sounds
    private Sound walkSound;
    private Sound jumpSound;
    private Sound playerHurtSound;
    private Sound playerDeathSound;
    private Sound enemyDeathSound;
    private Sound shootSound;
    private Sound coinPickupSound;
    private Sound keyPickupSound;

    // UI-UX
    private boolean isPaused = false;
    private Stage pauseGuiStage;
    private Skin uiSkin;
    private ShapeRenderer dimRenderer;

    public GameScreen(final GameStart game) {
        this.game = game;
        gameCamera = new OrthographicCamera();
        hudCamera = new OrthographicCamera();
//        hudCamera.setToOrtho(false, WORLD_WIDTH_PIXELS, WORLD_HEIGHT_PIXELS);
    }


    @Override
    public void show() {
        Gdx.app.log("GameScreen", "show() called");

        gameViewport = new com.badlogic.gdx.utils.viewport.ExtendViewport(WORLD_WIDTH_PIXELS, WORLD_HEIGHT_PIXELS, gameCamera);
        hudViewport = new com.badlogic.gdx.utils.viewport.ExtendViewport(WORLD_WIDTH_PIXELS, WORLD_HEIGHT_PIXELS, hudCamera);

        gameViewport.apply();
        hudViewport.apply();

        world = new World(new Vector2(0, -10f), true);
        debugRenderer = new Box2DDebugRenderer();
        contactListener = new WorldContactListener(this);
        world.setContactListener(contactListener);

        map = new TmxMapLoader().load("assets/lvl1_final.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map, 1); // map render

        // player textures
        playerIdleSheetTexture = new Texture(Gdx.files.internal("assets/skeleton_idle.png"));
        playerRunSheetTexture = new Texture(Gdx.files.internal("assets/skeleton_run.png"));
        playerJumpSheetTexture = new Texture(Gdx.files.internal("assets/player_jump.png"));
        playerFallSheetTexture = new Texture(Gdx.files.internal("assets/player_fall.png"));

        coinAnimationSheetTexture = new Texture(Gdx.files.internal("assets/coin_animation_sheet.png"));

        keyTexture = new Texture(Gdx.files.internal("assets/key.png"));
        doorClosedTexture = new Texture(Gdx.files.internal("assets/door_closed.png"));
        doorOpenTexture = new Texture(Gdx.files.internal("assets/door_open.png"));

        //enemy
        enemyPatrolSheetTexture = new Texture(Gdx.files.internal("assets/enemy_animation_sheet.png"));
        shootingEnemySheetTexture = new Texture(Gdx.files.internal("assets/shooting_enemy_animation_sheet.png"));

        enemyTexture = new Texture(Gdx.files.internal("assets/enemy.png"));
        shootingEnemyTexture = new Texture(Gdx.files.internal("assets/shooting_enemy.png"));
        projectileTexture = new Texture(Gdx.files.internal("assets/projectile.png"));

        //ui
        uiSkin = new Skin(Gdx.files.internal("assets/ui/uiskin.json"));

        try {
            uiSkin = new Skin(Gdx.files.internal("assets/ui/uiskin.json"));
            if (game.defaultFont != null) {
                if (uiSkin.has("default", TextButton.TextButtonStyle.class)) {
                    TextButton.TextButtonStyle buttonStyle = uiSkin.get("default", TextButton.TextButtonStyle.class);
                    buttonStyle.font = game.hudScoreFont;
                    buttonStyle.fontColor = Color.LIGHT_GRAY;
                }
            } else {
                Gdx.app.log("GameScreen", "game.defaultFont is null, pause menu UI font not changed.");
            }
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Could not load uiSkin for pause menu", e);
            uiSkin = new Skin();
        }

        //sounds
        try {
            walkSound = Gdx.audio.newSound(Gdx.files.internal("assets/sounds/walk.wav"));
            jumpSound = Gdx.audio.newSound(Gdx.files.internal("assets/sounds/jump.wav"));
            playerHurtSound = Gdx.audio.newSound(Gdx.files.internal("assets/sounds/player_hurt.wav"));
            playerDeathSound = Gdx.audio.newSound(Gdx.files.internal("assets/sounds/player_death.wav"));
            enemyDeathSound = Gdx.audio.newSound(Gdx.files.internal("assets/sounds/enemy_death.wav"));
            shootSound = Gdx.audio.newSound(Gdx.files.internal("assets/sounds/shoot.wav"));
            coinPickupSound = Gdx.audio.newSound(Gdx.files.internal("assets/sounds/coin.wav"));
            keyPickupSound = Gdx.audio.newSound(Gdx.files.internal("assets/sounds/key.wav"));
        } catch (Exception e) {
            Gdx.app.error("SoundLoader", "Error loading sounds", e);
            walkSound = jumpSound = playerHurtSound = playerDeathSound = enemyDeathSound = shootSound = coinPickupSound = keyPickupSound = null;
        }

        //ui
        pauseGuiStage = new Stage(hudViewport); //pause
        dimRenderer = new ShapeRenderer();

        shapeRenderer = new ShapeRenderer();
        animatedCoins = new Array<>();
        enemies = new Array<>();
        shootingEnemies = new Array<>();
        projectiles = new Array<>();
        doors = new Array<>();
        bodiesToRemove = new Array<>();
        fixturesToMakeSensor = new Array<>();

        score = 0;
        playerHasKey = false;

        findInitialPlayerSpawnPoint();
        player = new Player(world,
            contactListener,
            initialPlayerSpawnPointPixels,
            playerIdleSheetTexture,
            playerRunSheetTexture,
            playerJumpSheetTexture,
            playerFallSheetTexture,
            this);

        contactListener.setPlayer(player); // player go to contactListener

        createPhysicsFromMap();
        createCollectibles();
        createDoorsFromMap();
        createEnemiesFromMap();
        setupPauseUI();

        Gdx.app.log("GameScreen", "show() finished initialization");
    }

    private void setupPauseUI() {
        Table pauseTable = new Table();
        pauseTable.setFillParent(true);
        pauseTable.center();
        // pauseTable.setDebug(true);

        float buttonWidth = 130f;
        float buttonHeight = 20f;
        float padValue = 10f;

        TextButton resumeButton = new TextButton("Resume Game", uiSkin);
        resumeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                togglePause();
            }
        });

        TextButton restartButton = new TextButton("Restart Level", uiSkin);
        restartButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                togglePause(); //FIRST unpause!!!
                restartLevel();
            }
        });

        TextButton mainMenuButton = new TextButton("Main Menu", uiSkin);
        mainMenuButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                togglePause(); //unpause
                game.setScreen(new MainMenuScreen(game));
            }
        });

        pauseTable.add(resumeButton).width(buttonWidth).height(buttonHeight).padBottom(padValue).row();
        pauseTable.add(restartButton).width(buttonWidth).height(buttonHeight).padBottom(padValue).row();
        pauseTable.add(mainMenuButton).width(buttonWidth).height(buttonHeight).row();

        pauseGuiStage.addActor(pauseTable);
    }

    private void togglePause() {
        isPaused = !isPaused;
        if (isPaused) {
            Gdx.input.setInputProcessor(pauseGuiStage);
            Gdx.app.log("GameScreen", "Game Paused");
        } else {
            Gdx.input.setInputProcessor(null);
            Gdx.app.log("GameScreen", "Game Resumed");
        }
    }

    private void playSound(Sound sound) {
        if (sound != null) {
            sound.play(0.5f);
        }
    }
    private void playSound(Sound sound, float relativeVolume) {
        if (sound != null && game != null && game.prefs != null) {
            float masterVolume = game.prefs.getFloat("masterVolume", 0.5f);
            sound.play(masterVolume * relativeVolume);
        } else if (sound != null) {
            sound.play(0.5f * relativeVolume);
        }
    }

    public void playPlayerWalkSound() { playSound(walkSound, 0.2f); }
    public void playPlayerJumpSound() { playSound(jumpSound, 0.4f); }
    public void playPlayerHurtSound() { playSound(playerHurtSound, 0.5f); }
    public void playPlayerDeathSound() { playSound(playerDeathSound, 0.7f); }
    public void playEnemyDeathSound() { playSound(enemyDeathSound, 0.3f); }
    public void playShootSound() { playSound(shootSound, 0.4f); }
    public void playCoinPickupSound() { playSound(coinPickupSound, 0.3f); }
    public void playKeyPickupSound() { playSound(keyPickupSound); }

    public void scheduleFixtureToMakeSensor(Fixture fixture) {
        if (fixture != null && !fixturesToMakeSensor.contains(fixture, true)) {
            fixturesToMakeSensor.add(fixture);
        }
    }

    private void processPendingFixtureChanges() {
        if (world == null || world.isLocked() || fixturesToMakeSensor.isEmpty()) {
            return;
        }

        for (Fixture fixture : fixturesToMakeSensor) {
            if (fixture != null && fixture.getBody().isActive()) {
                fixture.setSensor(true);
                Object bodyUserData = fixture.getBody().getUserData();
                String bodyInfo = (bodyUserData instanceof DoorData) ? "Door" : String.valueOf(bodyUserData);
                Gdx.app.log("PHYSICS_CHANGE", "Fixture on body (" + bodyInfo + ") made sensor.");
            }
        }
        fixturesToMakeSensor.clear();
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

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            togglePause();
        }

        if (!isPaused) {
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

            //coins
            for (Coin coin : animatedCoins) {
                coin.update(delta);
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

            world.step(1 / 60f, 6, 2);
            removeScheduledBodies(); // remove bodies after world step
            processPendingFixtureChanges();
        } else {
            pauseGuiStage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        }

        // screen clear (BG)
        Gdx.gl.glClearColor(0f, 0f, 0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // cam update
        if (player != null && player.getBody() != null && !isPaused) {
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

        for (Coin coin : animatedCoins) coin.render(game.batch);

        for (Enemy enemy : enemies) enemy.draw(game.batch);

        for (ShootingEnemy sEnemy : shootingEnemies) sEnemy.draw(game.batch);

        for (Projectile projectile : projectiles) projectile.render(game.batch);

        if (player != null) player.render(game.batch);

        //key
        if (keyBody != null && keyBody.isActive() && keyTexture != null) {
            float keyDrawWidth = 8f;
            float keyDrawHeight = 8f;
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
        game.batch.end();

        // debug info Box2d (HITBOXES)
//        if (debugRenderer != null) {
//            debugRenderer.render(world, gameCamera.combined.cpy().scl(PPM));
//        }

        if (isPaused) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            dimRenderer.setProjectionMatrix(hudCamera.combined);
            dimRenderer.begin(ShapeRenderer.ShapeType.Filled);
            dimRenderer.setColor(0f, 0f, 0f, 0.5f);
            dimRenderer.rect(0, 0, hudViewport.getWorldWidth(), hudViewport.getWorldHeight());
            dimRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            pauseGuiStage.draw();
        }

        // HUD
        hudCamera.update();

        // Health Bar
        if (player != null && shapeRenderer != null) {
            float healthBarWidth = 80f;
            float healthBarHeight = 10f;
            float healthBarX = 10f;
            float healthBarY = hudCamera.viewportHeight - healthBarHeight - 10f;

            shapeRenderer.setProjectionMatrix(hudCamera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1f); //hp bg
            shapeRenderer.rect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);

            float currentHealthWidth = healthBarWidth * (player.getCurrentHealth() / player.getMaxHealth());
            if (currentHealthWidth < 0) currentHealthWidth = 0;
            shapeRenderer.setColor(1f, 0f, 0.25f, 1f); //hp
            shapeRenderer.rect(healthBarX, healthBarY, currentHealthWidth, healthBarHeight);
            shapeRenderer.end();
        }

        // text HUD
        game.batch.setProjectionMatrix(hudCamera.combined);
        game.batch.begin();

        float hudPadding = 10f;
        float actualHealthBarHeight = 10f;
        game.hudScoreFont.getData().setScale(0.5f);

        game.hudScoreFont.getCapHeight();   //High letters
        game.hudScoreFont.getLineHeight();  //else letters

        float scoreTextY = hudCamera.viewportHeight - actualHealthBarHeight - hudPadding - game.hudScoreFont.getCapHeight() - 5f;

        game.hudScoreFont.draw(game.batch, "Score: " + score, hudPadding, scoreTextY);

        if (player != null && player.isDead() && !isPaused) {
            if (game.defaultFont != null) {
                String gameOverText = "GAME OVER";
                com.badlogic.gdx.graphics.g2d.GlyphLayout layoutGO = new com.badlogic.gdx.graphics.g2d.GlyphLayout(game.defaultFont, gameOverText);
                game.defaultFont.draw(game.batch, gameOverText, (hudCamera.viewportWidth - layoutGO.width) / 2, hudCamera.viewportHeight / 2 + layoutGO.height + 30);
            }

            if (game.hudScoreFont != null) {
                String restartText = "Press 'R' to Restart";
                com.badlogic.gdx.graphics.g2d.GlyphLayout layoutR = new com.badlogic.gdx.graphics.g2d.GlyphLayout(game.hudScoreFont, restartText);
                game.hudScoreFont.draw(game.batch, restartText, (hudCamera.viewportWidth - layoutR.width) / 2, hudCamera.viewportHeight / 2 - 10);

                String menuText = "Press 'M' for Main Menu";
                com.badlogic.gdx.graphics.g2d.GlyphLayout layoutM = new com.badlogic.gdx.graphics.g2d.GlyphLayout(game.hudScoreFont, menuText);
                game.hudScoreFont.draw(game.batch, menuText, (hudCamera.viewportWidth - layoutM.width) / 2, hudCamera.viewportHeight / 2 - 10 - layoutR.height - 5);
            }
        }

        //key hood
        if (playerHasKey && keyTexture != null) {
            float keyIconSize = 24f;
            game.batch.draw(keyTexture, hudCamera.viewportWidth - keyIconSize, hudCamera.viewportHeight - keyIconSize, keyIconSize, keyIconSize);
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
            Object userData = body.getUserData();

            if (userData instanceof Coin){
                Coin coinToRemove = (Coin) userData;
                animatedCoins.removeValue(coinToRemove, true);
                Gdx.app.log("Removal", "Scheduled Coin object removed from list.");
            }

            if (body.getUserData() instanceof Enemy) {
                Enemy enemy = (Enemy) body.getUserData();
                enemies.removeValue(enemy, true);
            }

            if (body.getUserData() instanceof ShootingEnemy) {
                ShootingEnemy sEnemy = (ShootingEnemy) body.getUserData();

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

        for (Coin coin : animatedCoins) {
            if (coin.getBody() != null && coin.getBody().isActive()) {
                bodiesToDestroy.add(coin.getBody());
            }
        }


        for (Enemy enemy : enemies) {
            if (enemy.body != null) bodiesToDestroy.add(enemy.body);
        }

        for (Body b : bodiesToDestroy) {
            if (!world.isLocked()) {
                for (int i = animatedCoins.size - 1; i >= 0; i--) {
                    Coin c = animatedCoins.get(i);
                    if (c.getBody() == b) {
                        animatedCoins.removeIndex(i);
                        break;
                    }
                }
                world.destroyBody(b);
            } else {
                Gdx.app.error("RESTART", "World locked, cannot destroy body " + b.getUserData());
            }
        }
        bodiesToDestroy.clear();

        for (Projectile p : projectiles) {
            if (p.getBody() != null && p.getBody().isActive()) {
                if (!world.isLocked()) world.destroyBody(p.getBody());
            }
        }

        //key reset
        playerHasKey = false;
        if (keyBody != null && !keyBody.isActive()) {
        }

        animatedCoins.clear();
        doors.clear();
        projectiles.clear();
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

                // get surface_type from Tiles
                String surfaceType = object.getProperties().get("surface_type", "ground", String.class); // ground default

                if ("wall".equals(surfaceType)) {
                    fixtureDef.friction = 0.00f;
                } else {
                    fixtureDef.friction = 0.8f;
                }
                body.createFixture(fixtureDef).setUserData(surfaceType);
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
                    shape.setAsBox(Coin.VISUAL_COIN_SIZE / 2f / PPM, Coin.VISUAL_COIN_SIZE / 2f / PPM);
                    fixtureDef.shape = shape;
                    fixtureDef.isSensor = true;
                    coinBody.createFixture(fixtureDef).setUserData("coin_fixture");

                    if (coinAnimationSheetTexture != null) {
                        Coin animatedCoin = new Coin(coinBody, coinAnimationSheetTexture);
                        animatedCoins.add(animatedCoin);
                    } else {
                        Gdx.app.error("CoinCreation", "Coin animation sheet is null. Cannot create animated coin.");
                        world.destroyBody(coinBody);
                    }
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

        enemyShape.setAsBox(ENEMY_VISUAL_WIDTH / 2f / PPM, (ENEMY_VISUAL_HEIGHT - 4) / 2f / PPM);

        FixtureDef enemyFixtureDef = new FixtureDef();
        enemyFixtureDef.shape = enemyShape;
        enemyFixtureDef.density = 0.8f;
        enemyFixtureDef.friction = 0.4f;
        enemyFixtureDef.restitution = 0.0f;


        for (MapObject object : enemyLayer.getObjects()) {
            String type = object.getProperties().get("type", String.class);
            if (type == null) continue;

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
                if (enemyPatrolSheetTexture == null) {
                    Gdx.app.error("ENEMY_FACTORY", "Enemy patrol sheet texture is null, cannot create regular enemy.");
                    continue;
                }
                float patrolDistPixels = object.getProperties().get("patrolDistance", 64f, Float.class);

                enemyBodyDef.position.set(xPixels / PPM, yPixels / PPM);
                Body regEnemyBody = world.createBody(enemyBodyDef);
                regEnemyBody.createFixture(enemyFixtureDef).setUserData("enemy_fixture");

                Enemy enemy = new Enemy(regEnemyBody, enemyPatrolSheetTexture,
                    xPixels, patrolDistPixels,
                    ENEMY_VISUAL_WIDTH, ENEMY_VISUAL_HEIGHT, this);
                enemies.add(enemy);
                Gdx.app.log("GAME_SETUP", "Created animated patrolling enemy from map at " + xPixels + "," + yPixels);


            } else if ("shooting_enemy".equals(type)) {
                if (shootingEnemySheetTexture == null || projectileTexture == null) {
                    Gdx.app.error("ENEMY_FACTORY", "Shooting enemy animation sheet or projectile texture is null.");
                    continue;
                }

                float detectionRadiusPixels = object.getProperties().get("detectionRadius", ShootingEnemy.DETECTION_RADIUS * PPM, Float.class);
                float shootCooldownSeconds = object.getProperties().get("shootCooldown", ShootingEnemy.SHOOT_COOLDOWN, Float.class);

                ShootingEnemy sEnemy = new ShootingEnemy(world, this,
                    shootingEnemySheetTexture,
                    projectileTexture,
                    xPixels, yPixels,
                    SHOOTING_ENEMY_VISUAL_WIDTH, SHOOTING_ENEMY_VISUAL_HEIGHT,
                    detectionRadiusPixels, shootCooldownSeconds);

                shootingEnemies.add(sEnemy);
                Gdx.app.log("GAME_SETUP", "CREATED animated shooting enemy. Total shooting enemies: " + shootingEnemies.size);
                if (sEnemy.body == null) {
                    Gdx.app.error("EnemyFactory", "Shooting enemy body is NULL after creation!");
                } else {
                    Gdx.app.log("EnemyFactory", "Shooting enemy body position: " + sEnemy.body.getPosition());
                    if (sEnemy.getActiveAnimation() == null) {
                        Gdx.app.error("EnemyFactory", "Shooting enemy ACTIVE ANIMATION is NULL after creation!");
                    }
                }

                Gdx.app.log("GAME_SETUP", "Created animated shooting enemy from map at " + xPixels + "," + yPixels);
                Gdx.app.log("EnemyFactory", "MATCHED type 'shooting_enemy' at x: " + xPixels + ", y: " + yPixels);
                Gdx.app.log("EnemyFactory", "Attempting to create shooting_enemy. Sheet loaded: "
                    + (shootingEnemySheetTexture != null)
                    + ", Projectile tex loaded: " + (projectileTexture != null));
            }

            Gdx.app.log("EnemyFactory", "Processing object with type: " + type + " at X: "
                + object.getProperties().get("x", Float.class)
                + ", Y: " + object.getProperties().get("y", Float.class) );
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

                String nextLevel = object.getProperties().get("nextLevel", "main_menu", String.class); //na main menu
                boolean initiallyLocked = object.getProperties().get("initiallyLocked", true, Boolean.class);

                DoorData doorData = new DoorData(doorBody, new Rectangle(rect), nextLevel, initiallyLocked);

                Fixture doorFixture;
                if (initiallyLocked) {
                    fixtureDef.isSensor = false;
                    doorFixture = doorBody.createFixture(fixtureDef);
                } else {
                    fixtureDef.isSensor = true;
                    doorFixture = doorBody.createFixture(fixtureDef);
                    doorData.isOpen = true;
                }

                doors.add(doorData);

                Gdx.app.log("GAME_SETUP", "Door created leading to: " + nextLevel +
                    ", InitiallyLocked: " + initiallyLocked +
                    ", IsSensor: " + doorFixture.isSensor());
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

        if (hudViewport != null) {
            hudViewport.update(width, height, true);
        }

        if (pauseGuiStage != null) {
            pauseGuiStage.getViewport().update(width,height,true);
        }
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
        if (coinAnimationSheetTexture != null) coinAnimationSheetTexture.dispose();
        if (enemyPatrolSheetTexture != null) enemyPatrolSheetTexture.dispose();
        if (shootingEnemySheetTexture != null) shootingEnemySheetTexture.dispose();
        if (projectileTexture != null) projectileTexture.dispose();
        if (keyTexture != null) keyTexture.dispose();
        if (doorClosedTexture != null) doorClosedTexture.dispose();
        if (doorOpenTexture != null) doorOpenTexture.dispose();

        //sounds
        if (walkSound != null) walkSound.dispose();
        if (jumpSound != null) jumpSound.dispose();
        if (playerHurtSound != null) playerHurtSound.dispose();
        if (playerDeathSound != null) playerDeathSound.dispose();
        if (enemyDeathSound != null) enemyDeathSound.dispose();
        if (shootSound != null) shootSound.dispose();
        if (coinPickupSound != null) coinPickupSound.dispose();
        if (keyPickupSound != null) keyPickupSound.dispose();


        playerIdleSheetTexture = null;
        playerRunSheetTexture = null;
        playerJumpSheetTexture = null;
        playerFallSheetTexture = null;
        coinAnimationSheetTexture = null;
        enemyTexture = null;
        enemyPatrolSheetTexture = null;
        shootingEnemySheetTexture = null;

        if (player != null) {
            player.dispose();
            player = null;
        }

        if (world != null && !world.isLocked()) {
            for (Coin coin : animatedCoins) {
                if (coin.getBody() != null && coin.getBody().isActive()) {
                    world.destroyBody(coin.getBody());
                }
            }
        }
        animatedCoins.clear();

        if (world != null && !world.isLocked()) {
            Array<Body> allBodies = new Array<>();
            world.getBodies(allBodies);
            for (Body body : allBodies) {
                if (player != null && body == player.getBody()) continue;

                if (coinBodies.contains(body, true)) coinBodies.removeValue(body, true);
                if (body.getUserData() instanceof Enemy) {
                    enemies.removeValue((Enemy)body.getUserData(), true);
                }
                world.destroyBody(body);
            }
        }
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
