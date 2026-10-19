package client.confis;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Shared look for all new Confis screens: dark panel, accent outline, title, X button. */
public abstract class ConfisScreen extends Screen {
    protected final Screen parent;
    protected final Minecraft mc = Minecraft.getInstance();
    protected int px, py, pw, ph;
    private boolean dirty;

    protected ConfisScreen(String title, Screen parent) {
        super(Component.literal(title));
        this.parent = parent;
    }

    protected int panelW() { return 440; }
    protected int panelH() { return 260; }
    /** Add widgets here. Called on every (re)layout. */
    protected abstract void build();
    /** Draw custom things on top of the widgets. */
    protected abstract void draw(GuiGraphics g, int mx, int my);

    protected void markDirty() { dirty = true; }
    protected int accent() { return 0xFF000000 | Config.get().accent; }

    @Override
    protected void init() {
        pw = Math.min(panelW(), width - 16);
        ph = Math.min(panelH(), height - 16);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        btn("X", px + pw - 20, py + 5, 14, 14, this::onClose);
        build();
    }

    @Override
    public void tick() {
        if (dirty) {
            dirty = false;
            rebuildWidgets();
        }
    }

    protected Button btn(String label, int x, int y, int w, int h, Runnable action) {
        Button b = Button.builder(Component.literal(label), bt -> action.run()).bounds(x, y, w, h).build();
        addRenderableWidget(b);
        return b;
    }

    @Override public void renderBackground(GuiGraphics g, int mx, int my, float d) {}
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { mc.setScreen(parent); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float d) {
        g.fill(0, 0, width, height, 0x90000000);
        g.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, accent());
        g.fill(px, py, px + pw, py + ph, Config.get().panelBg);
        g.fill(px, py + 24, px + pw, py + 25, 0xFF3A3A3A);
        g.drawString(font, Component.literal(getTitle().getString()).withStyle(ChatFormatting.BOLD), px + 8, py + 8, accent(), true);
        super.render(g, mx, my, d);
        draw(g, mx, my);
    }
}
