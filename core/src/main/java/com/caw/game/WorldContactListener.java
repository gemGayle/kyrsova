package com.caw.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;

public class WorldContactListener implements ContactListener {
    private int footContacts = 0;
    private boolean playerIsOnGround = false;
    private GameScreen gameScreen;
    private Player player;

    public static final float ENEMY_DAMAGE = 25f;
    public static final float PROJECTILE_DAMAGE = 25f;

    public WorldContactListener(GameScreen gameScreen) {
        this.gameScreen = gameScreen;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    private boolean isType(Fixture fixture, String type) {
        return fixture != null && fixture.getUserData() != null && fixture.getUserData().equals(type);
    }

    private boolean isPlayerFixture(Fixture fixture) {
        return fixture != null && fixture.getUserData() != null && "player".equals(fixture.getUserData().toString());
    }

    private boolean isSurfaceType(Fixture fixture, String type) {
        return fixture != null && fixture.getUserData() != null && type.equals(fixture.getUserData().toString());
    }

    private boolean isPlayerFeetFixture(Fixture fixture) {
        return fixture != null && fixture.getUserData() != null && fixture.getUserData().equals("playerFeet");
    }

    private boolean isGroundFixture(Fixture fixture) {
        return fixture != null && fixture.getUserData() != null && fixture.getUserData().equals("ground");
    }

    private boolean isWallFixture(Fixture fixture) {
        return fixture != null && fixture.getUserData() != null && "wall".equals(fixture.getUserData().toString());
    }

    private boolean isCoinFixture(Fixture fixture) {
        return fixture != null && fixture.getUserData() != null &&
            "coin_fixture".equals(fixture.getUserData().toString());
    }

    private boolean isEnemyFixture(Fixture fixture) {
        if (fixture == null) return false;
        return fixture.getBody() != null && fixture.getBody().getUserData() instanceof Enemy;
    }

    private boolean isProjectileFixture(Fixture fixture) {
        return fixture != null && fixture.getUserData() instanceof Projectile;
    }


    @Override
    public void beginContact(Contact contact) {
        Fixture fixA = contact.getFixtureA();
        Fixture fixB = contact.getFixtureB();

        // Player - Projectile Collision
        Projectile projectile = null;
        Fixture playerContactFixture = null; // The fixture of the player that made contact

        if (isProjectileFixture(fixA) && (isPlayerFixture(fixB) || isPlayerFeetFixture(fixB))) {
            projectile = (Projectile) fixA.getUserData();
            playerContactFixture = fixB;
        } else if (isProjectileFixture(fixB) && (isPlayerFixture(fixA) || isPlayerFeetFixture(fixA))) {
            projectile = (Projectile) fixB.getUserData();
            playerContactFixture = fixA;
        }

        if (projectile != null && playerContactFixture != null) {
            if (player != null && !player.isDead() && !projectile.isScheduledForRemoval()) {
                // Check for player immune
                if (player.getInvulnerabilityTimer() > 0) {
                    Gdx.app.log("CONTACT", "Player invulnerable, projectile hit ignored by player.");
                } else {
                    Gdx.app.log("CONTACT", "Player (" + playerContactFixture.getUserData() + ") hit by projectile.");
                    player.takeDamage(PROJECTILE_DAMAGE, projectile.getBody());
                    projectile.scheduleForRemoval(); // mark projectile for removal
                }
                contact.setEnabled(false);
            } else if (projectile.isScheduledForRemoval()) {
                // If projectile is already marked for removal
                contact.setEnabled(false);
            } else if (player != null && player.isDead() && !projectile.isScheduledForRemoval()) {
                contact.setEnabled(false); // prevent interaction with dead player's body
            }
        }

        // player - ground
        if ((isPlayerFeetFixture(fixA) && isGroundFixture(fixB)) ||
            (isPlayerFeetFixture(fixB) && isGroundFixture(fixA))) {
            footContacts++;
            playerIsOnGround = true;
        }

        // player - coin
        Fixture coinBodyFixture = null;

        if ((isPlayerFixture(fixA) || isPlayerFeetFixture(fixA)) && isCoinFixture(fixB)) {
            playerContactFixture = fixA;
            coinBodyFixture = fixB;
        } else if ((isPlayerFixture(fixB) || isPlayerFeetFixture(fixB)) && isCoinFixture(fixA)) {
            playerContactFixture = fixB;
            coinBodyFixture = fixA;
        }

        if (playerContactFixture != null && coinBodyFixture != null) {
            Object coinBodyUserData = coinBodyFixture.getBody().getUserData();
            if (coinBodyUserData instanceof Coin) {
                Coin contactedCoin = (Coin) coinBodyUserData;
                if (!contactedCoin.isScheduledForRemoval()) {
                    Gdx.app.log("CONTACT", "Player touched an animated coin!");
                    gameScreen.collectCoin();
                    // coin sound
                    if (gameScreen != null) gameScreen.playCoinPickupSound();
                    contactedCoin.scheduleForRemoval();
                    gameScreen.scheduleBodyForRemoval(contactedCoin.getBody());
                }
            } else {
                Gdx.app.error("CoinContact", "Coin fixture detected, but body UserData is not a Coin object: " + coinBodyUserData);
                if (coinBodyFixture.getBody().getUserData() != null && "coin_body".equals(coinBodyFixture.getBody().getUserData().toString())) {
                    Gdx.app.log("CONTACT", "Player touched a legacy coin_body!");
                    gameScreen.collectCoin();
                    gameScreen.scheduleBodyForRemoval(coinBodyFixture.getBody());
                }
            }
        }

        // player - key
        if (checkContact(fixA, fixB, "player", "key") || checkContact(fixA, fixB, "playerFeet", "key")) {
            Fixture keyFixture = getFixtureByUserData(fixA, fixB, "key");
            if (keyFixture != null && keyFixture.getBody() == gameScreen.keyBody) {
                Gdx.app.log("CONTACT", "Player picked up the key!");
                gameScreen.playerHasKey = true;
                if (gameScreen != null) gameScreen.playKeyPickupSound();
                if (gameScreen.keyBody != null) {
                    gameScreen.scheduleBodyForRemoval(gameScreen.keyBody);
                    gameScreen.keyBody = null;
                }
            }
        }

        //player - door
        Object userDataBodyA = fixA.getBody().getUserData();
        Object userDataBodyB = fixB.getBody().getUserData();
        GameScreen.DoorData contactedDoor = null;
        Fixture playerFixture = null;
        Fixture doorContactedFixture = null;

        if (userDataBodyA instanceof GameScreen.DoorData) {
            contactedDoor = (GameScreen.DoorData) userDataBodyA;
            doorContactedFixture = fixA;
            playerFixture = fixB;
            if (contactedDoor.body != fixA.getBody()) {
                Gdx.app.error("DOOR_ASSERTION_FAIL", "DoorData on bodyA, but contactedDoor.body != fixA.getBody()");
            }
        } else if (userDataBodyB instanceof GameScreen.DoorData) {
            contactedDoor = (GameScreen.DoorData) userDataBodyB;
            doorContactedFixture = fixB;
            playerFixture = fixA;
            if (contactedDoor.body != fixB.getBody()) {
                Gdx.app.error("DOOR_ASSERTION_FAIL", "DoorData on bodyB, but contactedDoor.body != fixB.getBody()");
            }
        }

        // is other object a player?
        if (contactedDoor != null && playerFixture != null &&
            !(isPlayerFixture(playerFixture) || isPlayerFeetFixture(playerFixture))) {
            Gdx.app.log("DOOR_CONTACT_DEBUG", "Door contact detected, but other fixture is not player. Other fixture UserData: " + (playerFixture.getUserData() != null ? playerFixture.getUserData().toString() : "null"));
            contactedDoor = null;
            doorContactedFixture = null;
        }


        if (contactedDoor != null && player != null && !player.isDead() && doorContactedFixture != null) {
            Gdx.app.log("CONTACT_DOOR", "Player at door. Door Locked: " + contactedDoor.isLocked +
                ", Player HasKey: " + gameScreen.playerHasKey +
                ", Door Fixture IsSensor (at beginContact): " + doorContactedFixture.isSensor());

            if (doorContactedFixture.isSensor()) {
                Gdx.app.log("DOOR_PATH", "Path A: Door is SENSOR.");
                Gdx.app.log("DOOR_INTERACTION", "Player contacting SENSOR door. Transitioning.");
                if (!contactedDoor.isOpen) {
                    contactedDoor.isOpen = true;
                }
                loadNextLevel(contactedDoor.nextLevelAsset);
            } else {
                Gdx.app.log("DOOR_PATH", "Path B: Door is SOLID.");
                if (contactedDoor.isLocked) {
                    Gdx.app.log("DOOR_PATH", "Path B1: Door is SOLID and LOCKED.");
                    if (gameScreen.playerHasKey) {
                        Gdx.app.log("DOOR_PATH", "Path B1a: Door is SOLID, LOCKED, and PLAYER HAS KEY.");
                        Gdx.app.log("DOOR_INTERACTION", "Player has key for SOLID/LOCKED door. Unlocking.");
                        contactedDoor.isLocked = false;
                        contactedDoor.isOpen = true;

                        gameScreen.scheduleFixtureToMakeSensor(doorContactedFixture);
                        Gdx.app.log("DOOR_PHYSICS", "Door fixture on body for DoorData (" + contactedDoor.nextLevelAsset +
                            ") scheduled to become SENSOR.");
                    } else {
                        Gdx.app.log("DOOR_PATH", "Path B1b: Door is SOLID, LOCKED, but PLAYER NO KEY.");
                        Gdx.app.log("DOOR_INTERACTION", "Player at SOLID/LOCKED door, NO KEY. Player bumps.");
                    }
                } else {
                    Gdx.app.log("DOOR_PATH", "Path B2: Door is SOLID but NOT LOCKED (isLocked=false).");
                    Gdx.app.error("DOOR_LOGIC_ERROR", "Door fixture is SOLID, but DoorData.isLocked is FALSE. Scheduling to become sensor.");
                    if (!contactedDoor.isOpen) contactedDoor.isOpen = true;
                    gameScreen.scheduleFixtureToMakeSensor(doorContactedFixture);
                }
            }
            }

        // player - enemy
        Fixture playerSensorForEnemy = null;
        Fixture playerMainForEnemy = null;
        Fixture enemyBodyFixture = null;

        if (isEnemyFixture(fixA)) {
            enemyBodyFixture = fixA;
            if (isPlayerFeetFixture(fixB)) playerSensorForEnemy = fixB;
            if (isPlayerFixture(fixB)) playerMainForEnemy = fixB;
        } else if (isEnemyFixture(fixB)) {
            enemyBodyFixture = fixB;
            if (isPlayerFeetFixture(fixA)) playerSensorForEnemy = fixA;
            if (isPlayerFixture(fixA)) playerMainForEnemy = fixA;
        }

        if (enemyBodyFixture != null && player != null && !player.isDead()) {
            Object enemyUserData = enemyBodyFixture.getBody().getUserData();
            if (enemyUserData instanceof Enemy) {
                Enemy enemy = (Enemy) enemyUserData;

                if (enemy.isStomped() || enemy.isScheduledForRemoval()) {
                    if (playerSensorForEnemy != null || playerMainForEnemy != null) {
                        contact.setEnabled(false);
                    }
                    return;
                }

                // player immune check for enemy contact
                if (player.getInvulnerabilityTimer() > 0) {
                    Gdx.app.log("CONTACT", "Player invulnerable to enemy, contact ignored.");
                    contact.setEnabled(false); // disable contact if player is immune
                    return;
                }

                // jump on enemy
                if (playerSensorForEnemy != null) {
                    float playerVelocityY = player.getBody().getLinearVelocity().y;
                    float feetSensorOffsetY = -Player.FRAME_HEIGHT / 2f / Player.PPM - (3 / Player.PPM);
                    float playerFeetActualY = player.getBody().getPosition().y + feetSensorOffsetY;
                    float enemyTopY = enemy.getBody().getPosition().y + (enemy.getVisualHeight() / 2f / GameScreen.PPM * 0.7f);

                    // check if player is fallin and is above the enemy's main body
                    if (playerVelocityY < -0.1f && playerFeetActualY > enemyTopY - (enemy.getVisualHeight() / 2f / GameScreen.PPM * 0.3f)) {
                        Gdx.app.log("STOMP_SUCCESS", "Player stomped enemy!");
                        enemy.onStomped();
                        player.getBody().setLinearVelocity(player.getBody().getLinearVelocity().x, 4.0f); // bounce
                        contact.setEnabled(false); // disable next interaction in this contact
                        return;
                    }
                }

                // if regular contact => damage to player
                if (playerMainForEnemy != null || playerSensorForEnemy != null) {
                    Gdx.app.log("CONTACT", "Player normal collision with enemy.");
                    player.takeDamage(ENEMY_DAMAGE, enemy.getBody());
                }
            }
        }
    }

    private boolean checkContact(Fixture a, Fixture b, String userDataA, String userDataB) {
        return (a.getUserData() != null && a.getUserData().equals(userDataA) && b.getUserData() != null && b.getUserData().equals(userDataB)) ||
            (a.getUserData() != null && a.getUserData().equals(userDataB) && b.getUserData() != null && b.getUserData().equals(userDataA));
    }

    private Fixture getFixtureByUserData(Fixture a, Fixture b, String userDataToFind) {
        if (a.getUserData() != null && a.getUserData().equals(userDataToFind)) return a;
        if (b.getUserData() != null && b.getUserData().equals(userDataToFind)) return b;
        return null;
    }

    private void loadNextLevel(String levelAsset) {
        Gdx.app.log("LEVEL_TRANSITION", "Loading next level: " + levelAsset);
        if ("main_menu".equals(levelAsset)) {
            gameScreen.game.setScreen(new MainMenuScreen(gameScreen.game));
        } else if (levelAsset.endsWith(".tmx")) {
            Gdx.app.log("LEVEL_TRANSITION", "Next level logic not fully implemented yet. Asset: " + levelAsset);
            // back to main menu for now
            gameScreen.game.setScreen(new MainMenuScreen(gameScreen.game));
        }
    }

    @Override
    public void endContact(Contact contact) {
        Fixture fixA = contact.getFixtureA();
        Fixture fixB = contact.getFixtureB();

        if ((isPlayerFeetFixture(fixA) && isGroundFixture(fixB)) ||
            (isPlayerFeetFixture(fixB) && isGroundFixture(fixA))) {
            if (footContacts > 0) {
                footContacts--;
            }
            if (footContacts == 0) {
                playerIsOnGround = false;
            }
        }
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
        Fixture fixA = contact.getFixtureA();
        Fixture fixB = contact.getFixtureB();

        boolean playerIsA = isPlayerFixture(fixA);
        boolean playerIsB = isPlayerFixture(fixB);

        // Player-Coin
        if (((isPlayerFixture(fixA) || isPlayerFeetFixture(fixA)) && isCoinFixture(fixB)) ||
            ((isPlayerFixture(fixB) || isPlayerFeetFixture(fixB)) && isCoinFixture(fixA))) {
            contact.setEnabled(false);
        }

        // Player-Enemy
        Fixture pF_enemy = null;
        Fixture eF_enemy = null;

        if (isEnemyFixture(fixA)) { eF_enemy = fixA; pF_enemy = fixB; }
        else if (isEnemyFixture(fixB)) { eF_enemy = fixB; pF_enemy = fixA; }

        if (pF_enemy != null && eF_enemy != null && (isPlayerFixture(pF_enemy) || isPlayerFeetFixture(pF_enemy))) {
            Object enemyData = eF_enemy.getBody().getUserData();
            if (enemyData instanceof Enemy) {
                Enemy enemy = (Enemy) enemyData;
                if (enemy.isStomped() || enemy.isScheduledForRemoval()) {
                    contact.setEnabled(false);
                } else if (player != null && player.getInvulnerabilityTimer() > 0) {
                    contact.setEnabled(false);
                }
            }
        }

        // projectile interactions in preSolve
        Projectile projectileInstance = null;

        if (isProjectileFixture(fixA)) {
            projectileInstance = (Projectile) fixA.getUserData();
        } else if (isProjectileFixture(fixB)) {
            projectileInstance = (Projectile) fixB.getUserData();
        }

        if (projectileInstance != null) {
            if (projectileInstance.isScheduledForRemoval()) {
                contact.setEnabled(false);
                return;
            }

             Fixture other = (projectileInstance == fixA.getUserData()) ? fixB : fixA;
             if (isGroundFixture(other)) {
                projectileInstance.scheduleForRemoval();
                contact.setEnabled(false);
             }
        }

        Fixture doorFixtureInPreSolve = null;
        GameScreen.DoorData doorDataForPreSolve = null;
        Fixture otherFixtureInPreSolve = null;

        Object bodyUserDataA_ps = fixA.getBody().getUserData();
        Object bodyUserDataB_ps = fixB.getBody().getUserData();

        if (bodyUserDataA_ps instanceof GameScreen.DoorData) {
            doorDataForPreSolve = (GameScreen.DoorData) bodyUserDataA_ps;
            if (doorDataForPreSolve.body == fixA.getBody()) {
                doorFixtureInPreSolve = fixA;
                otherFixtureInPreSolve = fixB;
            }
        } else if (bodyUserDataB_ps instanceof GameScreen.DoorData) {
            doorDataForPreSolve = (GameScreen.DoorData) bodyUserDataB_ps;
            if (doorDataForPreSolve.body == fixB.getBody()) {
                doorFixtureInPreSolve = fixB;
                otherFixtureInPreSolve = fixA;
            }
        }

        if (doorDataForPreSolve != null && doorFixtureInPreSolve != null && otherFixtureInPreSolve != null) {
            boolean otherIsPlayer = isPlayerFixture(otherFixtureInPreSolve) || isPlayerFeetFixture(otherFixtureInPreSolve);

            if (otherIsPlayer) {
                if (doorFixtureInPreSolve.isSensor() || (!doorDataForPreSolve.isLocked && doorDataForPreSolve.isOpen)) {
                    contact.setEnabled(false);
                }
            }
        }

        if (((playerIsA || (fixA.getUserData() != null && fixA.getUserData().equals("playerFeet"))) && isSurfaceType(fixB, "coin_fixture")) ||
            ((playerIsB || (fixB.getUserData() != null && fixB.getUserData().equals("playerFeet"))) && isSurfaceType(fixA, "coin_fixture"))) {
            contact.setEnabled(false);
        }

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {}

    public boolean isPlayerOnGround() {
        return playerIsOnGround;
    }
}
