package qouteall.q_misc_util.mixin;

import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.q_misc_util.MiscGlobals;
import qouteall.q_misc_util.ducks.IEMinecraftServer_Misc;

import java.lang.ref.WeakReference;
import java.net.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer_Misc implements IEMinecraftServer_Misc {
    @Shadow
    public abstract boolean isDedicatedServer();
    
    @Shadow
    @Final
    public LevelStorageSource.LevelStorageAccess storageSource;
    
    @Shadow
    @Final
    private Executor executor;
    
    @Shadow
    @Final
    @Mutable
    private Map<ResourceKey<Level>, ServerLevel> levels;
    
    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    private void onConstruct(
        Thread thread,
        LevelStorageSource.LevelStorageAccess levelStorageAccess,
        PackRepository packRepository,
        WorldStem worldStem,
        Proxy proxy,
        DataFixer dataFixer,
        MinecraftSessionService minecraftSessionService,
        GameProfileRepository gameProfileRepository,
        GameProfileCache gameProfileCache,
        ChunkProgressListenerFactory chunkProgressListenerFactory,
        CallbackInfo ci
    ) {
        MiscGlobals.refMinecraftServer = new WeakReference<>((MinecraftServer) ((Object) this));
    }
    
    @Override
    public void addDimensionToWorldMap(ResourceKey<Level> dim, ServerLevel world) {
        LinkedHashMap<ResourceKey<Level>, ServerLevel> newMap =
            Maps.<ResourceKey<Level>, ServerLevel>newLinkedHashMap();
        
        Map<ResourceKey<Level>, ServerLevel> oldMap = this.levels;
        
        newMap.putAll(oldMap);
        newMap.put(dim, world);
        
        // do not directly mutate the map to avoid concurrency issues
        this.levels = newMap;
    }
    
    @Override
    public void removeDimensionFromWorldMap(ResourceKey<Level> dimension) {
        LinkedHashMap<ResourceKey<Level>, ServerLevel> newMap =
            Maps.<ResourceKey<Level>, ServerLevel>newLinkedHashMap();
        
        Map<ResourceKey<Level>, ServerLevel> oldMap = this.levels;
        
        for (Map.Entry<ResourceKey<Level>, ServerLevel> entry : oldMap.entrySet()) {
            if (entry.getKey() != dimension) {
                newMap.put(entry.getKey(), entry.getValue());
            }
        }
        
        // do not directly mutate the map to avoid concurrency issues
        this.levels = newMap;
    }
    
    @Override
    public LevelStorageSource.LevelStorageAccess ip_getStorageSource() {
        return storageSource;
    }
    
    @Override
    public Executor ip_getExecutor() {
        return executor;
    }
}
