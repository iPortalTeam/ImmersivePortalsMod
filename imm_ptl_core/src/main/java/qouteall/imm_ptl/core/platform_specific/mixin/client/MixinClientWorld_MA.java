package qouteall.imm_ptl.core.platform_specific.mixin.client;

import qouteall.imm_ptl.core.platform_specific.IEClientWorld_MA;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld_MA implements IEClientWorld_MA {

}
