package fr.eletutour.lavalamp.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

public class HmacDRBG {

    private byte[] K; // clé interne
    private byte[] V; // valeur interne
    private final String HMAC_ALGO = "HmacSHA256";

    public HmacDRBG(byte[] seed) {
        // Initialisation selon le principe HMAC-DRBG (simplifié)
        K = new byte[32]; // 32 bytes = 256 bits
        Arrays.fill(K, (byte) 0x00);
        V = new byte[32];
        Arrays.fill(V, (byte) 0x01);
        reseedInternal(seed);
    }

    private Mac hmac() throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGO);
        mac.init(new SecretKeySpec(K, HMAC_ALGO));
        return mac;
    }

    private void reseedInternal(byte[] seedMaterial) {
        try {
            Mac mac = hmac();
            // K = HMAC(K, V || 0x00 || seed)
            mac.update(V);
            mac.update((byte) 0x00);
            if (seedMaterial != null) mac.update(seedMaterial);
            K = mac.doFinal();

            mac = hmac();
            mac.update(V);
            V = mac.doFinal();

            if (seedMaterial != null) {
                mac = hmac();
                mac.update(V);
                mac.update((byte) 0x01);
                mac.update(seedMaterial);
                K = mac.doFinal();

                mac = hmac();
                mac.update(V);
                V = mac.doFinal();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Reseed with new entropy chunk. */
    public synchronized void reseed(byte[] seedMaterial) {
        reseedInternal(seedMaterial);
    }

    /** Generate output bytes. */
    public synchronized byte[] generate(int n) {
        try {
            Mac mac = hmac();
            byte[] output = new byte[n];
            int pos = 0;
            while (pos < n) {
                mac = hmac();
                mac.update(V);
                V = mac.doFinal();
                int toCopy = Math.min(V.length, n - pos);
                System.arraycopy(V, 0, output, pos, toCopy);
                pos += toCopy;
            }
            // after generate, reseed update K and V with no additional data
            reseedInternal(null);
            return output;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
