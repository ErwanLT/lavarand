package fr.eletutour.lavalamp.entropy;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.imageio.ImageIO;

/**
 * Transforme un BufferedImage de la lampe virtuelle en chunk d'entropie brut.
 * - downsample
 * - conversion grayscale
 * - mélange avec du bruit SecureRandom et du jitter système
 */
public class VirtualLavaEntropy {

    private final SecureRandom sysRandom = new SecureRandom();

    /**
     * Convertit l'image en tableau d'octets (grayscale downsampled)
     * qualityFactor : 1 = full resolution, 4 = 1/4 width & height
     */
    public byte[] snapshotToBytes(BufferedImage img, int qualityFactor) {
        if (img == null) return new byte[0];
        int w = Math.max(1, img.getWidth() / qualityFactor);
        int h = Math.max(1, img.getHeight() / qualityFactor);

        ByteBuffer buf = ByteBuffer.allocate(w * h + 48); // reserve extra
        for (int y = 0; y < img.getHeight(); y += qualityFactor) {
            for (int x = 0; x < img.getWidth(); x += qualityFactor) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = (rgb) & 0xff;
                // grayscale
                int gray = (r + g + b) / 3;
                buf.put((byte) (gray & 0xff));
            }
        }

        // add system jitter (nanoTime)
        long now = System.nanoTime();
        for (int i = 0; i < 8; i++) buf.put((byte) ((now >> (i * 8)) & 0xff));

        // add some SecureRandom noise
        byte[] noise = new byte[32];
        sysRandom.nextBytes(noise);
        buf.put(noise);

        int len = buf.position();
        byte[] out = Arrays.copyOf(buf.array(), len);
        // wipe sensitive buffers if needed (not strictly necessary in Java GC)
        Arrays.fill(noise, (byte) 0);
        return out;
    }

    /**
     * Sauvegarde un BufferedImage dans un fichier PNG.
     * @param img L'image à sauvegarder.
     * @param filename Le chemin du fichier où sauvegarder l'image.
     */
    public void saveSnapshot(BufferedImage img, String filename) {
        try {
            File outputfile = new File(filename);
            ImageIO.write(img, "png", outputfile);
            System.out.println("Snapshot saved to: " + filename);
        } catch (IOException e) {
            System.err.println("Error saving snapshot: " + e.getMessage());
        }
    }
}
