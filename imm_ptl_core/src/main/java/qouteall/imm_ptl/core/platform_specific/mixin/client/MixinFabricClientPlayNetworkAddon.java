package qouteall.imm_ptl.core.platform_specific.mixin.client;

import net.fabricmc.fabric.impl.networking.client.ClientPlayNetworkAddon;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPlayNetworkAddon.class)
public class MixinFabricClientPlayNetworkAddon {

}
