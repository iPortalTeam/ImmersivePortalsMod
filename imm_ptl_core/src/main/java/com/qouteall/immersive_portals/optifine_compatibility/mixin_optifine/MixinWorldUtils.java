package com.qouteall.immersive_portals.optifine_compatibility.mixin_optifine;

import com.qouteall.immersive_portals.render.context_management.RenderDimensionRedirect;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "net.optifine.util.WorldUtils", remap = false)
public class MixinWorldUtils {
    
    /**
     * @author qouteall
     * @reason avoid remapping issue
     */
    @Overwrite
    public static int getDimensionId(RegistryKey<World> dimension) {
        dimension = RenderDimensionRedirect.getRedirectedDimension(dimension);
        
        if (dimension == World.NETHER) {
            return -1;
        } else if (dimension == World.OVERWORLD) {
            return 0;
        } else {
            return dimension == World.END ? 1 : -2;
        }
    }

//    @ModifyVariable(
//        method = "Lnet/optifine/util/WorldUtils;getDimensionId(Lnet/minecraft/util/registry/RegistryKey;)I",
//        at = @At("HEAD"),
//        argsOnly = true,
//        require = 0
//    )
//    private static RegistryKey<World> modifyDimension(RegistryKey<World> dim) {
//        return RenderDimensionRedirect.getRedirectedDimension(dim);
//    }
//
//    //another one for real environment
//    @ModifyVariable(
//        method = "Lnet/optifine/util/WorldUtils;getDimensionId(Lnet/minecraft/class_5321;)I",
//        at = @At("HEAD"),
//        argsOnly = true,
//        require = 0
//    )
//    private static RegistryKey<World> modifyDimension1(RegistryKey<World> dim) {
//        return RenderDimensionRedirect.getRedirectedDimension(dim);
//    }
}
