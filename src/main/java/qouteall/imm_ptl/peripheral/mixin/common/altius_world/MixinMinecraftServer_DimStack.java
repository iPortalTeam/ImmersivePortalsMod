package qouteall.imm_ptl.peripheral.mixin.common.altius_world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.imm_ptl.peripheral.altius_world.BedrockReplacement;
import qouteall.q_misc_util.Helper;

import java.util.Map;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer_DimStack {
    @Shadow
    public abstract ServerWorld getWorld(RegistryKey<World> dimensionType);
    
    @Shadow @Final private Map<RegistryKey<World>, ServerWorld> worlds;
    
    @Inject(
        method = "createWorlds",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;setupSpawn(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/world/level/ServerWorldProperties;ZZ)V"
        )
    )
    private void onBeforeSetupSpawn(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci) {
        BedrockReplacement.onServerEarlyInit((MinecraftServer) (Object) this);
    }
    
    @Inject(
        method = "createWorlds",
        at = @At("RETURN")
    )
    private void onCreateWorldsFinishes(
        WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci
    ) {
        BedrockReplacement.onServerCreatedWorlds((MinecraftServer) (Object) this);
    }
}
