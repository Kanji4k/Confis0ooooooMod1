package client.confis;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Remembers the distance of your last hit (eye to the closest point of the target's hitbox). */
public final class ReachTracker {
    private static double last = -1;
    private static long lastTime;

    private ReachTracker() {}

    public static void register() {
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (level.isClientSide()) {
                Vec3 eye = player.getEyePosition();
                AABB b = entity.getBoundingBox();
                Vec3 p = new Vec3(clamp(eye.x, b.minX, b.maxX), clamp(eye.y, b.minY, b.maxY), clamp(eye.z, b.minZ, b.maxZ));
                last = eye.distanceTo(p);
                lastTime = System.currentTimeMillis();
            }
            return InteractionResult.PASS;
        });
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    /** Distance of the last hit, or -1 if none in the last 5 seconds. */
    public static double last() { return System.currentTimeMillis() - lastTime > 5000 ? -1 : last; }
}
