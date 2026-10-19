package client.confis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

/** Searchable list of everyone in the server's player list: skin face, name, ping. */
public class PlayerSearchScreen extends ConfisScreen {
    private final EditBox search;
    private String query = "";
    private int page;
    private List<PlayerInfo> shown = new ArrayList<>();

    public PlayerSearchScreen(Screen parent) {
        super("Online Players", parent);
        search = new EditBox(mc.font, 0, 0, 160, 14, Component.literal("Search"));
        search.setHint(Component.literal("Search player name..."));
        search.setMaxLength(16);
        search.setResponder(s -> { query = s.toLowerCase(Locale.ROOT).trim(); page = 0; markDirty(); });
    }

    @Override protected int panelW() { return 340; }
    @Override protected int panelH() { return 260; }

    private int perPage() { return Math.max(1, (ph - 80) / 20); }

    private void filter() {
        shown = new ArrayList<>();
        if (mc.getConnection() != null)
            for (PlayerInfo p : mc.getConnection().getOnlinePlayers())
                if (query.isEmpty() || p.getProfile().name().toLowerCase(Locale.ROOT).contains(query)) shown.add(p);
        shown.sort(Comparator.comparing(p -> p.getProfile().name().toLowerCase(Locale.ROOT)));
    }

    @Override
    protected void build() {
        filter();
        search.setX(px + 8); search.setY(py + 30); search.setWidth(160);
        addRenderableWidget(search);
        int per = perPage();
        int pages = Math.max(1, (shown.size() + per - 1) / per);
        page = Math.min(page, pages - 1);
        for (int n = 0; n < per && page * per + n < shown.size(); n++) {
            String name = shown.get(page * per + n).getProfile().name();
            btn("MSG", px + pw - 44, py + 52 + n * 20, 36, 16, () -> mc.setScreen(new MessengerScreen(this, name)));
        }
        btn("<", px + pw - 78, py + ph - 20, 18, 14, () -> { page = Math.max(0, page - 1); markDirty(); });
        btn(">", px + pw - 28, py + ph - 20, 18, 14, () -> { page = Math.min(pages - 1, page + 1); markDirty(); });
    }

    @Override
    protected void draw(GuiGraphics g, int mx, int my) {
        int per = perPage();
        int pages = Math.max(1, (shown.size() + per - 1) / per);
        g.drawString(font, shown.size() + " online", px + 176, py + 33, 0xFF9A9A9A, true);
        for (int n = 0; n < per && page * per + n < shown.size(); n++) {
            PlayerInfo p = shown.get(page * per + n);
            int y = py + 52 + n * 20;
            g.fill(px + 8, y, px + pw - 48, y + 18, 0xFF0D0D0D);
            PlayerFaceRenderer.draw(g, p.getSkin(), px + 11, y + 3, 12);
            g.drawString(font, p.getProfile().name(), px + 28, y + 5, 0xFFFFFFFF, true);
            String ping = p.getLatency() + " ms";
            g.drawString(font, ping, px + pw - 52 - font.width(ping), y + 5, 0xFF9A9A9A, true);
        }
        if (shown.isEmpty()) g.drawString(font, "No players found.", px + 10, py + 56, 0xFF7A7A7A, true);
        g.drawCenteredString(font, (page + 1) + "/" + pages, px + pw - 53, py + ph - 17, 0xFFFFFFFF);
    }
}
