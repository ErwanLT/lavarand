package fr.eletutour.lavalamp;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LavaLampApp extends JPanel {

    private final LavaLampSimulator simulator;

    public LavaLampApp(LavaLampSimulator simulator, VirtualLavaEntropy entropyCollector, HmacDRBG drbg) {
        this.simulator = simulator;

        // Animation timer (redraw ~30 FPS)
        Timer timer = new Timer(33, e -> repaint());
        timer.start();

        // Entropy collection every 3 seconds
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                BufferedImage snapshot = simulator.stepAndRender();
                byte[] entropy = entropyCollector.snapshotToBytes(snapshot, 4);
                byte[] whitened = EntropyUtils.sha256(entropy);
                drbg.reseed(whitened);

                byte[] random = drbg.generate(16);
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
        BufferedImage frame = simulator.stepAndRender();
        g.drawImage(frame, 0, 0, getWidth(), getHeight(), null);
    }


    static void main(String[] args) {
        // Initialisation du simulateur
        int width = 640, height = 480;
        byte[] seed = new byte[32];
        new SecureRandom().nextBytes(seed);

        LavaLampSimulator simulator = new LavaLampSimulator(width, height, 6, seed);
        VirtualLavaEntropy entropyCollector = new VirtualLavaEntropy();

        byte[] firstSnapshot = entropyCollector.snapshotToBytes(simulator.stepAndRender(), 4);
        byte[] firstSeed = EntropyUtils.sha256(firstSnapshot);
        HmacDRBG drbg = new HmacDRBG(firstSeed);

        JFrame frame = new JFrame("Lampe à lave virtuelle — Entropie vivante");
        LavaLampApp panel = new LavaLampApp(simulator, entropyCollector, drbg);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(width, height);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.add(panel);
        frame.setVisible(true);
    }
}
