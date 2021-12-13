package qouteall.imm_ptl.peripheral.altius_world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.imm_ptl.core.portal.global_portals.VerticalConnectingPortal;
import qouteall.q_misc_util.Helper;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AltiusInfo {
    
    public final boolean loop;
    public final List<AltiusEntry> entries;
    
    public AltiusInfo(List<AltiusEntry> entries, boolean loop) {
        this.entries = entries;
        this.loop = loop;
    }
    
    public static void initializeFuseViewProperty(Portal portal) {
        if (portal.getNormal().y < 0) {
            portal.fuseView = true;
        }
    }
    
    public static void createConnectionBetween(
        AltiusEntry a, AltiusEntry b
    ) {
        ServerWorld fromWorld = McHelper.getServerWorld(a.dimension);
        ServerWorld toWorld = McHelper.getServerWorld(b.dimension);
        
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
        
        PortalAPI.addGlobalPortal(fromWorld, connectingPortal);
        PortalAPI.addGlobalPortal(toWorld, reverse);
    }
    
    public void apply() {
        
        if (entries.isEmpty()) {
            McHelper.sendMessageToFirstLoggedPlayer(new LiteralText(
                "Error: No dimension for dimension stack"
            ));
            return;
        }
        
        if (!GlobalPortalStorage.getGlobalPortals(McHelper.getServerWorld(entries.get(0).dimension)).isEmpty()) {
            Helper.err("There are already global portals when initializing dimension stack");
            return;
        }
        
        Helper.wrapAdjacentAndMap(
            entries.stream(),
            (before, after) -> {
                createConnectionBetween(before, after);
                return null;
            }
        ).forEach(k -> {
        });
        
        if (loop) {
            createConnectionBetween(entries.get(entries.size() - 1), entries.get(0));
        }
        
        Map<RegistryKey<World>, BlockState> bedrockReplacementMap = new HashMap<>();
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
            new TranslatableText("imm_ptl.dim_stack_initialized")
        );
    }
    
    public NbtCompound toNbt() {
        NbtCompound nbtCompound = new NbtCompound();
        nbtCompound.putBoolean("loop", loop);
        NbtList list = new NbtList();
        for (AltiusEntry entry : entries) {
            list.add(entry.toNbt());
        }
        nbtCompound.put("entries", list);
        return nbtCompound;
    }
    
    public static AltiusInfo fromNbt(NbtCompound compound) {
        boolean loop = compound.getBoolean("loop");
        NbtList list = compound.getList("entries", new NbtCompound().getType());
        List<AltiusEntry> entries = list.stream()
            .map(n -> AltiusEntry.fromNbt(((NbtCompound) n))).collect(Collectors.toList());
        return new AltiusInfo(entries, loop);
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
            Optional<Block> block = Registry.BLOCK.getOrEmpty(new Identifier(str));
            return block.map(Block::getDefaultState).orElse(null);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
