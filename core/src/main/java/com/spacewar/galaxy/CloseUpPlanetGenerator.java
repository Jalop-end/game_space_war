package com.spacewar.galaxy;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * DETERMINISTICKY PIXEL-ART GENERATOR DETAILNICH PLANET A MESICU Z BLIZKOSTI (JAVA SEED ENGINE)
 *
 * Stejnych 7 typu hlavnich planet a 4 typy kamennych teles/mesicu jako na vesmirne mape,
 * vykreslenych zblizka pomoci 3D sferickeho fraktalniho sumu (FBM), hrebenoveho sumu (Ridged Noise)
 * a domenoveho zakriveni. Plne upraveno pro LibGDX (Pixmap / Texture) a propojeno s PlanetType.
 */
public class CloseUpPlanetGenerator {

    /**
     * 7 Typu hlavnich planet + 4 Typy kamennych planet / mesicu (shodne s vesmirnou mapou)
     */
    public enum BodyType {
        // 7 Hlavnich planet (Sheet.MAJOR 0..6)
        TERRAN,         // 0. Gaia (Terestricka - oceany, kontinenty, reky, polarni cepice, mracna)
        LAVA,           // 1. Pyros (Lavova - cedicove kry, lavova jezera a zhnouci pukliny)
        RINGED,         // 2. Chronos (Prstencovy obr - pasy plynu + detailni nakloneny prstenec se stinem)
        GAS,            // 3. Jove (Bourny obr - turbulentni pasy a obri rude anticyklony)
        ICE,            // 4. Cryos (Ledova - ledovcove plane a hluboke tektonicke kanony)
        DESERT,         // 5. Arrakis (Poustni - pisecne duny, stolove hory a prachove boure)
        TOXIC,          // 6. Xenos (Toxicka - purpurova biosfera a svitici kysela jezera)
        // 4 Mesice / Kamenne planety (Sheet.ROCKY 0..3)
        MOON_CRATER,    // 0: Luna (Kraterovy / Barren)
        MOON_ICE,       // 1: Europa (Ledovy s tektonickymi prasklinami / Frozen)
        MOON_VOLCANIC,  // 2: Io (Sopecny sirovy / Sulfur)
        MOON_CRYSTAL    // 3: Phobos (Krystalicky / Crystal)
    }

    /**
     * Prevede PlanetType z vesmirne mapy na odpovidajici BodyType pro detailni pohled.
     * Zajistuje 100% shodu typu planety pri priblizeni do soustavy.
     */
    public static BodyType fromPlanetType(PlanetType type) {
        if (type == null) return BodyType.TERRAN;
        switch (type) {
            case TERRAN:       return BodyType.TERRAN;
            case VOLCANIC:     return BodyType.LAVA;
            case RINGED_GIANT: return BodyType.RINGED;
            case GAS_GIANT:    return BodyType.GAS;
            case ICE:          return BodyType.ICE;
            case DESERT:       return BodyType.DESERT;
            case TOXIC:        return BodyType.TOXIC;
            case BARREN:       return BodyType.MOON_CRATER;
            case FROZEN:       return BodyType.MOON_ICE;
            case SULFUR:       return BodyType.MOON_VOLCANIC;
            case CRYSTAL:      return BodyType.MOON_CRYSTAL;
            default:           return BodyType.TERRAN;
        }
    }

    /**
     * Prevede radek mesice (0..3) z vesmirne mapy na odpovidajici BodyType.
     */
    public static BodyType fromMoonRow(int spriteRow) {
        switch (spriteRow) {
            case 0:  return BodyType.MOON_CRATER;
            case 1:  return BodyType.MOON_ICE;
            case 2:  return BodyType.MOON_VOLCANIC;
            case 3:  return BodyType.MOON_CRYSTAL;
            default: return BodyType.MOON_CRATER;
        }
    }

    /**
     * Vrati cesky nazev typu telesa (v cistem ASCII pro BitmapFont).
     */
    public static String czechTypeName(BodyType type) {
        switch (type) {
            case TERRAN:        return "Gaia (Terestricka)";
            case LAVA:          return "Pyros (Lavova)";
            case RINGED:        return "Chronos (Prstencovy obr)";
            case GAS:           return "Jove (Bourny plynny obr)";
            case ICE:           return "Cryos (Ledova)";
            case DESERT:        return "Arrakis (Poustni)";
            case TOXIC:         return "Xenos (Toxicka)";
            case MOON_CRATER:   return "Luna (Kraterove teleso)";
            case MOON_ICE:      return "Europa (Tektonicky led)";
            case MOON_VOLCANIC: return "Io (Sirove sopecne)";
            case MOON_CRYSTAL:  return "Phobos (Krystalicke)";
            default:            return "Nezname teleso";
        }
    }

    // 4x4 Bayerova matice pro ostry pixel-art dithering prechodu svetla
    private static final double[][] BAYER_4X4 = {
        {  0.0 / 16.0,  8.0 / 16.0,  2.0 / 16.0, 10.0 / 16.0 },
        { 12.0 / 16.0,  4.0 / 16.0, 14.0 / 16.0,  6.0 / 16.0 },
        {  3.0 / 16.0, 11.0 / 16.0,  1.0 / 16.0,  9.0 / 16.0 },
        { 15.0 / 16.0,  7.0 / 16.0, 13.0 / 16.0,  5.0 / 16.0 }
    };

    /**
     * Vygeneruje detailni planetu nebo mesic z blizkosti jako LibGDX Texture (s Nearest filtrem).
     */
    public static Texture generateTexture(BodyType type, int seedInt, int resolution) {
        int[] argbPixels = generateCloseUpPixels(
            type, seedInt, resolution, true, true, true, 0.0, 225.0
        );
        Pixmap pm = new Pixmap(resolution, resolution, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < resolution; y++) {
            int rowOffset = y * resolution;
            for (int x = 0; x < resolution; x++) {
                int argb = argbPixels[rowOffset + x];
                int a = (argb >>> 24) & 0xFF;
                if (a == 0) continue;
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;
                int rgba8888 = (r << 24) | (g << 16) | (b << 8) | a;
                pm.drawPixel(x, y, rgba8888);
            }
        }
        Texture tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pm.dispose();
        return tex;
    }

    public static TextureRegion generateRegion(BodyType type, int seedInt, int resolution) {
        return new TextureRegion(generateTexture(type, seedInt, resolution));
    }

    /**
     * Vrati primo surove pole ARGB pixelu (int[resolution * resolution]).
     */
    public static int[] generateCloseUpPixels(
            BodyType type,
            int seedInt,
            int resolution,
            boolean showClouds,
            boolean showAtmo,
            boolean showNightLights,
            double rotAngle,
            double lightAngleDeg
    ) {
        int[] pixels = new int[resolution * resolution];
        double cx = resolution / 2.0 - 0.5;
        double cy = resolution / 2.0 - 0.5;

        boolean isRinged = (type == BodyType.RINGED);
        double R = isRinged ? Math.floor(resolution * 0.29) : Math.floor(resolution * 0.43);

        // Smer svetla ze Slunce soustavy
        double sunRad = Math.toRadians(lightAngleDeg);
        double lx = Math.cos(sunRad) * 0.78;
        double ly = Math.sin(sunRad) * 0.78;
        double lz = 0.62;

        // Deterministicky generator makro-vlastnosti planety ze Seedu
        Mulberry32 rng = new Mulberry32(seedInt);
        double continentScale = 1.55 + rng.nextDouble() * 1.55;
        double seaLevel = 0.43 + (rng.nextDouble() - 0.5) * 0.16;
        double iceCapSize = 0.70 + (rng.nextDouble() - 0.5) * 0.20;
        double axialTilt = (rng.nextDouble() - 0.5) * 0.52;
        double warpFactor = 0.50 + rng.nextDouble() * 0.75;
        double ringTilt = -0.32 + (rng.nextDouble() - 0.5) * 0.34;
        double seedPhase = rng.nextDouble() * Math.PI * 2.0;

        // 3D posun v sumovem prostoru, aby kazda planeta mela zcela jiny tvar kontinentu/dun/prasklin
        double offX = (rng.nextDouble() - 0.5) * 120.0;
        double offY = (rng.nextDouble() - 0.5) * 120.0;
        double offZ = (rng.nextDouble() - 0.5) * 120.0;

        // Vygenerovani 3D krateru (pro mesice) nebo bournych ok (pro plynne obry)
        boolean isMoon = type.name().startsWith("MOON_");
        int numFeatures = isMoon ? (14 + (int) (rng.nextDouble() * 12)) : (4 + (int) (rng.nextDouble() * 5));
        double[][] features = new double[numFeatures][4]; // x, y, z, radius
        for (int i = 0; i < numFeatures; i++) {
            double theta = rng.nextDouble() * Math.PI * 2.0;
            double phi = Math.acos(2.0 * rng.nextDouble() - 1.0);
            features[i][0] = Math.sin(phi) * Math.cos(theta);
            features[i][1] = Math.sin(phi) * Math.sin(theta);
            features[i][2] = Math.cos(phi);
            features[i][3] = 0.12 + rng.nextDouble() * 0.24;
        }

        // 1. Zadni cast prstence (pro Chronos)
        if (isRinged) {
            renderDetailedRings(pixels, resolution, cx, cy, R, ringTilt, lx, ly, seedPhase, false);
        }

        double cosRot = Math.cos(rotAngle);
        double sinRot = Math.sin(rotAngle);
        double cosTilt = Math.cos(axialTilt);
        double sinTilt = Math.sin(axialTilt);

        int outlineRgb = getOutlineColor(type);
        int atmoGlowRgb = getAtmoGlowColor(type);
        int cityLightRgb = getCityLightColor(type);

        // 2. Hlavni koule planety / mesice
        for (int py = 0; py < resolution; py++) {
            for (int px = 0; px < resolution; px++) {
                double dx = px - cx;
                double dy = py - cy;
                double dist = Math.sqrt(dx * dx + dy * dy);
                double bayer = BAYER_4X4[py & 3][px & 3];

                // Atmosfericke halo okolo planety
                if (dist > R && dist <= R + 4.5 && showAtmo && atmoGlowRgb != 0) {
                    double fade = 1.0 - (dist - R) / 4.5;
                    double sunSide = Math.max(0.15, (dx / R) * lx + (dy / R) * ly + 0.5);
                    if (fade * sunSide > bayer * 0.75 + 0.12) {
                        int alpha = clamp((int) Math.round(fade * 190.0), 0, 255);
                        setPixel(pixels, resolution, px, py, (alpha << 24) | (atmoGlowRgb & 0x00FFFFFF));
                    }
                    continue;
                }

                if (dist > R) continue;

                // 1px tmavy pixelovy obrys na okraji koule
                if (dist >= R - 1.05) {
                    setPixel(pixels, resolution, px, py, 0xFF000000 | outlineRgb);
                    continue;
                }

                // 3D sfericka normala v bode (nx, ny, nz)
                double nx = dx / R;
                double ny = dy / R;
                double nz = Math.sqrt(Math.max(0.0, 1.0 - nx * nx - ny * ny));

                double rawLight = Math.max(0.04, nx * lx + ny * ly + nz * lz);
                double rimFactor = 1.0 - nz;

                // Natoceni 3D souradnice podle sklonu osy a rotace + posun seedu
                double tx = nx * cosTilt - ny * sinTilt;
                double ty = nx * sinTilt + ny * cosTilt;
                double sx = (tx * cosRot + nz * sinRot) * continentScale + offX;
                double sy = ty * continentScale + offY;
                double sz = (-tx * sinRot + nz * cosRot) * continentScale + offZ;

                // Matematicka rovnice povrchu ze Seedu
                double heightVal = warpedFbm3D(sx, sy, sz, seedInt, warpFactor, 5);
                double ridgeVal = ridged3D(sx * 1.4, sy * 1.4, sz * 1.4, seedInt + 409, 4);
                double moistureVal = fbm3D(sx * 1.2 + 9.2, sy * 1.2 + 4.1, sz * 1.2 + 2.7, seedInt + 887, 4);

                int chosenRgb = 0xFFFFFF;
                double nightGlowSignal = 0.0;
                boolean selfLuminous = false;

                switch (type) {
                    case TERRAN: {
                        double polarFactor = Math.abs(ty) + (heightVal - 0.5) * 0.28;
                        boolean isRiver = heightVal > seaLevel + 0.02 && heightVal < seaLevel + 0.24 && ridgeVal > 0.83;

                        if (polarFactor > iceCapSize) {
                            chosenRgb = (polarFactor > iceCapSize + 0.06) ? 0xF8FAFC : 0x38BDF8;
                        } else if (heightVal < seaLevel - 0.14) {
                            chosenRgb = 0x0F296B;
                        } else if (heightVal < seaLevel - 0.04) {
                            chosenRgb = 0x1D4ED8;
                        } else if (heightVal < seaLevel) {
                            chosenRgb = (heightVal > seaLevel - 0.015) ? 0x38BDF8 : 0x0284C7;
                        } else if (isRiver) {
                            chosenRgb = 0x0284C7;
                        } else if (heightVal < seaLevel + 0.028) {
                            chosenRgb = 0xFDE047;
                        } else if (heightVal < seaLevel + 0.15) {
                            chosenRgb = (moistureVal > 0.52) ? 0x15803D : 0x22C55E;
                            nightGlowSignal = ridgeVal;
                        } else if (heightVal < seaLevel + 0.25) {
                            chosenRgb = (moistureVal > 0.45) ? 0x15803D : 0x65A30D;
                        } else if (heightVal < seaLevel + 0.33) {
                            chosenRgb = 0x64748B;
                        } else {
                            chosenRgb = 0xF8FAFC;
                        }

                        if (showClouds) {
                            double cloudSwirl = warpedFbm3D(sx * 1.35 + 14.0, sy * 1.35, sz * 1.35, seedInt + 999, 0.9, 4);
                            if (cloudSwirl > 0.64 && (cloudSwirl - 0.64) * 3.2 > bayer * 0.65) {
                                chosenRgb = 0xFFFFFF;
                            }
                        }
                        break;
                    }
                    case LAVA: {
                        if (heightVal < 0.30) {
                            double depth = (0.30 - heightVal) / 0.30;
                            if (depth > 0.55) chosenRgb = 0xFEF9C3;
                            else if (depth > 0.32) chosenRgb = 0xFACC15;
                            else if (depth > 0.15) chosenRgb = 0xF97316;
                            else chosenRgb = 0xDC2626;
                            selfLuminous = true;
                        } else if (ridgeVal > 0.76) {
                            if (ridgeVal > 0.89) chosenRgb = 0xFEF9C3;
                            else if (ridgeVal > 0.84) chosenRgb = 0xFACC15;
                            else if (ridgeVal > 0.80) chosenRgb = 0xF97316;
                            else chosenRgb = 0xDC2626;
                            selfLuminous = true;
                        } else {
                            if (heightVal > 0.72) chosenRgb = 0x78716C;
                            else if (heightVal > 0.54) chosenRgb = 0x44403C;
                            else if (heightVal > 0.40) chosenRgb = 0x292524;
                            else chosenRgb = 0x14110F;
                        }
                        if (!selfLuminous && showClouds && moistureVal > 0.74 && bayer > 0.4) {
                            chosenRgb = 0x57534E;
                        }
                        break;
                    }
                    case RINGED: {
                        double turb = warpedFbm3D(sx * 0.9, sy * 2.2, sz * 0.9, seedInt + 311, 0.5, 4);
                        double bandSignal = Math.sin(ty * 14.0 + turb * 3.8 + seedPhase);
                        if (bandSignal > 0.58) chosenRgb = 0xFEF3C7;
                        else if (bandSignal > 0.15) chosenRgb = 0xFBBF24;
                        else if (bandSignal > -0.28) chosenRgb = 0xD97706;
                        else if (bandSignal > -0.68) chosenRgb = 0x92400E;
                        else chosenRgb = 0x451A03;
                        break;
                    }
                    case GAS: {
                        double rotNx = tx * cosRot + nz * sinRot;
                        double rotNz = -tx * sinRot + nz * cosRot;
                        boolean inStorm = false;
                        for (int i = 0; i < 2; i++) {
                            double dStorm = Math.sqrt(
                                Math.pow((rotNx - features[i][0]), 2) +
                                Math.pow((ty - features[i][1]) * 1.9, 2) +
                                Math.pow((rotNz - features[i][2]), 2)
                            );
                            if (dStorm < features[i][3]) {
                                double swirl = fbm3D(sx * 3.0, sy * 3.0, sz * 3.0, seedInt + 55, 3);
                                double normS = dStorm / features[i][3];
                                if (normS + (swirl - 0.5) * 0.25 < 0.38) chosenRgb = 0xFECACA;
                                else if (normS < 0.76) chosenRgb = 0xEF4444;
                                else chosenRgb = 0xB91C1C;
                                inStorm = true;
                                break;
                            }
                        }
                        if (!inStorm) {
                            double jet = warpedFbm3D(sx * 0.8, sy * 2.8, sz * 0.8, seedInt + 701, 0.7, 4);
                            double bWave = Math.sin(ty * 18.0 + jet * 4.6 + seedPhase);
                            if (bWave > 0.60) chosenRgb = 0xFFEDD5;
                            else if (bWave > 0.18) chosenRgb = 0xFDBA74;
                            else if (bWave > -0.22) chosenRgb = 0xF97316;
                            else if (bWave > -0.62) chosenRgb = 0xC2410C;
                            else chosenRgb = 0x7C2D12;
                        }
                        break;
                    }
                    case ICE: {
                        if (ridgeVal > 0.82) {
                            chosenRgb = (ridgeVal > 0.88) ? 0x082F49 : 0x0369A1;
                            nightGlowSignal = ridgeVal;
                        } else if (heightVal > 0.72) chosenRgb = 0xF0F9FF;
                        else if (heightVal > 0.56) chosenRgb = 0xBAE6FD;
                        else if (heightVal > 0.42) chosenRgb = 0x7DD3FC;
                        else if (heightVal > 0.28) chosenRgb = 0x38BDF8;
                        else chosenRgb = 0x0284C7;
                        break;
                    }
                    case DESERT: {
                        double duneWave = Math.sin((ty * 15.0 + (tx * cosRot + nz * sinRot) * 4.0) + heightVal * 6.5 + seedPhase);
                        if (heightVal < 0.28) chosenRgb = (heightVal < 0.20) ? 0x451A03 : 0x78350F;
                        else if (ridgeVal > 0.80) chosenRgb = 0xA16207;
                        else if (duneWave > 0.62) chosenRgb = 0xFEF08A;
                        else if (duneWave > 0.15) chosenRgb = 0xFACC15;
                        else if (duneWave > -0.35) chosenRgb = 0xEAB308;
                        else chosenRgb = 0xCA8A04;

                        if (showClouds && moistureVal > 0.72 && bayer > 0.35) {
                            chosenRgb = 0xFDE68A;
                        }
                        break;
                    }
                    case TOXIC: {
                        if (heightVal < seaLevel - 0.02) {
                            double acidDepth = (seaLevel - heightVal) / seaLevel;
                            if (acidDepth > 0.35) chosenRgb = 0xECFCCB;
                            else if (acidDepth > 0.15) chosenRgb = 0xBEF264;
                            else chosenRgb = 0x65A30D;
                            selfLuminous = true;
                        } else if (heightVal < seaLevel + 0.03) chosenRgb = 0x4D7C0F;
                        else if (ridgeVal > 0.78) {
                            chosenRgb = 0xC084FC;
                            nightGlowSignal = 0.9;
                        } else if (heightVal > 0.66) chosenRgb = 0x9333EA;
                        else if (heightVal > 0.48) chosenRgb = 0x6B21A8;
                        else chosenRgb = 0x3B0764;

                        if (!selfLuminous && showClouds && moistureVal > 0.68 && bayer > 0.35) {
                            chosenRgb = 0xF0ABFC;
                        }
                        break;
                    }
                    case MOON_CRATER:
                    case MOON_ICE:
                    case MOON_VOLCANIC:
                    case MOON_CRYSTAL: {
                        double rotNx = tx * cosRot + nz * sinRot;
                        double rotNz = -tx * sinRot + nz * cosRot;
                        double craterNormDist = -1.0;
                        double craterRad = 0.0;
                        for (int i = 0; i < numFeatures; i++) {
                            double d = Math.sqrt(
                                Math.pow(rotNx - features[i][0], 2) +
                                Math.pow(ty - features[i][1], 2) +
                                Math.pow(rotNz - features[i][2], 2)
                            );
                            if (d < features[i][3]) {
                                craterNormDist = d / features[i][3];
                                craterRad = features[i][3];
                                break;
                            }
                        }

                        if (type == BodyType.MOON_CRATER) {
                            if (craterNormDist >= 0.0) {
                                if (craterNormDist > 0.78) chosenRgb = 0xF1F5F9;
                                else if (craterNormDist < 0.22 && craterRad > 0.22) chosenRgb = 0xCBD5E1;
                                else chosenRgb = 0x1E293B;
                            } else if (heightVal < 0.38) chosenRgb = 0x334155;
                            else if (heightVal < 0.56) chosenRgb = 0x64748B;
                            else if (heightVal < 0.74) chosenRgb = 0x94A3B8;
                            else chosenRgb = 0xCBD5E1;
                        } else if (type == BodyType.MOON_ICE) {
                            double line1 = Math.abs(Math.sin(sx * 4.2 + sy * 2.8 + heightVal * 4.0));
                            double line2 = Math.abs(Math.cos(sy * 5.1 - sz * 3.4 + ridgeVal * 3.0));
                            if (line1 < 0.08 || line2 < 0.07) {
                                chosenRgb = (line1 < 0.035) ? 0x042F2E : 0x0F766E;
                            } else if (heightVal > 0.68) chosenRgb = 0xF0FDFA;
                            else if (heightVal > 0.50) chosenRgb = 0xCCFBF1;
                            else if (heightVal > 0.34) chosenRgb = 0x5EEAD4;
                            else chosenRgb = 0x14B8A6;
                        } else if (type == BodyType.MOON_VOLCANIC) {
                            if (craterNormDist >= 0.0 && craterNormDist < 0.65) {
                                if (craterNormDist < 0.22) { chosenRgb = 0xFFFFFF; selfLuminous = true; }
                                else if (craterNormDist < 0.42) { chosenRgb = 0xEA580C; selfLuminous = true; }
                                else chosenRgb = 0x1C1917;
                            } else if (heightVal > 0.72) chosenRgb = 0xFEF9C3;
                            else if (heightVal > 0.54) chosenRgb = 0xFDE047;
                            else if (heightVal > 0.36) chosenRgb = 0xEAB308;
                            else chosenRgb = 0xB45309;
                        } else {
                            if (ridgeVal > 0.80 || (craterNormDist >= 0.0 && craterNormDist < 0.45)) {
                                chosenRgb = (ridgeVal > 0.89) ? 0xFFF1F2 : ((ridgeVal > 0.84) ? 0xFDA4AF : 0xF43F5E);
                                selfLuminous = true;
                            } else if (heightVal > 0.68) chosenRgb = 0xB86B82;
                            else if (heightVal > 0.48) chosenRgb = 0x8A425A;
                            else if (heightVal > 0.30) chosenRgb = 0x592236;
                            else chosenRgb = 0x31101D;
                        }
                        break;
                    }
                }

                if (selfLuminous) {
                    setPixel(pixels, resolution, px, py, 0xFF000000 | chosenRgb);
                    continue;
                }

                // Atmosfericky rozptyl na osvetlenem okraji koule
                if (showAtmo && atmoGlowRgb != 0 && rimFactor > 0.68 && rawLight > 0.25) {
                    double mix = Math.min(1.0, (rimFactor - 0.68) * 1.1);
                    chosenRgb = blendRgb(chosenRgb, atmoGlowRgb, mix);
                }

                int litRgb = applyCloseUpShading(chosenRgb, rawLight, bayer, showNightLights, nightGlowSignal, cityLightRgb);
                setPixel(pixels, resolution, px, py, 0xFF000000 | litRgb);
            }
        }

        // 3. Predni cast prstence (pro Chronos)
        if (isRinged) {
            renderDetailedRings(pixels, resolution, cx, cy, R, ringTilt, lx, ly, seedPhase, true);
        }

        return pixels;
    }

    // =========================================================================
    // POMOCNE MATEMATICKE FUNKCE (3D SUM, FBM, RIDGED NOISE, DOMAIN WARP, HASH)
    // =========================================================================

    public static int stringToSeed(String str) {
        int h = (int) 2166136261L;
        String clean = (str == null || str.trim().isEmpty()) ? "0" : str.trim();
        for (int i = 0; i < clean.length(); i++) {
            h ^= clean.charAt(i);
            h *= 16777619;
        }
        return (h == 0) ? 1337 : h;
    }

    private static double hash3D(int ix, int iy, int iz, int seed) {
        int h = ix * 374761393 + iy * 668265263 + iz * 2147483647 + seed * 1442695041;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFFFFFFL) / 4294967296.0;
    }

    private static double noise3D(double x, double y, double z, int seed) {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        int zi = (int) Math.floor(z);

        double xf = x - xi;
        double yf = y - yi;
        double zf = z - zi;

        double u = xf * xf * (3.0 - 2.0 * xf);
        double v = yf * yf * (3.0 - 2.0 * yf);
        double w = zf * zf * (3.0 - 2.0 * zf);

        double n000 = hash3D(xi,     yi,     zi,     seed);
        double n100 = hash3D(xi + 1, yi,     zi,     seed);
        double n010 = hash3D(xi,     yi + 1, zi,     seed);
        double n110 = hash3D(xi + 1, yi + 1, zi,     seed);
        double n001 = hash3D(xi,     yi,     zi + 1, seed);
        double n101 = hash3D(xi + 1, yi,     zi + 1, seed);
        double n011 = hash3D(xi,     yi + 1, zi + 1, seed);
        double n111 = hash3D(xi + 1, yi + 1, zi + 1, seed);

        double x00 = n000 + u * (n100 - n000);
        double x10 = n010 + u * (n110 - n010);
        double x01 = n001 + u * (n101 - n001);
        double x11 = n011 + u * (n111 - n011);

        double y0 = x00 + v * (x10 - x00);
        double y1 = x01 + v * (x11 - x01);

        return y0 + w * (y1 - y0);
    }

    private static double fbm3D(double x, double y, double z, int seed, int octaves) {
        double total = 0.0, amp = 0.5, freq = 1.0, maxVal = 0.0;
        for (int i = 0; i < octaves; i++) {
            total += noise3D(x * freq, y * freq, z * freq, seed + i * 131) * amp;
            maxVal += amp;
            amp *= 0.5;
            freq *= 2.05;
        }
        return total / maxVal;
    }

    private static double ridged3D(double x, double y, double z, int seed, int octaves) {
        double total = 0.0, amp = 0.5, freq = 1.0, maxVal = 0.0;
        for (int i = 0; i < octaves; i++) {
            double n = noise3D(x * freq, y * freq, z * freq, seed + i * 257);
            n = 1.0 - Math.abs(n * 2.0 - 1.0);
            n = n * n;
            total += n * amp;
            maxVal += amp;
            amp *= 0.5;
            freq *= 2.1;
        }
        return total / maxVal;
    }

    private static double warpedFbm3D(double x, double y, double z, int seed, double warpStrength, int octaves) {
        double wx = fbm3D(x + 3.1, y + 1.7, z + 4.2, seed + 11, 3);
        double wy = fbm3D(x + 8.3, y + 2.8, z + 1.4, seed + 23, 3);
        double wz = fbm3D(x + 1.9, y + 6.4, z + 5.1, seed + 37, 3);
        return fbm3D(
            x + (wx - 0.5) * warpStrength,
            y + (wy - 0.5) * warpStrength,
            z + (wz - 0.5) * warpStrength,
            seed + 59,
            octaves
        );
    }

    private static void renderDetailedRings(
            int[] pixels, int resolution, double cx, double cy, double R,
            double ringTilt, double lx, double ly, double seedPhase, boolean isFront
    ) {
        double ringOuter = R * 1.68;
        double ringInner = R * 1.14;
        double cosT = Math.cos(ringTilt);
        double sinT = Math.sin(ringTilt);

        for (int py = 0; py < resolution; py++) {
            for (int px = 0; px < resolution; px++) {
                double dx = px - cx;
                double dy = py - cy;
                double rx = dx * cosT - dy * sinT;
                double ry = (dx * sinT + dy * cosT) * 3.2;

                if ((isFront && ry < 0) || (!isFront && ry >= 0)) continue;

                double rDist = Math.sqrt(rx * rx + ry * ry);
                if (rDist >= ringInner && rDist <= ringOuter) {
                    double normR = (rDist - ringInner) / (ringOuter - ringInner);
                    if ((normR > 0.44 && normR < 0.51) || (normR > 0.76 && normR < 0.79)) continue;

                    double ringWave = Math.sin(normR * 28.0 + seedPhase) * 0.5 + 0.5;
                    int ringRgb;
                    if (normR < 0.04 || normR > 0.96) ringRgb = 0x140B03;
                    else if (ringWave > 0.68) ringRgb = 0xFEF08A;
                    else if (ringWave > 0.35) ringRgb = 0xF59E0B;
                    else if (ringWave > 0.15) ringRgb = 0xB45309;
                    else ringRgb = 0x78350F;

                    double shadowDot = (dx * lx + dy * ly) / R;
                    boolean inPlanetShadow = !isFront && shadowDot < -0.25 && Math.abs(dx * ly - dy * lx) / R < 0.85;
                    if (inPlanetShadow) {
                        ringRgb = scaleRgb(ringRgb, 0.32);
                    }
                    setPixel(pixels, resolution, px, py, 0xFF000000 | ringRgb);
                }
            }
        }
    }

    private static int applyCloseUpShading(
            int rgb, double lightVal, double bayer,
            boolean showNightLights, double nightCityVal, int cityLightRgb
    ) {
        if (showNightLights && lightVal < 0.20 && nightCityVal > 0.78 && cityLightRgb != 0) {
            return cityLightRgb;
        }
        double l = lightVal + (bayer - 0.5) * 0.12;
        double mult;
        if (l > 0.82) mult = 1.14;
        else if (l > 0.56) mult = 1.00;
        else if (l > 0.34) mult = 0.80;
        else if (l > 0.16) mult = 0.56;
        else mult = 0.28;

        return scaleRgb(rgb, mult);
    }

    private static int scaleRgb(int rgb, double mult) {
        int r = clamp((int) Math.round(((rgb >> 16) & 0xFF) * mult), 0, 255);
        int g = clamp((int) Math.round(((rgb >> 8) & 0xFF) * mult), 0, 255);
        int b = clamp((int) Math.round((rgb & 0xFF) * mult), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private static int blendRgb(int rgb1, int rgb2, double mix) {
        int r1 = (rgb1 >> 16) & 0xFF, g1 = (rgb1 >> 8) & 0xFF, b1 = rgb1 & 0xFF;
        int r2 = (rgb2 >> 16) & 0xFF, g2 = (rgb2 >> 8) & 0xFF, b2 = rgb2 & 0xFF;
        int r = clamp((int) Math.round(r1 * (1.0 - mix) + r2 * mix), 0, 255);
        int g = clamp((int) Math.round(g1 * (1.0 - mix) + g2 * mix), 0, 255);
        int b = clamp((int) Math.round(b1 * (1.0 - mix) + b2 * mix), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private static void setPixel(int[] pixels, int resolution, int x, int y, int argb) {
        if (x >= 0 && y >= 0 && x < resolution && y < resolution) {
            pixels[y * resolution + x] = argb;
        }
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    private static int getOutlineColor(BodyType t) {
        switch (t) {
            case TERRAN: return 0x050A18;
            case LAVA:   return 0x0C0404;
            case RINGED: return 0x140B03;
            case GAS:    return 0x170804;
            case ICE:    return 0x04101C;
            case DESERT: return 0x170D03;
            case TOXIC:  return 0x110317;
            default:     return 0x090D16;
        }
    }

    private static int getAtmoGlowColor(BodyType t) {
        switch (t) {
            case TERRAN: return 0x38BDF8;
            case LAVA:   return 0xEF4444;
            case RINGED: return 0xFBBF24;
            case GAS:    return 0xFB923C;
            case ICE:    return 0x7DD3FC;
            case DESERT: return 0xFACC15;
            case TOXIC:  return 0xA3E635;
            default:     return 0;
        }
    }

    private static int getCityLightColor(BodyType t) {
        switch (t) {
            case TERRAN: return 0xFEF08A;
            case LAVA:   return 0xFB923C;
            case ICE:    return 0x67E8F9;
            case DESERT: return 0xFDE047;
            case TOXIC:  return 0xD9F99D;
            default:     return 0;
        }
    }

    private static class Mulberry32 {
        private int state;
        public Mulberry32(int seed) { this.state = seed; }
        public double nextDouble() {
            int t = (state += 0x6D2B79F5);
            t = (t ^ (t >>> 15)) * (t | 1);
            t ^= t + (t ^ (t >>> 7)) * (t | 61);
            return ((t ^ (t >>> 14)) & 0xFFFFFFFFL) / 4294967296.0;
        }
    }
}
