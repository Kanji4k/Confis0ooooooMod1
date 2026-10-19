package client.confis.mixin;

import client.confis.Config;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Nametags OFF: no entity shows a name above its head. Matched by name only, require = 0. */
@Mixin(EntityRenderer.class)
public class NametagMixin {
    @Inject(method = "shouldShowName", at = @At("RETURN"), cancellable = true, require = 0)
    private void confis$hideNames(Entity entity, double distSq, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.get().nametags) cir.setReturnValue(false);
    }
}
