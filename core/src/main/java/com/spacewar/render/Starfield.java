package com.spacewar.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * NEKONEČNÉ PIXELOVÉ VESMÍRNÉ POZADÍ (Infinite Scrolling Parallax Starfield)
 */
public class Starfield implements Disposable {

    private static final Color BG_COLOR = Color.valueOf("04060c");
    private static final Color[] STAR_COLORS = {
            Color.valueOf("ffffff"), Color.valueOf("e0f2fe"), Color.valueOf("93c5fd"),
            Color.valueOf("fde047"), Color.valueOf("cbd5e1"), Color.valueOf("64748b")
    };
    private static final int TOTAL_STARS = 320;

    private static final Layer[] LAYERS = {
            new Layer(0.15f, 0.45f, 1),
            new Layer(0.40f, 0.30f, 1),
            new Layer(0.75f, 0.17f, 2),
            new Layer(1.00f, 0.08f, 2)
    };

    private static final class Layer {
        final float parallax, weight;
        final int size;
        Layer(float parallax, float weight, int size) {
            this.parallax = parallax; this.weight = weight; this.size = size;
        }
    }

    private static final class Star {
        float x, y, parallax, baseAlpha, twinkleSpeed, phase;
        int size;
        Color color;
    }

    private final List<Star> stars = new ArrayList<>();
    private final Random random;
    private final Texture pixel;
    private final Matrix4 screenProjection = new Matrix4();
    private final Matrix4 savedProjection = new Matrix4();
    private int width, height;

    public Starfield(int width, int height) {
        this(width, height, System.nanoTime());
    }

    public Starfield(int width, int height, long seed) {
        this.random = new Random(seed);
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        this.pixel = new Texture(pm);
        pm.dispose();
        init(width, height);
    }

    public void init(int width, int height) {
        this.width = width;
        this.height = height;
        screenProjection.setToOrtho2D(0, 0, width, height);

        stars.clear();
        for (Layer layer : LAYERS) {
            int count = Math.round(TOTAL_STARS * layer.weight);
            for (int i = 0; i < count; i++) {
                Star s = new Star();
                s.x = random.nextFloat() * width;
                s.y = random.nextFloat() * height;
                s.parallax = layer.parallax;
                s.size = layer.size;
                s.color = STAR_COLORS[random.nextInt(STAR_COLORS.length)];
                s.baseAlpha = 0.4f + random.nextFloat() * 0.6f;
                s.twinkleSpeed = 1.5f + random.nextFloat() * 4.0f;
                s.phase = random.nextFloat() * (float) (Math.PI * 2);
                stars.add(s);
            }
        }
    }

    public void resize(int width, int height) {
        init(width, height);
    }

    public void render(SpriteBatch batch, float cameraX, float cameraY, float timeSeconds) {
        savedProjection.set(batch.getProjectionMatrix());
        batch.setProjectionMatrix(screenProjection);
        batch.begin();

        batch.setColor(BG_COLOR);
        batch.draw(pixel, 0, 0, width, height);

        for (Star s : stars) {
            float sx = (float) Math.floor(wrap(s.x - cameraX * s.parallax, width));
            float sy = (float) Math.floor(wrap(s.y - cameraY * s.parallax, height));

            float blink = Math.round(Math.sin(timeSeconds * s.twinkleSpeed + s.phase) * 2) / 2f;
            float alpha = Math.max(0.2f, Math.min(1f, s.baseAlpha + blink * 0.25f));

            batch.setColor(s.color.r, s.color.g, s.color.b, alpha);
            batch.draw(pixel, sx, sy, s.size, s.size);
        }

        batch.setColor(Color.WHITE);
        batch.end();
        batch.setProjectionMatrix(savedProjection);
    }

    private static float wrap(float value, float max) {
        return ((value % max) + max) % max;
    }

    @Override
    public void dispose() {
        pixel.dispose();
    }
}
