package client.confis;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Decides which entities get a hitbox outline. The outline itself is drawn by the game's own
 * hitbox renderer (the F3+B one); HitboxDispatcherMixin switches it on just for the types enabled here.
 */
public final class HitboxRenderer {
    public static final String[] KEYS = {"crystal", "exp", "friendly", "hostile", "firework", "pearl", "windcharge"};
    public static final String[] LABELS = {"Crystal", "XP orb", "Friendly mob", "Hostile mob", "Firework", "Ender pearl", "Wind charge"};

    private HitboxRenderer() {}

    /** Kept so ConfisClient does not need to change. Nothing to register any more. */
    public static void register() {}

    public static boolean on(String key) { return Config.get().hitboxes.getOrDefault(key, false); }

    private static int typeOf(Entity e) {
        EntityType<?> t = e.getType();
        if (t == EntityType.END_CRYSTAL) return 0;
        if (t == EntityType.EXPERIENCE_ORB) return 1;
        if (t == EntityType.FIREWORK_ROCKET) return 4;
        if (t == EntityType.ENDER_PEARL) return 5;
        if (t == EntityType.WIND_CHARGE || t == EntityType.BREEZE_WIND_CHARGE) return 6;
        if (e instanceof Enemy) return 3;
        if (e instanceof Mob) return 2;
        return -1;
    }

    /** True if this entity should show its hitbox right now. */
    public static boolean wants(Entity e) {
        if (!Config.get().hitboxMaster) return false;
        if (e == Minecraft.getInstance().player) return false;
        int t = typeOf(e);
        return t >= 0 && on(KEYS[t]);
    }
}
