package fr.eletutour.lavalamp.util;

import java.security.MessageDigest;
import java.util.Arrays;

public class EntropyUtils {

    public static byte[] sha256(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Estimation très simple de l'entropie de Shannon (bits par octet * taille).
     * Utile pour les alertes et monitoring — ce n'est pas une preuve formelle.
     */
    public static double shannonEntropy(byte[] data) {
        if (data.length == 0) return 0.0;
        int[] freq = new int[256];
        for (byte b : data) freq[b & 0xff]++;
        double entropy = 0.0;
        for (int f : freq) {
            if (f == 0) continue;
            double p = (double) f / data.length;
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        // total bits of entropy = entropy (bits/byte) * number of bytes
        return entropy * data.length;
    }

    public static byte[] concat(byte[] a, byte[] b) {
        byte[] out = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
