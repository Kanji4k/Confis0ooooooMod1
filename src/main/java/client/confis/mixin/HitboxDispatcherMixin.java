package client.confis.mixin;

import client.confis.HitboxRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Turns the game's own hitbox display on for just the entities picked in Hitboxes & Nametags,
 * then puts the flag back. Matched by name with require = 0: if Minecraft changes this code the
 * game still starts and the hitbox switches simply do nothing.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class HitboxDispatcherMixin {
    @Shadow private boolean shouldRenderHitBoxes;
    @Unique private boolean confis$saved;

    @Inject(method = "extractEntity", at = @At("HEAD"), require = 0)
    private void confis$before(Entity entity, float partialTick, CallbackInfoReturnable<EntityRenderState> cir) {
        confis$saved = shouldRenderHitBoxes;
        if (HitboxRenderer.wants(entity)) shouldRenderHitBoxes = true;
    }

    @Inject(method = "extractEntity", at = @At("RETURN"), require = 0)
    private void confis$after(Entity entity, float partialTick, CallbackInfoReturnable<EntityRenderState> cir) {
        shouldRenderHitBoxes = confis$saved;
    }
}
