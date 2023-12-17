package qouteall.q_misc_util.dimension;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import qouteall.dimlib.api.DimensionAPI;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.q_misc_util.MiscNetworking;
import qouteall.q_misc_util.ducks.IEMinecraftServer_Misc;

import java.util.HashSet;

public class DimensionIntId {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static DimIntIdMap clientRecord;
    
    public static void init() {
        DimensionAPI.SERVER_DIMENSION_DYNAMIC_UPDATE_EVENT.register(
            (server, dimensions) -> onServerDimensionChanged(server)
        );
        
        IPCGlobal.CLIENT_EXIT_EVENT.register(() -> {
            LOGGER.info("Reset client dim id record");
            clientRecord = null;
        });
    }
    
    @Environment(EnvType.CLIENT)
    public static @NotNull DimIntIdMap getClientMap() {
        Validate.notNull(clientRecord, "Client dim id record is not yet synced");
        return clientRecord;
    }
    
    public static @NotNull DimIntIdMap getServerMap(MinecraftServer server) {
        DimIntIdMap rec = ((IEMinecraftServer_Misc) server).ip_getDimIdRec();
        Validate.notNull(rec, "Server dim id record is not yet initialized");
        return rec;
    }
    
    public static void onServerStarted(MinecraftServer server) {
        DimIntIdMap rec = new DimIntIdMap();
        
        fillInVanillaDimIds(rec);
        
        for (ServerLevel world : server.getAllLevels()) {
            ResourceKey<Level> dimId = world.dimension();
            if (!rec.containsDimId(dimId)) {
                rec.add(dimId, rec.getNextIntegerId());
            }
        }
        
        ((IEMinecraftServer_Misc) server).ip_setDimIdRec(rec);
        LOGGER.info("Server dimension integer id mapping:\n{}", rec);
    }
    
    private static void fillInVanillaDimIds(DimIntIdMap rec) {
        if (!rec.containsDimId(Level.OVERWORLD)) {
            rec.add(Level.OVERWORLD, 0);
        }
        if (!rec.containsDimId(Level.NETHER)) {
            rec.add(Level.NETHER, -1);
        }
        if (!rec.containsDimId(Level.END)) {
            rec.add(Level.END, 1);
        }
    }
    
    public static void onServerDimensionChanged(MinecraftServer server) {
        DimIntIdMap map = getServerMap(server);
        
        for (ResourceKey<Level> levelKey : server.levelKeys()) {
            if (!map.containsDimId(levelKey)) {
                map.add(levelKey, map.getNextIntegerId());
            }
        }
        
        HashSet<ResourceKey<Level>> serverDimIds = new HashSet<>(server.levelKeys());
        
        // re-add vanilla dimensions. avoid them from being removed from mapping
        serverDimIds.add(Level.OVERWORLD);
        serverDimIds.add(Level.NETHER);
        serverDimIds.add(Level.END);
        
        map.removeUnused(serverDimIds);
        
        LOGGER.info("Current dimension integer id mapping:\n{}", map);
        
        var packet = MiscNetworking.DimIdSyncPacket.createPacket(server);
        
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(packet);
        }
    }
}
