package client.confis;

import net.minecraft.client.gui.GuiGraphics;

/** Original 12x12 pixel icons (drawn from bitmaps, no image files needed). */
public final class Icons {
    private Icons() {}

    private static final String[] FPS = {"...######...", "..##....##..", ".#........#.", "#.......#..#", "#......#...#", "#.....#....#", "#....##....#", "#...##.....#", "#..........#", "............", ".##########.", "............"};
    private static final String[] PING = {"............", "............", ".........##.", ".........##.", ".........##.", "......##.##.", "......##.##.", "...##.##.##.", "...##.##.##.", "##.##.##.##.", "##.##.##.##.", "##.##.##.##."};
    private static final String[] TPS = {".......###..", "......###...", ".....###....", "....###.....", "...#######..", "..#######...", "......###...", ".....###....", ".....##.....", "....##......", "....#.......", "............"};
    private static final String[] CPS = {"...######...", "..#..#...#..", ".#...#....#.", ".#...#....#.", ".##########.", ".#........#.", ".#........#.", ".#........#.", ".#........#.", "..#......#..", "...######...", "............"};
    private static final String[] CLOCK = {"...######...", "..#......#..", ".#...#....#.", "#....#.....#", "#....#.....#", "#....#.....#", "#....###...#", "#..........#", ".#........#.", "..#......#..", "...######...", "............"};
    private static final String[] DAY = {".....##.....", ".#...##...#.", "..#......#..", "....####....", "...######...", "##.######.##", "##.######.##", "...######...", "....####....", "..#......#..", ".#...##...#.", ".....##....."};
    private static final String[] ARMOR = {"##..####..##", "####....####", "####....####", ".##########.", ".##########.", ".##########.", "..########..", "..########..", "..########..", "..###..###..", "..###..###..", "............"};
    private static final String[] KEYSTROKES = {"....###.....", "....#.#.....", "....###.....", "............", "###.###.###.", "#.#.#.#.#.#.", "###.###.###.", "............", "###########.", "#.........#.", "###########.", "............"};
    private static final String[] MINIMAP = {"############", "#..........#", "#...##.....#", "#..####....#", "#...##..##.#", "#.......##.#", "#..........#", "#.##.......#", "#.###..#...#", "#..#..###..#", "#..........#", "############"};
    private static final String[] WAYPOINTS = {"....####....", "...######...", "..##....##..", "..#......#..", "..#..##..#..", "..#..##..#..", "..##....##..", "...#....#...", "....#..#....", ".....##.....", "............", "............"};
    private static final String[] EFFECTS = {"....####....", "...######...", "..########..", "..###..###..", "..########..", "...######...", "....####....", ".....##.....", "....####....", "...######...", "..########..", "............"};
    private static final String[] SCOREBOARD = {"############", "#..........#", "#.######...#", "#..........#", "#.####.....#", "#..........#", "#.#######..#", "#..........#", "#.###......#", "#..........#", "############", "............"};
    private static final String[] AI = {".....##.....", ".....##.....", "....####....", "##..####..##", "##.######.##", ".##########.", "...######...", "..########..", ".##########.", "##..####..##", ".....##.....", "............"};
    private static final String[] SEARCH = {".####..", "#....#.", "#....#.", "#....#.", ".####..", "....##.", ".....##"};
    private static final String[] REACH = {"............", "............", "#..........#", "#..........#", "#.#......#.#", "############", "#.#......#.#", "#..........#", "#..........#", "............", "............", "............"};
    private static final String[] RANGE = {"....####....", "..##....##..", ".#...##...#.", ".#..####..#.", "#..######..#", "#..######..#", ".#..####..#.", ".#...##...#.", "..##....##..", "....####....", "............", "............"};
    private static final String[] HEART = {".##.##.", "#######", "#######", ".#####.", "..###..", "...#..."};
    private static final String[] GEAR = {"...#...", ".#####.", ".#...#.", "##.#.##", ".#...#.", ".#####.", "...#..."};

    private static String[] get(String id) {
        return switch (id) {
            case "fps" -> FPS;
            case "ping" -> PING;
            case "tps" -> TPS;
            case "cps" -> CPS;
            case "clock" -> CLOCK;
            case "day" -> DAY;
            case "armor" -> ARMOR;
            case "keystrokes" -> KEYSTROKES;
            case "minimap" -> MINIMAP;
            case "waypoints" -> WAYPOINTS;
            case "effects" -> EFFECTS;
            case "scoreboard" -> SCOREBOARD;
            case "ai" -> AI;
            case "search" -> SEARCH;
            case "reach" -> REACH;
            case "range" -> RANGE;
            case "heart" -> HEART;
            case "gear" -> GEAR;
            default -> null;
        };
    }

    /** Draws the icon with its top-left at (x, y); each bitmap pixel becomes a scale x scale square. */
    public static void draw(GuiGraphics g, String id, int x, int y, int scale, int color) {
        String[] rows = get(id);
        if (rows == null) return;
        for (int j = 0; j < rows.length; j++) {
            String r = rows[j];
            int i = 0;
            while (i < r.length()) {
                if (r.charAt(i) != '#') { i++; continue; }
                int k = i;
                while (k < r.length() && r.charAt(k) == '#') k++;
                g.fill(x + i * scale, y + j * scale, x + k * scale, y + (j + 1) * scale, color);
                i = k;
            }
        }
    }

    public static int width(String id) { String[] r = get(id); return r == null ? 0 : r[0].length(); }
    public static int height(String id) { String[] r = get(id); return r == null ? 0 : r.length; }
}
