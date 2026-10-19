package client.confis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** One HUD mod (a card in the menu, a draggable box on screen). */
public abstract class Module {
    public final String id, name, category;
    public final double defX, defY;
    public int defBg = 0x80000000, defText = 0xFFFFFFFF, defAccent = 0xFFFFFFFF;
    /** If non-null, the options screen shows a third color editor with this label. */
    public String accentLabel = null;

    protected Module(String id, String name, String category, double defX, double defY) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.defX = defX;
        this.defY = defY;
    }

    public boolean movable() { return true; }

    public abstract int width(Minecraft mc);
    public abstract int height(Minecraft mc);
    public abstract void draw(GuiGraphics g, Minecraft mc, Config.ModuleCfg c, int x, int y, boolean preview);

    public Config.ModuleCfg cfg() {
        return Config.get().modules.computeIfAbsent(id, k -> {
            Config.ModuleCfg c = new Config.ModuleCfg();
            c.fx = defX;
            c.fy = defY;
            c.bg = defBg;
            c.text = defText;
            c.accent = defAccent;
            return c;
        });
    }

    public void resetColors() {
        Config.ModuleCfg c = cfg();
        c.bg = defBg;
        c.text = defText;
        c.accent = defAccent;
        c.showBg = true;
    }

    public void resetPosition() {
        Config.ModuleCfg c = cfg();
        c.fx = defX;
        c.fy = defY;
    }

    public int sw(Minecraft mc) { return Math.round(width(mc) * cfg().scale); }
    public int sh(Minecraft mc) { return Math.round(height(mc) * cfg().scale); }

    public int posX(Minecraft mc, int screenW) {
        int x = (int) Math.round(cfg().fx * screenW);
        return Math.max(0, Math.min(x, Math.max(0, screenW - sw(mc))));
    }

    public int posY(Minecraft mc, int screenH) {
        int y = (int) Math.round(cfg().fy * screenH);
        return Math.max(0, Math.min(y, Math.max(0, screenH - sh(mc))));
    }
}
