package fr.eletutour.lavalamp.simulation;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Simulateur simple de "lampe à lave" virtuelle :
 * - plusieurs blobs elliptiques se déplacent et s'entremêlent
 * - rendu sur BufferedImage
 *
 * Non déterministe si on mélange le seed avec SecureRandom/jitter.
 */
public class LavaLampSimulator {

    public static class Blob {
        double x, y;          // center (0..1)
        double vx, vy;        // velocity (units per second)
        double radius;        // relative (0..0.5)
        Color color;
    }

    private final int width;
    private final int height;
    private final List<Blob> blobs = new ArrayList<>();
    private final SecureRandom secureRandom;
    private final double damping = 0.999;
    private long lastUpdateNs;
    private final Color color1;
    private final Color color2;

    public LavaLampSimulator(int width, int height, int nbBlobs, byte[] seed) {
        this(width, height, nbBlobs, seed, null, null);
    }

    public LavaLampSimulator(int width, int height, int nbBlobs, byte[] seed, Color color1) {
        this(width, height, nbBlobs, seed, color1, null);
    }

    public LavaLampSimulator(int width, int height, int nbBlobs, byte[] seed, Color color1, Color color2) {
        this.width = width;
        this.height = height;
        this.secureRandom = new SecureRandom(seed != null ? seed : SecureRandom.getSeed(16));
        this.color1 = color1;
        this.color2 = color2;
        initBlobs(nbBlobs);
        this.lastUpdateNs = System.nanoTime();
    }

    private void initBlobs(int nb) {
        for (int i = 0; i < nb; i++) {
            Blob b = new Blob();
            // positions random in [0.2, 0.8] to avoid clipping
            b.x = 0.2 + secureRandom.nextDouble() * 0.6;
            b.y = 0.2 + secureRandom.nextDouble() * 0.6;
            // velocities small
            b.vx = (secureRandom.nextDouble() - 0.5) * 0.2;
            b.vy = (secureRandom.nextDouble() - 0.5) * 0.2;
            b.radius = 0.08 + secureRandom.nextDouble() * 0.2;

            Color baseColor = null;
            if (color1 != null && color2 != null) {
                baseColor = (i % 2 == 0) ? color1 : color2;
            } else if (color1 != null) {
                baseColor = color1;
            }

            if (baseColor != null) {
                float[] hsb = Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), null);
                float newBrightness = (float) (hsb[2] * (0.5 + secureRandom.nextDouble() * 0.5));
                b.color = Color.getHSBColor(hsb[0], hsb[1], newBrightness);
            } else {
                // choose a random palette color with alpha
                float h = secureRandom.nextFloat(); // random hue
                float s = (float) (0.7 + secureRandom.nextDouble() * 0.3);
                float br = (float) (0.6 + secureRandom.nextDouble() * 0.4);
                b.color = Color.getHSBColor(h, s, br);
            }
            blobs.add(b);
        }
    }

    /**
     * Met à jour la simulation selon dt (en secondes) et renvoie une image rendue.
     */
    public synchronized BufferedImage stepAndRender() {
        long now = System.nanoTime();
        double dt = Math.max(1e-6, (now - lastUpdateNs) / 1_000_000_000.0);
        lastUpdateNs = now;

        // update blobs positions with simple physics + gentle random perturbation
        for (Blob b : blobs) {
            // perturbation pour casser la périodicité (utilise secureRandom)
            double px = (secureRandom.nextDouble() - 0.5) * 0.02;
            double py = (secureRandom.nextDouble() - 0.5) * 0.02;

            b.vx = (b.vx + px) * damping;
            b.vy = (b.vy + py) * damping;

            b.x += b.vx * dt;
            b.y += b.vy * dt;

            // bounce within [0,1] with soft reflection
            if (b.x < 0.0) { b.x = 0.0; b.vx = Math.abs(b.vx) * 0.6; }
            if (b.x > 1.0) { b.x = 1.0; b.vx = -Math.abs(b.vx) * 0.6; }
            if (b.y < 0.0) { b.y = 0.0; b.vy = Math.abs(b.vy) * 0.6; }
            if (b.y > 1.0) { b.y = 1.0; b.vy = -Math.abs(b.vy) * 0.6; }
        }

        // render to image
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            // background (dark)
            g.setComposite(AlphaComposite.SrcOver);
            g.setColor(new Color(0, 0, 0));
            g.fillRect(0, 0, width, height);

            // render blobs with additive blending for lava-like glow
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (Blob b : blobs) {
                int cx = (int) (b.x * width);
                int cy = (int) (b.y * height);
                int r = (int) (b.radius * Math.min(width, height));
                // gradient circle for soft edges
                drawSoftBlob(g, cx, cy, r, b.color);
            }

            // slight global blur effect imitation: draw a translucent overlay
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.06f));
            g.setColor(new Color(200, 200, 200)); // Light gray
            // draw a very faint gradient to simulate light diffusion
            g.fillOval(width/4, height/4, width/2, height/2);
        } finally {
            g.dispose();
        }
        return img;
    }

    private void drawSoftBlob(Graphics2D g, int cx, int cy, int r, Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        // draw concentric circles with decreasing alpha for soft edges
        for (int i = r; i > 0; i -= Math.max(1, r / 12)) {
            float alpha = Math.max(0.02f, (float) i / r * 0.6f);

            // Create a gradient effect by varying brightness
            float brightness = hsb[2] * (0.5f + (1.0f - (float)i / r) * 1.0f);
            Color c = Color.getHSBColor(hsb[0], hsb[1], Math.min(1.0f, brightness));

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.min(255, (int)(alpha*255))));
            int d = i * 2;
            g.fillOval(cx - i, cy - i, d, d);
        }
    }
}
