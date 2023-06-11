package qouteall.imm_ptl.core.platform_specific;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.SharedConstants;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.chunk_loading.ImmPtlClientChunkMap;
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
    
    public static boolean shouldUpdateImmPtl(String latestReleaseVersion) {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            return false;
        }
        
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
    
    /**
     * {@link net.fabricmc.fabric.mixin.event.interaction.ServerPlayerInteractionManagerMixin#startBlockBreak(BlockPos, ServerboundPlayerActionPacket.Action, Direction, int, int, CallbackInfo)}
     * @return true to cancel
     */
    public static boolean onHandleBlockBreakAction(
        ServerPlayer player,
        ServerLevel level,
        BlockPos pos, ServerboundPlayerActionPacket.Action action,
        Direction face, int maxBuildHeight, int sequence
    ) {
        ServerboundPlayerActionPacket.Action playerAction = action;
        ServerLevel world = level;
        Direction direction = face;
    
        if (playerAction != ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) return false;
        InteractionResult result = AttackBlockCallback.EVENT.invoker().interact(player, world, InteractionHand.MAIN_HAND, pos, direction);
    
        if (result != InteractionResult.PASS) {
            // The client might have broken the block on its side, so make sure to let it know.
            player.connection.send(new ClientboundBlockUpdatePacket(world, pos));
        
            if (world.getBlockState(pos).hasBlockEntity()) {
                BlockEntity blockEntity = world.getBlockEntity(pos);
            
                if (blockEntity != null) {
                    Packet<ClientGamePacketListener> updatePacket = blockEntity.getUpdatePacket();
                
                    if (updatePacket != null) {
                        player.connection.send(updatePacket);
                    }
                }
            }
        
            return true;
        }
        return false;
    }
}
