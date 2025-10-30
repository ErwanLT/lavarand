package fr.eletutour.lavalamp.app;


import fr.eletutour.lavalamp.entropy.LavaLampEntropyGenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LavaLampApp extends JPanel {

    private final LavaLampEntropyGenerator entropyGenerator;

    public LavaLampApp(LavaLampEntropyGenerator entropyGenerator) {
        this.entropyGenerator = entropyGenerator;

        // Timer pour l'animation (redessiner à ~30 FPS)
        Timer timer = new Timer(33, e -> repaint());
        timer.start();

        // Collecte d'entropie toutes les 3 secondes
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                // La méthode `mixEntropyAndGenerate` s'occupe de tout le cycle
                byte[] random = entropyGenerator.mixEntropyAndGenerate(48);
                System.out.println("Entropy mixed (" + random.length + " bytes): "
                        + HexFormat.of().formatHex(random));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, 2, 3, TimeUnit.SECONDS);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // On récupère le simulateur depuis le générateur pour le rendu
        BufferedImage frame = entropyGenerator.getSimulator().stepAndRender();
        g.drawImage(frame, 0, 0, getWidth(), getHeight(), null);
    }

    static void main(String[] args) {
        // Paramètres
        int width = 640, height = 480;
        int nbBlobs = 16;
        int qualityFactor = 4;

        // Graine initiale
        byte[] seed = new byte[32];
        new SecureRandom().nextBytes(seed);

        // Initialisation du générateur d'entropie unifié
        LavaLampEntropyGenerator entropyGenerator = new LavaLampEntropyGenerator(width, height, nbBlobs, qualityFactor, seed);

        // Création de la fenêtre
        JFrame frame = new JFrame("Lampe à lave virtuelle — Entropie vivante");
        LavaLampApp panel = new LavaLampApp(entropyGenerator);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(width, height);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.add(panel);
        frame.setVisible(true);
    }
}
