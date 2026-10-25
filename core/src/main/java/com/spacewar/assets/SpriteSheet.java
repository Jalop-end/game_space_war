package com.spacewar.assets;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

import java.util.List;

public class SpriteSheet implements Disposable {
    private final Texture texture;
    private final List<List<TextureRegion>> rows;

    SpriteSheet(Texture texture, List<List<TextureRegion>> rows) {
        this.texture = texture;
        this.rows = rows;
    }

    public int rowCount() { return rows.size(); }

    public List<TextureRegion> row(int index) {
        return rows.get(Math.floorMod(index, rows.size()));
    }

    public TextureRegion pick(int rowIndex, float screenHeightPx) {
        List<TextureRegion> variants = row(rowIndex);
        for (int i = variants.size() - 1; i >= 0; i--) {
            TextureRegion r = variants.get(i);
            if (r.getRegionHeight() >= screenHeightPx * 0.85f) return r;
        }
        return variants.get(0);
    }

    @Override
    public void dispose() { texture.dispose(); }
}
