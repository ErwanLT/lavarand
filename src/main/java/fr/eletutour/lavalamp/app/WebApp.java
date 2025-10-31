package fr.eletutour.lavalamp.app;

import com.google.gson.Gson;
import fr.eletutour.lavalamp.entropy.LavaLampEntropyGenerator;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import static spark.Spark.*;

public class WebApp {

    private static final int NUM_LAMPS = 10;
    private static final Map<Integer, LavaLampEntropyGenerator> entropyGenerators = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        // Initialisation des générateurs d'entropie
        int width = 256, height = 192;
        Color[] colors = {Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE, Color.MAGENTA, Color.CYAN};
        IntStream.range(0, NUM_LAMPS).forEach(i -> {
            LavaLampEntropyGenerator generator = new LavaLampEntropyGenerator(
                    width, height, 12, 8, SecureRandom.getSeed(32), colors[i % colors.length]
            );
            entropyGenerators.put(i, generator);
        });


        // Configuration de Spark
        port(4567);
        staticFiles.location("/public");

        // --- API Endpoints ---

        // Endpoint pour obtenir une image de la simulation (en Base64)
        get("/api/frame", (req, res) -> {
            int lampId = 0;
            try {
                if (req.queryParams("id") != null) {
                    lampId = Integer.parseInt(req.queryParams("id"));
                }
            } catch (NumberFormatException e) {
                halt(400, "Invalid 'id' parameter");
            }

            LavaLampEntropyGenerator generator = entropyGenerators.get(lampId);
            if (generator == null) {
                halt(404, "Lamp with id " + lampId + " not found.");
            }

            BufferedImage frame = generator.getSimulator().stepAndRender();

            // Convertir l'image en PNG Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(frame, "png", baos);
            String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());

            res.type("application/json");
            return new Gson().toJson(Map.of("image", base64Image, "id", lampId));
        });

        // Endpoint pour générer des octets aléatoires
        get("/api/random", (req, res) -> {
            int requestedBytes = 32;
            try {
                if (req.queryParams("bytes") != null) {
                    requestedBytes = Integer.parseInt(req.queryParams("bytes"));
                }
            } catch (NumberFormatException e) {
                halt(400, "Invalid 'bytes' parameter");
            }

            final int bytes = requestedBytes; // Use a final variable

            // On va mixer l'entropie de toutes les lampes
            byte[] finalEntropy = new byte[bytes];

            // Use a standard for-each loop to avoid lambda variable scope issues
            for (LavaLampEntropyGenerator generator : entropyGenerators.values()) {
                byte[] randomBytes = generator.mixEntropyAndGenerate(bytes);
                for (int i = 0; i < bytes; i++) {
                    finalEntropy[i] ^= randomBytes[i];
                }
            }

            String hexString = HexFormat.of().formatHex(finalEntropy);

            res.type("application/json");
            return new Gson().toJson(Map.of("random", hexString));
        });

        System.out.println("Serveur démarré sur http://localhost:4567 avec " + NUM_LAMPS + " lampes.");
    }
}
