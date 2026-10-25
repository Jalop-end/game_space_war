package com.spacewar.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;

public class Assets implements Disposable {
    public final SpriteSheet planets;
    public final SpriteSheet rocky;
    public final SpriteSheet stars;
    public final SpriteSheet blackHoles;
    public final SpriteSheet ships;

    public Assets() {
        planets = SpriteSheetSlicer.load(Gdx.files.internal("sprites/planets_major.png"));
        rocky = SpriteSheetSlicer.load(Gdx.files.internal("sprites/planets_rocky.png"));
        stars = SpriteSheetSlicer.load(Gdx.files.internal("sprites/stars.png"));
        blackHoles = SpriteSheetSlicer.load(Gdx.files.internal("sprites/black_holes.png"));
        ships = SpriteSheetSlicer.load(Gdx.files.internal("sprites/ships_combat.png"));
    }

    @Override
    public void dispose() {
        planets.dispose();
        rocky.dispose();
        stars.dispose();
        blackHoles.dispose();
        ships.dispose();
    }
}
