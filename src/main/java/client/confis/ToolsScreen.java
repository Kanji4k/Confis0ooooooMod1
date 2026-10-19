package client.confis;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** Launcher for everything new. Opened from the TOOLS button in the main menu. */
public class ToolsScreen extends ConfisScreen {
    public ToolsScreen(Screen parent) { super("Confis0oooooo Tools", parent); }

    @Override protected int panelW() { return 330; }
    @Override protected int panelH() { return 182; }

    @Override
    protected void build() {
        int w = (pw - 16 - 8) / 2, h = 24, x = px + 8, y = py + 34;
        btn("AI Assistant", x, y, w, h, () -> mc.setScreen(new AiScreen(this)));
        btn("Messenger", x + w + 8, y, w, h, () -> mc.setScreen(new MessengerScreen(this, null)));
        y += h + 6;
        btn("Item Guide", x, y, w, h, () -> mc.setScreen(new ItemGuideScreen(this)));
        btn("Online Players", x + w + 8, y, w, h, () -> mc.setScreen(new PlayerSearchScreen(this)));
        y += h + 6;
        btn("Item Drop Scale", x, y, w, h, () -> mc.setScreen(new ItemScaleScreen(this)));
        btn("Add Waypoint (coords)", x + w + 8, y, w, h, () -> mc.setScreen(new WaypointAddScreen(this)));
        y += h + 6;
        btn("Hitboxes & Nametags", x, y, pw - 16, h, () -> mc.setScreen(new HitboxScreen(this)));
    }

    @Override protected void draw(GuiGraphics g, int mx, int my) {}
}
