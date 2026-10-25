package com.spacewar.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.spacewar.SpaceWarGame;
import com.spacewar.assets.SpriteSheet;
import com.spacewar.galaxy.*;

import java.util.HashMap;
import java.util.Map;

public class GalaxyScreen extends ScreenAdapter {
    private static final float MIN_ZOOM = 0.12f;
    private static final float BLACK_HOLE_SIZE = 420f;

    private final SpaceWarGame game;
    private final Galaxy galaxy;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Matrix4 uiMatrix = new Matrix4();
    private final Vector3 tmp = new Vector3();
    private final GlyphLayout layout = new GlyphLayout();

    // Cache unikatnich proceduralnich textur pro planety a mesice (CloseUpPlanetGenerator)
    private final Map<Planet, Texture> planetTextures = new HashMap<>();
    private final Map<Moon, Texture> moonTextures = new HashMap<>();

    private boolean initialized;
    private float maxZoom = 10f;
    private float time;
    private float bgX, bgY, lastCamX, lastCamY;

    // Vybrana / najeta hvezda v galaxii
    private StarSystem selected;
    private StarSystem hoveredStar;

    // Otevrena soustava (detailni obrazovka pouze pro danou hvezdu a jeji planety)
    private StarSystem openedSystem;
    private Planet selectedPlanet;
    private Planet hoveredPlanet;
    private float systemTransition = 0f; // 0 = galaxie, 1 = plne otevrena soustava

    // Ulozena pozice kamery pred otevrenim soustavy pro plynuly navrat
    private float savedGalaxyCamX, savedGalaxyCamY, savedGalaxyZoom;

    // UI obdelniky pro klikani
    private final Rectangle backBtnBounds = new Rectangle();
    private final Rectangle openSysBtnBounds = new Rectangle();
    private final Rectangle[] planetListBounds = new Rectangle[8];

    // Aktualni pozice planet na obrazovce v rezimu otevrene soustavy
    private final float[] sysPlanetScreenX = new float[8];
    private final float[] sysPlanetScreenY = new float[8];
    private final float[] sysPlanetScreenR = new float[8];

    private boolean flying;
    private final Vector2 flyTarget = new Vector2();
    private float flyZoom;

    public GalaxyScreen(SpaceWarGame game, GalaxySize size, long seed) {
        this(game, size, seed, Faction.BLUE);
    }

    public GalaxyScreen(SpaceWarGame game, GalaxySize size, long seed, Faction playerFaction) {
        this.game = game;
        this.galaxy = GalaxyGenerator.generate(size, seed, playerFaction != null ? playerFaction : Faction.BLUE);
        for (int i = 0; i < planetListBounds.length; i++) {
            planetListBounds[i] = new Rectangle();
        }
        // Na zacatku vyber domovskou soustavu hrace
        this.selected = galaxy.getHomeworldSystem(this.galaxy.playerFaction);
    }

    @Override
    public void show() {
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
        Gdx.input.setInputProcessor(new InputMultiplexer(new KeysAndScroll(), new GestureDetector(new Gestures())));
    }

    @Override
    public void resize(int w, int h) {
        Vector3 pos = camera.position.cpy();
        float zoom = camera.zoom;
        camera.setToOrtho(false, w, h);
        maxZoom = galaxy.size.radius * 2.3f / Math.min(w, h);
        if (initialized) {
            camera.position.set(pos);
            camera.zoom = zoom;
        } else {
            camera.position.set(0, 0, 0);
            camera.zoom = maxZoom * 0.85f;
            initialized = true;
        }
        lastCamX = camera.position.x;
        lastCamY = camera.position.y;
        uiMatrix.setToOrtho2D(0, 0, w, h);
        game.starfield.resize(w, h);
    }

    /**
     * Zajisti, ze vsechny planety a mesice v dane soustave maji vygenerovanou
     * unikatni 3D proceduralni texturu z CloseUpPlanetGenerator (se shodnym typem).
     */
    private void ensureSystemTextures(StarSystem system) {
        if (system == null) return;
        for (Planet p : system.planets) {
            if (!planetTextures.containsKey(p)) {
                CloseUpPlanetGenerator.BodyType bType = p.closeUpType();
                Texture tex = CloseUpPlanetGenerator.generateTexture(bType, p.seed, 160);
                planetTextures.put(p, tex);
            }
            for (Moon m : p.moons) {
                if (!moonTextures.containsKey(m)) {
                    CloseUpPlanetGenerator.BodyType mType = m.closeUpType();
                    Texture mTex = CloseUpPlanetGenerator.generateTexture(mType, m.seed, 64);
                    moonTextures.put(m, mTex);
                }
            }
        }
    }

    private Texture getPlanetTexture(Planet p) {
        Texture tex = planetTextures.get(p);
        if (tex == null) {
            tex = CloseUpPlanetGenerator.generateTexture(p.closeUpType(), p.seed, 160);
            planetTextures.put(p, tex);
        }
        return tex;
    }

    private Texture getMoonTexture(Moon m) {
        Texture tex = moonTextures.get(m);
        if (tex == null) {
            tex = CloseUpPlanetGenerator.generateTexture(m.closeUpType(), m.seed, 64);
            moonTextures.put(m, tex);
        }
        return tex;
    }

    /**
     * Otevre detailni pohled pouze na vybranou hvezdnou soustavu.
     */
    private void openStarSystem(StarSystem system) {
        if (system == null) return;
        ensureSystemTextures(system);
        if (openedSystem == null) {
            savedGalaxyCamX = camera.position.x;
            savedGalaxyCamY = camera.position.y;
            savedGalaxyZoom = camera.zoom;
        }
        selected = system;
        openedSystem = system;
        systemTransition = 0f;

        // Vyber vychozi planetu (domovskou, pokud existuje, jinak prvni)
        Planet hw = system.getHomeworldPlanet();
        selectedPlanet = (hw != null) ? hw : (system.planets.isEmpty() ? null : system.planets.get(0));

        // Kamera plynule prileti ke hvezde
        flying = true;
        flyTarget.set(system.x, system.y);
        flyZoom = clampZoom(system.radius * 2.2f / Math.min(camera.viewportWidth, camera.viewportHeight));
    }

    /**
     * Zavre detail soustavy a vrati pohled zpet na celou galaxii.
     */
    private void closeStarSystem() {
        if (openedSystem == null) return;
        openedSystem = null;
        selectedPlanet = null;
        hoveredPlanet = null;
        systemTransition = 0f;
        flying = true;
        flyTarget.set(savedGalaxyCamX, savedGalaxyCamY);
        flyZoom = clampZoom(savedGalaxyZoom);
    }

    @Override
    public void render(float delta) {
        time += delta;
        // V otevrene soustave planety obihaji pomaleji a majestalneji, aby se na ne dobre klikalo
        galaxy.update(openedSystem != null ? delta * 0.25f : delta);

        if (openedSystem != null) {
            systemTransition = Math.min(1f, systemTransition + delta * 3.2f);
        }

        handleKeys(delta);
        updateFlight(delta);
        clampCamera();
        camera.update();

        bgX += (camera.position.x - lastCamX) / camera.zoom;
        bgY += (camera.position.y - lastCamY) / camera.zoom;
        lastCamX = camera.position.x;
        lastCamY = camera.position.y;

        updateHoverState();

        ScreenUtils.clear(0, 0, 0, 1);
        game.starfield.render(game.batch, bgX, bgY, time);

        if (openedSystem != null) {
            // Vykresleni POUZE vybrane hvezdne soustavy pres obrazovku s unikatnimi planetami
            drawOpenedStarSystem();
            drawOpenedSystemUi();
        } else {
            // Vykresleni cele galaxie
            float planetAlpha = MathUtils.clamp((2.2f - camera.zoom) / 1.0f, 0f, 1f);
            float moonAlpha = MathUtils.clamp((0.9f - camera.zoom) / 0.4f, 0f, 1f);

            drawOrbits(planetAlpha, moonAlpha);
            drawWorld(planetAlpha, moonAlpha);
            drawGalaxyUi();
        }
    }

    private void updateHoverState() {
        int mx = Gdx.input.getX();
        int my = Gdx.graphics.getHeight() - Gdx.input.getY();

        if (openedSystem != null) {
            hoveredStar = null;
            hoveredPlanet = pickSystemPlanet(mx, my);
            boolean overUi = backBtnBounds.contains(mx, my);
            if (!overUi && openedSystem != null) {
                for (int i = 0; i < openedSystem.planets.size() && i < planetListBounds.length; i++) {
                    if (planetListBounds[i].contains(mx, my)) {
                        overUi = true;
                        break;
                    }
                }
            }
            Gdx.graphics.setSystemCursor((hoveredPlanet != null || overUi)
                    ? Cursor.SystemCursor.Hand : Cursor.SystemCursor.Arrow);
        } else {
            hoveredPlanet = null;
            hoveredStar = pickStar(Gdx.input.getX(), Gdx.input.getY());
            boolean overBtn = selected != null && openSysBtnBounds.contains(mx, my);
            Gdx.graphics.setSystemCursor((hoveredStar != null || overBtn)
                    ? Cursor.SystemCursor.Hand : Cursor.SystemCursor.Arrow);
        }
    }

    // =========================================================================
    // REZIM 1: VYKRESLENI OTEVRENE HVEZDNE SOUSTAVY (POUZE DANA SOUSTAVA)
    // =========================================================================

    private void drawOpenedStarSystem() {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        StarSystem s = openedSystem;
        if (s == null) return;

        float centerX = w * 0.50f;
        float centerY = h * 0.50f;

        // Skalovani soustavy tak, aby vzdy krasne vyplnila aktualni velikost obrazovky
        float maxRadiusOnScreen = Math.min(w * 0.36f, h * 0.42f);
        float minRadiusOnScreen = Math.min(w, h) * 0.135f;
        int pCount = s.planets.size();

        // Vypocet pozic a polomeru drah planet na obrazovce
        float[] orbitRadii = new float[pCount];
        float minOrigOrbit = pCount > 0 ? s.planets.get(0).orbitRadius : 50f;
        float maxOrigOrbit = pCount > 0 ? s.planets.get(pCount - 1).orbitRadius : 200f;

        for (int i = 0; i < pCount; i++) {
            Planet p = s.planets.get(i);
            float t;
            if (pCount == 1) {
                t = 0.5f;
            } else if (maxOrigOrbit > minOrigOrbit + 1f) {
                t = (p.orbitRadius - minOrigOrbit) / (maxOrigOrbit - minOrigOrbit);
                // Rovnomernejsi rozlozeni kombinovane s puvodnim rozestupem
                float evenT = i / (float) (pCount - 1);
                t = MathUtils.lerp(evenT, t, 0.45f);
            } else {
                t = i / (float) (pCount - 1);
            }
            float targetR = MathUtils.lerp(minRadiusOnScreen, maxRadiusOnScreen, t);
            // Animace priblizeni pri otevreni soustavy
            float animScale = MathUtils.lerp(0.35f, 1.0f, smoothStep(systemTransition));
            orbitRadii[i] = targetR * animScale;

            // Velikost planety v detailnim pohledu soustavy
            float drawDiam;
            switch (p.type) {
                case RINGED_GIANT: drawDiam = 90f; break;
                case GAS_GIANT:    drawDiam = 68f; break;
                case TERRAN:       drawDiam = 56f; break;
                case VOLCANIC:
                case ICE:
                case DESERT:
                case TOXIC:        drawDiam = 52f; break;
                default:           drawDiam = 42f; break;
            }
            drawDiam *= animScale;

            sysPlanetScreenX[i] = centerX + MathUtils.cos(p.angle) * orbitRadii[i];
            sysPlanetScreenY[i] = centerY + MathUtils.sin(p.angle) * orbitRadii[i];
            sysPlanetScreenR[i] = drawDiam * 0.5f;
        }

        ShapeRenderer sr = game.shapes;
        SpriteBatch b = game.batch;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sr.setProjectionMatrix(uiMatrix);

        // 1. Slunecni korona a zare centralni hvezdy
        float starDrawSize = (78f + (3 - s.sizeClass) * 14f) * MathUtils.lerp(0.5f, 1f, smoothStep(systemTransition));
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int ring = 5; ring >= 1; ring--) {
            float glowR = starDrawSize * (0.55f + ring * 0.25f) + (float) Math.sin(time * 2.5f + ring) * 3f;
            sr.setColor(1f, 0.78f, 0.25f, 0.045f * (6 - ring));
            sr.circle(centerX, centerY, glowR, 64);
        }
        sr.end();

        // 2. Obihajici drahy planet a mesicu
        sr.begin(ShapeRenderer.ShapeType.Line);

        // Pokud jde o domovskou soustavu, vykresli frakcni prstenec kolem hvezdy
        if (s.isHomeworldSystem()) {
            Color fc = s.ownerFaction.color;
            float beaconR = starDrawSize * 0.78f + (float) Math.sin(time * 3.5f) * 4f;
            sr.setColor(fc.r, fc.g, fc.b, 0.75f);
            sr.circle(centerX, centerY, beaconR, 56);
        }

        for (int i = 0; i < pCount; i++) {
            Planet p = s.planets.get(i);
            boolean isSel = (p == selectedPlanet);
            boolean isHov = (p == hoveredPlanet);

            if (p.isHomeworld) {
                Color hc = p.homeworldFaction.color;
                sr.setColor(hc.r, hc.g, hc.b, isSel ? 0.75f : 0.45f);
            } else if (isSel || isHov) {
                sr.setColor(0.55f, 0.80f, 1.0f, 0.65f);
            } else {
                sr.setColor(0.35f, 0.50f, 0.78f, 0.30f);
            }
            sr.circle(centerX, centerY, orbitRadii[i], 100);

            // Drahy mesicu kolem planety
            float px = sysPlanetScreenX[i];
            float py = sysPlanetScreenY[i];
            float baseMoonOrbit = (p.type == PlanetType.RINGED_GIANT ? sysPlanetScreenR[i] * 0.92f : sysPlanetScreenR[i] * 1.05f) + 11f;
            sr.setColor(0.5f, 0.65f, 0.9f, 0.28f);
            for (int m = 0; m < p.moons.size(); m++) {
                float mr = baseMoonOrbit + m * 11f;
                sr.circle(px, py, mr, 36);
            }

            // Zvyrazneni domovskeho sveta a vybrane planety
            if (p.isHomeworld) {
                Color hc = p.homeworldFaction.color;
                float pulse = (float) Math.sin(time * 4f) * 3f;
                sr.setColor(hc.r, hc.g, hc.b, 0.9f);
                sr.circle(px, py, sysPlanetScreenR[i] + 6f + pulse, 40);
            }
            if (isSel || isHov) {
                sr.setColor(isSel ? new Color(1f, 0.9f, 0.35f, 0.95f) : new Color(0.7f, 0.9f, 1f, 0.8f));
                sr.circle(px, py, sysPlanetScreenR[i] + 10f, 40);
            }
        }
        sr.end();

        // 3. Vykresleni centralni hvezdy + unikatnich proceduralnich planet a mesicu z CloseUpPlanetGenerator
        b.setProjectionMatrix(uiMatrix);
        b.begin();

        // Centralni hvezda
        TextureRegion starReg = game.assets.stars.row(0).get(0);
        b.setColor(Color.WHITE);
        b.draw(starReg, centerX - starDrawSize / 2f, centerY - starDrawSize / 2f, starDrawSize, starDrawSize);

        // Planety a jejich mesice
        for (int i = 0; i < pCount; i++) {
            Planet p = s.planets.get(i);
            float px = sysPlanetScreenX[i];
            float py = sysPlanetScreenY[i];
            float diam = sysPlanetScreenR[i] * 2f;

            Texture pTex = getPlanetTexture(p);
            b.setColor(Color.WHITE);
            b.draw(pTex, px - diam / 2f, py - diam / 2f, diam, diam);

            // Mesice
            float baseMoonOrbit = (p.type == PlanetType.RINGED_GIANT ? sysPlanetScreenR[i] * 0.92f : sysPlanetScreenR[i] * 1.05f) + 11f;
            for (int m = 0; m < p.moons.size(); m++) {
                Moon moon = p.moons.get(m);
                float mr = baseMoonOrbit + m * 11f;
                float mx = px + MathUtils.cos(moon.angle) * mr;
                float my = py + MathUtils.sin(moon.angle) * mr;
                float mDiam = 16f + (moon.diameter - 5f) * 2f;

                Texture mTex = getMoonTexture(moon);
                b.draw(mTex, mx - mDiam / 2f, my - mDiam / 2f, mDiam, mDiam);
            }
        }

        // 4. Popisky hvezdy a jednotlivych planet primo v soustave
        BitmapFont font = game.font;
        String starLabel = s.name.toUpperCase() + " (" + s.className().toUpperCase() + ")";
        font.setColor(1f, 0.88f, 0.4f, 0.95f);
        layout.setText(font, starLabel);
        font.draw(b, layout, centerX - layout.width / 2f, centerY - starDrawSize * 0.55f - 6f);

        for (int i = 0; i < pCount; i++) {
            Planet p = s.planets.get(i);
            float px = sysPlanetScreenX[i];
            float py = sysPlanetScreenY[i];
            float labelY = py - sysPlanetScreenR[i] - 8f;

            if (p.isHomeworld) {
                Color hc = p.homeworldFaction.color;
                font.setColor(hc.r, hc.g, hc.b, 1f);
                String lbl = "* " + p.name + " [" + p.homeworldFaction.raceName + "]";
                layout.setText(font, lbl);
                font.draw(b, layout, px - layout.width / 2f, labelY);

                font.setColor(1f, 0.95f, 0.7f, 0.95f);
                String sub = p.type.displayName + " (DOMOVSKA)";
                layout.setText(font, sub);
                font.draw(b, layout, px - layout.width / 2f, labelY - 16f);
            } else {
                boolean isSel = (p == selectedPlanet);
                font.setColor(isSel ? Color.WHITE : new Color(0.82f, 0.90f, 1f, 0.92f));
                String lbl = p.name + " (" + p.type.displayName + ")";
                layout.setText(font, lbl);
                font.draw(b, layout, px - layout.width / 2f, labelY);
            }
        }

        b.end();
    }

    private void drawOpenedSystemUi() {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        StarSystem s = openedSystem;
        if (s == null) return;

        ShapeRenderer sr = game.shapes;
        SpriteBatch b = game.batch;
        BitmapFont font = game.font;

        int mx = Gdx.input.getX();
        int my = h - Gdx.input.getY();

        // Tlacitko ZPET DO GALAXIE vlevo nahore
        backBtnBounds.set(14f, h - 84f, 250f, 36f);
        boolean hoverBack = backBtnBounds.contains(mx, my);

        // Levy panel: seznam planet v soustave
        float leftW = 330f;
        float leftH = 115f + s.planets.size() * 30f + (s.isHomeworldSystem() ? 42f : 0f);
        float leftX = 14f;
        float leftY = 14f;

        // Pravy panel: detailni 160x160 nahled vybrane planety a jejich mesicu
        float rightW = 310f;
        float rightH = 355f;
        float rightX = w - rightW - 14f;
        float rightY = 14f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        sr.setProjectionMatrix(uiMatrix);
        sr.begin(ShapeRenderer.ShapeType.Filled);

        // Horni lista
        sr.setColor(0.02f, 0.04f, 0.09f, 0.92f);
        sr.rect(0, h - 38, w, 38);

        // Tlacitko Zpet do galaxie
        sr.setColor(hoverBack ? 0.16f : 0.08f, hoverBack ? 0.34f : 0.20f, hoverBack ? 0.62f : 0.40f, 0.95f);
        sr.rect(backBtnBounds.x, backBtnBounds.y, backBtnBounds.width, backBtnBounds.height);

        // Levy panel soustavy
        sr.setColor(0.02f, 0.04f, 0.10f, 0.88f);
        sr.rect(leftX, leftY, leftW, leftH);
        if (s.isHomeworldSystem()) {
            Color fc = s.ownerFaction.color;
            sr.setColor(fc.r, fc.g, fc.b, 0.85f);
            sr.rect(leftX, leftY + leftH - 5f, leftW, 5f);
        }

        // Radky planet v levem panelu
        float rowStartY = leftY + leftH - (s.isHomeworldSystem() ? 118f : 76f);
        for (int i = 0; i < s.planets.size() && i < planetListBounds.length; i++) {
            Planet p = s.planets.get(i);
            float ry = rowStartY - i * 30f;
            planetListBounds[i].set(leftX + 8f, ry - 20f, leftW - 16f, 26f);
            boolean rowSel = (p == selectedPlanet);
            boolean rowHov = planetListBounds[i].contains(mx, my);
            if (rowSel) {
                sr.setColor(0.16f, 0.30f, 0.55f, 0.90f);
                sr.rect(planetListBounds[i].x, planetListBounds[i].y, planetListBounds[i].width, planetListBounds[i].height);
            } else if (rowHov) {
                sr.setColor(0.09f, 0.17f, 0.32f, 0.80f);
                sr.rect(planetListBounds[i].x, planetListBounds[i].y, planetListBounds[i].width, planetListBounds[i].height);
            }
        }

        // Pravy panel detailu planety
        if (selectedPlanet != null) {
            sr.setColor(0.02f, 0.04f, 0.10f, 0.90f);
            sr.rect(rightX, rightY, rightW, rightH);

            // Pozadi boxu pro 160x160 planetu
            sr.setColor(0.01f, 0.02f, 0.05f, 0.95f);
            sr.rect(rightX + (rightW - 172f) / 2f, rightY + rightH - 210f, 172f, 172f);
        }
        sr.end();

        // Obrysy UI
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.35f, 0.55f, 0.9f, 1f);
        sr.line(0, h - 38, w, h - 38);

        sr.setColor(hoverBack ? Color.WHITE : new Color(0.4f, 0.75f, 1f, 0.9f));
        sr.rect(backBtnBounds.x, backBtnBounds.y, backBtnBounds.width, backBtnBounds.height);

        Color leftBorder = s.isHomeworldSystem() ? s.ownerFaction.color : new Color(0.35f, 0.55f, 0.9f, 0.9f);
        sr.setColor(leftBorder);
        sr.rect(leftX, leftY, leftW, leftH);

        if (selectedPlanet != null) {
            Color rightBorder = selectedPlanet.isHomeworld ? selectedPlanet.homeworldFaction.color : new Color(0.35f, 0.55f, 0.9f, 0.9f);
            sr.setColor(rightBorder);
            sr.rect(rightX, rightY, rightW, rightH);
            sr.setColor(0.25f, 0.45f, 0.75f, 0.6f);
            sr.rect(rightX + (rightW - 172f) / 2f, rightY + rightH - 210f, 172f, 172f);
        }
        sr.end();

        // Texty a ikony v UI
        b.setProjectionMatrix(uiMatrix);
        b.begin();

        // Horni lista
        font.setColor(1f, 0.9f, 0.4f, 1f);
        font.draw(b, "SOUSTAVA: " + s.name.toUpperCase() + " (" + s.className().toUpperCase() + ")", 14, h - 13);

        Color pfc = galaxy.playerFaction.color;
        font.setColor(pfc.r, pfc.g, pfc.b, 1f);
        String factionInfo = "FRAKCE: " + galaxy.playerFaction.factionName.toUpperCase() + " (YOU)";
        layout.setText(font, factionInfo);
        font.draw(b, layout, w / 2f - layout.width / 2f, h - 13);

        font.setColor(0.85f, 0.9f, 1f, 1f);
        layout.setText(font, "ESC / PRAVE TL. = ZPET DO GALAXIE");
        font.draw(b, layout, w - layout.width - 14, h - 13);

        // Text tlacitka Zpet
        font.setColor(Color.WHITE);
        layout.setText(font, "<< ZPET DO GALAXIE (ESC)");
        font.draw(b, layout,
                backBtnBounds.x + (backBtnBounds.width - layout.width) / 2f,
                backBtnBounds.y + (backBtnBounds.height + layout.height) / 2f);

        // Texty v levem panelu
        float ty = leftY + leftH - 18f;
        if (s.isHomeworldSystem()) {
            Faction f = s.ownerFaction;
            font.setColor(f.color);
            font.draw(b, "* DOMOVSKA SOUSTAVA: " + f.raceName + " *", leftX + 14f, ty);
            ty -= 20f;
            font.setColor(f.lightColor);
            font.draw(b, "Frakce: " + f.factionName, leftX + 14f, ty);
            ty -= 22f;
        }

        font.setColor(1f, 0.85f, 0.35f, 1f);
        font.draw(b, "Hvezda: " + s.name + " (" + s.className() + ")", leftX + 14f, ty);
        ty -= 22f;

        font.setColor(0.75f, 0.85f, 1f, 0.9f);
        font.draw(b, "Klikni na planetu pro detail (" + s.planets.size() + "):", leftX + 14f, ty);

        for (int i = 0; i < s.planets.size() && i < planetListBounds.length; i++) {
            Planet p = s.planets.get(i);
            float ry = rowStartY - i * 30f;

            // Mala ikonka planety v seznamu
            Texture pTex = getPlanetTexture(p);
            b.setColor(Color.WHITE);
            b.draw(pTex, leftX + 14f, ry - 18f, 22f, 22f);

            if (p.isHomeworld) {
                Color hc = p.homeworldFaction.color;
                font.setColor(hc.r, hc.g, hc.b, 1f);
                font.draw(b, "* " + p.name + " - " + p.type.displayName + " [DOMOV]", leftX + 42f, ry);
            } else {
                font.setColor(p == selectedPlanet ? Color.WHITE : new Color(0.82f, 0.90f, 1f, 1f));
                String mInfo = p.moons.isEmpty() ? "" : " (" + p.moons.size() + " mes.)";
                font.draw(b, p.name + " - " + p.type.displayName + mInfo, leftX + 42f, ry);
            }
        }

        // Pravy panel: Detail vybrane planety (160x160 CloseUpPlanetGenerator)
        if (selectedPlanet != null) {
            Planet p = selectedPlanet;
            float rty = rightY + rightH - 16f;

            font.setColor(1f, 0.9f, 0.4f, 1f);
            String header = "DETAIL PLANETY: " + p.name.toUpperCase();
            layout.setText(font, header);
            font.draw(b, layout, rightX + (rightW - layout.width) / 2f, rty);

            // Velka 160x160 proceduralni textura planety
            Texture pTex = getPlanetTexture(p);
            float previewX = rightX + (rightW - 160f) / 2f;
            float previewY = rightY + rightH - 204f;
            b.setColor(Color.WHITE);
            b.draw(pTex, previewX, previewY, 160f, 160f);

            float infoY = rightY + rightH - 224f;
            CloseUpPlanetGenerator.BodyType bType = p.closeUpType();

            font.setColor(0.65f, 0.88f, 1f, 1f);
            font.draw(b, "Typ: " + CloseUpPlanetGenerator.czechTypeName(bType), rightX + 16f, infoY);
            infoY -= 20f;

            String hexSeed = String.format("#%08X", p.seed);
            font.setColor(0.75f, 0.82f, 0.92f, 0.9f);
            font.draw(b, "Tvar povrchu (Seed): " + hexSeed, rightX + 16f, infoY);
            infoY -= 20f;

            if (p.isHomeworld) {
                Color hc = p.homeworldFaction.color;
                font.setColor(hc.r, hc.g, hc.b, 1f);
                font.draw(b, "DOMOVSKA PLANETA: " + p.homeworldFaction.raceName, rightX + 16f, infoY);
                infoY -= 20f;
                font.setColor(1f, 0.95f, 0.6f, 1f);
                font.draw(b, "Populace: " + p.population + " / " + p.maxPopulation + " mld.", rightX + 16f, infoY);
                infoY -= 22f;
            } else {
                font.setColor(0.7f, 0.78f, 0.88f, 0.9f);
                font.draw(b, "Stav: Neosidlena planeta", rightX + 16f, infoY);
                infoY -= 22f;
            }

            // Vykresleni mesicu vybrane planety zblizka
            if (p.moons.isEmpty()) {
                font.setColor(0.6f, 0.68f, 0.8f, 0.8f);
                font.draw(b, "Mesice: Zadne obihajici mesice", rightX + 16f, infoY);
            } else {
                font.setColor(0.85f, 0.92f, 1f, 1f);
                font.draw(b, "Obihajici mesice (" + p.moons.size() + "):", rightX + 16f, infoY);
                for (int m = 0; m < p.moons.size(); m++) {
                    Moon moon = p.moons.get(m);
                    Texture mTex = getMoonTexture(moon);
                    float mxPos = rightX + 20f + m * 92f;
                    float myPos = rightY + 12f;
                    b.setColor(Color.WHITE);
                    b.draw(mTex, mxPos, myPos, 36f, 36f);

                    font.getData().setScale(0.82f);
                    font.setColor(0.78f, 0.86f, 1f, 0.9f);
                    String mName;
                    switch (moon.spriteRow) {
                        case 0: mName = "Luna"; break;
                        case 1: mName = "Europa"; break;
                        case 2: mName = "Io"; break;
                        default: mName = "Phobos"; break;
                    }
                    font.draw(b, mName, mxPos + 40f, myPos + 24f);
                    font.getData().setScale(1f);
                }
            }
        }

        b.end();
    }

    private Planet pickSystemPlanet(float screenX, float uiY) {
        if (openedSystem == null) return null;
        for (int i = 0; i < openedSystem.planets.size(); i++) {
            float dx = screenX - sysPlanetScreenX[i];
            float dy = uiY - sysPlanetScreenY[i];
            float hitR = Math.max(sysPlanetScreenR[i] + 10f, 28f);
            if (dx * dx + dy * dy <= hitR * hitR) {
                return openedSystem.planets.get(i);
            }
        }
        return null;
    }

    private float smoothStep(float x) {
        float t = MathUtils.clamp(x, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    // =========================================================================
    // REZIM 2: VYKRESLENI GALAXIE (Z ODDALENI)
    // =========================================================================

    private void drawOrbits(float planetAlpha, float moonAlpha) {
        if (planetAlpha <= 0f) return;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        ShapeRenderer sr = game.shapes;
        sr.setProjectionMatrix(camera.combined);
        sr.begin(ShapeRenderer.ShapeType.Line);
        for (StarSystem s : galaxy.stars) {
            if (!isVisible(s.x, s.y, s.radius)) continue;
            sr.setColor(0.45f, 0.55f, 0.8f, 0.22f * planetAlpha);
            for (Planet p : s.planets) sr.circle(s.x, s.y, p.orbitRadius, 72);
            if (moonAlpha > 0f) {
                sr.setColor(0.45f, 0.55f, 0.8f, 0.15f * moonAlpha);
                for (Planet p : s.planets)
                    for (Moon m : p.moons) sr.circle(p.x, p.y, m.orbitRadius, 32);
            }
        }
        sr.end();
    }

    private void drawWorld(float planetAlpha, float moonAlpha) {
        ShapeRenderer sr = game.shapes;
        float zoom = camera.zoom;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        sr.setProjectionMatrix(camera.combined);
        sr.begin(ShapeRenderer.ShapeType.Line);
        for (StarSystem s : galaxy.stars) {
            if (!isVisible(s.x, s.y, s.radius)) continue;
            if (s.isHomeworldSystem()) {
                Color c = s.ownerFaction.color;
                float pulse = (float) Math.sin(time * 3.5f) * 4f;
                float beaconR = Math.max(s.diameter * 0.95f, 26f * zoom) + pulse;

                sr.setColor(c.r, c.g, c.b, 0.85f);
                sr.circle(s.x, s.y, beaconR, 48);

                sr.setColor(c.r, c.g, c.b, 0.45f);
                sr.circle(s.x, s.y, beaconR * 0.75f, 36);

                float armLen = beaconR * 0.35f;
                sr.line(s.x - beaconR - armLen, s.y, s.x - beaconR + armLen, s.y);
                sr.line(s.x + beaconR - armLen, s.y, s.x + beaconR + armLen, s.y);
                sr.line(s.x, s.y - beaconR - armLen, s.x, s.y - beaconR + armLen);
                sr.line(s.x, s.y + beaconR - armLen, s.x, s.y + beaconR + armLen);
            }

            if (planetAlpha > 0f) {
                for (Planet p : s.planets) {
                    if (p.isHomeworld) {
                        Color c = p.homeworldFaction.color;
                        sr.setColor(c.r, c.g, c.b, 0.9f * planetAlpha);
                        sr.circle(p.x, p.y, p.diameter * 0.85f + (float) Math.sin(time * 4f) * 2f, 32);
                    }
                }
            }
        }
        sr.end();

        SpriteBatch b = game.batch;
        b.setProjectionMatrix(camera.combined);
        b.begin();

        drawSprite(b, game.assets.blackHoles, galaxy.blackHoleVariant, 0, 0,
                Math.max(BLACK_HOLE_SIZE, 40f * zoom), 1f);

        for (StarSystem s : galaxy.stars) {
            if (!isVisible(s.x, s.y, s.radius)) continue;
            drawSprite(b, game.assets.stars, 0, s.x, s.y, Math.max(s.diameter, 12f * zoom), 1f);
            if (planetAlpha <= 0f) continue;

            // Pri velkem priblizeni ke hvezde na mape galaxie pouzijeme rovnez jeji unikatni proceduralni texturu,
            // nebo pri strednim oddaleni odpovidajici typ z planets_major / planets_rocky (oba maji 100% shodny typ!)
            boolean useCloseUpOnMap = (zoom < 0.75f) || (!s.planets.isEmpty() && planetTextures.containsKey(s.planets.get(0)));
            for (Planet p : s.planets) {
                if (useCloseUpOnMap) {
                    Texture pTex = getPlanetTexture(p);
                    float drawD = (p.type == PlanetType.RINGED_GIANT) ? p.diameter * 1.35f : p.diameter;
                    b.setColor(1f, 1f, 1f, planetAlpha);
                    b.draw(pTex, p.x - drawD / 2f, p.y - drawD / 2f, drawD, drawD);
                } else {
                    SpriteSheet sheet = p.type.sheet == PlanetType.Sheet.MAJOR ? game.assets.planets : game.assets.rocky;
                    drawSprite(b, sheet, p.type.spriteRow, p.x, p.y, p.diameter, planetAlpha);
                }
                if (moonAlpha <= 0f) continue;
                for (Moon m : p.moons) {
                    if (useCloseUpOnMap) {
                        Texture mTex = getMoonTexture(m);
                        b.setColor(1f, 1f, 1f, moonAlpha);
                        b.draw(mTex, m.x - m.diameter / 2f, m.y - m.diameter / 2f, m.diameter, m.diameter);
                    } else {
                        drawSprite(b, game.assets.rocky, m.spriteRow, m.x, m.y, m.diameter, moonAlpha);
                    }
                }
            }
        }
        b.setColor(Color.WHITE);
        b.end();

        StarSystem highlight = (hoveredStar != null) ? hoveredStar : selected;
        if (highlight != null) {
            sr.setProjectionMatrix(camera.combined);
            sr.begin(ShapeRenderer.ShapeType.Line);
            sr.setColor(1f, 0.85f, 0.3f, 0.9f);
            sr.circle(highlight.x, highlight.y, Math.max(highlight.diameter * 0.8f, 18f * zoom), 48);
            sr.end();
        }
    }

    private void drawSprite(SpriteBatch b, SpriteSheet sheet, int row, float x, float y, float height, float alpha) {
        TextureRegion r = sheet.pick(row, height / camera.zoom);
        float width = height * r.getRegionWidth() / (float) r.getRegionHeight();
        b.setColor(1f, 1f, 1f, alpha);
        b.draw(r, x - width / 2f, y - height / 2f, width, height);
    }

    private void drawGalaxyUi() {
        int w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        BitmapFont font = game.font;
        SpriteBatch b = game.batch;
        ShapeRenderer sr = game.shapes;
        float zoom = camera.zoom;

        b.setProjectionMatrix(uiMatrix);
        b.begin();
        float nameAlpha = MathUtils.clamp((6f - zoom) / 2f, 0f, 1f);
        if (nameAlpha > 0f) {
            for (StarSystem s : galaxy.stars) {
                if (!isVisible(s.x, s.y, s.radius)) continue;
                tmp.set(s.x, s.y - Math.max(s.diameter, 12f * zoom) / 2f, 0f);
                camera.project(tmp);

                if (s.isHomeworldSystem()) {
                    Color c = s.ownerFaction.color;
                    font.setColor(c.r, c.g, c.b, Math.max(0.7f, nameAlpha));
                    String label = "* " + s.name.toUpperCase() + " [" + s.ownerFaction.raceName + "]";
                    if (s.ownerFaction == galaxy.playerFaction) {
                        label += " (YOU)";
                    }
                    layout.setText(font, label);
                    font.draw(b, layout, tmp.x - layout.width / 2f, tmp.y - 6f);
                } else {
                    font.setColor(0.8f, 0.85f, 1f, 0.85f * nameAlpha);
                    layout.setText(font, s.name);
                    font.draw(b, layout, tmp.x - layout.width / 2f, tmp.y - 6f);
                }
            }
        }
        b.end();

        StarSystem infoStar = (hoveredStar != null) ? hoveredStar : selected;
        float panelW = 430f;
        float panelH = 0f;
        if (infoStar != null) {
            panelH = 180f + infoStar.planets.size() * 24f;
            if (infoStar.isHomeworldSystem()) panelH += 45f;
            openSysBtnBounds.set(24f, 22f, panelW - 24f, 36f);
        }

        int mx = Gdx.input.getX();
        int my = h - Gdx.input.getY();
        boolean hoverOpenBtn = infoStar != null && openSysBtnBounds.contains(mx, my);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        sr.setProjectionMatrix(uiMatrix);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.02f, 0.04f, 0.09f, 0.90f);
        sr.rect(0, h - 38, w, 38);
        if (infoStar != null) {
            sr.rect(12, 12, panelW, panelH);
            if (infoStar.isHomeworldSystem()) {
                Color fc = infoStar.ownerFaction.color;
                sr.setColor(fc.r, fc.g, fc.b, 0.8f);
                sr.rect(12, 12 + panelH - 6, panelW, 6);
            }
            // Tlacitko Otevrit soustavu
            sr.setColor(hoverOpenBtn ? 0.12f : 0.06f, hoverOpenBtn ? 0.45f : 0.30f, hoverOpenBtn ? 0.22f : 0.14f, 0.95f);
            sr.rect(openSysBtnBounds.x, openSysBtnBounds.y, openSysBtnBounds.width, openSysBtnBounds.height);
        }
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.35f, 0.55f, 0.9f, 1f);
        sr.line(0, h - 38, w, h - 38);
        if (infoStar != null) {
            Color borderCol = infoStar.isHomeworldSystem() ? infoStar.ownerFaction.color : new Color(0.35f, 0.55f, 0.9f, 1f);
            sr.setColor(borderCol);
            sr.rect(12, 12, panelW, panelH);

            sr.setColor(hoverOpenBtn ? new Color(0.4f, 1f, 0.6f, 1f) : new Color(0.25f, 0.85f, 0.45f, 0.9f));
            sr.rect(openSysBtnBounds.x, openSysBtnBounds.y, openSysBtnBounds.width, openSysBtnBounds.height);
        }
        sr.end();

        b.begin();
        font.setColor(0.85f, 0.9f, 1f, 1f);
        String topText = "SPACE WAR  |  " + galaxy.size.displayName.toUpperCase() + " GALAXY (" + galaxy.stars.size() + " STARS)";
        font.draw(b, topText, 12, h - 13);

        Color pfc = galaxy.playerFaction.color;
        font.setColor(pfc.r, pfc.g, pfc.b, 1f);
        String factionInfo = "FRAKCE: " + galaxy.playerFaction.factionName.toUpperCase() + " (YOU)";
        layout.setText(font, factionInfo);
        font.draw(b, layout, w / 2f - layout.width / 2f, h - 13);

        font.setColor(0.85f, 0.9f, 1f, 1f);
        layout.setText(font, "ESC = MENU");
        font.draw(b, layout, w - layout.width - 12, h - 13);

        font.setColor(0.6f, 0.75f, 0.95f, 0.95f);
        layout.setText(font, "Klik na hvezdu: OTEVRIT SOUSTAVU | Kolecko: Zoom | Tah: Posun");
        font.draw(b, layout, w - layout.width - 12, 22);

        if (infoStar != null) {
            float ty = 12 + panelH - 18;

            if (infoStar.isHomeworldSystem()) {
                Faction f = infoStar.ownerFaction;
                font.setColor(f.color);
                String hwTitle = "* DOMOVSKA SOUSTAVA: " + f.factionName.toUpperCase() + " *";
                font.draw(b, hwTitle, 24, ty);
                ty -= 20;

                font.setColor(f.lightColor);
                String raceDesc = "Rasa: " + f.raceName + " (" + f.trait + ")";
                font.draw(b, raceDesc, 24, ty);
                ty -= 20;

                if (f == galaxy.playerFaction) {
                    font.setColor(1f, 0.9f, 0.3f, 1f);
                    font.draw(b, ">>> TVUJ DOMOVSKY SYSTEM! (YOUR HOME) <<<", 24, ty);
                    ty -= 22;
                }
            }

            font.setColor(1f, 0.85f, 0.35f, 1f);
            font.draw(b, "Hvezda: " + infoStar.name + " (" + infoStar.className() + ")", 24, ty);
            ty -= 24;

            font.setColor(0.85f, 0.9f, 1f, 1f);
            font.draw(b, "Planety v soustave (" + infoStar.planets.size() + "):", 24, ty);
            ty -= 24;

            for (Planet p : infoStar.planets) {
                if (p.isHomeworld) {
                    Color hc = p.homeworldFaction.color;
                    font.setColor(hc.r, hc.g, hc.b, 1f);
                    String hwLabel = "* " + p.name + " - " + p.type.displayName + " [DOMOVSKA PLANETA]";
                    hwLabel += " Pop: " + p.population + "/" + p.maxPopulation;
                    font.draw(b, hwLabel, 24, ty);
                } else {
                    font.setColor(0.80f, 0.88f, 1f, 1f);
                    String moons = p.moons.isEmpty() ? "" : " (mesice: " + p.moons.size() + ")";
                    font.draw(b, p.name + "  -  " + p.type.displayName + moons, 24, ty);
                }
                ty -= 22;
            }

            font.setColor(Color.WHITE);
            String btnLabel = ">> OTEVRIT SOUSTAVU " + infoStar.name.toUpperCase() + " <<";
            layout.setText(font, btnLabel);
            font.draw(b, layout,
                    openSysBtnBounds.x + (openSysBtnBounds.width - layout.width) / 2f,
                    openSysBtnBounds.y + (openSysBtnBounds.height + layout.height) / 2f);
        }
        b.end();
    }

    private boolean isVisible(float x, float y, float r) {
        float hw = camera.viewportWidth * camera.zoom / 2f + r;
        float hh = camera.viewportHeight * camera.zoom / 2f + r;
        return Math.abs(x - camera.position.x) < hw && Math.abs(y - camera.position.y) < hh;
    }

    private float clampZoom(float z) { return MathUtils.clamp(z, MIN_ZOOM, maxZoom); }

    private void clampCamera() {
        float limit = galaxy.size.radius * 1.15f;
        camera.position.x = MathUtils.clamp(camera.position.x, -limit, limit);
        camera.position.y = MathUtils.clamp(camera.position.y, -limit, limit);
        camera.zoom = clampZoom(camera.zoom);
    }

    private void zoomAt(float screenX, float screenY, float factor) {
        tmp.set(screenX, screenY, 0);
        camera.unproject(tmp);
        float bx = tmp.x, by = tmp.y;
        camera.zoom = clampZoom(camera.zoom * factor);
        camera.update();
        tmp.set(screenX, screenY, 0);
        camera.unproject(tmp);
        camera.position.add(bx - tmp.x, by - tmp.y, 0);
    }

    private void updateFlight(float delta) {
        if (!flying) return;
        float a = 1f - (float) Math.pow(0.02, delta);
        camera.position.x += (flyTarget.x - camera.position.x) * a;
        camera.position.y += (flyTarget.y - camera.position.y) * a;
        camera.zoom += (flyZoom - camera.zoom) * a;
        if (Math.abs(camera.zoom - flyZoom) < 0.002f
                && Vector2.dst(camera.position.x, camera.position.y, flyTarget.x, flyTarget.y) < 1f) {
            flying = false;
        }
    }

    private void handleKeys(float delta) {
        if (openedSystem != null) return;
        float speed = 600f * camera.zoom * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) { camera.position.x -= speed; flying = false; }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) { camera.position.x += speed; flying = false; }
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) { camera.position.y += speed; flying = false; }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) { camera.position.y -= speed; flying = false; }
        if (Gdx.input.isKeyPressed(Input.Keys.EQUALS) || Gdx.input.isKeyPressed(Input.Keys.PLUS)) camera.zoom = clampZoom(camera.zoom * (1f - delta));
        if (Gdx.input.isKeyPressed(Input.Keys.MINUS)) camera.zoom = clampZoom(camera.zoom * (1f + delta));
    }

    private StarSystem pickStar(float screenX, float screenY) {
        tmp.set(screenX, screenY, 0);
        camera.unproject(tmp);
        StarSystem best = null;
        float bestDist = Float.MAX_VALUE;
        for (StarSystem s : galaxy.stars) {
            float hit = Math.max(s.diameter * 0.7f, 22f * camera.zoom);
            if (camera.zoom < 1.5f) hit = Math.max(hit, s.radius);
            float d = Vector2.dst(tmp.x, tmp.y, s.x, s.y);
            if (d < hit && d < bestDist) { best = s; bestDist = d; }
        }
        return best;
    }

    @Override
    public void dispose() {
        for (Texture t : planetTextures.values()) {
            t.dispose();
        }
        planetTextures.clear();
        for (Texture t : moonTextures.values()) {
            t.dispose();
        }
        moonTextures.clear();
    }

    private class KeysAndScroll extends InputAdapter {
        @Override
        public boolean scrolled(float amountX, float amountY) {
            if (openedSystem != null) {
                // Koleckem mysi smerem ven (oddaleni) lze take plynule opustit soustavu zpet do galaxie
                if (amountY > 0 && systemTransition >= 0.9f) {
                    closeStarSystem();
                }
                return true;
            }
            flying = false;
            zoomAt(Gdx.input.getX(), Gdx.input.getY(), amountY > 0 ? 1.15f : 1f / 1.15f);
            return true;
        }

        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK || keycode == Input.Keys.BACKSPACE) {
                if (openedSystem != null) {
                    closeStarSystem();
                } else {
                    Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
                    dispose();
                    game.setScreen(new MenuScreen(game));
                }
                return true;
            }
            if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
                if (openedSystem == null && selected != null) {
                    openStarSystem(selected);
                    return true;
                }
            }
            return false;
        }
    }

    private class Gestures extends GestureDetector.GestureAdapter {
        private float startZoom;

        @Override
        public boolean touchDown(float x, float y, int pointer, int button) {
            startZoom = camera.zoom;
            if (button == Input.Buttons.RIGHT && openedSystem != null) {
                closeStarSystem();
                return true;
            }
            return false;
        }

        @Override
        public boolean pan(float x, float y, float deltaX, float deltaY) {
            if (openedSystem != null) return false;
            flying = false;
            camera.translate(-deltaX * camera.zoom, deltaY * camera.zoom);
            return true;
        }

        @Override
        public boolean zoom(float initialDistance, float distance) {
            if (openedSystem != null) return false;
            flying = false;
            camera.zoom = clampZoom(startZoom * initialDistance / distance);
            return true;
        }

        @Override
        public boolean tap(float x, float y, int count, int button) {
            int uiY = Gdx.graphics.getHeight() - (int) y;

            if (openedSystem != null) {
                // Klik na tlacitko Zpet do galaxie
                if (backBtnBounds.contains(x, uiY)) {
                    closeStarSystem();
                    return true;
                }
                // Klik na radek planety v levem seznamu
                for (int i = 0; i < openedSystem.planets.size() && i < planetListBounds.length; i++) {
                    if (planetListBounds[i].contains(x, uiY)) {
                        selectedPlanet = openedSystem.planets.get(i);
                        return true;
                    }
                }
                // Klik primo na obihajici planetu v soustave
                Planet clickedPlanet = pickSystemPlanet(x, uiY);
                if (clickedPlanet != null) {
                    selectedPlanet = clickedPlanet;
                    return true;
                }
                return true;
            }

            // Rezim mapy galaxie:
            // Klik na tlacitko Otevrit soustavu v panelu
            StarSystem infoStar = (hoveredStar != null) ? hoveredStar : selected;
            if (infoStar != null && openSysBtnBounds.contains(x, uiY)) {
                openStarSystem(infoStar);
                return true;
            }

            // Klik na hvezdu -> otevrit soustavu!
            StarSystem s = pickStar(x, y);
            if (s != null) {
                selected = s;
                openStarSystem(s);
            } else if (count == 1) {
                selected = null;
            }
            return true;
        }
    }
}
