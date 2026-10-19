package client.confis;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/**
 * Black-and-white module hub. Everything is drawn immediate-mode with the vanilla Font (so
 * resource-pack fonts apply). Mouse and typing are polled from GLFW, which keeps this independent
 * of Screen's input-method signatures that change between Minecraft versions.
 */
public class ConfigScreen extends Screen {
    private enum Tab {
        ALL("ALL"), INFO("INFO"), HUD("HUD"), MAP("MAP"), FAVORITES("FAVORITES"),
        WAYPOINTS("WAYPOINTS"), SETTINGS("SETTINGS");
        final String label;
        Tab(String label) { this.label = label; }
    }

    private enum Mode { MENU, OPTIONS, EDIT }

    private static final int[] PRESETS = {0xFFFFFF, 0x000000, 0xFF5555, 0xFFAA00, 0xFFFF55,
            0x55FF55, 0x55FFFF, 0x5555FF, 0xFF55FF};

    private static final int BTN = 0xFF1E1E1E;
    private static final int BTN_TXT = 0xFFFFFFFF;
    private static final int DIM = 0xFF7A7A7A;
    private static final int LINE = 0xFF3A3A3A;
    private static final int COLOR_EDITOR_H = 11 + 14 + 33 + 13;

    private Tab tab = Tab.ALL;
    private Mode mode = Mode.MENU;
    private int page = 0;
    private Module optMod;

    // search box
    private String query = "";
    private boolean searchFocus;
    private final boolean[] keyWas = new boolean[GLFW.GLFW_KEY_LAST + 1];

    // mouse state (polled)
    private int mx, my;
    private boolean down, wasDown, clicked;
    private String activeSlider;

    // hud dragging
    private Module dragging;
    private int dragOffX, dragOffY;

    // panel geometry
    private int px, py, pw, ph;

    public ConfigScreen() {
        super(Component.literal(ConfisClient.NAME));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        Config.save();
    }

    // ------------------------------------------------------------------ input
    private void pollMouse() {
        long win = GLFW.glfwGetCurrentContext();
        boolean d = win != 0L && GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        clicked = d && !wasDown;
        boolean released = !d && wasDown;
        down = d;
        wasDown = d;
        if (released) {
            activeSlider = null;
            Config.save();
        }
    }

    /** Typing for the search box: edge-detects letter / digit / space / backspace keys. */
    private void pollTyping() {
        long win = GLFW.glfwGetCurrentContext();
        if (win == 0L) return;
        for (int k = GLFW.GLFW_KEY_A; k <= GLFW.GLFW_KEY_Z; k++) {
            boolean d = GLFW.glfwGetKey(win, k) == GLFW.GLFW_PRESS;
            if (d && !keyWas[k] && searchFocus && query.length() < 16) query += (char) ('a' + (k - GLFW.GLFW_KEY_A));
            keyWas[k] = d;
        }
        for (int k = GLFW.GLFW_KEY_0; k <= GLFW.GLFW_KEY_9; k++) {
            boolean d = GLFW.glfwGetKey(win, k) == GLFW.GLFW_PRESS;
            if (d && !keyWas[k] && searchFocus && query.length() < 16) query += (char) ('0' + (k - GLFW.GLFW_KEY_0));
            keyWas[k] = d;
        }
        boolean sp = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
        if (sp && !keyWas[GLFW.GLFW_KEY_SPACE] && searchFocus && query.length() < 16) query += " ";
        keyWas[GLFW.GLFW_KEY_SPACE] = sp;
        boolean bs = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_BACKSPACE) == GLFW.GLFW_PRESS;
        if (bs && !keyWas[GLFW.GLFW_KEY_BACKSPACE] && searchFocus && !query.isEmpty()) {
            query = query.substring(0, query.length() - 1);
            page = 0;
        }
        keyWas[GLFW.GLFW_KEY_BACKSPACE] = bs;
    }

    private boolean in(int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** True once if the mouse was clicked inside the rectangle this frame. */
    private boolean clickIn(int x, int y, int w, int h) {
        if (clicked && in(x, y, w, h)) {
            clicked = false;
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- colors
    private static int lighten(int c, int amt) {
        int a = c >>> 24;
        int r = Math.min(255, ((c >> 16) & 255) + amt);
        int g = Math.min(255, ((c >> 8) & 255) + amt);
        int b = Math.min(255, (c & 255) + amt);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int darken(int c, int amt) {
        int a = c >>> 24;
        int r = Math.max(0, ((c >> 16) & 255) - amt);
        int g = Math.max(0, ((c >> 8) & 255) - amt);
        int b = Math.max(0, (c & 255) - amt);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int opaque(int c) {
        return 0xFF000000 | c;
    }

    /** Black or white, whichever reads better on top of color c. */
    private static int contrast(int c) {
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        return (r * 299 + g * 587 + b * 114) / 1000 > 140 ? 0xFF000000 : 0xFFFFFFFF;
    }

    // --------------------------------------------------------------- widgets
    private boolean button(GuiGraphics g, int x, int y, int w, int h, String label, int fill, int textCol) {
        boolean hov = in(x, y, w, h);
        g.fill(x, y, x + w, y + h, hov ? lighten(fill, 28) : fill);
        g.drawCenteredString(this.font, label, x + w / 2, y + (h - 8) / 2, textCol);
        return clickIn(x, y, w, h);
    }

    private float slider(GuiGraphics g, String id, String tag, int x, int y, int w, int h, float v) {
        if (clicked && in(x, y, w, h)) {
            activeSlider = id;
            clicked = false;
        }
        if (down && id.equals(activeSlider)) {
            v = Math.max(0f, Math.min(1f, (mx - x) / (float) w));
        }
        g.fill(x, y, x + w, y + h, 0xFF1A1A1A);
        g.fill(x, y, x + Math.round(w * v), y + h, 0xFF8C8C8C);
        int kx = x + Math.round((w - 2) * v);
        g.fill(kx, y - 1, kx + 2, y + h + 1, 0xFFFFFFFF);
        g.drawString(this.font, tag, x + 3, y + 1, 0xFFFFFFFF, true);
        return v;
    }

    /** Swatch + RGB(A) sliders + quick presets. Returns the (possibly changed) ARGB color. */
    private int colorEditor(GuiGraphics g, String id, int x, int y, int w, String label, int argb, boolean alpha) {
        g.drawString(this.font, label, x, y, 0xFFFFFFFF, true);
        y += 11;
        int a = alpha ? (argb >>> 24) : 255;
        int r = (argb >> 16) & 255, gr = (argb >> 8) & 255, b = argb & 255;

        g.fill(x - 1, y - 1, x + 13, y + 11, 0xFFFFFFFF);
        g.fill(x, y, x + 12, y + 10, opaque(argb));
        String hex = String.format("#%02X%02X%02X", r, gr, b) + (alpha ? "  A " + Math.round(a / 2.55f) + "%" : "");
        g.drawString(this.font, hex, x + 18, y + 1, 0xFFCCCCCC, true);
        y += 14;

        r = Math.round(slider(g, id + "r", "R", x, y, w, 9, r / 255f) * 255);
        y += 11;
        gr = Math.round(slider(g, id + "g", "G", x, y, w, 9, gr / 255f) * 255);
        y += 11;
        b = Math.round(slider(g, id + "b", "B", x, y, w, 9, b / 255f) * 255);
        y += 11;
        if (alpha) {
            a = Math.round(slider(g, id + "a", "A", x, y, w, 9, a / 255f) * 255);
            y += 11;
        }

        int sw = (w - 8 * 2) / PRESETS.length;
        for (int i = 0; i < PRESETS.length; i++) {
            int sx = x + i * (sw + 2);
            g.fill(sx - 1, y - 1, sx + sw + 1, y + 11, in(sx, y, sw, 10) ? 0xFFFFFFFF : 0xFF444444);
            g.fill(sx, y, sx + sw, y + 10, opaque(PRESETS[i]));
            if (clickIn(sx, y, sw, 10)) {
                r = (PRESETS[i] >> 16) & 255;
                gr = (PRESETS[i] >> 8) & 255;
                b = PRESETS[i] & 255;
            }
        }
        return (a << 24) | (r << 16) | (gr << 8) | b;
    }

    private void outline(GuiGraphics g, int x, int y, int w, int h, int col) {
        g.fill(x, y, x + w, y + 1, col);
        g.fill(x, y + h - 1, x + w, y + h, col);
        g.fill(x, y, x + 1, y + h, col);
        g.fill(x + w - 1, y, x + w, y + h, col);
    }

    // ---------------------------------------------------------------- render
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        mx = mouseX;
        my = mouseY;
        pollMouse();
        pollTyping();

        if (mode == Mode.EDIT) {
            renderEdit(g);
            return;
        }
        super.render(g, mouseX, mouseY, delta);

        Config cfg = Config.get();
        pw = Math.min(480, this.width - 16);
        ph = Math.min(284, this.height - 16);
        px = (this.width - pw) / 2;
        py = (this.height - ph) / 2;
        int accent = opaque(cfg.accent);

        outline(g, px - 1, py - 1, pw + 2, ph + 2, accent);
        g.fill(px, py, px + pw, py + ph, cfg.panelBg);
        g.fill(px, py, px + pw, py + 24, darken(cfg.panelBg, 6));
        g.fill(px, py + 24, px + pw, py + 25, LINE);

        // name on top of the menu
        Component title = Component.literal(ConfisClient.NAME).withStyle(ChatFormatting.BOLD);
        g.drawString(this.font, title, px + 8, py + 8, accent, true);
        int titleW = this.font.width(title);

        // close button
        if (button(g, px + pw - 20, py + 5, 14, 14, "X", BTN, BTN_TXT)) {
            this.onClose();
            return;
        }

        if (mode == Mode.OPTIONS) {
            if (button(g, px + pw - 62, py + 5, 38, 14, "BACK", BTN, BTN_TXT)) {
                mode = Mode.MENU;
                optMod = null;
                return;
            }
            g.drawString(this.font, (optMod == null ? "" : optMod.name.toUpperCase(Locale.ROOT)) + " OPTIONS",
                    px + 8 + titleW + 14, py + 8, DIM, true);
            renderOptions(g, cfg);
            return;
        }

        // header: current page name, search box, edit-hud button
        g.drawString(this.font, tab == Tab.ALL ? "ALL MODULES" : tab.label, px + 8 + titleW + 14, py + 8, DIM, true);
        if (button(g, px + pw - 20 - 4 - 56, py + 5, 56, 14, "EDIT HUD", accent, contrast(accent))) {
            mode = Mode.EDIT;
            dragging = null;
            return;
        }
        int sbW = 110;
        int toolsX = px + pw - 20 - 4 - 56 - 6 - 46;
        if (button(g, toolsX, py + 5, 46, 14, "TOOLS", BTN, BTN_TXT)) {
            Minecraft.getInstance().setScreen(new ToolsScreen(this));
            return;
        }
        int aiX = toolsX - 6 - 14;
        g.fill(aiX, py + 5, aiX + 14, py + 19, in(aiX, py + 5, 14, 14) ? 0xFF3A3A3A : BTN);
        Icons.draw(g, "ai", aiX + 1, py + 6, 1, 0xFFFFFFFF);
        if (clickIn(aiX, py + 5, 14, 14)) {
            Minecraft.getInstance().setScreen(new AiScreen(this));
            return;
        }
        int sbX = aiX - 6 - sbW;
        if (tab.ordinal() <= Tab.FAVORITES.ordinal()) {
            if (clicked) searchFocus = in(sbX, py + 5, sbW, 14);
            g.fill(sbX, py + 5, sbX + sbW, py + 19, 0xFF000000);
            outline(g, sbX, py + 5, sbW, 14, searchFocus ? accent : LINE);
            boolean caret = searchFocus && (System.currentTimeMillis() / 500) % 2 == 0;
            String shown = query.isEmpty() && !searchFocus ? "Search modules..." : query + (caret ? "_" : "");
            g.drawString(this.font, shown, sbX + 4, py + 8, query.isEmpty() && !searchFocus ? DIM : 0xFFFFFFFF, true);
        } else {
            searchFocus = false;
        }

        // tab bar along the top (replaces the old left-hand list)
        int tabY = py + 29;
        int tabGap = 3;
        int tabW = (pw - 16 - tabGap * (Tab.values().length - 1)) / Tab.values().length;
        for (int i = 0; i < Tab.values().length; i++) {
            Tab t = Tab.values()[i];
            boolean active = t == tab;
            int x = px + 8 + i * (tabW + tabGap);
            if (button(g, x, tabY, tabW, 16, t.label, active ? accent : BTN, active ? contrast(accent) : BTN_TXT)) {
                tab = t;
                page = 0;
            }
        }

        switch (tab) {
            case WAYPOINTS -> renderWaypoints(g, cfg);
            case SETTINGS -> renderSettings(g, cfg);
            default -> renderMods(g, cfg);
        }
    }

    // ------------------------------------------------------------- mods grid
    private void renderMods(GuiGraphics g, Config cfg) {
        int accent = opaque(cfg.accent);
        int gx = px + 8;
        int gw = pw - 16;
        int gy = py + 52;

        String q = query.trim().toLowerCase(Locale.ROOT);
        List<Module> list = new ArrayList<>();
        for (Module m : Modules.ALL) {
            boolean ok = switch (tab) {
                case INFO -> m.category.equals("INFO");
                case HUD -> m.category.equals("HUD");
                case MAP -> m.category.equals("MAP");
                case FAVORITES -> m.cfg().favorite;
                default -> true;
            };
            if (ok && (q.isEmpty() || m.name.toLowerCase(Locale.ROOT).contains(q))) list.add(m);
        }

        int cols = 5;
        int gap = 4;
        int cw = (gw - (cols - 1) * gap) / cols;
        int chh = 68;
        int rowsPer = Math.max(1, (ph - 52 - 6 - 16) / (chh + gap));
        int perPage = rowsPer * cols;
        int pages = Math.max(1, (list.size() + perPage - 1) / perPage);
        if (page >= pages) page = pages - 1;

        if (list.isEmpty()) {
            g.drawString(this.font, tab == Tab.FAVORITES ? "No favorites yet. Click the heart on a module."
                    : "No modules match your search.", gx + 4, gy + 6, DIM, true);
        }

        for (int n = 0; n < perPage; n++) {
            int idx = page * perPage + n;
            if (idx >= list.size()) break;
            Module m = list.get(idx);
            Config.ModuleCfg c = m.cfg();
            int x = gx + (n % cols) * (cw + gap);
            int y = gy + (n / cols) * (chh + gap);
            boolean on = c.enabled;

            g.fill(x, y, x + cw, y + chh, 0xFF0D0D0D);
            outline(g, x, y, cw, chh, in(x, y, cw, chh) ? 0xFF9A9A9A : (on ? 0xFF5A5A5A : LINE));

            g.drawString(this.font, m.name, x + 5, y + 5, on ? 0xFFFFFFFF : DIM, true);

            // favorite heart (top-right)
            int hx = x + cw - 13, hy = y + 5;
            boolean fav = c.favorite;
            Icons.draw(g, "heart", hx, hy, 1, fav ? accent : 0xFF555555);
            if (clickIn(hx - 2, hy - 2, 11, 10)) {
                c.favorite = !c.favorite;
                Config.save();
            }

            // big icon
            Icons.draw(g, m.id, x + cw / 2 - 12, y + 19, 2, on ? 0xFFFFFFFF : 0xFF4A4A4A);

            // gear (options) + enabled/disabled bar
            int by = y + chh - 16;
            boolean gearHov = in(x + 4, by, 14, 12);
            g.fill(x + 4, by, x + 18, by + 12, gearHov ? 0xFF3A3A3A : 0xFF222222);
            Icons.draw(g, "gear", x + 7, by + 2, 1, 0xFFFFFFFF);
            if (clickIn(x + 4, by, 14, 12)) {
                optMod = m;
                mode = Mode.OPTIONS;
            }
            if (button(g, x + 21, by, cw - 25, 12, on ? "Enabled" : "Disabled",
                    on ? accent : 0xFF161616, on ? contrast(accent) : DIM)) {
                c.enabled = !c.enabled;
                Config.save();
            }
        }

        if (pages > 1) {
            int by = py + ph - 17;
            if (button(g, px + pw - 78, by, 18, 12, "<", BTN, BTN_TXT)) page = Math.max(0, page - 1);
            g.drawCenteredString(this.font, (page + 1) + "/" + pages, px + pw - 44, by + 2, 0xFFFFFFFF);
            if (button(g, px + pw - 28, by, 18, 12, ">", BTN, BTN_TXT)) page = Math.min(pages - 1, page + 1);
        }
    }

    // ----------------------------------------------------------- options page
    private void renderOptions(GuiGraphics g, Config cfg) {
        if (optMod == null) return;
        Module m = optMod;
        Config.ModuleCfg c = m.cfg();
        int cols = m.accentLabel != null ? 3 : 2;
        int colW = (pw - 16 - (cols - 1) * 12) / cols;
        int y = py + 36;
        int x = px + 8;

        c.bg = colorEditor(g, "o_bg", x, y, colW, "Background color", c.bg, true);
        x += colW + 12;
        c.text = colorEditor(g, "o_tx", x, y, colW, "Text color", c.text, true);
        if (m.accentLabel != null) {
            x += colW + 12;
            c.accent = colorEditor(g, "o_ac", x, y, colW, m.accentLabel, c.accent, true);
        }

        int by = y + COLOR_EDITOR_H + 14;
        int bw = (pw - 16 - 8) / 2;
        if (button(g, px + 8, by, bw, 14, "Background: " + (c.showBg ? "ON" : "OFF"), BTN, BTN_TXT)) {
            c.showBg = !c.showBg;
            Config.save();
        }
        if (button(g, px + 8 + bw + 8, by, bw, 14, "Reset colors", BTN, BTN_TXT)) {
            m.resetColors();
            Config.save();
        }
        if (m.movable()) {
            int sy = by + 22;
            g.drawString(this.font, "Scale: " + String.format("%.2fx", c.scale), px + 8, sy + 3, 0xFFFFFFFF, true);
            if (button(g, px + 90, sy, 16, 14, "-", BTN, BTN_TXT)) { c.scale = Math.max(0.5f, Math.round((c.scale - 0.1f) * 10) / 10f); Config.save(); }
            if (button(g, px + 110, sy, 16, 14, "+", BTN, BTN_TXT)) { c.scale = Math.min(3.0f, Math.round((c.scale + 0.1f) * 10) / 10f); Config.save(); }
            if (button(g, px + 130, sy, 30, 14, "1.0x", BTN, BTN_TXT)) { c.scale = 1.0f; Config.save(); }
        }
        if (!m.movable()) {
            g.drawString(this.font, "Colors apply to the in-world waypoint labels.", px + 8, by + 22, DIM, true);
        }
    }

    // ---------------------------------------------------------- settings tab
    private void renderSettings(GuiGraphics g, Config cfg) {
        int w = (pw - 30) / 2;
        int lx = px + 10;
        int y = py + 54;

        cfg.accent = opaque(colorEditor(g, "s_acc", lx, y, w, "Menu accent color", cfg.accent, false));
        int bg = colorEditor(g, "s_bg", lx + w + 10, y, w, "Menu background", cfg.panelBg, true);
        if ((bg >>> 24) < 0x40) bg = (0x40 << 24) | (bg & 0xFFFFFF);
        cfg.panelBg = bg;

        int by = y + COLOR_EDITOR_H + 16;
        if (button(g, lx, by, w, 14, "Clock: " + (cfg.clock24h ? "24-hour" : "12-hour"), BTN, BTN_TXT)) {
            cfg.clock24h = !cfg.clock24h;
        }
        if (button(g, lx + w + 10, by, w, 14, "Minimap zoom: " + cfg.mapZoom + "x", BTN, BTN_TXT)) {
            cfg.mapZoom = cfg.mapZoom % 4 + 1;
        }
        by += 18;
        if (button(g, lx, by, w, 14, "Reset all positions", BTN, BTN_TXT)) {
            for (Module m : Modules.ALL) { m.resetPosition(); m.cfg().scale = 1f; }
            Config.save();
        }
        if (button(g, lx + w + 10, by, w, 14, "Reset all colors", BTN, BTN_TXT)) {
            for (Module m : Modules.ALL) m.resetColors();
            cfg.accent = 0xFFFFFFFF;
            cfg.panelBg = 0xF2050505;
            Config.save();
        }
by += 18;
if (button(g, lx, by, w, 14, "Name badge (C): " + (cfg.nameBadge ? "ON" : "OFF"), BTN, BTN_TXT)) {
    cfg.nameBadge = !cfg.nameBadge;
    Config.save();
}
        // name at the bottom right of the settings page
        Component name = Component.literal(ConfisClient.NAME).withStyle(ChatFormatting.BOLD);
        g.drawString(this.font, name, px + pw - 8 - this.font.width(name), py + ph - 14, opaque(cfg.accent), true);
    }

    // -------------------------------------------------------- waypoints tab
    private void renderWaypoints(GuiGraphics g, Config cfg) {
        Minecraft mc = Minecraft.getInstance();
        int accent = opaque(cfg.accent);
        if (button(g, px + 8 + 190 + 6, py + 52, 120, 14, "+ Add by coordinates", BTN, BTN_TXT)) {
            mc.setScreen(new WaypointAddScreen(this));
            return;
        }
        if (button(g, px + 8, py + 52, 190, 14, "+ Add waypoint at my position", accent, contrast(accent))
                && mc.player != null && mc.level != null) {
            cfg.waypointList.add(new Waypoint("Waypoint " + (cfg.waypointList.size() + 1),
                    mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ(),
                    mc.level.dimension().toString(), Waypoints.nextColor(cfg)));
            Config.save();
        }

        int rowH = 17;
        int top = py + 72;
        int perPage = Math.max(1, (ph - 72 - 22) / rowH);
        int pages = Math.max(1, (cfg.waypointList.size() + perPage - 1) / perPage);
        if (page >= pages) page = pages - 1;

        if (cfg.waypointList.isEmpty()) {
            g.drawString(this.font, "No waypoints yet. Click the button above to add one here.",
                    px + 8, top + 4, DIM, true);
        }
        for (int n = 0; n < perPage; n++) {
            int idx = page * perPage + n;
            if (idx >= cfg.waypointList.size()) break;
            Waypoint w = cfg.waypointList.get(idx);
            int y = top + n * rowH;
            g.fill(px + 8, y, px + pw - 8, y + rowH - 2, 0xFF0D0D0D);
            outline(g, px + 8, y, pw - 16, rowH - 2, LINE);

            g.fill(px + 11, y + 2, px + 24, y + 13, 0xFFFFFFFF);
            g.fill(px + 12, y + 3, px + 23, y + 12, opaque(w.color));
            if (clickIn(px + 11, y + 2, 13, 11)) {
                int ci = 0;
                for (int i = 0; i < Waypoints.PALETTE.length; i++) if (Waypoints.PALETTE[i] == w.color) ci = i;
                w.color = Waypoints.PALETTE[(ci + 1) % Waypoints.PALETTE.length];
                Config.save();
            }
            String dim = w.dim.contains("nether") ? "Nether" : w.dim.contains("end") ? "End"
                    : w.dim.contains("overworld") ? "Overworld" : "?";
            g.drawString(this.font, w.name, px + 30, y + 4, 0xFFFFFFFF, true);
            g.drawString(this.font, w.x + ", " + w.y + ", " + w.z + "  (" + dim + ")",
                    px + 120, y + 4, 0xFFCCCCCC, true);
            if (button(g, px + pw - 8 - 16, y + 1, 14, 13, "X", BTN, BTN_TXT)) {
                cfg.waypointList.remove(idx);
                Config.save();
                break;
            }
        }

        if (pages > 1) {
            int by = py + ph - 17;
            if (button(g, px + pw - 78, by, 18, 12, "<", BTN, BTN_TXT)) page = Math.max(0, page - 1);
            g.drawCenteredString(this.font, (page + 1) + "/" + pages, px + pw - 44, by + 2, 0xFFFFFFFF);
            if (button(g, px + pw - 28, by, 18, 12, ">", BTN, BTN_TXT)) page = Math.min(pages - 1, page + 1);
        }
    }

    // -------------------------------------------------------------- edit HUD
    private void renderEdit(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        Config cfg = Config.get();
        int accent = opaque(cfg.accent);
        int W = this.width, H = this.height;

        g.drawCenteredString(this.font, "Drag any element to move it", W / 2, 8, 0xFFFFFFFF);
        if (button(g, W / 2 - 62, 22, 60, 14, "DONE", accent, contrast(accent))) {
            mode = Mode.MENU;
            dragging = null;
            Config.save();
            return;
        }
        if (button(g, W / 2 + 2, 22, 60, 14, "RESET", BTN, BTN_TXT)) {
            for (Module m : Modules.ALL) m.resetPosition();
            Config.save();
        }

        List<Module> movable = new ArrayList<>();
        for (Module m : Modules.ALL) if (m.movable() && m.cfg().enabled) movable.add(m);

        if (clicked && dragging == null) {
            for (int i = movable.size() - 1; i >= 0; i--) {
                Module m = movable.get(i);
                int x = m.posX(mc, W), y = m.posY(mc, H);
                if (in(x, y, m.sw(mc), m.sh(mc))) {
                    dragging = m;
                    dragOffX = mx - x;
                    dragOffY = my - y;
                    clicked = false;
                    break;
                }
            }
        }
        if (dragging != null) {
            if (down) {
                int w = dragging.sw(mc), h = dragging.sh(mc);
                int nx = Math.max(0, Math.min(W - w, mx - dragOffX));
                int ny = Math.max(0, Math.min(H - h, my - dragOffY));
                if (nx < 5) nx = 0;
                if (ny < 5) ny = 0;
                if (nx > W - w - 5) nx = W - w;
                if (ny > H - h - 5) ny = H - h;
                if (Math.abs(nx + w / 2 - W / 2) < 4) nx = W / 2 - w / 2;
                Config.ModuleCfg c = dragging.cfg();
                c.fx = nx / (double) W;
                c.fy = ny / (double) H;
            } else {
                dragging = null;
                Config.save();
            }
        }

        for (Module m : movable) {
            int x = m.posX(mc, W), y = m.posY(mc, H);
            int w = m.sw(mc), h = m.sh(mc);
            boolean active = m == dragging;
            if (in(x, y, w, h) || active) g.fill(x, y, x + w, y + h, 0x30FFFFFF);
            outline(g, x - 1, y - 1, w + 2, h + 2, active ? 0xFFFFFFFF : 0xFF9A9A9A);
        }
    }
}
