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

    // Конструктор для передачі GameScreen
    public WorldContactListener(GameScreen gameScreen) {
        this.gameScreen = gameScreen;
    }

    public WorldContactListener() {
        this.gameScreen = null; // Або кидати виняток, якщо GameScreen обов'язковий
        Gdx.app.error("WorldContactListener", "Default constructor called, GameScreen will be null!");
    }


    private boolean isType(Fixture fixture, String type) {
        return fixture != null && fixture.getUserData() != null && fixture.getUserData().equals(type);
    }

    @Override
    public void beginContact(Contact contact) {
        Fixture fixA = contact.getFixtureA();
        Fixture fixB = contact.getFixtureB();

        // Перевірка контакту ніг гравця з землею
        if ((isType(fixA, "playerFeet") && isType(fixB, "ground")) ||
            (isType(fixA, "ground") && isType(fixB, "playerFeet"))) {
            footContacts++;
            playerIsOnGround = true;
            Gdx.app.log("CONTACT", "Player feet touching ground. Contacts: " + footContacts);
        }

        // Перевірка контакту з монетою
        Fixture playerFixture = null;
        Fixture coinFixture = null;

        if (isType(fixA, "player") && isType(fixB, "coin")) {
            playerFixture = fixA;
            coinFixture = fixB;
        } else if (isType(fixA, "coin") && isType(fixB, "player")) {
            playerFixture = fixB;
            coinFixture = fixA;
        }

        if (playerFixture != null && coinFixture != null) {
            Gdx.app.log("CONTACT", "Player touched a coin!");
            if (gameScreen != null) {
                gameScreen.collectCoin(); // Збільшуємо рахунок
                gameScreen.scheduleBodyForRemoval(coinFixture.getBody());
            } else {
                Gdx.app.error("CONTACT_LISTENER", "GameScreen is null, cannot process coin collection properly.");
            }
        }

        //Перевірка зіткення з ворогом
        if ((isType(fixA, "player") && isType(fixB, "enemy")) ||
            (isType(fixA, "enemy") && isType(fixB, "player"))) {

            Gdx.app.log("CONTACT", "Player hit an enemy!");
            if (gameScreen != null) {
                gameScreen.requestPlayerReset();
            }
        }
    }

    @Override
    public void endContact(Contact contact) {
        Fixture fixA = contact.getFixtureA();
        Fixture fixB = contact.getFixtureB();

        if ((isType(fixA, "playerFeet") && isType(fixB, "ground")) ||
            (isType(fixA, "ground") && isType(fixB, "playerFeet"))) {
            if (footContacts > 0) {
                footContacts--;
            }
            if (footContacts == 0) {
                playerIsOnGround = false;
            }
            // Gdx.app.log("CONTACT", "Player feet contact ended. Contacts: " + footContacts + ", onGround: " + playerIsOnGround);
            // Логування тут може бути надто частим, краще прибрати або зменшити
        }
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
        Fixture fixA = contact.getFixtureA();
        Fixture fixB = contact.getFixtureB();

        // Якщо гравець (не сенсор ніг) контактує з монетою, вимикаємо контакт,
        // щоб він не впливав на фізику гравця, оскільки монета і так сенсор.
        // Це може бути надлишковим, оскільки монета вже є сенсором,
        // але для певності можна залишити.
        if ((isType(fixA, "player") && isType(fixB, "coin")) ||
            (isType(fixA, "coin") && isType(fixB, "player"))) {
            contact.setEnabled(false);
        }
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {}

    public boolean isPlayerOnGround() {
        return playerIsOnGround;
    }
}
