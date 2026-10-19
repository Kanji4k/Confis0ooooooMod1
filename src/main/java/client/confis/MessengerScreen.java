package client.confis;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Contacts on the left (online players + people you talked to), conversation on the right. */
public class MessengerScreen extends ConfisScreen {
    private static String selected;
    private final EditBox input;
    private final KeyPoll enter = KeyPoll.enter();
    private int page;
    private List<String> contacts = new ArrayList<>();

    public MessengerScreen(Screen parent, String preselect) {
        super("Messenger", parent);
        if (preselect != null) selected = preselect;
        input = new EditBox(mc.font, 0, 0, 100, 14, Component.literal("Message"));
        input.setMaxLength(200);
        input.setHint(Component.literal("Type a message..."));
    }

    @Override protected int panelW() { return 460; }
    @Override protected int panelH() { return 270; }

    private void refreshContacts() {
        Set<String> names = new LinkedHashSet<>(MessengerLog.CHATS.keySet());
        if (mc.getConnection() != null)
            for (PlayerInfo p : mc.getConnection().getOnlinePlayers()) {
                String n = p.getProfile().name();
                if (mc.player == null || !n.equals(mc.player.getGameProfile().name())) names.add(n);
            }
        contacts = new ArrayList<>(names);
    }

    private int perPage() { return Math.max(1, (ph - 60) / 18); }

    @Override
    protected void build() {
        refreshContacts();
        int per = perPage();
        int pages = Math.max(1, (contacts.size() + per - 1) / per);
        page = Math.min(page, pages - 1);
        for (int n = 0; n < per && page * per + n < contacts.size(); n++) {
            String name = contacts.get(page * per + n);
            btn("", px + 8, py + 32 + n * 18, 130, 16, () -> {
                selected = name;
                MessengerLog.UNREAD.remove(name);
                markDirty();
            });
        }
        btn("<", px + 8, py + ph - 20, 18, 14, () -> { page = Math.max(0, page - 1); markDirty(); });
        btn(">", px + 120, py + ph - 20, 18, 14, () -> { page = Math.min(pages - 1, page + 1); markDirty(); });

        input.setX(px + 146); input.setY(py + ph - 22); input.setWidth(pw - 146 - 62);
        addRenderableWidget(input);
        setInitialFocus(input);
        btn("SEND", px + pw - 52, py + ph - 23, 44, 16, this::send);
    }

    private void send() {
        if (selected == null) return;
        MessengerLog.send(selected, input.getValue());
        input.setValue("");
    }

    @Override
    public void tick() {
        super.tick();
        if (enter.pressed() && input.isFocused()) send();
    }

    @Override
    protected void draw(GuiGraphics g, int mx, int my) {
        int per = perPage();
        for (int n = 0; n < per && page * per + n < contacts.size(); n++) {
            String name = contacts.get(page * per + n);
            int y = py + 32 + n * 18;
            PlayerInfo info = findInfo(name);
            if (info != null) PlayerFaceRenderer.draw(g, info.getSkin(), px + 10, y + 2, 12);
            else g.fill(px + 10, y + 2, px + 22, y + 14, 0xFF444444);
            int unread = MessengerLog.UNREAD.getOrDefault(name, 0);
            boolean sel = name.equals(selected);
            g.drawString(font, name + (unread > 0 ? " (" + unread + ")" : ""), px + 26, y + 4,
                    sel ? accent() : unread > 0 ? 0xFFFFFF55 : 0xFFFFFFFF, true);
        }
        g.fill(px + 142, py + 28, px + 143, py + ph - 26, 0xFF3A3A3A);

        int x0 = px + 148, w = pw - 156;
        if (selected == null) {
            g.drawString(font, "Pick a player on the left.", x0, py + 34, 0xFF7A7A7A, true);
            return;
        }
        g.drawString(font, "Chat with " + selected, x0, py + 30, accent(), true);
        List<FormattedCharSequence> lines = new ArrayList<>();
        List<Integer> cols = new ArrayList<>();
        for (MessengerLog.Entry e : MessengerLog.CHATS.getOrDefault(selected, List.of()))
            for (FormattedCharSequence l : font.split(Component.literal((e.mine() ? "You: " : selected + ": ") + e.text()), w)) {
                lines.add(l);
                cols.add(e.mine() ? 0xFFB0B0B0 : 0xFFFFFFFF);
            }
        int top = py + 44, rows = Math.max(1, (ph - 44 - 30) / 10);
        for (int i = 0; i < rows && i < lines.size(); i++) {
            int idx = Math.max(0, lines.size() - rows) + i;
            if (idx >= lines.size()) break;
            g.drawString(font, lines.get(idx), x0, top + i * 10, cols.get(idx), true);
        }
    }

    private PlayerInfo findInfo(String name) {
        if (mc.getConnection() == null) return null;
        for (PlayerInfo p : mc.getConnection().getOnlinePlayers()) if (p.getProfile().name().equals(name)) return p;
        return null;
    }
}
