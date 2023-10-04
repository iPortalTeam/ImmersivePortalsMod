package qouteall.imm_ptl.core.mixin.common.other_sync;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerConfigurationPacketListenerImpl.class)
public interface IEServerConfigurationPacketListenerImpl {
    @Accessor("gameProfile")
    GameProfile ip_getGameProfile();
}
