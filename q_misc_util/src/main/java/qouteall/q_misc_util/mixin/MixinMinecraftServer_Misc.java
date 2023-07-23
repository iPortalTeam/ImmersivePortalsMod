package qouteall.q_misc_util.mixin;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.apache.commons.lang3.Validate;
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
public abstract class MixinMinecraftServer_Misc extends ReentrantBlockableEventLoop implements IEMinecraftServer_Misc {
    public MixinMinecraftServer_Misc(String string) {
        super(string);
        throw new RuntimeException();
    }
    
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
    
    @Shadow
    protected abstract void waitUntilNextTick();
    
    @Shadow
    private boolean stopped;
    
    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    private void onConstruct(
        Thread thread, LevelStorageSource.LevelStorageAccess levelStorageAccess, PackRepository packRepository, WorldStem worldStem, Proxy proxy, DataFixer dataFixer, Services services, ChunkProgressListenerFactory chunkProgressListenerFactory, CallbackInfo ci
    ) {
        MiscGlobals.refMinecraftServer = new WeakReference<>((MinecraftServer) ((Object) this));
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
    public LevelStorageSource.LevelStorageAccess ip_getStorageSource() {
        return storageSource;
    }
    
    @Override
    public Executor ip_getExecutor() {
        return executor;
    }
    
    @Override
    public void ip_waitUntilNextTick() {
        Validate.isTrue(!runningTask());
        
        waitUntilNextTick();
    }
    
    @Override
    public void ip_setStopped(boolean arg) {
        Validate.isTrue(!runningTask());
        
        stopped = arg;
    }
}
