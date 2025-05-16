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
    public static final float PROJECTILE_DAMAGE = 20f; // Damage from projectile

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
        return fixture != null && fixture.getUserData() != null && fixture.getUserData().equals("player");
    }

    private boolean isPlayerFeetFixture(Fixture fixture) {
        return fixture != null && fixture.getUserData() != null && fixture.getUserData().equals("playerFeet");
    }

    private boolean isGroundFixture(Fixture fixture) {
        return fixture != null && fixture.getUserData() != null && fixture.getUserData().equals("ground");
    }

    private boolean isCoinFixture(Fixture fixture) {
        return fixture != null && fixture.getUserData() != null && fixture.getUserData().equals("coin");
    }

    private boolean isEnemyFixture(Fixture fixture) {
        if (fixture == null) return false;
        return fixture.getBody() != null && fixture.getBody().getUserData() instanceof Enemy;
    }

    // Helper to check if fixture's UserData is a Projectile instance
    private boolean isProjectileFixture(Fixture fixture) {
        return fixture != null && fixture.getUserData() instanceof Projectile;
    }


    @Override
    public void beginContact(Contact contact) {
        Fixture fixA = contact.getFixtureA();
        Fixture fixB = contact.getFixtureB();

        // Player - Projectile Collision
        Projectile projectile = null;
        Fixture projectileFixture = null;
        Fixture playerContactFixture = null; // The fixture of the player that made contact

        if (isProjectileFixture(fixA) && (isPlayerFixture(fixB) || isPlayerFeetFixture(fixB))) {
            projectile = (Projectile) fixA.getUserData();
            projectileFixture = fixA;
            playerContactFixture = fixB;
        } else if (isProjectileFixture(fixB) && (isPlayerFixture(fixA) || isPlayerFeetFixture(fixA))) {
            projectile = (Projectile) fixB.getUserData();
            projectileFixture = fixB;
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
        Fixture playerCoinF = null;
        Fixture coinItselfF = null;
        if (isPlayerFixture(fixA) && isCoinFixture(fixB)) {
            playerCoinF = fixA;
            coinItselfF = fixB;
        } else if (isPlayerFixture(fixB) && isCoinFixture(fixA)) {
            playerCoinF = fixB;
            coinItselfF = fixA;
        }

        if (playerCoinF != null && coinItselfF != null) {
            Gdx.app.log("CONTACT", "Player touched a coin!");
            if (gameScreen != null) {
                gameScreen.collectCoin();
                gameScreen.scheduleBodyForRemoval(coinItselfF.getBody());
            }
        }

        // player - key
        if (checkContact(fixA, fixB, "player", "key") || checkContact(fixA, fixB, "playerFeet", "key")) {
            Fixture keyFixture = getFixtureByUserData(fixA, fixB, "key");
            if (keyFixture != null && keyFixture.getBody() == gameScreen.keyBody) {
                Gdx.app.log("CONTACT", "Player picked up the key!");
                gameScreen.playerHasKey = true;
                if (gameScreen.keyBody != null) {
                    gameScreen.scheduleBodyForRemoval(gameScreen.keyBody);
                    gameScreen.keyBody = null;
                }
                // TODO: sound
            }
        }

        //player - door
        Object userDataA = fixA.getUserData();
        Object userDataB = fixB.getUserData();
        GameScreen.DoorData contactedDoor = null;
        Fixture playerDoorContactFixture = null;

        if (userDataA instanceof GameScreen.DoorData && (isPlayerFixture(fixB) || isPlayerFeetFixture(fixB))) {
            contactedDoor = (GameScreen.DoorData) userDataA;
            playerDoorContactFixture = fixB;
        } else if (userDataB instanceof GameScreen.DoorData && (isPlayerFixture(fixA) || isPlayerFeetFixture(fixA))) {
            contactedDoor = (GameScreen.DoorData) userDataB;
            playerDoorContactFixture = fixA;
        }

        if (contactedDoor != null && player != null && !player.isDead()) {
            Gdx.app.log("CONTACT", "Player at door. Locked: " + contactedDoor.isLocked + ", HasKey: " + gameScreen.playerHasKey);
            if (contactedDoor.isLocked) {
                if (gameScreen.playerHasKey) {
                    Gdx.app.log("DOOR", "Door unlocked and opened by player!");
                    contactedDoor.isOpen = true;
                    contactedDoor.isLocked = false;
                    loadNextLevel(contactedDoor.nextLevelAsset);
                } else {
                    Gdx.app.log("DOOR", "Door is locked. Player needs a key.");
                }
            } else {
                Gdx.app.log("DOOR", "Player entered an unlocked door.");
                contactedDoor.isOpen = true;
                loadNextLevel(contactedDoor.nextLevelAsset);
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
                        contact.setEnabled(false); // prevent interaction with stomped/removed enemy
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
        // Тут має бути логіка переходу. Наприклад:
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

        // Player-Coin
        if ((isPlayerFixture(fixA) && isCoinFixture(fixB)) ||
            (isPlayerFixture(fixB) && isCoinFixture(fixA))) {
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
            // If projectile is already scheduled for removal, disable contacts
            if (projectileInstance.isScheduledForRemoval()) {
                contact.setEnabled(false);
                return;
            }

            // projectile - ground
             Fixture other = (projectileInstance == fixA.getUserData()) ? fixB : fixA;
             if (isGroundFixture(other)) {
                projectileInstance.scheduleForRemoval();
                contact.setEnabled(false);
             }
        }
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {}

    public boolean isPlayerOnGround() {
        return playerIsOnGround;
    }
}
