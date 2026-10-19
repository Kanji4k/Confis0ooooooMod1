package client.confis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Config {
    /** Per-mod settings. Colors are ARGB ints. Position is a fraction of the screen. */
    public static class ModuleCfg {
        public boolean enabled = true;
        public double fx, fy;
        public int bg = 0x80000000;
        public int text = 0xFFFFFFFF;
        public int accent = 0xFFFFFFFF;
        public boolean showBg = true;
        public boolean favorite = false;
        /** Size multiplier for this HUD element (0.5 - 3.0). */
        public float scale = 1.0f;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("confis0oooooo.json");
    private static Config instance;

    public Map<String, ModuleCfg> modules = new LinkedHashMap<>();
    public int accent = 0xFFFFFFFF;
    public int panelBg = 0xF2050505;
    public boolean clock24h = true;
    public boolean nameBadge = true;
    public int mapZoom = 2;
    public List<Waypoint> waypointList = new ArrayList<>();
    /** AI chat: your own Anthropic API key (stored in config/confis0oooooo.json) and model. */
    public String aiKey = "";
    public String aiModel = "claude-sonnet-4-6";
    /** Dropped-item size: default for all items + per-item overrides keyed by item id (e.g. "diamond"). */
    public float dropScale = 1.0f;
    /** Hitbox switches by type key (crystal, exp, friendly, hostile, firework, pearl, windcharge) and nametags. */
    public boolean hitboxMaster = true;
    public Map<String, Boolean> hitboxes = new LinkedHashMap<>();
    public boolean nametags = true;
    public Map<String, Float> itemScales = new LinkedHashMap<>();

    public static Config get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        try (Reader r = Files.newBufferedReader(FILE)) {
            instance = GSON.fromJson(r, Config.class);
        } catch (Exception ignored) {
        }
        if (instance == null) instance = new Config();
        if (instance.modules == null) instance.modules = new LinkedHashMap<>();
        if (instance.waypointList == null) instance.waypointList = new ArrayList<>();
        if (instance.hitboxes == null) instance.hitboxes = new LinkedHashMap<>();
        if (instance.itemScales == null) instance.itemScales = new LinkedHashMap<>();
        if (instance.aiModel == null || instance.aiModel.isBlank()) instance.aiModel = "claude-sonnet-4-6";
        if (instance.dropScale <= 0f) instance.dropScale = 1.0f;
        for (ModuleCfg m : instance.modules.values()) if (m.scale <= 0f) m.scale = 1.0f;
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(FILE)) {
            GSON.toJson(get(), w);
        } catch (Exception ignored) {
        }
    }
}
