package fr.eletutour.lavalamp;

import java.awt.image.BufferedImage;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

public class MainVirtualLava {

    public static void main(String[] args) throws Exception {
        // param
        int width = 640;
        int height = 480;
        int nbBlobs = 6;
        int qualityFactor = 4; // downsample factor for snapshots

        // seed mixing: SecureRandom + low-entropy jitter to prevent full determinism
        byte[] initialSeed = new byte[32];
        new java.security.SecureRandom().nextBytes(initialSeed);
        // optional: mix with System.nanoTime bits
        long t = System.nanoTime();
        for (int i = 0; i < 8; i++) initialSeed[i] ^= (byte) ((t >> (i * 8)) & 0xff);

        LavaLampSimulator sim = new LavaLampSimulator(width, height, nbBlobs, initialSeed);
        VirtualLavaEntropy snapshotter = new VirtualLavaEntropy();

        // initial snapshot -> seed DRBG
        BufferedImage img0 = sim.stepAndRender();
        byte[] chunk0 = snapshotter.snapshotToBytes(img0, qualityFactor);
        byte[] seed0 = EntropyUtils.sha256(chunk0); // whitening
        HmacDRBG drbg = new HmacDRBG(seed0);
        System.out.println("Initial seed (sha256): " + HexFormat.of().formatHex(seed0));

        // generate multiple cycles: advance simulation a few frames, take snapshot, reseed, generate random
        for (int cycle = 0; cycle < 8; cycle++) {
            // step simulation multiple frames to increase diffusion
            for (int f = 0; f < 6; f++) {
                sim.stepAndRender();
                TimeUnit.MILLISECONDS.sleep(60); // ~60 ms per frame
            }
            BufferedImage snap = sim.stepAndRender();
            byte[] chunk = snapshotter.snapshotToBytes(snap, qualityFactor);
            byte[] whitened = EntropyUtils.sha256(chunk);

            // **Important** : mix whitened with a small sysRandom chunk to prevent pure determinism
            byte[] sysNoise = new byte[16];
            new java.security.SecureRandom().nextBytes(sysNoise);
            byte[] toReseed = EntropyUtils.concat(whitened, sysNoise);

            drbg.reseed(EntropyUtils.sha256(toReseed)); // reseed with SHA256 of mix
            byte[] out = drbg.generate(48);
            System.out.println("Cycle " + cycle + " -> generated: " + HexFormat.of().formatHex(out).substring(0, 96) + "...");

            // clear temporary arrays
            java.util.Arrays.fill(sysNoise, (byte) 0);
        }
    }
}
