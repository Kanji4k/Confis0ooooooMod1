package client.confis.mixin;

import client.confis.TpsTracker;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class TpsMixin {
    // Matched by name only, so it survives packet signature changes. require = 0: if it ever
    // fails to apply the game still runs and TPS just shows "--".
    @Inject(method = "handleSetTime", at = @At("HEAD"), require = 0)
    private void confis$onSetTime(CallbackInfo ci) {
        TpsTracker.onTimePacket();
    }
}
