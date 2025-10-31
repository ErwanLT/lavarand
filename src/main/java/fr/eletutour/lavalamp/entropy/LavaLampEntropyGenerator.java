package fr.eletutour.lavalamp.entropy;

import fr.eletutour.lavalamp.crypto.HmacDRBG;
import fr.eletutour.lavalamp.simulation.LavaLampSimulator;
import fr.eletutour.lavalamp.util.EntropyUtils;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Encapsule la logique de génération d'entropie à partir de la simulation
 * de lampe à lave pour être réutilisée par différentes interfaces (CLI, GUI).
 */
public class LavaLampEntropyGenerator {

    private final LavaLampSimulator simulator;
    private final VirtualLavaEntropy entropyCollector;
    private final HmacDRBG drbg;
    private final int qualityFactor;
    private int generationCount = 0;
    private static final int SAVE_INTERVAL = 5; // Save every 5 generations

    public LavaLampEntropyGenerator(int width, int height, int nbBlobs, int qualityFactor, byte[] initialSeed) {
        this(width, height, nbBlobs, qualityFactor, initialSeed, null);
    }

    public LavaLampEntropyGenerator(int width, int height, int nbBlobs, int qualityFactor, byte[] initialSeed, java.awt.Color fixedColor) {
        this.simulator = new LavaLampSimulator(width, height, nbBlobs, initialSeed, fixedColor);
        this.entropyCollector = new VirtualLavaEntropy();
        this.qualityFactor = qualityFactor;

        // Initialisation du DRBG avec la première image de la simulation
        BufferedImage firstSnapshot = simulator.stepAndRender();
        byte[] firstSeedBytes = entropyCollector.snapshotToBytes(firstSnapshot, this.qualityFactor);
        byte[] whitenedSeed = EntropyUtils.sha256(firstSeedBytes);
        this.drbg = new HmacDRBG(whitenedSeed);
    }

    /**
     * Avance la simulation, collecte l'entropie, la mélange avec du bruit système,
     * ré-ensemence le DRBG et génère un nombre d'octets aléatoires.
     *
     * @param numBytes le nombre d'octets aléatoires à générer.
     * @return un tableau d'octets aléatoires.
     */
    public synchronized byte[] mixEntropyAndGenerate(int numBytes) {
        // Avance la simulation de plusieurs images pour augmenter la diffusion
        for (int f = 0; f < 6; f++) {
            simulator.stepAndRender();
        }

        // Collecte la nouvelle entropie de l'image actuelle
        BufferedImage snap = simulator.stepAndRender();
        byte[] chunk = entropyCollector.snapshotToBytes(snap, qualityFactor);
        byte[] whitened = EntropyUtils.sha256(chunk);

        // **Important** : mélange avec un petit bloc de bruit système pour éviter le déterminisme pur
        byte[] sysNoise = new byte[16];
        new SecureRandom().nextBytes(sysNoise);
        byte[] toReseed = EntropyUtils.concat(whitened, sysNoise);

        // Ré-ensemence le DRBG avec le SHA256 du mélange
        drbg.reseed(EntropyUtils.sha256(toReseed));

        // Génère et retourne les octets
        byte[] generatedBytes = drbg.generate(numBytes);

        // Sauvegarde l'image périodiquement pour inspection
        generationCount++;
        if (generationCount % SAVE_INTERVAL == 0) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            String filename = "snapshot_" + timestamp + ".png";
            entropyCollector.saveSnapshot(snap, filename);
        }

        return generatedBytes;
    }

    /**
     * Retourne le simulateur pour permettre le rendu externe (ex: GUI).
     * @return l'instance de LavaLampSimulator.
     */
    public LavaLampSimulator getSimulator() {
        return simulator;
    }
}
