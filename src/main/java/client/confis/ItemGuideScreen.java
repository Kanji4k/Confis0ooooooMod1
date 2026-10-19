package client.confis;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

/** Every item in the game: icon, name, id and its description (tooltip). Searchable. */
public class ItemGuideScreen extends ConfisScreen {
    private static List<ItemStack> ALL;

    public static List<ItemStack> allItems() {
        if (ALL == null) {
            ALL = new ArrayList<>();
            for (Item it : BuiltInRegistries.ITEM) if (it != Items.AIR) ALL.add(new ItemStack(it));
        }
        return ALL;
    }

    public static String idOf(ItemStack s) { return BuiltInRegistries.ITEM.getKey(s.getItem()).getPath(); }

    private static final int COLS = 12, ROWS = 8, SLOT = 18;

    private final EditBox search;
    private List<ItemStack> shown = new ArrayList<>();
    private String query = "";
    private int page;
    private ItemStack selected;

    public ItemGuideScreen(Screen parent) {
        super("Item Guide", parent);
        search = new EditBox(mc.font, 0, 0, 150, 14, Component.literal("Search"));
        search.setHint(Component.literal("Search items..."));
        search.setMaxLength(32);
        search.setResponder(s -> { query = s.toLowerCase(Locale.ROOT).trim(); page = 0; markDirty(); });
        filter();
    }

    @Override protected int panelW() { return 490; }
    @Override protected int panelH() { return 280; }

    private void filter() {
        shown = new ArrayList<>();
        for (ItemStack s : allItems())
            if (query.isEmpty() || idOf(s).contains(query) || s.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query))
                shown.add(s);
    }

    @Override
    protected void build() {
        filter();
        search.setX(px + 8); search.setY(py + 30); search.setWidth(150);
        addRenderableWidget(search);
        int per = COLS * ROWS;
        int pages = Math.max(1, (shown.size() + per - 1) / per);
        page = Math.min(page, pages - 1);
        for (int n = 0; n < per && page * per + n < shown.size(); n++) {
            ItemStack s = shown.get(page * per + n);
            btn("", px + 8 + (n % COLS) * SLOT, py + 52 + (n / COLS) * SLOT, SLOT, SLOT, () -> selected = s);
        }
        btn("<", px + 8, py + ph - 20, 18, 14, () -> { page = Math.max(0, page - 1); markDirty(); });
        btn(">", px + 8 + COLS * SLOT - 18, py + ph - 20, 18, 14, () -> { page = Math.min(pages - 1, page + 1); markDirty(); });
    }

    @Override
    protected void draw(GuiGraphics g, int mx, int my) {
        int per = COLS * ROWS;
        int pages = Math.max(1, (shown.size() + per - 1) / per);
        for (int n = 0; n < per && page * per + n < shown.size(); n++)
            g.renderItem(shown.get(page * per + n), px + 9 + (n % COLS) * SLOT, py + 53 + (n / COLS) * SLOT);
        g.drawCenteredString(font, (page + 1) + "/" + pages + "  (" + shown.size() + " items)",
                px + 8 + COLS * SLOT / 2, py + ph - 17, 0xFFFFFFFF);

        int dx = px + 8 + COLS * SLOT + 10, dw = pw - (dx - px) - 8;
        g.fill(dx - 5, py + 28, dx - 4, py + ph - 8, 0xFF3A3A3A);
        if (selected == null) {
            g.drawString(font, "Click an item to read about it.", dx, py + 34, 0xFF7A7A7A, true);
            return;
        }
        g.pose().pushMatrix();
        g.pose().translate(dx, py + 32);
        g.pose().scale(3f, 3f);
        g.renderItem(selected, 0, 0);
        g.pose().popMatrix();
        g.drawString(font, selected.getHoverName().getString(), dx + 56, py + 34, accent(), true);
        g.drawString(font, "minecraft:" + idOf(selected), dx + 56, py + 46, 0xFF9A9A9A, true);
        g.drawString(font, "Max stack: " + selected.getMaxStackSize()
                + (selected.isDamageableItem() ? "   Durability: " + selected.getMaxDamage() : ""),
                dx + 56, py + 58, 0xFFCCCCCC, true);

        int y = py + 86;
        List<Component> tip = selected.getTooltipLines(Item.TooltipContext.EMPTY, mc.player, TooltipFlag.NORMAL);
        for (int i = 1; i < tip.size(); i++)                       // line 0 is the name, already shown
            for (FormattedCharSequence l : font.split(tip.get(i), dw)) {
                if (y > py + ph - 14) return;
                g.drawString(font, l, dx, y, 0xFFFFFFFF, true);
                y += 10;
            }
        if (tip.size() <= 1) g.drawString(font, "No extra description for this item.", dx, y, 0xFF7A7A7A, true);
    }
}
