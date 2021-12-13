package qouteall.imm_ptl.peripheral.mixin.common.altius_world;

import net.minecraft.world.SpawnHelper;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SpawnHelper.class)
public class MixinSpawnHelper {
    
    // TODO recover
    //avoid spawning on top of nether in altius world
    //normally mob cannot spawn on bedrock but altius replaces it with obsidian
//    @Redirect(
//        method = "getEntitySpawnPos",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/world/WorldView;getTopY(Lnet/minecraft/world/Heightmap$Type;II)I"
//        )
//    )
//    private static int redirectGetTopY(
//        WorldView worldView,
//        Heightmap.Type heightmap,
//        int x,
//        int z
//    ) {
//        int topY = worldView.getTopY(heightmap, x, z);
//        int dimHeight = worldView.getTopY();
//        if (AltiusGameRule.getIsDimensionStack()) {
//            if (worldView.getRegistryKey() == World.NETHER) {
//                return Math.min(height, dimHeight - 3);
//            }
//        }
//        return height;
//    }
}
