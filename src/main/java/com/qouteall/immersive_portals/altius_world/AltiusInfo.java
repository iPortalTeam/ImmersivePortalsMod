package com.qouteall.immersive_portals.altius_world;

import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ducks.IELevelProperties;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import com.qouteall.immersive_portals.portal.global_portals.VerticalConnectingPortal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AltiusInfo {
    //store identifier because forge
    private List<Identifier> dimsFromTopToDown;
    
    public AltiusInfo(List<DimensionType> dimsFromTopToDown) {
        this.dimsFromTopToDown = dimsFromTopToDown.stream().map(
            dimensionType -> DimensionType.getId(dimensionType)
        ).collect(Collectors.toList());
    }
    
    public static AltiusInfo getDummy() {
        return new AltiusInfo(new ArrayList<>());
    }
    
    public static AltiusInfo fromTag(CompoundTag tag) {
        ListTag listTag = tag.getList("dimensions", 8);
        List<DimensionType> dimensionTypeList = new ArrayList<>();
        listTag.forEach(t -> {
            StringTag t1 = (StringTag) t;
            String dimId = t1.asString();
            DimensionType dimensionType = Registry.DIMENSION_TYPE.get(new Identifier(dimId));
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
        return ((IELevelProperties) McHelper.getServer().getWorld(DimensionType.OVERWORLD)
            .getLevelProperties()).getAltiusInfo();
    }
    
    public static boolean isAltius() {
        return getInfoFromServer() != null;
    }
    
    public void createPortals() {
        GlobalPortalStorage gps =
            GlobalPortalStorage.get(McHelper.getServer().getWorld(DimensionType.OVERWORLD));
        if (gps.data == null || gps.data.isEmpty()) {
            Helper.wrapAdjacentAndMap(
                dimsFromTopToDown.stream(),
                (top, down) -> {
                    VerticalConnectingPortal.connectMutually(
                        DimensionType.byId(top), DimensionType.byId(down),
                        0, getHeight(DimensionType.byId(down))
                    );
                    return null;
                }
            ).forEach(k -> {
            });
            Helper.log("Initialized Portals For Altius");
        }
    }
    
    public static int getHeight(DimensionType dimensionType) {
        if (dimensionType == DimensionType.THE_NETHER) {
            if (O_O.isNetherHigherModPresent()) {
                return 256;
            }
            return 128;
        }
        return 256;
    }
}
