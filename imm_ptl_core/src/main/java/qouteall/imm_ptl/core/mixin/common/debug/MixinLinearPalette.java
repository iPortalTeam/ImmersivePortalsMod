package qouteall.imm_ptl.core.mixin.common.debug;

import net.minecraft.core.IdMap;
import net.minecraft.world.level.chunk.LinearPalette;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LinearPalette.class)
public class MixinLinearPalette<T> {
    // reveal the issue early to help debugging
    @Redirect(
        method = "write",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/IdMap;getId(Ljava/lang/Object;)I"
        )
    )
    private int onGetId(IdMap<T> idMap, T object) {
        int id = idMap.getId(object);
        if (id == -1) {
            throw new RuntimeException(
                "(Note: ImmPtl is just doing checking to help debugging) Cannot find id for %s. Something is wrong with registries.".formatted(object)
            );
        }
        return id;
    }
}
