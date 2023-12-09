package qouteall.imm_ptl.core.mixin.client.accessor;

import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientLevel.ClientLevelData.class)
public interface IEClientLevelData {
    @Accessor("isFlat")
    public boolean ip_getIsFlat();
}
