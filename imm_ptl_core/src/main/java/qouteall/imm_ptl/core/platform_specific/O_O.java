package qouteall.imm_ptl.core.platform_specific;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.SharedConstants;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import qouteall.imm_ptl.core.chunk_loading.MyClientChunkManager;
import qouteall.imm_ptl.core.portal.custom_portal_gen.PortalGenInfo;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Optional;

public class O_O {
    public static boolean isDimensionalThreadingPresent = false;
    
    public static boolean isForge() {
        return false;
    }
    
    @Environment(EnvType.CLIENT)
    public static void onPlayerChangeDimensionClient(
        ResourceKey<Level> from, ResourceKey<Level> to
    ) {
        RequiemCompat.onPlayerTeleportedClient();
    }

//    @Environment(EnvType.CLIENT)
//    public static void segregateClientEntity(
//        ClientWorld fromWorld,
//        Entity entity
//    ) {
//        ((IEClientWorld_MA) fromWorld).segregateEntity(entity);
//        entity.removed = false;
//    }
//
//    public static void segregateServerEntity(
//        ServerWorld fromWorld,
//        Entity entity
//    ) {
//        fromWorld.removeEntity(entity);
//        entity.removed = false;
//    }
//
//    public static void segregateServerPlayer(
//        ServerWorld fromWorld,
//        ServerPlayerEntity player
//    ) {
//        fromWorld.removePlayer(player);
//        player.removed = false;
//    }
    
    public static void onPlayerTravelOnServer(
        ServerPlayer player,
        ResourceKey<Level> from,
        ResourceKey<Level> to
    ) {
        RequiemCompat.onPlayerTeleportedServer(player);
    }
    
    public static Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }
    
    private static final BlockState obsidianState = Blocks.OBSIDIAN.defaultBlockState();
    
    public static boolean isObsidian(BlockState blockState) {
        return blockState == obsidianState;
    }
    
    public static void postClientChunkLoadEvent(LevelChunk chunk) {
        ClientChunkEvents.CHUNK_LOAD.invoker().onChunkLoad(
            ((ClientLevel) chunk.getLevel()), chunk
        );
    }
    
    public static void postClientChunkUnloadEvent(LevelChunk chunk) {
        ClientChunkEvents.CHUNK_UNLOAD.invoker().onChunkUnload(
            ((ClientLevel) chunk.getLevel()), chunk
        );
    }
    
    public static boolean isDedicatedServer() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
    }
    
    public static void postPortalSpawnEventForge(PortalGenInfo info) {
    
    }
    
    @Environment(EnvType.CLIENT)
    public static ClientChunkCache createMyClientChunkManager(ClientLevel world, int loadDistance) {
        return new MyClientChunkManager(world, loadDistance);
    }
    
    public static boolean getIsPehkuiPresent() {
        return FabricLoader.getInstance().isModLoaded("pehkui");
    }
    
    @Nullable
    public static String getImmPtlModInfoUrl() {
        String gameVersion = SharedConstants.getCurrentVersion().getName();
        
        if (O_O.isForge()) {
            return "https://qouteall.fun/immptl_info/forge-%s.json".formatted(gameVersion);
        }
        else {
            return "https://qouteall.fun/immptl_info/%s.json".formatted(gameVersion);
        }
    }
    
    public static boolean isModLoadedWithinVersion(String modId, @Nullable String startVersion, @Nullable String endVersion) {
        Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer(modId);
        if (modContainer.isPresent()) {
            Version version = modContainer.get().getMetadata().getVersion();
            
            try {
                if (startVersion != null) {
                    int i = Version.parse(startVersion).compareTo(version);
                    if (i > 0) {
                        return false;
                    }
                }
                
                if (endVersion != null) {
                    int i = Version.parse(endVersion).compareTo(version);
                    if (i < 0) {
                        return false;
                    }
                }
            }
            catch (VersionParsingException e) {
                e.printStackTrace();
            }
            
            return true;
            
        }
        else {
            return false;
        }
    }
    
    public static boolean shouldUpdateImmPtl(String latestReleaseVersion) {
        Version currentVersion = FabricLoader.getInstance()
            .getModContainer("imm_ptl_core").get().getMetadata().getVersion();
        try {
            Version latestVersion = Version.parse(latestReleaseVersion);
            
            if (latestVersion.compareTo(currentVersion) > 0) {
                return true;
            }
        }
        catch (VersionParsingException e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    public static String getModDownloadLink() {
        return "https://www.curseforge.com/minecraft/mc-mods/immersive-portals-mod";
    }
}
