package com.qouteall.imm_ptl_peripheral.altius_world;

import com.qouteall.imm_ptl_peripheral.ducks.IELevelProperties;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.dimension_sync.DimId;
import com.qouteall.immersive_portals.portal.global_portals.VerticalConnectingPortal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AltiusInfo {
    
    private List<Identifier> dimsFromTopToDown;
    
    public final boolean loop;
    public final boolean respectSpaceRatio;
    
    public AltiusInfo(
        List<RegistryKey<World>> dimsFromTopToDown
    ) {
        this(dimsFromTopToDown, false, false);
    }
    
    public AltiusInfo(
        List<RegistryKey<World>> dimsFromTopToDown, boolean loop, boolean respectSpaceRatio
    ) {
        this.dimsFromTopToDown = dimsFromTopToDown.stream().map(
            dimensionType -> dimensionType.getValue()
        ).collect(Collectors.toList());
        this.loop = loop;
        this.respectSpaceRatio = respectSpaceRatio;
    }
    
    // deprecated. used for upgrading old dimension stack
    public static AltiusInfo fromTag(CompoundTag tag) {
        ListTag listTag = tag.getList("dimensions", 8);
        List<RegistryKey<World>> dimensionTypeList = new ArrayList<>();
        listTag.forEach(t -> {
            StringTag t1 = (StringTag) t;
            String dimId = t1.asString();
            RegistryKey<World> dimensionType = DimId.idToKey(dimId);
            if (dimensionType != null) {
                dimensionTypeList.add(dimensionType);
            }
            else {
                Helper.log("Unknown Dimension Id " + dimId);
            }
        });
        return new AltiusInfo(dimensionTypeList);
    }
    
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        ListTag listTag = new ListTag();
        dimsFromTopToDown.forEach(dimensionType -> {
            listTag.add(listTag.size(), StringTag.of(
                dimensionType.toString()
            ));
        });
        tag.put("dimensions", listTag);
        return tag;
    }
    
    public static AltiusInfo getInfoFromServer() {
        SaveProperties saveProperties = McHelper.getServer().getSaveProperties();
        
        return ((IELevelProperties) saveProperties).getAltiusInfo();
    }
    
    public static void removeAltius() {
        SaveProperties saveProperties = McHelper.getServer().getSaveProperties();
        
        ((IELevelProperties) saveProperties).setAltiusInfo(null);
    }
    
    // use AltiusGameRule
    @Deprecated
    public static boolean isAltius() {
        return getInfoFromServer() != null;
    }
    
    public void createPortals() {
        List<ServerWorld> worldsFromTopToDown = dimsFromTopToDown.stream().flatMap(identifier -> {
            RegistryKey<World> dimension = DimId.idToKey(identifier);
            ServerWorld world = McHelper.getServer().getWorld(dimension);
            
            if (world != null) {
                return Stream.of(world);
            }
            else {
                McHelper.sendMessageToFirstLoggedPlayer(new LiteralText(
                    "Error: Dimension stack has invalid dimension " + dimension.getValue()
                ));
                
                return Stream.empty();
            }
        }).collect(Collectors.toList());
        
        if (worldsFromTopToDown.isEmpty()) {
            McHelper.sendMessageToFirstLoggedPlayer(new LiteralText(
                "Error: No dimension for dimension stack"
            ));
            return;
        }
        
        if (!McHelper.getGlobalPortals(worldsFromTopToDown.get(0)).isEmpty()) {
            Helper.err("There are already global portals when initializing dimension stack");
            return;
        }
        
        Helper.wrapAdjacentAndMap(
            worldsFromTopToDown.stream(),
            (top, down) -> {
                VerticalConnectingPortal.connectMutually(
                    top.getRegistryKey(), down.getRegistryKey(),
                    0, VerticalConnectingPortal.getHeight(down.getRegistryKey()),
                    respectSpaceRatio
                );
                return null;
            }
        ).forEach(k -> {
        });
        
        McHelper.sendMessageToFirstLoggedPlayer(
            new TranslatableText("imm_ptl.dim_stack_initialized")
        );
    }
    
}
