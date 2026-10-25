package com.spacewar;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.spacewar.assets.Assets;
import com.spacewar.render.Starfield;
import com.spacewar.screens.MenuScreen;

public class SpaceWarGame extends Game {
    public SpriteBatch batch;
    public ShapeRenderer shapes;
    public BitmapFont font;
    public Assets assets;
    public Starfield starfield;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont();
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        font.setUseIntegerPositions(true);
        assets = new Assets();
        starfield = new Starfield(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        setScreen(new MenuScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
        batch.dispose();
        shapes.dispose();
        font.dispose();
        assets.dispose();
        starfield.dispose();
    }
}
