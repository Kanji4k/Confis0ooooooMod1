package client.confis;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** ON/OFF switches for each hitbox type and for nametags. */
public class HitboxScreen extends ConfisScreen {
    public HitboxScreen(Screen parent) { super("Hitboxes & Nametags", parent); }

    @Override protected int panelW() { return 320; }
    @Override protected int panelH() { return 232; }

    private static String lbl(String n, boolean on) { return n + ": " + (on ? "ON" : "OFF"); }

    @Override
    protected void build() {
        Config c = Config.get();
        int w = (pw - 16 - 8) / 2, h = 18, x = px + 8, y = py + 32;
        Button[] master = new Button[1];
        master[0] = btn(lbl("Show hitboxes", c.hitboxMaster), x, y, pw - 16, h, () -> {
            c.hitboxMaster = !c.hitboxMaster; Config.save();
            master[0].setMessage(Component.literal(lbl("Show hitboxes", c.hitboxMaster)));
        });
        y += h + 8;
        for (int i = 0; i < HitboxRenderer.KEYS.length; i++) {
            String key = HitboxRenderer.KEYS[i], label = HitboxRenderer.LABELS[i];
            Button[] b = new Button[1];
            b[0] = btn(lbl(label, HitboxRenderer.on(key)), x + (i % 2) * (w + 8), y + (i / 2) * (h + 4), w, h, () -> {
                c.hitboxes.put(key, !HitboxRenderer.on(key)); Config.save();
                b[0].setMessage(Component.literal(lbl(label, HitboxRenderer.on(key))));
            });
        }
        y += 4 * (h + 4) + 6;
        Button[] nt = new Button[1];
        nt[0] = btn(lbl("Nametags", c.nametags), x, y, pw - 16, h, () -> {
            c.nametags = !c.nametags; Config.save();
            nt[0].setMessage(Component.literal(lbl("Nametags", c.nametags)));
        });
    }

    @Override protected void draw(GuiGraphics g, int mx, int my) {}
}
