package qouteall.imm_ptl.core.mixin.client.accessor;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ClientLevel.class)
public interface IEClientLevel_Accessor {
    
    @Accessor("mapData")
    Map<String, MapItemSavedData> ip_getMapData();
    
    @Mutable
    @Accessor("mapData")
    void ip_setMapData(Map<String, MapItemSavedData> mapData);
}
