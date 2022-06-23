package qouteall.imm_ptl.core.mixin.common.other_sync;

import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.platform_specific.IPNetworking;

@Mixin(MapItemSavedData.class)
public class MixinMapItemSavedData {
    @Shadow
    @Final
    public ResourceKey<Level> dimension;
    
    @Inject(
        method = "getUpdatePacket",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onGetUpdatePacket(int mapId, Player player, CallbackInfoReturnable<@Nullable Packet<?>> cir) {
        Packet packet = cir.getReturnValue();
        if (packet != null) {
            cir.setReturnValue(IPNetworking.createRedirectedMessage(dimension, packet));
        }
    }
}
