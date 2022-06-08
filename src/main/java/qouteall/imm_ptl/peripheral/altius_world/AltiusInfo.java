package qouteall.imm_ptl.peripheral.altius_world;

import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
import java.util.stream.Collectors;

public class AltiusInfo {
    
    public final boolean loop;
    public final boolean gravityChange;
    public final List<AltiusEntry> entries;
    
    public AltiusInfo(List<AltiusEntry> entries, boolean loop, boolean gravityChange) {
        this.entries = entries;
        this.loop = loop;
        this.gravityChange = gravityChange;
    }
    
    public static void initializeFuseViewProperty(Portal portal) {
        if (portal.getNormal().y < 0) {
            portal.fuseView = true;
        }
    }
    
    public static void createConnectionBetween(
        AltiusEntry a, AltiusEntry b, boolean gravityChange
    ) {
        ServerLevel fromWorld = McHelper.getServerWorld(a.dimension);
        ServerLevel toWorld = McHelper.getServerWorld(b.dimension);
        
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
        
        PortalAPI.addGlobalPortal(fromWorld, connectingPortal);
        PortalAPI.addGlobalPortal(toWorld, reverse);
    }
    
    public void apply() {
        
        if (entries.isEmpty()) {
            McHelper.sendMessageToFirstLoggedPlayer(Component.literal(
                "Error: No dimension for dimension stack"
            ));
            return;
        }
        
        MinecraftServer server = MiscHelper.getServer();
        for (AltiusEntry entry : entries) {
            if (server.getLevel(entry.dimension) == null) {
                McHelper.sendMessageToFirstLoggedPlayer(Component.literal(
                    "Failed to apply dimension stack. Missing dimension " + entry.dimension.location()
                ));
                return;
            }
        }
        
        if (!GlobalPortalStorage.getGlobalPortals(McHelper.getServerWorld(entries.get(0).dimension)).isEmpty()) {
            Helper.err("There are already global portals when initializing dimension stack");
            return;
        }
        
        Helper.wrapAdjacentAndMap(
            entries.stream(),
            (before, after) -> {
                createConnectionBetween(before, after, gravityChange);
                return null;
            }
        ).forEach(k -> {
        });
        
        if (loop) {
            createConnectionBetween(entries.get(entries.size() - 1), entries.get(0), gravityChange);
        }
        
        Map<ResourceKey<Level>, BlockState> bedrockReplacementMap = new HashMap<>();
        for (AltiusEntry entry : entries) {
            String bedrockReplacementStr = entry.bedrockReplacementStr;
            
            BlockState bedrockReplacement = parseBlockString(bedrockReplacementStr);
            
            if (bedrockReplacement != null) {
                bedrockReplacementMap.put(entry.dimension, bedrockReplacement);
            }
            GlobalPortalStorage gps = GlobalPortalStorage.get(McHelper.getServerWorld(entry.dimension));
            gps.bedrockReplacement = bedrockReplacement;
            gps.onDataChanged();
        }
        AltiusManagement.bedrockReplacementMap = bedrockReplacementMap;
        
        McHelper.sendMessageToFirstLoggedPlayer(
            Component.translatable("imm_ptl.dim_stack_initialized")
        );
    }
    
    public CompoundTag toNbt() {
        CompoundTag nbtCompound = new CompoundTag();
        nbtCompound.putBoolean("loop", loop);
        nbtCompound.putBoolean("gravityChange", gravityChange);
        ListTag list = new ListTag();
        for (AltiusEntry entry : entries) {
            list.add(entry.toNbt());
        }
        nbtCompound.put("entries", list);
        return nbtCompound;
    }
    
    public static AltiusInfo fromNbt(CompoundTag compound) {
        boolean loop = compound.getBoolean("loop");
        boolean gravityChange = compound.getBoolean("gravityChange");
        ListTag list = compound.getList("entries", new CompoundTag().getId());
        List<AltiusEntry> entries = list.stream()
            .map(n -> AltiusEntry.fromNbt(((CompoundTag) n))).collect(Collectors.toList());
        return new AltiusInfo(entries, loop, gravityChange);
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
            Optional<Block> block = Registry.BLOCK.getOptional(new ResourceLocation(str));
            return block.map(Block::defaultBlockState).orElse(null);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
