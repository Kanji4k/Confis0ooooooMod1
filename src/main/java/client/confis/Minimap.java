package client.confis;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.client.multiplayer.ClientLevel;

public final class Minimap {
    private static final int N = 40;      // cells per side
    private static final int CELL = 2;    // pixels per cell
    public static final int SIZE = N * CELL;
    private static final int UNKNOWN = 0xFF101010;

    private static final int[] colors = new int[N * N];
    private static long lastUpdate;
    private static final BlockPos.MutableBlockPos POS = new BlockPos.MutableBlockPos();

    private Minimap() {}

    /** Draws the map with its top-left corner at (x0, y0). Total size: SIZE+4 wide, SIZE+16 tall. */
    public static void render(GuiGraphics g, Minecraft mc, Config.ModuleCfg c, int x0, int y0) {
        if (mc.level == null || mc.player == null) return;
        Config cfg = Config.get();
        long now = System.currentTimeMillis();
        int zoom = Math.max(1, Math.min(4, cfg.mapZoom));
        if (now - lastUpdate > 250) {
            update(mc.level, mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ(), zoom);
            lastUpdate = now;
        }

        if (c.showBg) g.fill(x0, y0, x0 + SIZE + 4, y0 + SIZE + 4, c.bg);
        int x = x0 + 2, y = y0 + 2;

        for (int j = 0; j < N; j++) {
            int i = 0;
            while (i < N) {
                int col = colors[j * N + i];
                int k = i + 1;
                while (k < N && colors[j * N + k] == col) k++;
                g.fill(x + i * CELL, y + j * CELL, x + k * CELL, y + (j + 1) * CELL, col);
                i = k;
            }
        }

        String dim = mc.level.dimension().toString();
        double px = mc.player.getX(), pz = mc.player.getZ();
        for (Waypoint w : cfg.waypointList) {
            if (!dim.equals(w.dim)) continue;
            double cx = (w.x + 0.5 - px) / zoom, cz = (w.z + 0.5 - pz) / zoom;
            cx = Math.max(-N / 2 + 1, Math.min(N / 2 - 1, cx));
            cz = Math.max(-N / 2 + 1, Math.min(N / 2 - 1, cz));
            int sx = x + SIZE / 2 + (int) (cx * CELL);
            int sy = y + SIZE / 2 + (int) (cz * CELL);
            g.fill(sx - 2, sy - 2, sx + 3, sy + 3, 0xFF000000);
            g.fill(sx - 1, sy - 1, sx + 2, sy + 2, 0xFF000000 | w.color);
        }

        int mx = x + SIZE / 2, my = y + SIZE / 2;
        g.fill(mx - 1, my - 1, mx + 2, my + 2, 0xFFFFFFFF);
        double yaw = Math.toRadians(mc.player.getYRot());
        int tx = mx + (int) Math.round(-Math.sin(yaw) * 5);
        int ty = my + (int) Math.round(Math.cos(yaw) * 5);
        g.fill(tx, ty, tx + 2, ty + 2, c.accent);

        g.drawString(mc.font, "N", x + SIZE / 2 - 2, y + 1, c.text, true);
        String coords = mc.player.getBlockX() + ", " + mc.player.getBlockY() + ", " + mc.player.getBlockZ();
        g.drawString(mc.font, coords, x0 + SIZE + 4 - mc.font.width(coords), y0 + SIZE + 6, c.text, true);
    }

    private static void update(ClientLevel level, int px, int py, int pz, int zoom) {
        boolean ceiling = level.dimensionType().hasCeiling();
        for (int j = 0; j < N; j++) {
            for (int i = 0; i < N; i++) {
                int wx = px + (i - N / 2) * zoom;
                int wz = pz + (j - N / 2) * zoom;
                colors[j * N + i] = sample(level, wx, wz, py, zoom, ceiling);
            }
        }
    }

    private static int sample(ClientLevel level, int wx, int wz, int py, int zoom, boolean ceiling) {
        if (!level.hasChunk(wx >> 4, wz >> 4)) return UNKNOWN;
        int y = surfaceY(level, wx, wz, py, ceiling);
        if (y == Integer.MIN_VALUE) return UNKNOWN;

        POS.set(wx, y, wz);
        BlockState state = level.getBlockState(POS);
        MapColor mc = state.getMapColor(level, POS);
        int rgb = (mc == MapColor.NONE) ? 0x101010 : mc.col;

        double shade = 1.0;
        if (level.hasChunk(wx >> 4, (wz - zoom) >> 4)) {
            int yn = surfaceY(level, wx, wz - zoom, py, ceiling);
            if (yn != Integer.MIN_VALUE) {
                if (y > yn) shade = 1.18;
                else if (y < yn) shade = 0.78;
            }
        }
        int r = Math.min(255, (int) (((rgb >> 16) & 0xFF) * shade));
        int gr = Math.min(255, (int) (((rgb >> 8) & 0xFF) * shade));
        int b = Math.min(255, (int) ((rgb & 0xFF) * shade));
        return 0xFF000000 | (r << 16) | (gr << 8) | b;
    }

    private static int surfaceY(ClientLevel level, int x, int z, int py, boolean ceiling) {
        if (!ceiling) {
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            return y <= level.getMinY() ? Integer.MIN_VALUE : y;
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = py + 2; y > py - 48 && y > level.getMinY(); y--) {
            p.set(x, y, z);
            if (!level.getBlockState(p).isAir()) {
                p.set(x, y + 1, z);
                if (level.getBlockState(p).isAir()) return y;
            }
        }
        return Integer.MIN_VALUE;
    }
}
