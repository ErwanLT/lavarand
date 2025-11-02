package fr.eletutour.lavalamp.app;


import fr.eletutour.lavalamp.entropy.LavaLampEntropyGenerator;

import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

public class MainVirtualLava {

    public static void main(String[] args) throws Exception {
        // Paramètres de la simulation
        int width = 640;
        int height = 480;
        int nbBlobs = 6;
        int qualityFactor = 4; // Facteur de sous-échantillonnage pour les captures

        // Création de la graine initiale
        byte[] initialSeed = new byte[32];
        new java.security.SecureRandom().nextBytes(initialSeed);
        long t = System.nanoTime();
        for (int i = 0; i < 8; i++) initialSeed[i] ^= (byte) ((t >> (i * 8)) & 0xff);

        // Initialisation du générateur d'entropie
        java.awt.Color color1 = new java.awt.Color(255, 100, 0); // Orange
        java.awt.Color color2 = new java.awt.Color(0, 50, 255);   // Bleu
        LavaLampEntropyGenerator entropyGenerator = new LavaLampEntropyGenerator(width, height, nbBlobs, qualityFactor, initialSeed, color1, color2);

        System.out.println("Générateur initialisé. Démarrage des cycles de génération...");

        // Génération de plusieurs cycles de données aléatoires
        for (int cycle = 0; cycle < 8; cycle++) {
            byte[] out = entropyGenerator.mixEntropyAndGenerate(48);
            System.out.println("Cycle " + cycle + " -> generated: " + HexFormat.of().formatHex(out).substring(0, 96) + "...");

            // Pause pour simuler un rythme et permettre à l'état du système de changer
            TimeUnit.MILLISECONDS.sleep(200);
        }
    }
}
