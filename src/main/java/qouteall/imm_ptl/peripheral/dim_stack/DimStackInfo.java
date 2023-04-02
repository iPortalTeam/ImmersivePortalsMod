package qouteall.imm_ptl.peripheral.dim_stack;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.imm_ptl.core.portal.global_portals.VerticalConnectingPortal;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// will be serialized by GSON
public class DimStackInfo {
    
    public boolean loop;
    public boolean gravityTransform;
    public List<DimStackEntry> entries;
    
    public DimStackInfo() {}
    
    public DimStackInfo(List<DimStackEntry> entries, boolean loop, boolean gravityTransform) {
        this.entries = entries;
        this.loop = loop;
        this.gravityTransform = gravityTransform;
    }
    
    public static void initializeFuseViewProperty(Portal portal) {
        if (portal.getNormal().y < 0) {
            portal.fuseView = true;
        }
    }
    
    public static void createConnectionBetween(
        DimStackEntry a, DimStackEntry b, boolean gravityChange
    ) {
        ServerLevel fromWorld = McHelper.getServerWorld(a.getDimension());
        ServerLevel toWorld = McHelper.getServerWorld(b.getDimension());
        
        boolean xorFlipped = a.flipped ^ b.flipped;
        
        int fromWorldMinY = McHelper.getMinY(fromWorld);
        if (a.bottomY != null) {
            fromWorldMinY = a.bottomY;
        }
        int fromWorldMaxY = McHelper.getMaxContentYExclusive(fromWorld);
        if (a.topY != null) {
            fromWorldMaxY = a.topY;
        }
        int toWorldMinY = McHelper.getMinY(toWorld);
        if (b.bottomY != null) {
            toWorldMinY = b.bottomY;
        }
        int toWorldMaxY = McHelper.getMaxContentYExclusive(toWorld);
        if (b.topY != null) {
            toWorldMaxY = b.topY;
        }
        
        VerticalConnectingPortal connectingPortal = VerticalConnectingPortal.createConnectingPortal(
            fromWorld,
            a.flipped ? VerticalConnectingPortal.ConnectorType.ceil :
                VerticalConnectingPortal.ConnectorType.floor,
            toWorld,
            b.scale / a.scale,
            xorFlipped,
            b.horizontalRotation - a.horizontalRotation,
            fromWorldMinY, fromWorldMaxY,
            toWorldMinY, toWorldMaxY
        );
        
        VerticalConnectingPortal reverse = PortalAPI.createReversePortal(connectingPortal);
        
        initializeFuseViewProperty(connectingPortal);
        initializeFuseViewProperty(reverse);
        
        if (gravityChange) {
            connectingPortal.setTeleportChangesGravity(true);
            reverse.setTeleportChangesGravity(true);
        }
        
        if (a.connectsNext) {
            PortalAPI.addGlobalPortal(fromWorld, connectingPortal);
        }
        
        if (b.connectsPrevious) {
            PortalAPI.addGlobalPortal(toWorld, reverse);
        }
    }
    
    public void apply() {
        
        if (entries.isEmpty()) {
            McHelper.sendMessageToFirstLoggedPlayer(Component.literal(
                "Error: No dimension for dimension stack"
            ));
            return;
        }
        
        MinecraftServer server = MiscHelper.getServer();
        for (DimStackEntry entry : entries) {
            if (server.getLevel(entry.getDimension()) == null) {
                McHelper.sendMessageToFirstLoggedPlayer(Component.literal(
                    "Failed to apply dimension stack. Missing dimension " + entry.dimensionIdStr
                ).withStyle(ChatFormatting.RED));
                return;
            }
        }
        
        if (!GlobalPortalStorage.getGlobalPortals(McHelper.getServerWorld(entries.get(0).getDimension())).isEmpty()) {
            Helper.err("There are already global portals when initializing dimension stack");
            McHelper.sendMessageToFirstLoggedPlayer(Component.literal(
                "Failed to apply dimension stack because there are already global portals when initializing dimension stack"
            ).withStyle(ChatFormatting.RED));
            return;
        }
        
        Helper.wrapAdjacentAndMap(
            entries.stream(),
            (before, after) -> {
                createConnectionBetween(before, after, gravityTransform);
                return null;
            }
        ).forEach(k -> {
        });
        
        if (loop) {
            createConnectionBetween(entries.get(entries.size() - 1), entries.get(0), gravityTransform);
        }
        
        Map<ResourceKey<Level>, BlockState> bedrockReplacementMap = new HashMap<>();
        for (DimStackEntry entry : entries) {
            String bedrockReplacementStr = entry.bedrockReplacementStr;
            
            BlockState bedrockReplacement = parseBlockString(bedrockReplacementStr);
            
            if (bedrockReplacement != null) {
                bedrockReplacementMap.put(entry.getDimension(), bedrockReplacement);
            }
            GlobalPortalStorage gps = GlobalPortalStorage.get(McHelper.getServerWorld(entry.getDimension()));
            gps.bedrockReplacement = bedrockReplacement;
            gps.onDataChanged();
        }
        DimStackManagement.bedrockReplacementMap = bedrockReplacementMap;
        
        McHelper.sendMessageToFirstLoggedPlayer(
            Component.translatable("imm_ptl.dim_stack_initialized")
        );
    }
    
    @Nullable
    public static BlockState parseBlockString(@Nullable String str) {
        if (str == null) {
            return null;
        }
        if (str.isEmpty()) {
            return null;
        }
        
        try {
            Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(new ResourceLocation(str));
            return block.map(Block::defaultBlockState).orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
