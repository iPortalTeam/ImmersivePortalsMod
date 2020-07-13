package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.dimension_sync.DimId;
import com.qouteall.immersive_portals.portal.nether_portal.GeneralBreakablePortal;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalGeneration;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalMatcher;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CustomizablePortalGenerationOld {
    public static class Entry {
        public RegistryKey<World> fromDimension;
        public int fromSpaceRatio;
        public RegistryKey<World> toDimension;
        public int toSpaceRatio;
        public Block frameBlock;
        
        public Entry(
            RegistryKey<World> fromDimension,
            int fromSpaceRatio,
            RegistryKey<World> toDimension,
            int toSpaceRatio,
            Block frameBlock
        ) {
            this.fromDimension = fromDimension;
            this.fromSpaceRatio = fromSpaceRatio;
            this.toDimension = toDimension;
            this.toSpaceRatio = toSpaceRatio;
            this.frameBlock = frameBlock;
        }
        
        public Entry getReverse() {
            return new Entry(
                toDimension,
                toSpaceRatio,
                fromDimension,
                fromSpaceRatio,
                frameBlock
            );
        }
    }
    
    public static Entry readEntry(String str) {
        String[] components = Arrays.stream(str.split(","))
            .map(String::trim).toArray(String[]::new);
        if (components.length != 5) {
            Helper.err("Invalid Entry " + str);
            return null;
        }
    
        RegistryKey<World> fromDim = DimId.idToKey(new Identifier(components[0]));
        if (fromDim == null) {
            Helper.err("Invalid dimension type " + components[0]);
            return null;
        }
        int fromSpaceRatio = Integer.decode(components[1]);
        if (fromSpaceRatio == 0) {
            Helper.err("Invalid space ratio");
            return null;
        }
        RegistryKey<World> toDim = DimId.idToKey(new Identifier(components[2]));
        if (toDim == null) {
            Helper.err("Invalid dimension type " + components[2]);
        }
        int toSpaceRatio = Integer.decode(components[3]);
        if (toSpaceRatio == 0) {
            Helper.err("Invalid space ratio");
            return null;
        }
        Block frameBlock = Registry.BLOCK.get(new Identifier(components[4]));
        if (frameBlock == Blocks.AIR) {
            Helper.err("Invalid frame block");
            return null;
        }
        return new Entry(
            fromDim,
            fromSpaceRatio,
            toDim,
            toSpaceRatio,
            frameBlock
        );
    }
    
    public static String writeEntry(Entry entry) {
        return String.format(
            "%s,%s,%s,%s,%s",
            entry.fromDimension,
            entry.fromSpaceRatio,
            entry.toDimension,
            entry.toSpaceRatio
        );
    }
    
    private static List<String> configContent = new ArrayList<>();
    private static List<Entry> entryCache = null;
    
    public static void onConfigChanged(List<String> config) {
        configContent = config;
        entryCache = null;
    }
    
    private static void initCache() {
        if (entryCache == null) {
            entryCache = configContent.stream()
                .map(CustomizablePortalGenerationOld::readEntry)
                .filter(Objects::nonNull)
                .flatMap(entry -> Stream.of(
                    entry, entry.getReverse()
                ))
                .collect(Collectors.toList());
        }
    }
    
    public static boolean onFireLit(
        ServerWorld fromWorld,
        BlockPos firePos,
        Block frameBlock
    ) {
        initCache();
    
        RegistryKey<World> fromDim = fromWorld.getRegistryKey();
        Entry entry = entryCache.stream().filter(
            entry_ -> entry_.fromDimension == fromDim && entry_.frameBlock == frameBlock
        ).findFirst().orElse(null);
        if (entry == null) {
            return false;
        }
        
        ServerWorld toWorld = McHelper.getServer().getWorld(entry.toDimension);
        return NetherPortalGeneration.startGeneratingPortal(
            fromWorld,
            firePos,
            toWorld,
            Global.netherPortalFindingRadius,
            Global.netherPortalFindingRadius - 10,
            s -> true,
            (pos) -> Helper.divide(Helper.scale(pos, entry.toSpaceRatio), entry.fromSpaceRatio),
            //this side area
            NetherPortalMatcher::isAirOrFire,
            //this side frame
            blockState -> blockState.getBlock() == entry.frameBlock,
            //other side area
            BlockState::isAir,
            //other side frame
            (blockState) -> blockState.getBlock() == entry.frameBlock,
            (shape) -> NetherPortalGeneration.embodyNewFrame(
                toWorld, shape, entry.frameBlock.getDefaultState()
            ),
            info -> NetherPortalGeneration.generateBreakablePortalEntities(
                info, GeneralBreakablePortal.entityType
            )
        ) != null;
    }
}
