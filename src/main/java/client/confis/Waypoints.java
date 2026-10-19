package client.confis;

import net.minecraft.world.phys.Vec3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Draws waypoint markers projected onto the screen. */
public final class Waypoints {
    public static final int[] PALETTE = {0xFF5555, 0x55FF55, 0x5555FF, 0xFFFF55, 0xFF55FF, 0x55FFFF, 0xFFAA00};

    private Waypoints() {}

    public static int nextColor(Config c) {
        return PALETTE[c.waypointList.size() % PALETTE.length];
    }

    public static void render(GuiGraphics g, Minecraft mc, Config.ModuleCfg mod) {
        Config c = Config.get();
        if (c.waypointList.isEmpty() || mc.level == null || mc.player == null) return;
        String dim = mc.level.dimension().toString();

        Vec3 eye = mc.player.getEyePosition();
        Vec3 f = mc.player.getViewVector(1.0F);
        double rx = -f.z, rz = f.x;
        double rl = Math.sqrt(rx * rx + rz * rz);
        if (rl < 1e-6) return;
        rx /= rl; rz /= rl;
        double ux = -rz * f.y;
        double uy = rz * f.x - rx * f.z;
        double uz = rx * f.y;

        int W = g.guiWidth(), H = g.guiHeight();
        double tanHalf = Math.tan(Math.toRadians(mc.options.fov().get()) / 2.0);
        double aspect = (double) W / H;

        for (Waypoint w : c.waypointList) {
            if (!dim.equals(w.dim)) continue;
            double dx = w.x + 0.5 - eye.x, dy = w.y + 0.5 - eye.y, dz = w.z + 0.5 - eye.z;
            double cz = dx * f.x + dy * f.y + dz * f.z;
            if (cz < 0.1) continue;
            double cx = dx * rx + dz * rz;
            double cy = dx * ux + dy * uy + dz * uz;
            int sx = (int) (W / 2.0 + (cx / cz) / (tanHalf * aspect) * (W / 2.0));
            int sy = (int) (H / 2.0 - (cy / cz) / tanHalf * (H / 2.0));
            if (sx < 4 || sx > W - 4 || sy < 12 || sy > H - 12) continue;

            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            String label = w.name + " [" + (int) dist + "m]";
            int tw = mc.font.width(label);
            if (mod.showBg) g.fill(sx - tw / 2 - 2, sy - 16, sx + tw / 2 + 2, sy - 5, mod.bg);
            g.drawCenteredString(mc.font, label, sx, sy - 14, mod.text);
            g.fill(sx - 3, sy - 3, sx + 4, sy + 4, 0xFF000000);
            g.fill(sx - 2, sy - 2, sx + 3, sy + 3, 0xFF000000 | w.color);
        }
    }
}
