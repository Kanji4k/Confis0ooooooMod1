package client.confis;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;

public final class Modules {
    public static final List<Module> ALL = new ArrayList<>();

    static {
        ALL.add(new TextModule("fps", "FPS", "INFO", 0.010, 0.015, mc -> "FPS: " + Hud.fps()));
        ALL.add(new TextModule("ping", "Ping", "INFO", 0.010, 0.055, mc -> {
            int ping = -1;
            if (mc.getConnection() != null && mc.player != null) {
                PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                if (info != null) ping = info.getLatency();
            }
            return "Ping: " + (ping < 0 ? "--" : ping + " ms");
        }));
        ALL.add(new TextModule("tps", "Server TPS", "INFO", 0.010, 0.095, mc -> {
            double t = TpsTracker.get();
            return "TPS: " + (t < 0 ? "--" : String.format("%.1f", t));
        }));
        ALL.add(new TextModule("cps", "CPS", "INFO", 0.010, 0.135,
                mc -> "CPS: " + Cps.left() + " | " + Cps.right()));
        ALL.add(new TextModule("clock", "Clock", "INFO", 0.010, 0.175, mc -> {
            DateTimeFormatter f = DateTimeFormatter.ofPattern(Config.get().clock24h ? "HH:mm" : "hh:mm a");
            return "Time: " + LocalTime.now().format(f);
        }));
        ALL.add(new TextModule("day", "Day", "INFO", 0.010, 0.215,
                mc -> "Day: " + (mc.level == null ? 0 : mc.level.getGameTime() / 24000L)));
        ALL.add(new ArmorModule());
        ALL.add(new KeystrokesModule());
        ALL.add(new MinimapModule());
        ALL.add(new WaypointsModule());
        ALL.add(new ExtraModules.EffectTimerModule());
        ALL.add(new ExtraModules.ScoreboardModule());
        ALL.add(new ExtraModules.ReachModule());
        ALL.add(new ExtraModules.RangeModule());
    }

    private Modules() {}

    public static Module byId(String id) {
        for (Module m : ALL) if (m.id.equals(id)) return m;
        return null;
    }

    // ---------------------------------------------------------------- text box
    public static class TextModule extends Module {
        private final Function<Minecraft, String> text;

        TextModule(String id, String name, String cat, double x, double y, Function<Minecraft, String> text) {
            super(id, name, cat, x, y);
            this.text = text;
        }

        @Override public int width(Minecraft mc) { return mc.font.width(text.apply(mc)) + 8; }
        @Override public int height(Minecraft mc) { return 13; }

        @Override
        public void draw(GuiGraphics g, Minecraft mc, Config.ModuleCfg c, int x, int y, boolean preview) {
            String s = text.apply(mc);
            int w = mc.font.width(s) + 8;
            if (c.showBg) g.fill(x, y, x + w, y + 13, c.bg);
            g.drawString(mc.font, s, x + 4, y + 3, c.text, true);
        }
    }

    // ------------------------------------------------------------------- armor
    public static class ArmorModule extends Module {
        private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};

        ArmorModule() { super("armor", "Armor Status", "HUD", 0.010, 0.400); }

        @Override public int width(Minecraft mc) { return 58; }
        @Override public int height(Minecraft mc) { return SLOTS.length * 19 - 2; }

        @Override
        public void draw(GuiGraphics g, Minecraft mc, Config.ModuleCfg c, int x, int y, boolean preview) {
            if (mc.player == null) return;
            int row = 0;
            for (EquipmentSlot slot : SLOTS) {
                int ry = y + row * 19;
                row++;
                ItemStack stack = mc.player.getItemBySlot(slot);
                if (stack.isEmpty()) {
                    if (preview) {
                        if (c.showBg) g.fill(x, ry, x + 58, ry + 17, c.bg);
                        g.drawString(mc.font, "-", x + 6, ry + 5, c.text, true);
                    }
                    continue;
                }
                if (c.showBg) g.fill(x, ry, x + 58, ry + 17, c.bg);
                g.renderItem(stack, x + 1, ry);
                if (stack.isDamageableItem()) {
                    int left = stack.getMaxDamage() - stack.getDamageValue();
                    g.drawString(mc.font, String.valueOf(left), x + 22, ry + 5, c.text, true);
                } else if (stack.getCount() > 1) {
                    g.drawString(mc.font, "x" + stack.getCount(), x + 22, ry + 5, c.text, true);
                }
            }
        }
    }

    // -------------------------------------------------------------- keystrokes
    public static class KeystrokesModule extends Module {
        KeystrokesModule() {
            super("keystrokes", "Keystrokes", "HUD", 0.010, 0.690);
            defAccent = 0xB0FFFFFF;
            accentLabel = "Pressed key color";
        }

        @Override public int width(Minecraft mc) { return 70; }
        @Override public int height(Minecraft mc) { return 84; }

        @Override
        public void draw(GuiGraphics g, Minecraft mc, Config.ModuleCfg c, int x, int y, boolean preview) {
            int box = 22, gap = 2;
            var o = mc.options;
            key(g, mc, c, "W", x + box + gap, y, box, box, o.keyUp.isDown());
            key(g, mc, c, "A", x, y + box + gap, box, box, o.keyLeft.isDown());
            key(g, mc, c, "S", x + box + gap, y + box + gap, box, box, o.keyDown.isDown());
            key(g, mc, c, "D", x + 2 * (box + gap), y + box + gap, box, box, o.keyRight.isDown());
            int my = y + 2 * (box + gap);
            int mw = (box * 3 + gap) / 2;
            key(g, mc, c, "LMB " + Cps.left(), x, my, mw, box, o.keyAttack.isDown());
            key(g, mc, c, "RMB " + Cps.right(), x + mw + gap, my, mw, box, o.keyUse.isDown());
            key(g, mc, c, "", x, my + box + gap, box * 3 + gap * 2, 12, o.keyJump.isDown());
        }

        private static void key(GuiGraphics g, Minecraft mc, Config.ModuleCfg c, String label,
                                int x, int y, int w, int h, boolean down) {
            if (down) g.fill(x, y, x + w, y + h, c.accent);
            else if (c.showBg) g.fill(x, y, x + w, y + h, c.bg);
            if (!label.isEmpty()) {
                g.drawCenteredString(mc.font, label, x + w / 2, y + (h - 8) / 2, down ? 0xFF000000 : c.text);
            }
        }
    }

    // ----------------------------------------------------------------- minimap
    public static class MinimapModule extends Module {
        MinimapModule() {
            super("minimap", "Minimap", "MAP", 0.890, 0.060);
            defBg = 0xFF000000;
            defAccent = 0xFF9A9A9A;
            accentLabel = "Facing marker color";
        }

        @Override public int width(Minecraft mc) { return Minimap.SIZE + 4; }
        @Override public int height(Minecraft mc) { return Minimap.SIZE + 16; }

        @Override
        public void draw(GuiGraphics g, Minecraft mc, Config.ModuleCfg c, int x, int y, boolean preview) {
            Minimap.render(g, mc, c, x, y);
        }
    }

    // --------------------------------------------------------------- waypoints
    /** Not a box on the HUD: draws markers in the world view. Only has colors + on/off. */
    public static class WaypointsModule extends Module {
        WaypointsModule() { super("waypoints", "Waypoints", "MAP", 0, 0); defBg = 0x80000000; }

        @Override public boolean movable() { return false; }
        @Override public int width(Minecraft mc) { return 0; }
        @Override public int height(Minecraft mc) { return 0; }
        @Override public void draw(GuiGraphics g, Minecraft mc, Config.ModuleCfg c, int x, int y, boolean preview) {}
    }
}
