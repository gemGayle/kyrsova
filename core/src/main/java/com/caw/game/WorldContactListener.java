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

    private boolean isFeetContactingGround(Fixture fixA, Fixture fixB) {
        Object dataA = fixA.getUserData();
        Object dataB = fixB.getUserData();

        if (dataA == null || dataB == null || !(dataA instanceof String) || !(dataB instanceof String)) {
            return false;
        }

        String strA = (String) dataA;
        String strB = (String) dataB;

        return (strA.equals("playerFeet") && strB.equals("ground")) ||
            (strA.equals("ground") && strB.equals("playerFeet"));
    }

    @Override
    public void beginContact(Contact contact) {
        Fixture fixA = contact.getFixtureA();
        Fixture fixB = contact.getFixtureB();

        if (isFeetContactingGround(fixA, fixB)) {
            footContacts++;
            playerIsOnGround = true;
            Gdx.app.log("CONTACT", "Player feet touching ground. Contacts: " + footContacts); // Логування LibGDX
        }

    }

    @Override
    public void endContact(Contact contact) {
        Fixture fixA = contact.getFixtureA();
        Fixture fixB = contact.getFixtureB();

        if (isFeetContactingGround(fixA, fixB)) {
            if (footContacts > 0) {
                footContacts--;
            }
            if (footContacts == 0) {
                playerIsOnGround = false;
                Gdx.app.log("CONTACT", "Player feet left ground. Contacts: " + footContacts);
            } else {
                Gdx.app.log("CONTACT", "Feet contact ended, but others remain. Contacts: " + footContacts);
            }
        }


    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {

    }

    public boolean isPlayerOnGround() {
        return playerIsOnGround;
    }
}
