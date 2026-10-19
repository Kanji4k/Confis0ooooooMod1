package client.confis;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Create a waypoint by typing its name and X / Y / Z (and choosing the dimension). */
public class WaypointAddScreen extends ConfisScreen {
    private static final String[] DIMS = {"overworld", "the_nether", "the_end"};
    private static final String[] DIM_LABEL = {"Overworld", "Nether", "End"};

    private final EditBox name, x, y, z;
    private int dim = 0, colorIdx;
    private String error = "";

    public WaypointAddScreen(Screen parent) {
        super("Add Waypoint", parent);
        var f = mc.font;
        name = new EditBox(f, 0, 0, 100, 14, Component.literal("Name"));
        x = new EditBox(f, 0, 0, 100, 14, Component.literal("X"));
        y = new EditBox(f, 0, 0, 100, 14, Component.literal("Y"));
        z = new EditBox(f, 0, 0, 100, 14, Component.literal("Z"));
        name.setMaxLength(24);
        name.setValue("Waypoint " + (Config.get().waypointList.size() + 1));
        for (EditBox b : new EditBox[]{x, y, z}) {
            b.setMaxLength(9);
            b.setFilter(s -> s.matches("-?\\d*"));
        }
        if (mc.player != null) {
            x.setValue(String.valueOf(mc.player.getBlockX()));
            y.setValue(String.valueOf(mc.player.getBlockY()));
            z.setValue(String.valueOf(mc.player.getBlockZ()));
        }
        colorIdx = Config.get().waypointList.size() % Waypoints.PALETTE.length;
    }

    @Override protected int panelW() { return 300; }
    @Override protected int panelH() { return 190; }

    @Override
    protected void build() {
        int fx = px + 60, fw = pw - 70;
        place(name, fx, py + 34, fw);
        place(x, fx, py + 54, fw);
        place(y, fx, py + 74, fw);
        place(z, fx, py + 94, fw);
        Button d = btn(DIM_LABEL[dim], fx, py + 114, 90, 14, () -> { dim = (dim + 1) % 3; markDirty(); });
        btn("Color", fx + 96, py + 114, 60, 14, () -> { colorIdx = (colorIdx + 1) % Waypoints.PALETTE.length; });
        btn("Use my position", px + 8, py + ph - 24, 100, 16, () -> {
            if (mc.player == null) return;
            x.setValue(String.valueOf(mc.player.getBlockX()));
            y.setValue(String.valueOf(mc.player.getBlockY()));
            z.setValue(String.valueOf(mc.player.getBlockZ()));
        });
        btn("SAVE", px + pw - 68, py + ph - 24, 60, 16, this::save);
    }

    private void place(EditBox b, int bx, int by, int bw) {
        b.setX(bx); b.setY(by); b.setWidth(bw);
        addRenderableWidget(b);
    }

    private void save() {
        try {
            int ix = Integer.parseInt(x.getValue()), iy = Integer.parseInt(y.getValue()), iz = Integer.parseInt(z.getValue());
            String cur = mc.level == null ? "overworld" : mc.level.dimension().toString();
            String dimStr = cur.replaceAll("overworld|the_nether|the_end", DIMS[dim]);
            String n = name.getValue().isBlank() ? "Waypoint" : name.getValue().trim();
            Config.get().waypointList.add(new Waypoint(n, ix, iy, iz, dimStr, Waypoints.PALETTE[colorIdx]));
            Config.save();
            onClose();
        } catch (NumberFormatException e) {
            error = "X, Y and Z must be whole numbers.";
        }
    }

    @Override
    protected void draw(GuiGraphics g, int mx, int my) {
        g.drawString(font, "Name", px + 8, py + 37, 0xFFFFFFFF, true);
        g.drawString(font, "X", px + 8, py + 57, 0xFFFFFFFF, true);
        g.drawString(font, "Y", px + 8, py + 77, 0xFFFFFFFF, true);
        g.drawString(font, "Z", px + 8, py + 97, 0xFFFFFFFF, true);
        g.drawString(font, "Dimension", px + 8, py + 117, 0xFFFFFFFF, true);
        int c = 0xFF000000 | Waypoints.PALETTE[colorIdx];
        g.fill(px + 60 + 162, py + 114, px + 60 + 176, py + 128, 0xFFFFFFFF);
        g.fill(px + 60 + 163, py + 115, px + 60 + 175, py + 127, c);
        if (!error.isEmpty()) g.drawString(font, error, px + 8, py + 140, 0xFFFF5555, true);
    }
}
