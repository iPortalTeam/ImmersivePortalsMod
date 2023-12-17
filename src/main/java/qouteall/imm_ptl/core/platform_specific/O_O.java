package qouteall.imm_ptl.core.platform_specific;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.impl.util.version.SemanticVersionImpl;
import net.minecraft.SharedConstants;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.chunk_loading.ImmPtlClientChunkMap;
import qouteall.imm_ptl.core.network.ImmPtlNetworkConfig;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.custom_portal_gen.PortalGenInfo;
import qouteall.q_misc_util.Helper;

import java.nio.file.Path;
import java.util.List;
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
    
    public static void onPlayerTravelOnServer(
        ServerPlayer player,
        ServerLevel fromWorld, ServerLevel toWorld
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
        return new ImmPtlClientChunkMap(world, loadDistance);
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
            // it's in github pages
            // https://github.com/qouteall/immptl_info
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
    
    public static @NotNull ImmPtlNetworkConfig.ModVersion getImmPtlVersion() {
        Version version = FabricLoader.getInstance()
            .getModContainer("iportal").orElseThrow()
            .getMetadata().getVersion();
        
        if (!(version instanceof SemanticVersionImpl semanticVersion)) {
            // in dev env, its ${version}
            return ImmPtlNetworkConfig.ModVersion.OTHER;
        }
        
        if (semanticVersion.getVersionComponentCount() != 3) {
            Helper.LOGGER.error(
                "immersive portals version {} is not in regular form", semanticVersion
            );
            return ImmPtlNetworkConfig.ModVersion.OTHER;
        }
        
        return new ImmPtlNetworkConfig.ModVersion(
            semanticVersion.getVersionComponent(0),
            semanticVersion.getVersionComponent(1),
            semanticVersion.getVersionComponent(2)
        );
    }
    
    public static String getImmPtlVersionStr() {
        return FabricLoader.getInstance()
            .getModContainer("iportal").orElseThrow()
            .getMetadata().getVersion().toString();
    }
    
    public static boolean shouldUpdateImmPtl(String latestReleaseVersion) {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            return false;
        }
        
        Version currentVersion = FabricLoader.getInstance()
            .getModContainer("iportal").get().getMetadata().getVersion();
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
        return "https://modrinth.com/mod/immersiveportals";
    }
    
    public static String getIssueLink() {
        return "https://github.com/iPortalTeam/ImmersivePortalsMod/issues";
    }
    
    @Nullable
    public static ResourceLocation getModIconLocation(String modid) {
        String path = FabricLoader.getInstance().getModContainer(modid)
            .flatMap(c -> c.getMetadata().getIconPath(512))
            .orElse(null);
        if (path == null) {
            return null;
        }
        
        // for example, if the icon path is "assets/modid/icon.png"
        // then the result should be modid:icon.png
        
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.startsWith("assets")) {
            path = path.substring("assets".length());
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        String[] parts = path.split("/");
        if (parts.length != 2) {
            return null;
        }
        return new ResourceLocation(parts[0], parts[1]);
    }
    
    @Nullable
    public static String getModName(String modid) {
        return FabricLoader.getInstance().getModContainer(modid)
            .map(c -> c.getMetadata().getName())
            .orElse(null);
    }
    
    // most quilt installations use quilted fabric api
    public static boolean isQuilt() {
        return FabricLoader.getInstance().isModLoaded("quilted_fabric_api");
    }
    
    public static List<String> getLoadedModIds() {
        return FabricLoader.getInstance().getAllMods().stream()
            .map(c -> c.getMetadata().getId()).sorted().toList();
    }
    
    public static boolean allowTeleportingEntity(Entity entity, Portal portal) {
        // ForgeHooks.onTravelToDimension() on Forge
        return true;
    }
    
    public static boolean isDevEnv() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
