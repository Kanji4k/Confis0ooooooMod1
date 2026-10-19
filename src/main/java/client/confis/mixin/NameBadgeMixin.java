package client.confis.mixin;

import client.confis.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class NameBadgeMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true, require = 0)
    private void confis$badge(CallbackInfoReturnable<Component> cir) {
        if (!Config.get().nameBadge) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || (Object) this != mc.player) return;
        Component original = cir.getReturnValue();
        if (original == null || original.getString().startsWith("C ")) return;
        cir.setReturnValue(Component.empty()
                .append(Component.literal("C").withStyle(ChatFormatting.BOLD, ChatFormatting.WHITE))
                .append(Component.literal(" "))
                .append(original));
    }
}
