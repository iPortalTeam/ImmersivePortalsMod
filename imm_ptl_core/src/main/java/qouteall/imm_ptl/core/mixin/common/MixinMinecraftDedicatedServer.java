package qouteall.imm_ptl.core.mixin.common;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ServerResources;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.platform_specific.O_O;

@Mixin(DedicatedServer.class)
public class MixinMinecraftDedicatedServer {
    @Inject(
        method = "<init>", at = @At("RETURN")
    )
    private void onInitEnded(
        Thread serverThread, RegistryAccess.RegistryHolder registryManager,
        LevelStorageSource.LevelStorageAccess session, PackRepository dataPackManager,
        ServerResources serverResourceManager, WorldData saveProperties,
        DedicatedServerSettings propertiesLoader, DataFixer dataFixer,
        MinecraftSessionService sessionService, GameProfileRepository gameProfileRepo,
        GameProfileCache userCache,
        ChunkProgressListenerFactory worldGenerationProgressListenerFactory,
        CallbackInfo ci
    ) {
        // loading it requires getting the server directory
        O_O.loadConfigFabric();
    }
}
