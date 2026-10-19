package client.confis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public final class Hud {
    private static int frames, fps;
    private static long lastFpsTime = System.nanoTime();
    private static boolean shiftWasDown, ctrlWasDown;

    private Hud() {}

    public static int fps() { return fps; }

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        pollMenuKey(mc);
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;

        countFrame();
        Cps.poll(mc);

        boolean preview = mc.screen instanceof ConfigScreen;
        int W = g.guiWidth(), H = g.guiHeight();
        for (Module m : Modules.ALL) {
            Config.ModuleCfg c = m.cfg();
            if (!c.enabled) continue;
            if (!m.movable()) {
                if (m instanceof Modules.WaypointsModule) Waypoints.render(g, mc, c);
                continue;
            }
            int hx = m.posX(mc, W), hy = m.posY(mc, H);
            float s = c.scale;
            g.pose().pushMatrix();
            g.pose().translate(hx, hy);
            g.pose().scale(s, s);
            m.draw(g, mc, c, 0, 0, preview);
            g.pose().popMatrix();
        }
    }

    /** Right Shift opens / closes the menu. Polled from the render thread's GL context. */
    private static void pollMenuKey(Minecraft mc) {
        long window = GLFW.glfwGetCurrentContext();
        if (window == 0L) return;
        boolean down = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (down && !shiftWasDown) {
            if (mc.screen == null && mc.player != null) mc.setScreen(new ConfigScreen());
            else if (mc.screen instanceof ConfigScreen) mc.setScreen(null);
        }
        shiftWasDown = down;
        boolean ctrl = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        if (ctrl && !ctrlWasDown && mc.screen == null && mc.player != null) mc.setScreen(new AiScreen(null));
        ctrlWasDown = ctrl;
    }

    private static void countFrame() {
        frames++;
        long now = System.nanoTime();
        if (now - lastFpsTime >= 1_000_000_000L) {
            fps = frames;
            frames = 0;
            lastFpsTime = now;
        }
    }
}
