package qouteall.q_misc_util.mixin.dimension;

import com.google.common.collect.Maps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.WorldData;
import org.apache.commons.lang3.Validate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.q_misc_util.api.DimensionAPI;
import qouteall.q_misc_util.dimension.DimensionIdManagement;
import qouteall.q_misc_util.ducks.IEMinecraftServer_Misc;

import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer_D implements IEMinecraftServer_Misc {
    @Shadow
    @Final
    protected WorldData worldData;
    
    @Shadow
    public abstract RegistryAccess.Frozen registryAccess();
    
    @Mutable
    @Shadow
    @Final
    private Map<ResourceKey<Level>, ServerLevel> levels;
    
    @Shadow
    public abstract boolean isStopped();
    
    private boolean ip_canDirectlyRegisterDimension = false;
    
    private boolean ip_finishedCreatingWorlds = false;
    
    @Inject(method = "Lnet/minecraft/server/MinecraftServer;createLevels(Lnet/minecraft/server/level/progress/ChunkProgressListener;)V", at = @At("HEAD"))
    private void onBeforeCreateWorlds(
        ChunkProgressListener worldGenerationProgressListener, CallbackInfo ci
    ) {
        Validate.isTrue(
            !ip_canDirectlyRegisterDimension, "invalid server initialization status"
        );
        ip_canDirectlyRegisterDimension = true;
        
        DimensionAPI.SERVER_DIMENSIONS_LOAD_EVENT.invoker().run(
            (MinecraftServer) (Object) this
        );
        
        ip_canDirectlyRegisterDimension = false;
    }
    
    @Inject(
        method = "Lnet/minecraft/server/MinecraftServer;createLevels(Lnet/minecraft/server/level/progress/ChunkProgressListener;)V",
        at = @At("RETURN")
    )
    private void onFinishedLoadingAllWorlds(
        CallbackInfo ci
    ) {
        MinecraftServer this_ = (MinecraftServer) (Object) this;
        DimensionIdManagement.onServerStarted(this_);
        ip_finishedCreatingWorlds = true;
    }
    
    
    @Override
    public void ip_addDimensionToWorldMap(ResourceKey<Level> dim, ServerLevel world) {
        // use read-copy-update to avoid concurrency issues
        LinkedHashMap<ResourceKey<Level>, ServerLevel> newMap =
            Maps.<ResourceKey<Level>, ServerLevel>newLinkedHashMap();
        
        Map<ResourceKey<Level>, ServerLevel> oldMap = this.levels;
        
        newMap.putAll(oldMap);
        newMap.put(dim, world);
        
        this.levels = newMap;
    }
    
    @Override
    public void ip_removeDimensionFromWorldMap(ResourceKey<Level> dimension) {
        // use read-copy-update to avoid concurrency issues
        LinkedHashMap<ResourceKey<Level>, ServerLevel> newMap =
            Maps.<ResourceKey<Level>, ServerLevel>newLinkedHashMap();
        
        Map<ResourceKey<Level>, ServerLevel> oldMap = this.levels;
        
        for (Map.Entry<ResourceKey<Level>, ServerLevel> entry : oldMap.entrySet()) {
            if (entry.getKey() != dimension) {
                newMap.put(entry.getKey(), entry.getValue());
            }
        }
        
        this.levels = newMap;
    }
    
    @Override
    public boolean ip_getCanDirectlyRegisterDimensions() {
        return ip_canDirectlyRegisterDimension;
    }
    
    @Override
    public boolean ip_getIsFinishedCreatingWorlds() {
        return ip_finishedCreatingWorlds;
    }
}
