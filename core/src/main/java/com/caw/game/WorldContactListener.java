package com.caw.game;

import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;

import javax.swing.*;

public class WorldContactListener implements ContactListener {
    private int groundContacts = 0;

    @Override
    public void beginContact(Contact contact) {
        Fixture fixA = contact.getFixtureA();
        Fixture fixB = contact.getFixtureB();

        boolean isPlayerA = fixA.getBody().getUserData() != null && fixA.getBody().getUserData().equals("player");
        boolean isGroundA = fixA.getBody().getUserData() != null && fixA.getBody().getUserData().equals("ground");
        boolean isPlayerB = fixB.getBody().getUserData() != null && fixB.getBody().getUserData().equals("player");
        boolean isGroundB = fixB.getBody().getUserData() != null && fixB.getBody().getUserData().equals("ground");

        if ((isPlayerA && isGroundB) || (isPlayerB && isGroundA)){
            groundContacts++;
            System.out.println("player touched the ground: " + groundContacts);
        }

    }

    @Override
    public void endContact(Contact contact) {
        Fixture fixA = contact.getFixtureA();
        Fixture fixB = contact.getFixtureB();

        boolean isPlayerA = fixA.getBody().getUserData() != null && fixA.getBody().getUserData().equals("player");
        boolean isGroundA = fixA.getBody().getUserData() != null && fixA.getBody().getUserData().equals("ground");
        boolean isPlayerB = fixB.getBody().getUserData() != null && fixB.getBody().getUserData().equals("player");
        boolean isGroundB = fixB.getBody().getUserData() != null && fixB.getBody().getUserData().equals("ground");

        if ((isPlayerA && isGroundB) || (isPlayerB && isGroundA)){
            if (groundContacts > 0){
                groundContacts--;
            }
            System.out.println("player left the ground: " + groundContacts);
        }
    }

    @Override
    public void preSolve(Contact contact, Manifold manifold) {

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse contactImpulse) {

    }

    public boolean isPlayerOnGround(){
        return groundContacts > 0;
    }
}
