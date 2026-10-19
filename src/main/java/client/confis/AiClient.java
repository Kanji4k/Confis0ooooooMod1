package client.confis;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import javax.imageio.ImageIO;

/** Talks to the Anthropic Messages API. The key comes from the user's own config, never from the jar. */
public final class AiClient {
    public record Msg(String role, String text, String image) {}

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final String SYSTEM =
            "You are Confis0oooooo AI, an assistant built into a Minecraft Java Edition client (1.21.x). "
            + "You are an expert on Minecraft: items, blocks, mobs, redstone, crafting, enchanting, potions, "
            + "commands, biomes, structures, speedrunning, PvP and servers. You can also answer general "
            + "real-life questions. When a screenshot is attached, describe and use what you see. "
            + "Be honest when you are unsure. The chat window is small, so answer concisely in plain text "
            + "(no markdown headings, no tables).";

    private AiClient() {}

    public static String ask(List<Msg> history) throws Exception {
        Config cfg = Config.get();
        if (cfg.aiKey == null || cfg.aiKey.isBlank())
            throw new IllegalStateException("No API key yet. Click KEY and paste your Anthropic API key.");

        // only keep the two most recent images to keep requests small
        int imagesLeft = 2;
        boolean[] keep = new boolean[history.size()];
        for (int i = history.size() - 1; i >= 0; i--)
            if (history.get(i).image() != null && imagesLeft-- > 0) keep[i] = true;

        JsonObject body = new JsonObject();
        body.addProperty("model", cfg.aiModel);
        body.addProperty("max_tokens", 1024);
        body.addProperty("system", SYSTEM);
        JsonArray msgs = new JsonArray();
        for (int i = 0; i < history.size(); i++) {
            Msg m = history.get(i);
            JsonObject o = new JsonObject();
            o.addProperty("role", m.role());
            JsonArray content = new JsonArray();
            if (keep[i]) {
                JsonObject img = new JsonObject();
                img.addProperty("type", "image");
                JsonObject src = new JsonObject();
                src.addProperty("type", "base64");
                src.addProperty("media_type", "image/png");
                src.addProperty("data", m.image());
                img.add("source", src);
                content.add(img);
            }
            JsonObject t = new JsonObject();
            t.addProperty("type", "text");
            t.addProperty("text", m.text());
            content.add(t);
            o.add("content", content);
            msgs.add(o);
        }
        body.add("messages", msgs);

        HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.anthropic.com/v1/messages"))
                .timeout(Duration.ofSeconds(90))
                .header("content-type", "application/json")
                .header("x-api-key", cfg.aiKey.trim())
                .header("anthropic-version", "2023-06-01")
                .POST(BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> res = HTTP.send(req, BodyHandlers.ofString());
        JsonObject j = JsonParser.parseString(res.body()).getAsJsonObject();
        if (res.statusCode() != 200) {
            String msg = j.has("error") ? j.getAsJsonObject("error").get("message").getAsString() : res.body();
            throw new IOException("HTTP " + res.statusCode() + ": " + msg);
        }
        StringBuilder sb = new StringBuilder();
        for (var el : j.getAsJsonArray("content")) {
            JsonObject b = el.getAsJsonObject();
            if ("text".equals(b.get("type").getAsString())) sb.append(b.get("text").getAsString());
        }
        return sb.toString().trim();
    }

    /** Base64 PNG of the newest file in .minecraft/screenshots (press F2 in game to take one), or null. */
    public static String latestScreenshot() throws IOException {
        Path dir = FabricLoader.getInstance().getGameDir().resolve("screenshots");
        if (!Files.isDirectory(dir)) return null;
        Path best;
        try (var s = Files.list(dir)) {
            best = s.filter(p -> p.toString().toLowerCase().endsWith(".png"))
                    .max(Comparator.comparingLong(p -> p.toFile().lastModified())).orElse(null);
        }
        if (best == null) return null;
        byte[] bytes = Files.readAllBytes(best);
        if (bytes.length > 3_500_000) {           // API limit is 5 MB: shrink big screenshots
            BufferedImage src = ImageIO.read(best.toFile());
            int w = 1280, h = Math.max(1, src.getHeight() * w / src.getWidth());
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            var g2 = out.createGraphics();
            g2.drawImage(src.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
            g2.dispose();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(out, "png", bos);
            bytes = bos.toByteArray();
        }
        return Base64.getEncoder().encodeToString(bytes);
    }
}
