package client.confis;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Resize dropped items: one default for all, plus a per-item override found with the search box. */
public class ItemScaleScreen extends ConfisScreen {
    private final EditBox search;
    private String query = "";
    private int page;
    private List<ItemStack> shown = new ArrayList<>();

    public ItemScaleScreen(Screen parent) {
        super("Item Drop Scale", parent);
        search = new EditBox(mc.font, 0, 0, 150, 14, Component.literal("Search"));
        search.setHint(Component.literal("Search an item..."));
        search.setMaxLength(32);
        search.setResponder(s -> { query = s.toLowerCase(Locale.ROOT).trim(); page = 0; markDirty(); });
    }

    @Override protected int panelW() { return 400; }
    @Override protected int panelH() { return 270; }

    private int perPage() { return Math.max(1, (ph - 84) / 20); }

    private void filter() {
        shown = new ArrayList<>();
        for (ItemStack s : ItemGuideScreen.allItems())
            if (query.isEmpty() || ItemGuideScreen.idOf(s).contains(query)
                    || s.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) shown.add(s);
    }

    private void change(String id, int dir) {
        Config c = Config.get();
        c.itemScales.put(id, DropScale.step(DropScale.forId(id), dir));
        Config.save();
    }

    @Override
    protected void build() {
        filter();
        Config c = Config.get();
        btn("-", px + pw - 70, py + 30, 18, 14, () -> { c.dropScale = DropScale.step(c.dropScale, -1); Config.save(); });
        btn("+", px + pw - 48, py + 30, 18, 14, () -> { c.dropScale = DropScale.step(c.dropScale, 1); Config.save(); });
        search.setX(px + 8); search.setY(py + 52); search.setWidth(150);
        addRenderableWidget(search);

        int per = perPage();
        int pages = Math.max(1, (shown.size() + per - 1) / per);
        page = Math.min(page, pages - 1);
        for (int n = 0; n < per && page * per + n < shown.size(); n++) {
            String id = ItemGuideScreen.idOf(shown.get(page * per + n));
            int y = py + 72 + n * 20;
            btn("-", px + pw - 72, y, 18, 16, () -> change(id, -1));
            btn("+", px + pw - 52, y, 18, 16, () -> change(id, 1));
            btn("R", px + pw - 30, y, 18, 16, () -> { c.itemScales.remove(id); Config.save(); });
        }
        btn("<", px + pw - 78, py + ph - 20, 18, 14, () -> { page = Math.max(0, page - 1); markDirty(); });
        btn(">", px + pw - 28, py + ph - 20, 18, 14, () -> { page = Math.min(pages - 1, page + 1); markDirty(); });
        btn("Reset all", px + 8, py + ph - 20, 70, 14, () -> { c.itemScales.clear(); c.dropScale = 1f; Config.save(); });
    }

    @Override
    protected void draw(GuiGraphics g, int mx, int my) {
        Config c = Config.get();
        g.drawString(font, "Default size for all drops: " + String.format("%.2fx", c.dropScale), px + 8, py + 33, 0xFFFFFFFF, true);
        int per = perPage();
        int pages = Math.max(1, (shown.size() + per - 1) / per);
        for (int n = 0; n < per && page * per + n < shown.size(); n++) {
            ItemStack s = shown.get(page * per + n);
            String id = ItemGuideScreen.idOf(s);
            int y = py + 72 + n * 20;
            g.fill(px + 8, y, px + pw - 76, y + 18, 0xFF0D0D0D);
            g.renderItem(s, px + 10, y + 1);
            g.drawString(font, s.getHoverName().getString(), px + 32, y + 5, 0xFFFFFFFF, true);
            boolean custom = c.itemScales.containsKey(id);
            String t = String.format("%.2fx", DropScale.forId(id));
            g.drawString(font, t, px + pw - 80 - font.width(t), y + 5, custom ? accent() : 0xFF7A7A7A, true);
        }
        g.drawCenteredString(font, (page + 1) + "/" + pages, px + pw - 53, py + ph - 17, 0xFFFFFFFF);
    }
}
