package com.qouteall.imm_ptl_peripheral.altius_world;

import com.qouteall.imm_ptl_peripheral.ducks.IELevelProperties;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.dimension_sync.DimId;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import com.qouteall.immersive_portals.portal.global_portals.VerticalConnectingPortal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// Mostly deprecated
public class AltiusInfo {
    
    private List<Identifier> dimsFromTopToDown;
    
    public AltiusInfo(List<RegistryKey<World>> dimsFromTopToDown) {
        this.dimsFromTopToDown = dimsFromTopToDown.stream().map(
            dimensionType -> dimensionType.getValue()
        ).collect(Collectors.toList());
    }
    
    public static AltiusInfo getDummy() {
        return new AltiusInfo(new ArrayList<>());
    }
    
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
        if (dimsFromTopToDown.isEmpty()) {
            Helper.err("Dimension List is empty?");
            return;
        }
        RegistryKey<World> topDimension = DimId.idToKey(dimsFromTopToDown.get(0));
        if (topDimension == null) {
            Helper.err("Invalid Dimension " + dimsFromTopToDown.get(0));
            return;
        }
        ServerWorld topWorld = McHelper.getServer().getWorld(topDimension);
        if (topWorld == null) {
            Helper.err("Missing Dimension " + topDimension.getValue());
            return;
        }
        GlobalPortalStorage gps = GlobalPortalStorage.get(topWorld);
        if (gps.data == null || gps.data.isEmpty()) {
            Helper.wrapAdjacentAndMap(
                dimsFromTopToDown.stream(),
                (top, down) -> {
                    VerticalConnectingPortal.connectMutually(
                        DimId.idToKey(top), DimId.idToKey(down),
                        0, VerticalConnectingPortal.getHeight(DimId.idToKey(down))
                    );
                    return null;
                }
            ).forEach(k -> {
            });
            Helper.log("Initialized Portals For Altius");
        }
    }
    
}
