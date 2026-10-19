package client.confis;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/** The black "Confis0oooooo AI" chat window: history on top, "Ask anything..." bar at the bottom. */
public class AiScreen extends ConfisScreen {
    private static final List<AiClient.Msg> HISTORY = new ArrayList<>();   // survives closing the window
    private static final List<String> ERRORS = new ArrayList<>();

    private final EditBox input, keyBox;
    private final KeyPoll enter = KeyPoll.enter();
    private boolean busy, showKey;
    private String pendingImage;
    private int scroll;
    private List<FormattedCharSequence> lines = new ArrayList<>();
    private List<Integer> lineColors = new ArrayList<>();
    private int wrapW = -1;
    private boolean relayout = true;

    public AiScreen(Screen parent) {
        super("Confis0oooooo AI", parent);
        input = new EditBox(mc.font, 0, 0, 100, 12, Component.literal("Ask anything..."));
        input.setBordered(false);
        input.setMaxLength(600);
        input.setHint(Component.literal("Ask anything..."));
        keyBox = new EditBox(mc.font, 0, 0, 100, 12, Component.literal("API key"));
        keyBox.setMaxLength(200);
        keyBox.setHint(Component.literal("Paste Anthropic API key (sk-ant-...)"));
        showKey = Config.get().aiKey == null || Config.get().aiKey.isBlank();
    }

    @Override protected int panelW() { return 440; }
    @Override protected int panelH() { return 280; }

    @Override
    protected void build() {
        int by = py + ph - 22;
        input.setX(px + 28); input.setY(by + 5); input.setWidth(pw - 28 - 150);
        addRenderableWidget(input);
        setInitialFocus(input);
        btn("PIC", px + pw - 118, by + 2, 26, 16, this::attach);
        btn("SEND", px + pw - 88, by + 2, 36, 16, this::send);
        btn("KEY", px + pw - 48, by + 2, 26, 16, () -> { showKey = !showKey; markDirty(); });
        btn("NEW", px + pw - 70, py + 5, 28, 14, () -> { HISTORY.clear(); ERRORS.clear(); relayout = true; });
        if (showKey) {
            keyBox.setX(px + 8); keyBox.setY(py + 30); keyBox.setWidth(pw - 70);
            addRenderableWidget(keyBox);
            btn("SAVE", px + pw - 56, py + 29, 48, 14, () -> {
                Config.get().aiKey = keyBox.getValue().trim();
                Config.save();
                keyBox.setValue("");
                showKey = false;
                markDirty();
            });
        }
        relayout = true;
    }

    private void attach() {
        try {
            String img = AiClient.latestScreenshot();
            if (img == null) { ERRORS.add("No screenshot found. Press F2 in game first."); }
            else { pendingImage = img; }
        } catch (Exception e) {
            ERRORS.add("Could not read screenshot: " + e.getMessage());
        }
        relayout = true;
    }

    private void send() {
        String q = input.getValue().trim();
        if (q.isEmpty() || busy) return;
        input.setValue("");
        HISTORY.add(new AiClient.Msg("user", q, pendingImage));
        pendingImage = null;
        busy = true;
        scroll = 0;
        relayout = true;
        List<AiClient.Msg> snapshot = new ArrayList<>(HISTORY);
        if (snapshot.size() > 20) snapshot = snapshot.subList(snapshot.size() - 20, snapshot.size());
        List<AiClient.Msg> toSend = snapshot;
        new Thread(() -> {
            String reply; boolean ok = true;
            try { reply = AiClient.ask(toSend); }
            catch (Exception e) { reply = e.getMessage() == null ? e.toString() : e.getMessage(); ok = false; }
            String r = reply; boolean good = ok;
            mc.execute(() -> {
                if (good) HISTORY.add(new AiClient.Msg("assistant", r, null)); else ERRORS.add(r);
                busy = false;
                relayout = true;
            });
        }, "confis-ai").start();
    }

    @Override
    public void tick() {
        super.tick();
        if (enter.pressed() && input.isFocused()) send();
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        scroll = Math.max(0, scroll + (sy > 0 ? 3 : -3));
        return true;
    }

    private void layoutLines(int w) {
        lines = new ArrayList<>();
        lineColors = new ArrayList<>();
        for (AiClient.Msg m : HISTORY) {
            boolean me = m.role().equals("user");
            String pre = (me ? "You: " : "AI: ") + (m.image() != null ? "[screenshot] " : "");
            for (FormattedCharSequence l : font.split(Component.literal(pre + m.text()), w)) {
                lines.add(l);
                lineColors.add(me ? 0xFFB0B0B0 : 0xFFFFFFFF);
            }
            lines.add(FormattedCharSequence.EMPTY);
            lineColors.add(0);
        }
        for (String e : ERRORS)
            for (FormattedCharSequence l : font.split(Component.literal("! " + e), w)) {
                lines.add(l);
                lineColors.add(0xFFFF5555);
            }
        relayout = false;
        wrapW = w;
    }

    @Override
    protected void draw(GuiGraphics g, int mx, int my) {
        int w = pw - 24;
        if (relayout || w != wrapW) layoutLines(w);

        int top = py + (showKey ? 48 : 30), bottom = py + ph - 30;
        int rows = Math.max(1, (bottom - top) / 10);
        int max = Math.max(0, lines.size() - rows);
        scroll = Math.min(scroll, max);
        int start = Math.max(0, lines.size() - rows - scroll);
        if (lines.isEmpty() && !busy) {
            g.drawString(font, "Ask me anything about Minecraft or real life.", px + 12, top + 4, 0xFF7A7A7A, true);
            g.drawString(font, "PIC attaches your newest screenshot (F2) so I can see it.", px + 12, top + 16, 0xFF7A7A7A, true);
        }
        for (int i = 0; i < rows && start + i < lines.size(); i++)
            g.drawString(font, lines.get(start + i), px + 12, top + i * 10, lineColors.get(start + i), true);
        if (busy) g.drawString(font, "AI is thinking" + ".".repeat((int) (System.currentTimeMillis() / 400 % 4)),
                px + 12, bottom - 10, 0xFF9A9A9A, true);
        if (pendingImage != null) g.drawString(font, "[screenshot attached]", px + 12, bottom, 0xFF55FF55, true);

        // "Ask anything..." bar with magnifier, like the design
        int by = py + ph - 22;
        g.fill(px + 8, by + 2, px + pw - 124, by + 18, 0xFF000000);
        g.fill(px + 8, by + 2, px + pw - 124, by + 3, 0xFF3A3A3A);
        g.fill(px + 8, by + 17, px + pw - 124, by + 18, 0xFF3A3A3A);
        Icons.draw(g, "search", px + 14, by + 6, 1, 0xFFFFFFFF);
    }
}
