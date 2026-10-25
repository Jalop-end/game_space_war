package com.spacewar.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.ArrayList;
import java.util.List;

public final class SpriteSheetSlicer {
    private static final int WHITE_THRESHOLD = 240;
    private static final int MIN_GAP = 4;
    private static final int MIN_SIZE = 4;

    private SpriteSheetSlicer() {}

    public static SpriteSheet load(FileHandle file) {
        Pixmap original = new Pixmap(file);
        int w = original.getWidth(), h = original.getHeight();
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        pm.drawPixmap(original, 0, 0);
        original.dispose();

        boolean[] opaque = removeBackground(pm, w, h);

        Texture texture = new Texture(pm);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pm.dispose();

        boolean[] rowFilled = new boolean[h];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if (opaque[y * w + x]) { rowFilled[y] = true; break; }

        List<List<TextureRegion>> rows = new ArrayList<>();
        StringBuilder log = new StringBuilder();
        for (int[] rb : bands(rowFilled)) {
            boolean[] colFilled = new boolean[w];
            for (int x = 0; x < w; x++)
                for (int y = rb[0]; y <= rb[1]; y++)
                    if (opaque[y * w + x]) { colFilled[x] = true; break; }

            List<TextureRegion> row = new ArrayList<>();
            for (int[] cb : bands(colFilled)) {
                int minY = Integer.MAX_VALUE, maxY = -1;
                for (int y = rb[0]; y <= rb[1]; y++)
                    for (int x = cb[0]; x <= cb[1]; x++)
                        if (opaque[y * w + x]) { minY = Math.min(minY, y); maxY = Math.max(maxY, y); }
                if (maxY < 0) continue;
                row.add(new TextureRegion(texture, cb[0], minY, cb[1] - cb[0] + 1, maxY - minY + 1));
            }
            if (!row.isEmpty()) {
                row.sort((a, b) -> b.getRegionHeight() - a.getRegionHeight());
                rows.add(row);
                log.append(row.size()).append(' ');
            }
        }
        if (rows.isEmpty()) throw new IllegalStateException("No sprites found in " + file.path());
        Gdx.app.log("Sprites", file.name() + ": " + rows.size() + " rows, sprites per row: " + log);
        return new SpriteSheet(texture, rows);
    }

    private static boolean[] removeBackground(Pixmap pm, int w, int h) {
        boolean[] bg = new boolean[w * h];
        int[] queue = new int[w * h];
        int tail = 0;
        for (int x = 0; x < w; x++) { tail = seed(pm, bg, queue, tail, x, 0, w); tail = seed(pm, bg, queue, tail, x, h - 1, w); }
        for (int y = 0; y < h; y++) { tail = seed(pm, bg, queue, tail, 0, y, w); tail = seed(pm, bg, queue, tail, w - 1, y, w); }
        int head = 0;
        while (head < tail) {
            int i = queue[head++];
            int x = i % w, y = i / w;
            if (x > 0)     tail = seed(pm, bg, queue, tail, x - 1, y, w);
            if (x < w - 1) tail = seed(pm, bg, queue, tail, x + 1, y, w);
            if (y > 0)     tail = seed(pm, bg, queue, tail, x, y - 1, w);
            if (y < h - 1) tail = seed(pm, bg, queue, tail, x, y + 1, w);
        }
        boolean[] opaque = new boolean[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                if (bg[i]) pm.drawPixel(x, y, 0x00000000);
                else opaque[i] = (pm.getPixel(x, y) & 0xff) > 0;
            }
        }
        return opaque;
    }

    private static int seed(Pixmap pm, boolean[] bg, int[] queue, int tail, int x, int y, int w) {
        int i = y * w + x;
        if (bg[i] || !isBackground(pm.getPixel(x, y))) return tail;
        bg[i] = true;
        queue[tail] = i;
        return tail + 1;
    }

    private static boolean isBackground(int rgba) {
        int a = rgba & 0xff;
        if (a < 20) return true;
        int r = (rgba >>> 24) & 0xff, g = (rgba >>> 16) & 0xff, b = (rgba >>> 8) & 0xff;
        return r >= WHITE_THRESHOLD && g >= WHITE_THRESHOLD && b >= WHITE_THRESHOLD;
    }

    private static List<int[]> bands(boolean[] filled) {
        List<int[]> out = new ArrayList<>();
        int start = -1, last = -1;
        for (int i = 0; i < filled.length; i++) {
            if (!filled[i]) continue;
            if (start < 0) start = i;
            else if (i - last > MIN_GAP) { add(out, start, last); start = i; }
            last = i;
        }
        if (start >= 0) add(out, start, last);
        return out;
    }

    private static void add(List<int[]> out, int start, int end) {
        if (end - start + 1 >= MIN_SIZE) out.add(new int[]{start, end});
    }
}
