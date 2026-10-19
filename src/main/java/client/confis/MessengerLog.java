package client.confis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Private messages between players. Sending uses the server's /msg command, so it works with any
 * player on the server (the other player does not need this client to read it, but if they do they
 * get it in the Messenger too).
 */
public final class MessengerLog {
    public record Entry(boolean mine, String text) {}

    public static final Map<String, List<Entry>> CHATS = new LinkedHashMap<>();
    public static final Map<String, Integer> UNREAD = new LinkedHashMap<>();

    private MessengerLog() {}

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> { if (!overlay) onMessage(msg); });
    }

    private static void onMessage(Component msg) {
        if (msg.getContents() instanceof TranslatableContents tc
                && tc.getKey().equals("commands.message.display.incoming")
                && tc.getArgs().length >= 2) {
            String from = str(tc.getArgs()[0]);
            String text = str(tc.getArgs()[1]);
            add(from, false, text);
            UNREAD.merge(from, 1, Integer::sum);
        }
    }

    private static String str(Object o) { return o instanceof Component c ? c.getString() : String.valueOf(o); }

    public static void add(String who, boolean mine, String text) {
        CHATS.computeIfAbsent(who, k -> new ArrayList<>()).add(new Entry(mine, text));
    }

    public static void send(String who, String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || who == null || text.isBlank()) return;
        mc.player.connection.sendCommand("msg " + who + " " + text);
        add(who, true, text);
    }
}
