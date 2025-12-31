package qouteall.imm_ptl.core.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import qouteall.imm_ptl.core.ducks.IEAbstractClientPlayer;
import qouteall.imm_ptl.core.ducks.IEEntityLevelSetter;

@Mixin(AbstractClientPlayer.class)
public abstract class MixinAbstractClientPlayer implements IEAbstractClientPlayer {
    @Override
    public void ip_setClientLevel(ClientLevel clientWorld) {
        ((IEEntityLevelSetter) (Object) this).ip_setLevel(clientWorld);
    }
}
