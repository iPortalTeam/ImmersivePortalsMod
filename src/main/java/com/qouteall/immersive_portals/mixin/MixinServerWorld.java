package com.qouteall.immersive_portals.mixin;

import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerWorld.class)
public class MixinServerWorld {
//    /**
//     * {@link ServerWorld#tick(BooleanSupplier)}
//     * if forced chunks and players are empty, it will not tick entities
//     * make it always tick entities
//     */
//    @Redirect(
//        method = "Lnet/minecraft/server/world/ServerWorld;tick(Ljava/util/function/BooleanSupplier;)V",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/server/world/ServerWorld;getForcedChunks()Lit/unimi/dsi/fastutil/longs/LongSet;"
//        )
//    )
//    private LongSet redirectGetForcedChunks(ServerWorld world) {
//        return new LongOpenHashSet(new long[]{2333L});
//    }
}
