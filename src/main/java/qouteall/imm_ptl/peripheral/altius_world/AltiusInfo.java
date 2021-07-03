package qouteall.imm_ptl.peripheral.altius_world;

import qouteall.q_misc_util.Helper;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.imm_ptl.core.portal.global_portals.VerticalConnectingPortal;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.chunk.Chunk;

import java.util.List;

public class AltiusInfo {
    
    public final boolean loop;
    public final List<AltiusEntry> entries;
    
    public AltiusInfo(List<AltiusEntry> entries, boolean loop) {
        this.entries = entries;
        this.loop = loop;
    }
    
    public static void initializeFuseViewProperty(Portal portal) {
        if (portal.world.getDimension().hasSkyLight()) {
            if (portal.getNormal().y < 0) {
                portal.fuseView = true;
            }
        }
    }
    
    public static void createConnectionBetween(
        AltiusEntry a, AltiusEntry b
    ) {
        ServerWorld fromWorld = McHelper.getServerWorld(a.dimension);
        
        ServerWorld toWorld = McHelper.getServerWorld(b.dimension);
        
        boolean xorFlipped = a.flipped ^ b.flipped;
        
        VerticalConnectingPortal connectingPortal = VerticalConnectingPortal.createConnectingPortal(
            fromWorld,
            a.flipped ? VerticalConnectingPortal.ConnectorType.ceil :
                VerticalConnectingPortal.ConnectorType.floor,
            toWorld,
            b.scale / a.scale,
            xorFlipped,
            b.horizontalRotation - a.horizontalRotation
        );
        
        VerticalConnectingPortal reverse = PortalAPI.createReversePortal(connectingPortal);
        
        initializeFuseViewProperty(connectingPortal);
        initializeFuseViewProperty(reverse);
        
        PortalAPI.addGlobalPortal(fromWorld, connectingPortal);
        PortalAPI.addGlobalPortal(toWorld, reverse);
    }
    
    public void createPortals() {
        
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
        
        McHelper.sendMessageToFirstLoggedPlayer(
            new TranslatableText("imm_ptl.dim_stack_initialized")
        );
    }
    
    public static void replaceBedrock(ServerWorld world, Chunk chunk) {
        if (AltiusGameRule.getIsDimensionStack()) {
            BlockPos.Mutable mutable = new BlockPos.Mutable();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = chunk.getBottomY(); y < chunk.getTopY(); y++) {
                        mutable.set(x, y, z);
                        BlockState blockState = chunk.getBlockState(mutable);
                        if (blockState.getBlock() == Blocks.BEDROCK) {
                            chunk.setBlockState(
                                mutable,
                                Blocks.OBSIDIAN.getDefaultState(),
                                false
                            );
                        }
                    }
                }
            }
        }
    }
    
}
