package client.confis.mixin;

import client.confis.Config;
import client.confis.DropScale;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Scales dropped items. Everything is matched by method NAME with require = 0, so if a Minecraft
 * update changes the signature the game still starts and drops just keep their normal size.
 */
@Mixin(ItemEntityRenderer.class)
public class ItemDropScaleMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    private void confis$remember(ItemEntity entity, ItemEntityRenderState state, float partial, CallbackInfo ci) {
        String id = BuiltInRegistries.ITEM.getKey(entity.getItem().getItem()).getPath();
        DropScale.BY_STATE.put(state, DropScale.forId(id));
    }

    @Inject(method = "submit", at = @At("HEAD"), require = 0)
    private void confis$push(ItemEntityRenderState state, PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector c,
                             net.minecraft.client.renderer.state.CameraRenderState cam, CallbackInfo ci) {
        float s = DropScale.BY_STATE.getOrDefault(state, 1f);
        pose.pushPose();
        pose.scale(s, s, s);
    }

    @Inject(method = "submit", at = @At("RETURN"), require = 0)
    private void confis$pop(ItemEntityRenderState state, PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector c,
                            net.minecraft.client.renderer.state.CameraRenderState cam, CallbackInfo ci) {
        pose.popPose();
    }
}
