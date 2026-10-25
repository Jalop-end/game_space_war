package com.spacewar.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.spacewar.SpaceWarGame;
import com.spacewar.galaxy.Faction;
import com.spacewar.galaxy.GalaxySize;

public class MenuScreen extends ScreenAdapter {
    private static final float V_WIDTH = 1280f;
    private static final float V_HEIGHT = 720f;

    private static class FactionCard {
        Faction faction;
        Rectangle bounds = new Rectangle();
        Rectangle btnBounds = new Rectangle();
        float centerX;
    }

    private static class SizeBtn {
        GalaxySize size;
        Rectangle bounds = new Rectangle();
        float centerX;
    }

    private final SpaceWarGame game;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final GlyphLayout layout = new GlyphLayout();
    private final Vector3 mouseWorld = new Vector3();

    // Vybrané nastavení pro novou hru
    private Faction selectedFaction = Faction.BLUE;
    private GalaxySize selectedSize = GalaxySize.MEDIUM;

    private final FactionCard[] cards = new FactionCard[4];
    private final SizeBtn[] sizeBtns = new SizeBtn[3];
    private final Rectangle startBtnBounds = new Rectangle(380f, 35f, 520f, 50f);
    private final Rectangle exitBtnBounds = new Rectangle(1140f, 665f, 110f, 35f);

    // Globální zarovnaná výška pro ikony lodí napříč všemi 4 kartami
    private final float shipCenterY = 430f;

    private float time;

    public MenuScreen(SpaceWarGame game) {
        this.game = game;
        this.viewport = new FitViewport(V_WIDTH, V_HEIGHT, camera);
        camera.position.set(V_WIDTH / 2f, V_HEIGHT / 2f, 0);

        // 4 karty ras – dokonale symetrické a zarovnané horizontálně
        Faction[] factions = Faction.values();
        float cardW = 276f;
        float cardH = 370f;
        float gap = 28f;
        float startX = 46f; // (1280 - (4 * 276 + 3 * 28)) / 2 = 46f
        float cardY = 220f;

        for (int i = 0; i < 4; i++) {
            cards[i] = new FactionCard();
            cards[i].faction = factions[i];
            float cx = startX + i * (cardW + gap);
            cards[i].bounds.set(cx, cardY, cardW, cardH);
            cards[i].centerX = cx + cardW / 2f;
            cards[i].btnBounds.set(cx + 16f, cardY + 16f, cardW - 32f, 42f);
        }

        // Tlačítka velikosti galaxie – dokonale vycentrovaná
        GalaxySize[] sizes = GalaxySize.values();
        float sbW = 210f;
        float sbH = 40f;
        float sbGap = 20f;
        float sbStartX = (V_WIDTH - (3 * sbW + 2 * sbGap)) / 2f; // 305f
        float sbY = 115f;

        for (int i = 0; i < 3; i++) {
            sizeBtns[i] = new SizeBtn();
            sizeBtns[i].size = sizes[i];
            float sx = sbStartX + i * (sbW + sbGap);
            sizeBtns[i].bounds.set(sx, sbY, sbW, sbH);
            sizeBtns[i].centerX = sx + sbW / 2f;
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                return handleClick(screenX, screenY);
            }
        });
    }

    private boolean handleClick(int screenX, int screenY) {
        viewport.unproject(mouseWorld.set(screenX, screenY, 0));
        float x = mouseWorld.x;
        float y = mouseWorld.y;

        for (FactionCard c : cards) {
            if (c.bounds.contains(x, y)) {
                selectedFaction = c.faction;
                return true;
            }
        }

        for (SizeBtn sb : sizeBtns) {
            if (sb.bounds.contains(x, y)) {
                selectedSize = sb.size;
                return true;
            }
        }

        if (startBtnBounds.contains(x, y)) {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
            game.setScreen(new GalaxyScreen(game, selectedSize, System.currentTimeMillis(), selectedFaction));
            return true;
        }

        if (exitBtnBounds.contains(x, y)) {
            Gdx.app.exit();
            return true;
        }

        return false;
    }

    @Override
    public void resize(int w, int h) {
        viewport.update(w, h, true);
        game.starfield.resize(w, h);
    }

    @Override
    public void render(float delta) {
        time += delta;

        // Souřadnice myši v rozlišení 1280x720
        viewport.unproject(mouseWorld.set(Gdx.input.getX(), Gdx.input.getY(), 0));
        float mx = mouseWorld.x;
        float my = mouseWorld.y;

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            handleClick(Gdx.input.getX(), Gdx.input.getY());
        }

        // Změna kurzoru myši na ruku při najetí
        boolean isHovering = false;
        for (FactionCard c : cards) {
            if (c.bounds.contains(mx, my)) { isHovering = true; break; }
        }
        if (!isHovering) {
            for (SizeBtn sb : sizeBtns) {
                if (sb.bounds.contains(mx, my)) { isHovering = true; break; }
            }
        }
        if (!isHovering && (startBtnBounds.contains(mx, my) || exitBtnBounds.contains(mx, my))) {
            isHovering = true;
        }
        Gdx.graphics.setSystemCursor(isHovering ? Cursor.SystemCursor.Hand : Cursor.SystemCursor.Arrow);

        ScreenUtils.clear(0, 0, 0, 1);
        game.starfield.render(game.batch, time * 18f, time * 6f, time);

        camera.update();
        SpriteBatch batch = game.batch;
        ShapeRenderer shapes = game.shapes;

        // 1. ZÁHLAVÍ MENU
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        drawCentered(batch, "S P A C E   W A R", 685f, 3.0f, Color.WHITE);
        drawCentered(batch, "KROK 1: ZVOL SI SVOU RASU A FLOTILU", 645f, 1.30f, new Color(0.50f, 0.85f, 1f, 1f));
        drawCentered(batch, "Kazda rasa zacina ve svem kvadrantu galaxie a vlastni unikatni flotilu.", 620f, 0.92f, new Color(0.75f, 0.82f, 0.92f, 0.85f));
        batch.end();

        // 2. KRESLENÍ TVARŮ A PODSTAVCŮ PRO IKONY LODÍ
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(camera.combined);

        for (FactionCard c : cards) {
            boolean isSelected = (c.faction == selectedFaction);
            boolean isHover = c.bounds.contains(mx, my);
            Color fc = c.faction.color;

            // Výplň těla karty
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            if (isSelected) {
                shapes.setColor(fc.r * 0.22f, fc.g * 0.22f, fc.b * 0.22f, 0.96f);
            } else if (isHover) {
                shapes.setColor(0.08f, 0.12f, 0.22f, 0.90f);
            } else {
                shapes.setColor(0.03f, 0.05f, 0.10f, 0.82f);
            }
            shapes.rect(c.bounds.x, c.bounds.y, c.bounds.width, c.bounds.height);

            // Horní barevný akcent
            shapes.setColor(fc.r, fc.g, fc.b, isSelected ? 1f : 0.65f);
            shapes.rect(c.bounds.x, c.bounds.y + c.bounds.height - 6f, c.bounds.width, 6f);

            // Středový kruhový podstavec pro ikonu lodi (stejná výška pro všech 4 ras)
            shapes.setColor(fc.r * 0.32f, fc.g * 0.32f, fc.b * 0.32f, 0.65f);
            shapes.circle(c.centerX, shipCenterY, 46f, 36);

            // Tlačítko uvnitř karty (výplň)
            if (isSelected) {
                shapes.setColor(fc.r, fc.g, fc.b, 1f);
            } else if (isHover) {
                shapes.setColor(0.18f, 0.26f, 0.44f, 0.95f);
            } else {
                shapes.setColor(0.10f, 0.15f, 0.26f, 0.85f);
            }
            shapes.rect(c.btnBounds.x, c.btnBounds.y, c.btnBounds.width, c.btnBounds.height);
            shapes.end();

            // Obrysové linky a technologické kruhy
            shapes.begin(ShapeRenderer.ShapeType.Line);

            // Dělící linka pod názvem
            shapes.setColor(fc.r * 0.5f, fc.g * 0.5f, fc.b * 0.5f, 0.4f);
            shapes.line(c.bounds.x + 20f, c.bounds.y + c.bounds.height - 72f, c.bounds.x + c.bounds.width - 20f, c.bounds.y + c.bounds.height - 72f);

            // Radarový kroužek kolem ikony lodi
            shapes.setColor(fc.r, fc.g, fc.b, isSelected ? 0.9f : 0.45f);
            shapes.circle(c.centerX, shipCenterY, 48f, 36);
            if (isSelected) {
                // Pulsující aktivní kroužek
                float pulse = (float) Math.sin(time * 4f) * 3f;
                shapes.setColor(Color.WHITE);
                shapes.circle(c.centerX, shipCenterY, 54f + pulse, 36);
            }

            // Obrys těla karty
            if (isSelected) {
                float pulse = (float) Math.sin(time * 5f) * 0.18f;
                shapes.setColor(Math.min(1f, fc.r + pulse), Math.min(1f, fc.g + pulse), Math.min(1f, fc.b + pulse), 1f);
                shapes.rect(c.bounds.x, c.bounds.y, c.bounds.width, c.bounds.height);
                shapes.rect(c.bounds.x + 1, c.bounds.y + 1, c.bounds.width - 2, c.bounds.height - 2);
                shapes.rect(c.bounds.x + 2, c.bounds.y + 2, c.bounds.width - 4, c.bounds.height - 4);
            } else {
                shapes.setColor(isHover ? fc : new Color(0.25f, 0.40f, 0.65f, 0.6f));
                shapes.rect(c.bounds.x, c.bounds.y, c.bounds.width, c.bounds.height);
            }

            // Obrys tlačítka uvnitř karty
            shapes.setColor(isSelected ? Color.WHITE : new Color(0.35f, 0.5f, 0.8f, 0.7f));
            shapes.rect(c.btnBounds.x, c.btnBounds.y, c.btnBounds.width, c.btnBounds.height);
            shapes.end();
        }

        // Tlačítka velikosti galaxie (výplně)
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (SizeBtn sb : sizeBtns) {
            boolean active = (sb.size == selectedSize);
            boolean hover = sb.bounds.contains(mx, my);
            if (active) {
                shapes.setColor(0.18f, 0.45f, 0.85f, 0.95f);
            } else {
                shapes.setColor(hover ? 0.10f : 0.04f, hover ? 0.16f : 0.07f, hover ? 0.28f : 0.14f, 0.85f);
            }
            shapes.rect(sb.bounds.x, sb.bounds.y, sb.bounds.width, sb.bounds.height);
        }

        // Start tlačítko
        boolean hoverStart = startBtnBounds.contains(mx, my);
        shapes.setColor(hoverStart ? 0.08f : 0.04f, hoverStart ? 0.42f : 0.28f, hoverStart ? 0.18f : 0.12f, 0.95f);
        shapes.rect(startBtnBounds.x, startBtnBounds.y, startBtnBounds.width, startBtnBounds.height);

        // Exit tlačítko
        boolean hoverExit = exitBtnBounds.contains(mx, my);
        shapes.setColor(hoverExit ? 0.25f : 0.12f, 0.05f, 0.05f, 0.85f);
        shapes.rect(exitBtnBounds.x, exitBtnBounds.y, exitBtnBounds.width, exitBtnBounds.height);
        shapes.end();

        // Obrysy spodních tlačítek
        shapes.begin(ShapeRenderer.ShapeType.Line);
        for (SizeBtn sb : sizeBtns) {
            boolean active = (sb.size == selectedSize);
            shapes.setColor(active ? new Color(0.4f, 0.85f, 1f, 1f) : new Color(0.25f, 0.4f, 0.65f, 0.6f));
            shapes.rect(sb.bounds.x, sb.bounds.y, sb.bounds.width, sb.bounds.height);
            if (active) shapes.rect(sb.bounds.x + 1, sb.bounds.y + 1, sb.bounds.width - 2, sb.bounds.height - 2);
        }

        shapes.setColor(hoverStart ? new Color(0.4f, 1f, 0.6f, 1f) : new Color(0.2f, 0.85f, 0.4f, 1f));
        shapes.rect(startBtnBounds.x, startBtnBounds.y, startBtnBounds.width, startBtnBounds.height);
        shapes.rect(startBtnBounds.x + 1, startBtnBounds.y + 1, startBtnBounds.width - 2, startBtnBounds.height - 2);

        shapes.setColor(hoverExit ? new Color(1f, 0.4f, 0.4f, 1f) : new Color(0.6f, 0.25f, 0.25f, 0.8f));
        shapes.rect(exitBtnBounds.x, exitBtnBounds.y, exitBtnBounds.width, exitBtnBounds.height);
        shapes.end();

        // 3. TEXTY A DOKONALE ZAROVNANÉ IKONY LODÍ
        batch.begin();

        for (int i = 0; i < 4; i++) {
            FactionCard c = cards[i];
            Faction f = c.faction;
            boolean isSelected = (f == selectedFaction);

            // Název rasy – dokonale zarovnané na střed karty
            Color tc = isSelected ? Color.WHITE : f.lightColor;
            drawCenteredAt(batch, f.raceName, c.centerX, c.bounds.y + c.bounds.height - 30f, 1.4f, tc);

            // Název frakce – dokonale zarovnané na střed karty
            drawCenteredAt(batch, f.factionName, c.centerX, c.bounds.y + c.bounds.height - 56f, 0.95f, new Color(0.75f, 0.82f, 0.95f, 0.85f));

            // Vykreslení lodi – dokonale zarovnané na střed podstavce (stejný X na kartě, stejný Y na celé obrazovce)
            if (game.assets.ships != null && game.assets.ships.rowCount() > f.shipRow) {
                TextureRegion ship = game.assets.ships.row(f.shipRow).get(0);
                float maxShipW = 110f;
                float maxShipH = 80f;
                float scale = Math.min(maxShipW / ship.getRegionWidth(), maxShipH / ship.getRegionHeight());
                float shipW = ship.getRegionWidth() * scale;
                float shipH = ship.getRegionHeight() * scale;

                float sx = c.centerX - shipW / 2f;
                float sy = shipCenterY - shipH / 2f;

                batch.setColor(1f, 1f, 1f, isSelected ? 1f : 0.85f);
                batch.draw(ship, sx, sy, shipW, shipH);
                batch.setColor(Color.WHITE);
            }

            // Kvadrant – dokonale zarovnané na střed karty
            String quadName = "";
            switch (f.quadrant) {
                case 0: quadName = "KVADRANT 1 (SV)"; break;
                case 1: quadName = "KVADRANT 2 (SZ)"; break;
                case 2: quadName = "KVADRANT 3 (JZ)"; break;
                case 3: quadName = "KVADRANT 4 (JV)"; break;
            }
            drawCenteredAt(batch, quadName, c.centerX, c.bounds.y + 118f, 1.05f, f.color);

            // Vlastnost rasy – zkrácený popis dokonale zarovnaný
            String desc = "";
            switch (f) {
                case BLUE: desc = "Prumysl, stity a flotily"; break;
                case GREEN: desc = "Populace a regenerace"; break;
                case RED: desc = "Railguny a pancerovani"; break;
                case YELLOW: desc = "Energie a lasery"; break;
            }
            drawCenteredAt(batch, desc, c.centerX, c.bounds.y + 90f, 0.88f, new Color(0.85f, 0.9f, 1f, 0.95f));

            // Tlačítko uvnitř karty – dokonale zarovnané na střed tlačítka
            String btnText = isSelected ? ">> VYBRANO <<" : "ZVOLIT RASU";
            Color btnTextColor = isSelected ? Color.BLACK : Color.WHITE;
            drawCenteredAt(batch, btnText, c.centerX, c.btnBounds.y + c.btnBounds.height / 2f, 1.15f, btnTextColor);
        }

        // KROK 2: Velikost galaxie nápis
        drawCentered(batch, "KROK 2: VELIKOST GALAXIE:", 175f, 1.1f, new Color(0.8f, 0.9f, 1f, 1f));

        // Tlačítka velikosti galaxie – text vycentrovaný v každém tlačítku
        for (SizeBtn sb : sizeBtns) {
            boolean active = (sb.size == selectedSize);
            String label = sb.size.displayName.toUpperCase() + " (" + sb.size.starCount + " HVEZD)";
            drawCenteredAt(batch, label, sb.centerX, sb.bounds.y + sb.bounds.height / 2f, 1.05f, active ? Color.WHITE : new Color(0.7f, 0.8f, 0.9f, 1f));
        }

        // Start tlačítko text
        float startBtnCenterX = startBtnBounds.x + startBtnBounds.width / 2f;
        float startBtnCenterY = startBtnBounds.y + startBtnBounds.height / 2f;
        drawCenteredAt(batch, "* VSTOUPIT DO GALAXIE A ZACIT HRAT *", startBtnCenterX, startBtnCenterY, 1.25f, Color.WHITE);

        // Exit tlačítko text
        float exitBtnCenterX = exitBtnBounds.x + exitBtnBounds.width / 2f;
        float exitBtnCenterY = exitBtnBounds.y + exitBtnBounds.height / 2f;
        drawCenteredAt(batch, "UKONCIT", exitBtnCenterX, exitBtnCenterY, 1.0f, new Color(0.95f, 0.75f, 0.75f, 1f));

        batch.end();
    }

    private void drawCenteredAt(SpriteBatch batch, String text, float centerX, float centerY, float scale, Color color) {
        BitmapFont font = game.font;
        font.getData().setScale(scale);
        font.setColor(color);
        layout.setText(font, text);
        font.draw(batch, layout, centerX - layout.width / 2f, centerY + layout.height / 2f);
        font.getData().setScale(1f);
    }

    private void drawCentered(SpriteBatch batch, String text, float centerY, float scale, Color color) {
        drawCenteredAt(batch, text, V_WIDTH / 2f, centerY, scale, color);
    }
}
