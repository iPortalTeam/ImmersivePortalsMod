package qouteall.q_misc_util.mixin.dimension;

import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PrimaryLevelData.class)
public class MixinLevelProperties_D {
//    @Shadow
//    @Final
//    private WorldGenSettings worldGenSettings;
//
//    @Shadow
//    @Final
//    @Mutable
//    private Lifecycle worldGenSettingsLifecycle;
//
//    @Inject(
//        method = "Lnet/minecraft/world/level/storage/PrimaryLevelData;setTagData(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/nbt/CompoundTag;)V",
//        at = @At("HEAD")
//    )
//    private void onUpdateProperties(
//        RegistryAccess dynamicRegistryManager, CompoundTag compoundTag,
//        CompoundTag compoundTag2, CallbackInfo ci
//    ) {
//        ((IEGeneratorOptions) (Object) worldGenSettings).setDimOptionRegistry(
//            DimensionMisc.getAdditionalDimensionsRemoved(
//                ((MappedRegistry) worldGenSettings.dimensions().dimensions())
//            )
//        );
//    }
}
