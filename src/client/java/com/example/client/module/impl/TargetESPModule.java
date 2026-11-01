package com.example.client.module.impl;

import com.example.client.category.Category;
import com.example.client.module.Module;
import com.example.client.setting.BooleanSetting;
import com.example.client.setting.ModeSetting;
import com.example.client.setting.NumberSetting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;

/**
 * Moduł TargetESP:
 * Wyraziste i stabilne wizualizacje celu z kwadratem 3D:
 * - Blokada skoku (Ground Lock): gdy przeciwnik skacze (np. krytuje lub bunnyhopuje), box i esp NIE SKACZĄ!
 * - 3 dopracowane style z kwadratem ze zrzutu ekranu
 */
public class TargetESPModule extends Module {

    public final ModeSetting style = new ModeSetting("Styl", "Kwadrat ze Spiralą",
            "Kwadrat ze Spiralą", "Cyber Kwadrat (Skaner)", "Pływający Kwadrat (Jello)");

    public final ModeSetting boxColor = new ModeSetting("Kolor Kwadratu", "Cyan",
            "Cyan", "Różowy / Neon", "Fiolet", "Biały", "Złoty", "Tęcza");

    public final ModeSetting spiralColor = new ModeSetting("Kolor Spirali", "Różowy / Neon",
            "Różowy / Neon", "Cyan", "Fiolet", "Złoty", "Tęcza", "Biały");

    public final ModeSetting boxMode = new ModeSetting("Typ Boxa", "Narożniki",
            "Narożniki", "Pełna Kostka", "Podwójna Ramka");

    public final BooleanSetting groundLock = new BooleanSetting("Blokada skoku", true);
    public final BooleanSetting scannerBeam = new BooleanSetting("Laser skanera", true);
    public final BooleanSetting groundRing = new BooleanSetting("Ślad u stóp", true);
    public final BooleanSetting glowingCore = new BooleanSetting("Biały rdzeń", true);

    public final NumberSetting speed = new NumberSetting("Prędkość", 2.2, 0.5, 5.0, 0.1);
    public final NumberSetting boxPadding = new NumberSetting("Rozmiar kwadratu", 0.08, 0.0, 0.25, 0.01);
    public final NumberSetting spiralRadius = new NumberSetting("Promień spirali", 0.60, 0.35, 1.2, 0.05);

    private LivingEntity lastTarget = null;
    private long lastTargetTime = 0;

    // Stabilizacja wysokości (Anti-Jump / Ground Lock)
    private double smoothGroundY = -9999;
    private int lastGroundTargetId = -1;

    public TargetESPModule() {
        super("TargetESP", "Wyraziste i stabilne wizualizacje celu z kwadratem 3D", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
        addSettings(style, boxColor, spiralColor, boxMode, groundLock, scannerBeam, groundRing, glowingCore, speed, boxPadding, spiralRadius);
        setToggled(true);
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (!isToggled() || mc.player == null) return;

        long now = System.currentTimeMillis();
        Entity crosshairEntity = mc.crosshairPickEntity;

        if (crosshairEntity instanceof LivingEntity && crosshairEntity != mc.player && ((LivingEntity) crosshairEntity).isAlive()) {
            this.lastTarget = (LivingEntity) crosshairEntity;
            this.lastTargetTime = now;
        }

        // Płynne podtrzymanie celu
        if (lastTarget == null || !lastTarget.isAlive() || (now - lastTargetTime > 750)) {
            smoothGroundY = -9999;
            return;
        }

        MultiBufferSource consumers = context.consumers();
        if (consumers == null) return;

        Vec3 cameraPos = context.camera().getPosition();
        PoseStack poseStack = context.matrixStack();
        VertexConsumer buf = consumers.getBuffer(RenderType.lines());

        double entityY = lastTarget.getY();

        // BLOKADA SKOKU: gdy przeciwnik skacze, poziom Y jest zablokowany na ziemi
        if (groundLock.isEnabled()) {
            if (lastGroundTargetId != lastTarget.getId() || smoothGroundY <= -9000) {
                smoothGroundY = entityY;
                lastGroundTargetId = lastTarget.getId();
            }

            if (lastTarget.onGround()) {
                smoothGroundY = entityY;
            } else {
                // Gdy gracz skacze do góry, nie podążamy za skokiem!
                // Aktualizujemy tylko gdy spadł znacznie niżej (np. zeskoczył z klifu)
                if (entityY < smoothGroundY - 1.2) {
                    smoothGroundY += (entityY - smoothGroundY) * 0.16;
                }
            }
            entityY = smoothGroundY;
        } else {
            smoothGroundY = entityY;
        }

        double targetX = lastTarget.getX() - cameraPos.x;
        double targetY = entityY - cameraPos.y;
        double targetZ = lastTarget.getZ() - cameraPos.z;
        float height = lastTarget.getBbHeight();

        float time = (float) ((now % 10000000L) / 1000.0);
        String currentStyle = style.getMode();

        switch (currentStyle) {
            case "Cyber Kwadrat (Skaner)" -> renderCyberBox(buf, poseStack, lastTarget, targetX, targetY, targetZ, height, time);
            case "Pływający Kwadrat (Jello)" -> renderJelloScanner(buf, poseStack, targetX, targetY, targetZ, height, time);
            default -> renderBoxWithSpiral(buf, poseStack, lastTarget, targetX, targetY, targetZ, height, time);
        }
    }

    // =========================================================================
    // STYL 1: KWADRAT ZE SPIRALĄ (ZE ZDJĘCIA: SZEŚCIAN + CZYSTA NEONOWA SPIRALA)
    // =========================================================================
    private void renderBoxWithSpiral(VertexConsumer buf, PoseStack poseStack, LivingEntity target,
                                     double tx, double ty, double tz, float entityHeight, float time) {
        // 1. Render stabilnego kwadratu (sześcianu ze zdjęcia)
        renderMainBox(buf, poseStack, target, tx, ty, tz, entityHeight, time);

        // 2. Render czystej, ostrej, lśniącej spirali 3D
        poseStack.pushPose();
        PoseStack.Pose pose = poseStack.last();
        Matrix4f mat = pose.pose();

        int segments = 80;
        float revs = 2.0f;
        float rad = (float) spiralRadius.getValue();
        float rotSpeed = (float) speed.getValue();
        float baseAngle = time * rotSpeed;
        boolean hasCore = glowingCore.isEnabled();

        int[] sColor = getSpiralRgb(time);
        int sr = sColor[0], sg = sColor[1], sb = sColor[2];

        for (int i = 0; i < segments; i++) {
            float p1 = (float) i / segments;
            float p2 = (float) (i + 1) / segments;

            float y1 = (float) (ty + (p1 * entityHeight * 0.94f) + (entityHeight * 0.03f));
            float y2 = (float) (ty + (p2 * entityHeight * 0.94f) + (entityHeight * 0.03f));

            float a1 = baseAngle + (float) (p1 * revs * 2.0 * Math.PI);
            float a2 = baseAngle + (float) (p2 * revs * 2.0 * Math.PI);

            float x1 = (float) (tx + Math.cos(a1) * rad);
            float z1 = (float) (tz + Math.sin(a1) * rad);
            float x2 = (float) (tx + Math.cos(a2) * rad);
            float z2 = (float) (tz + Math.sin(a2) * rad);

            float fade = (float) Math.sin(p1 * Math.PI);
            int alpha = (int) (255 * Math.min(1.0f, fade * 2.5f));

            // Centralny rdzeń (biały/jasny)
            if (hasCore) {
                line(buf, mat, pose, x1, y1, z1, x2, y2, z2, 255, 235, 255, alpha);
            }

            // Ostra, gęsta poświata neonowa
            float off = 0.012f;
            line(buf, mat, pose, x1, y1 + off, z1, x2, y2 + off, z2, sr, sg, sb, (int)(alpha * 0.85f));
            line(buf, mat, pose, x1, y1 - off, z1, x2, y2 - off, z2, sr, sg, sb, (int)(alpha * 0.85f));
        }

        // 3. Pierścień u podstawy
        if (groundRing.isEnabled()) {
            drawGroundRing(buf, mat, pose, tx, (float) ty, tz, rad * 1.1f, sr, sg, sb, 180);
        }

        poseStack.popPose();
    }

    // =========================================================================
    // STYL 2: CYBER KWADRAT (SKANER 3D Z LASEREM)
    // =========================================================================
    private void renderCyberBox(VertexConsumer buf, PoseStack poseStack, LivingEntity target,
                                double tx, double ty, double tz, float entityHeight, float time) {
        renderMainBox(buf, poseStack, target, tx, ty, tz, entityHeight, time);

        // Laserowy skaner omiatający cel w górę i w dół wewnątrz kwadratu
        if (scannerBeam.isEnabled()) {
            float pad = (float) boxPadding.getValue();
            float halfW = (target.getBbWidth() * 0.5f) + pad;
            float fullH = entityHeight + (pad * 2f);

            float minX = (float) (tx - halfW);
            float maxX = (float) (tx + halfW);
            float minZ = (float) (tz - halfW);
            float maxZ = (float) (tz + halfW);
            float minY = (float) (ty - pad);

            // Ruch harmoniczny (sine wave)
            float wave = (float) ((Math.sin(time * speed.getValue() * 1.8f) + 1.0) * 0.5);
            float scanY = minY + (wave * fullH);

            poseStack.pushPose();
            PoseStack.Pose pose = poseStack.last();
            Matrix4f mat = pose.pose();

            int[] bCol = getBoxRgb(time);
            int r = bCol[0], g = bCol[1], b = bCol[2];

            // Poziomy świecący kwadrat lasera
            line(buf, mat, pose, minX, scanY, minZ, maxX, scanY, minZ, 255, 255, 255, 255);
            line(buf, mat, pose, maxX, scanY, minZ, maxX, scanY, maxZ, 255, 255, 255, 255);
            line(buf, mat, pose, maxX, scanY, maxZ, minX, scanY, maxZ, 255, 255, 255, 255);
            line(buf, mat, pose, minX, scanY, maxZ, minX, scanY, minZ, 255, 255, 255, 255);

            // Poświata lasera
            float off = 0.015f;
            line(buf, mat, pose, minX, scanY + off, minZ, maxX, scanY + off, minZ, r, g, b, 190);
            line(buf, mat, pose, maxX, scanY + off, minZ, maxX, scanY + off, maxZ, r, g, b, 190);
            line(buf, mat, pose, maxX, scanY + off, maxZ, minX, scanY + off, maxZ, r, g, b, 190);
            line(buf, mat, pose, minX, scanY + off, maxZ, minX, scanY + off, minZ, r, g, b, 190);

            line(buf, mat, pose, minX, scanY - off, minZ, maxX, scanY - off, minZ, r, g, b, 190);
            line(buf, mat, pose, maxX, scanY - off, minZ, maxX, scanY - off, maxZ, r, g, b, 190);
            line(buf, mat, pose, maxX, scanY - off, maxZ, minX, scanY - off, maxZ, r, g, b, 190);
            line(buf, mat, pose, minX, scanY - off, maxZ, minX, scanY - off, minZ, r, g, b, 190);

            poseStack.popPose();
        }
    }

    // =========================================================================
    // STYL 3: PŁYWAJĄCY KWADRAT (JELLO SCANNER 3D)
    // =========================================================================
    private void renderJelloScanner(VertexConsumer buf, PoseStack poseStack,
                                    double tx, double ty, double tz, float entityHeight, float time) {
        poseStack.pushPose();
        PoseStack.Pose pose = poseStack.last();
        Matrix4f mat = pose.pose();

        float rotSpeed = (float) speed.getValue();
        float baseRadius = (float) spiralRadius.getValue() * 1.15f;
        int[] rgb = getBoxRgb(time);
        int r = rgb[0], g = rgb[1], b = rgb[2];

        // Falowanie góra/dół od stóp do głowy
        float wave = (float) ((Math.sin(time * rotSpeed * 1.5f) + 1.0) * 0.5);
        float currentY = (float) (ty + 0.1f + wave * (entityHeight - 0.2f));

        // Obrót kwadratu wokół własnej osi Y
        float angle = time * rotSpeed * 1.2f;

        // Rysowanie obracającego się kwadratu
        drawRotatedSquare(buf, mat, pose, tx, currentY, tz, baseRadius, angle, r, g, b, 255);
        // Lekka poświata
        drawRotatedSquare(buf, mat, pose, tx, currentY + 0.015f, tz, baseRadius * 1.02f, angle, 255, 255, 255, 180);
        drawRotatedSquare(buf, mat, pose, tx, currentY - 0.015f, tz, baseRadius * 1.02f, angle, r, g, b, 120);

        // Pionowe linie łączące narożniki ku ziemi (efekt hologramu)
        for (int i = 0; i < 4; i++) {
            float cornerAngle = angle + (float) (i * Math.PI / 2.0);
            float cx = (float) (tx + Math.cos(cornerAngle) * baseRadius);
            float cz = (float) (tz + Math.sin(cornerAngle) * baseRadius);
            line(buf, mat, pose, cx, currentY, cz, cx, (float) ty, cz, r, g, b, 60);
        }

        // Pierścień pod stopami
        if (groundRing.isEnabled()) {
            drawGroundRing(buf, mat, pose, tx, (float) ty, tz, baseRadius, r, g, b, 160);
        }

        poseStack.popPose();
    }

    private void drawRotatedSquare(VertexConsumer buf, Matrix4f mat, PoseStack.Pose pose,
                                   double tx, float y, double tz, float radius, float angle,
                                   int r, int g, int b, int a) {
        float[] x = new float[4];
        float[] z = new float[4];
        for (int i = 0; i < 4; i++) {
            float aRad = angle + (float) (i * Math.PI / 2.0 + Math.PI / 4.0);
            x[i] = (float) (tx + Math.cos(aRad) * radius);
            z[i] = (float) (tz + Math.sin(aRad) * radius);
        }

        for (int i = 0; i < 4; i++) {
            int next = (i + 1) % 4;
            line(buf, mat, pose, x[i], y, z[i], x[next], y, z[next], r, g, b, a);
        }
    }

    // =========================================================================
    // GŁÓWNY RENDER KWADRATU / BOXA 3D (STABILNY, NIE SKACZE PRZY SKOKACH)
    // =========================================================================
    private void renderMainBox(VertexConsumer buf, PoseStack poseStack, LivingEntity target,
                              double tx, double ty, double tz, float entityHeight, float time) {
        float pad = (float) boxPadding.getValue();
        float halfW = (target.getBbWidth() * 0.5f) + pad;
        float fullH = entityHeight + (pad * 2f);

        float minX = (float) (tx - halfW);
        float maxX = (float) (tx + halfW);
        float minZ = (float) (tz - halfW);
        float maxZ = (float) (tz + halfW);
        float minY = (float) (ty - pad);
        float maxY = (float) (minY + fullH);

        int[] rgb = getBoxRgb(time);
        int r = rgb[0], g = rgb[1], b = rgb[2];

        poseStack.pushPose();
        PoseStack.Pose pose = poseStack.last();
        Matrix4f mat = pose.pose();

        String bMode = boxMode.getMode();
        if (bMode.equalsIgnoreCase("Narożniki")) {
            // Precyzyjne, taktyczne narożniki jak na screenie
            float lenX = (maxX - minX) * 0.28f;
            float lenY = (maxY - minY) * 0.20f;
            float lenZ = (maxZ - minZ) * 0.28f;

            // 8 narożników (3 linie z każdego wierzchołka)
            renderCorner(buf, mat, pose, minX, minY, minZ, lenX, lenY, lenZ, r, g, b);
            renderCorner(buf, mat, pose, maxX, minY, minZ, -lenX, lenY, lenZ, r, g, b);
            renderCorner(buf, mat, pose, minX, minY, maxZ, lenX, lenY, -lenZ, r, g, b);
            renderCorner(buf, mat, pose, maxX, minY, maxZ, -lenX, lenY, -lenZ, r, g, b);

            renderCorner(buf, mat, pose, minX, maxY, minZ, lenX, -lenY, lenZ, r, g, b);
            renderCorner(buf, mat, pose, maxX, maxY, minZ, -lenX, -lenY, lenZ, r, g, b);
            renderCorner(buf, mat, pose, minX, maxY, maxZ, lenX, -lenY, -lenZ, r, g, b);
            renderCorner(buf, mat, pose, maxX, maxY, maxZ, -lenX, -lenY, -lenZ, r, g, b);
        } else {
            // Pełna kostka
            drawBoxLines(buf, mat, pose, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, 255);
            if (bMode.equalsIgnoreCase("Podwójna Ramka")) {
                drawBoxLines(buf, mat, pose, minX - 0.015f, minY - 0.015f, minZ - 0.015f,
                        maxX + 0.015f, maxY + 0.015f, maxZ + 0.015f, 255, 255, 255, 120);
            }
        }

        poseStack.popPose();
    }

    private void renderCorner(VertexConsumer buf, Matrix4f mat, PoseStack.Pose pose,
                              float x, float y, float z, float dx, float dy, float dz,
                              int r, int g, int b) {
        line(buf, mat, pose, x, y, z, x + dx, y, z, r, g, b, 255);
        line(buf, mat, pose, x, y, z, x, y + dy, z, r, g, b, 255);
        line(buf, mat, pose, x, y, z, x, y, z + dz, r, g, b, 255);

        // Biały akcent w samym narożniku dla wyrazistości
        if (glowingCore.isEnabled()) {
            line(buf, mat, pose, x, y, z, x + dx * 0.35f, y, z, 255, 255, 255, 255);
            line(buf, mat, pose, x, y, z, x, y + dy * 0.35f, z, 255, 255, 255, 255);
            line(buf, mat, pose, x, y, z, x, y, z + dz * 0.35f, 255, 255, 255, 255);
        }
    }

    private void drawBoxLines(VertexConsumer buf, Matrix4f mat, PoseStack.Pose pose,
                              float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                              int r, int g, int b, int a) {
        line(buf, mat, pose, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        line(buf, mat, pose, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        line(buf, mat, pose, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        line(buf, mat, pose, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

        line(buf, mat, pose, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(buf, mat, pose, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        line(buf, mat, pose, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        line(buf, mat, pose, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

        line(buf, mat, pose, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        line(buf, mat, pose, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(buf, mat, pose, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        line(buf, mat, pose, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }

    private void drawGroundRing(VertexConsumer buf, Matrix4f mat, PoseStack.Pose pose,
                                double cx, float cy, double cz, float rad, int r, int g, int b, int a) {
        int segs = 32;
        for (int i = 0; i < segs; i++) {
            float a1 = (float) (i * 2.0 * Math.PI / segs);
            float a2 = (float) ((i + 1) * 2.0 * Math.PI / segs);
            float x1 = (float) (cx + Math.cos(a1) * rad);
            float z1 = (float) (cz + Math.sin(a1) * rad);
            float x2 = (float) (cx + Math.cos(a2) * rad);
            float z2 = (float) (cz + Math.sin(a2) * rad);
            line(buf, mat, pose, x1, cy, z1, x2, cy, z2, r, g, b, a);
        }
    }

    private int[] getBoxRgb(float time) {
        return switch (boxColor.getMode()) {
            case "Różowy / Neon" -> new int[]{255, 30, 220};
            case "Fiolet" -> new int[]{185, 45, 255};
            case "Biały" -> new int[]{245, 245, 255};
            case "Złoty" -> new int[]{255, 210, 35};
            case "Tęcza" -> {
                float hue = (time * 0.25f) % 1.0f;
                int c = Color.HSBtoRGB(hue, 0.85f, 1.0f);
                yield new int[]{(c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF};
            }
            default -> new int[]{0, 235, 255}; // Cyan ze zdjęcia
        };
    }

    private int[] getSpiralRgb(float time) {
        return switch (spiralColor.getMode()) {
            case "Cyan" -> new int[]{0, 235, 255};
            case "Fiolet" -> new int[]{185, 45, 255};
            case "Złoty" -> new int[]{255, 210, 35};
            case "Biały" -> new int[]{245, 245, 255};
            case "Tęcza" -> {
                float hue = ((time * 0.3f) + 0.3f) % 1.0f;
                int c = Color.HSBtoRGB(hue, 0.85f, 1.0f);
                yield new int[]{(c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF};
            }
            default -> new int[]{255, 30, 220}; // Różowy / Neon ze zdjęcia
        };
    }

    private void line(VertexConsumer buf, Matrix4f mat, PoseStack.Pose pose,
                      float x1, float y1, float z1, float x2, float y2, float z2,
                      int r, int g, int b, int a) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0f) return;
        float nx = dx / len, ny = dy / len, nz = dz / len;
        buf.addVertex(mat, x1, y1, z1).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
        buf.addVertex(mat, x2, y2, z2).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
    }
}
