package client.confis;

import java.util.ArrayDeque;
import net.minecraft.client.Minecraft;

/** Counts clicks by watching the attack/use key for rising edges once per rendered frame. */
public final class Cps {
    private static final ArrayDeque<Long> LEFT = new ArrayDeque<>();
    private static final ArrayDeque<Long> RIGHT = new ArrayDeque<>();
    private static boolean wasLeft, wasRight;

    private Cps() {}

    public static void poll(Minecraft mc) {
        long now = System.currentTimeMillis();
        boolean l = mc.options.keyAttack.isDown();
        boolean r = mc.options.keyUse.isDown();
        if (l && !wasLeft) LEFT.add(now);
        if (r && !wasRight) RIGHT.add(now);
        wasLeft = l;
        wasRight = r;
        trim(LEFT, now);
        trim(RIGHT, now);
    }

    private static void trim(ArrayDeque<Long> q, long now) {
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
    }

    public static int left() { return LEFT.size(); }
    public static int right() { return RIGHT.size(); }
}
