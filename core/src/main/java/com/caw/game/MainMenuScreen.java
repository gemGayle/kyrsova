package com.caw.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;


public class MainMenuScreen implements Screen {
    private final GameStart game;
    private Stage stage;
    private Skin skin;
    private Slider volumeSlider;

    public MainMenuScreen(final GameStart gam) {
        this.game = gam;
        stage = new Stage(new ScreenViewport());

        try {
            skin = new Skin(Gdx.files.internal("assets/ui/uiskin.json"));

            if (skin.has("default", TextButton.TextButtonStyle.class) && game.defaultFont != null) {
                TextButton.TextButtonStyle buttonStyle = skin.get("default", TextButton.TextButtonStyle.class);
                buttonStyle.font = game.defaultFont;
            }
            if (skin.has("default", Label.LabelStyle.class) && game.defaultFont != null) {
                Label.LabelStyle labelStyle = skin.get("default", Label.LabelStyle.class);
                labelStyle.font = game.defaultFont;
            }
            if (skin.has("title", Label.LabelStyle.class) && game.defaultFont != null) {
                Label.LabelStyle titleStyle = skin.get("title", Label.LabelStyle.class);
                titleStyle.font = game.defaultFont;
            }

        } catch (Exception e) {
            Gdx.app.error("MainMenuScreen", "Could not load skin 'assets/ui/uiskin.json'", e);
            skin = new Skin();
        }

        setupUI();
    }

    private void setupUI() {
        Table table = new Table();
        table.setFillParent(true);
        table.align(Align.center);
        // table.setDebug(true);

        stage.addActor(table);

        float buttonWidth = 250f;
        float buttonHeight = 40f;
        float padValue = 15f;


        Label titleLabel = new Label("GAME MENU", skin);
        game.defaultFont.getData().setScale(1.0f);
        titleLabel.setAlignment(Align.center);
        titleLabel.setFontScale(4.0f);
        table.add(titleLabel).padBottom(padValue * 2);
        table.row();

        // start game button
        TextButton startGameButton = new TextButton("Start Game", skin);
        startGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameScreen(game));
                dispose();
            }
        });
        table.add(startGameButton).width(buttonWidth).height(buttonHeight).padBottom(padValue);
        table.row();

        // volume slider
        Label volumeLabel = new Label("Master Volume:", skin);
        volumeSlider = new Slider(0f, 1f, 0.01f, false, skin);
        volumeSlider.setValue(game.prefs.getFloat("masterVolume", 0.5f));
        volumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.prefs.putFloat("masterVolume", volumeSlider.getValue());
                game.prefs.flush();
            }
        });
        Table volumeTable = new Table(skin);
        volumeTable.add(volumeLabel).padRight(10);
        volumeTable.add(volumeSlider).width(200f);
        table.add(volumeTable).padBottom(padValue);
        table.row();

        // Exit game button
        TextButton exitButton = new TextButton("Exit Game", skin);
        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });
        table.add(exitButton).width(buttonWidth).height(buttonHeight);
    }


    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (stage != null && stage.getViewport() != null) {
            stage.getViewport().update(width, height, true);
        }
    }

    @Override
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        if (stage != null) {
            stage.dispose();
        }
        if (skin != null) {
            skin.dispose();
        }
        Gdx.app.log("MainMenuScreen", "Disposed");
    }
}
