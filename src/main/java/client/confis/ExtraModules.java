package client.confis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/** New HUD mods: Effect Timer and a fully custom Scoreboard. Both get colors, background and scale. */
public final class ExtraModules {
    private ExtraModules() {}

    private static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n >= 1 && n <= 10 ? r[n] : String.valueOf(n);
    }

    // ------------------------------------------------------------ effect timer
    public static class EffectTimerModule extends Module {
        private record Row(String name, String time, boolean low) {}

        public EffectTimerModule() {
            super("effects", "Effect Timer", "HUD", 0.010, 0.300);
            defAccent = 0xFFFF5555;
            accentLabel = "Expiring (under 10s) color";
        }

        private List<Row> rows(Minecraft mc) {
            List<Row> out = new ArrayList<>();
            if (mc.player != null) {
                List<MobEffectInstance> list = new ArrayList<>(mc.player.getActiveEffects());
                list.sort(Comparator.comparingInt(MobEffectInstance::getDuration).reversed());
                for (MobEffectInstance e : list) {
                    String name = e.getEffect().value().getDisplayName().getString() + " " + roman(e.getAmplifier() + 1);
                    int secs = Math.max(0, e.getDuration() / 20);
                    String t = e.isInfiniteDuration() ? "inf" : (secs / 60) + ":" + String.format("%02d", secs % 60);
                    out.add(new Row(name, t, !e.isInfiniteDuration() && secs < 10));
                }
            }
            if (out.isEmpty() && mc.screen instanceof ConfigScreen) {        // placeholder while editing the HUD
                out.add(new Row("Speed II", "1:30", false));
                out.add(new Row("Regeneration I", "0:08", true));
            }
            return out;
        }

        @Override
        public int width(Minecraft mc) {
            int w = 70;
            for (Row r : rows(mc)) w = Math.max(w, mc.font.width(r.name()) + mc.font.width(r.time()) + 18);
            return w;
        }

        @Override public int height(Minecraft mc) { return Math.max(1, rows(mc).size()) * 12 + 1; }

        @Override
        public void draw(GuiGraphics g, Minecraft mc, Config.ModuleCfg c, int x, int y, boolean preview) {
            List<Row> rows = rows(mc);
            if (rows.isEmpty()) return;
            int w = width(mc);
            boolean blink = (System.currentTimeMillis() / 400) % 2 == 0;
            for (int i = 0; i < rows.size(); i++) {
                Row r = rows.get(i);
                int ry = y + i * 12;
                if (c.showBg) g.fill(x, ry, x + w, ry + 11, c.bg);
                g.drawString(mc.font, r.name(), x + 4, ry + 2, c.text, true);
                int col = r.low() && blink ? c.accent : c.text;
                g.drawString(mc.font, r.time(), x + w - 4 - mc.font.width(r.time()), ry + 2, col, true);
            }
        }
    }

    // -------------------------------------------------------------- scoreboard
    public static class ScoreboardModule extends Module {
        private record Line(Component name, String score) {}
        private Component title = Component.empty();

        public ScoreboardModule() {
            super("scoreboard", "Scoreboard", "HUD", 0.850, 0.350);
            defAccent = 0xFFFF5555;
            accentLabel = "Score number color";
        }

        private List<Line> lines(Minecraft mc) {
            List<Line> out = new ArrayList<>();
            if (mc.level != null) {
                Scoreboard sb = mc.level.getScoreboard();
                Objective obj = sb.getDisplayObjective(DisplaySlot.SIDEBAR);
                if (obj != null) {
                    title = obj.getDisplayName();
                    List<PlayerScoreEntry> entries = new ArrayList<>(sb.listPlayerScores(obj));
                    entries.removeIf(e -> e.owner().startsWith("#"));
                    entries.sort(Comparator.comparingInt(PlayerScoreEntry::value).reversed());
                    for (PlayerScoreEntry e : entries.subList(0, Math.min(15, entries.size()))) {
                        Component n = PlayerTeam.formatNameForTeam(sb.getPlayersTeam(e.owner()), e.ownerName());
                        out.add(new Line(n, String.valueOf(e.value())));
                    }
                }
            }
            if (out.isEmpty() && mc.screen instanceof ConfigScreen) {
                title = Component.literal("Scoreboard");
                out.add(new Line(Component.literal("Kills"), "12"));
                out.add(new Line(Component.literal("Coins"), "340"));
            }
            return out;
        }

        @Override
        public int width(Minecraft mc) {
            List<Line> l = lines(mc);
            if (l.isEmpty()) return 60;
            int w = mc.font.width(title);
            for (Line r : l) w = Math.max(w, mc.font.width(r.name()) + mc.font.width(r.score()) + 14);
            return w + 8;
        }

        @Override public int height(Minecraft mc) { return lines(mc).isEmpty() ? 13 : (lines(mc).size() + 1) * 10 + 4; }

        @Override
        public void draw(GuiGraphics g, Minecraft mc, Config.ModuleCfg c, int x, int y, boolean preview) {
            List<Line> l = lines(mc);
            if (l.isEmpty()) return;
            int w = width(mc), h = height(mc);
            if (c.showBg) g.fill(x, y, x + w, y + h, c.bg);
            g.drawString(mc.font, title, x + (w - mc.font.width(title)) / 2, y + 3, c.text, true);
            for (int i = 0; i < l.size(); i++) {
                int ry = y + 13 + i * 10;
                g.drawString(mc.font, l.get(i).name(), x + 4, ry, c.text, true);
                g.drawString(mc.font, l.get(i).score(), x + w - 4 - mc.font.width(l.get(i).score()), ry, c.accent, true);
            }
        }
    }

    // ------------------------------------------------------------ reach display
    public static class ReachModule extends Module {
        public ReachModule() { super("reach", "Reach Display", "INFO", 0.010, 0.255); }
        private String text() {
            double r = ReachTracker.last();
            return "Reach: " + (r < 0 ? "--" : String.format("%.2f", r) + " blocks");
        }
        @Override public int width(Minecraft mc) { return mc.font.width(text()) + 8; }
        @Override public int height(Minecraft mc) { return 13; }
        @Override
        public void draw(GuiGraphics g, Minecraft mc, Config.ModuleCfg c, int x, int y, boolean preview) {
            if (c.showBg) g.fill(x, y, x + width(mc), y + 13, c.bg);
            g.drawString(mc.font, text(), x + 4, y + 3, c.text, true);
        }
    }

    // -------------------------------------------------------------- range display
    /** Shows the distance to whatever you are aiming at and whether it is inside your attack reach. */
    public static class RangeModule extends Module {
        public RangeModule() {
            super("range", "Hit Range", "INFO", 0.010, 0.295);
            defAccent = 0xFF55FF55;
            accentLabel = "In-range color";
        }
        private double dist(Minecraft mc) {
            if (mc.player == null || mc.crosshairPickEntity == null) return -1;
            var b = mc.crosshairPickEntity.getBoundingBox();
            var eye = mc.player.getEyePosition();
            var p = new net.minecraft.world.phys.Vec3(Math.max(b.minX, Math.min(eye.x, b.maxX)),
                    Math.max(b.minY, Math.min(eye.y, b.maxY)), Math.max(b.minZ, Math.min(eye.z, b.maxZ)));
            return eye.distanceTo(p);
        }
        private String text(Minecraft mc) {
            double d = dist(mc);
            double max = mc.player == null ? 3.0 : mc.player.entityInteractionRange();
            return d < 0 ? "Range: -- / " + String.format("%.1f", max) : "Range: " + String.format("%.2f", d) + " / " + String.format("%.1f", max);
        }
        @Override public int width(Minecraft mc) { return mc.font.width(text(mc)) + 8; }
        @Override public int height(Minecraft mc) { return 13; }
        @Override
        public void draw(GuiGraphics g, Minecraft mc, Config.ModuleCfg c, int x, int y, boolean preview) {
            double d = dist(mc);
            boolean in = d >= 0 && mc.player != null && d <= mc.player.entityInteractionRange();
            if (c.showBg) g.fill(x, y, x + width(mc), y + 13, c.bg);
            g.drawString(mc.font, text(mc), x + 4, y + 3, in ? c.accent : c.text, true);
        }
    }
}
