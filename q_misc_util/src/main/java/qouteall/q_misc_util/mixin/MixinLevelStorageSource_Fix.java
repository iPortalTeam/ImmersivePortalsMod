package qouteall.q_misc_util.mixin;

import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelStorageSource.class)
public class MixinLevelStorageSource_Fix {
    // moved to dimension data fix
//    @Shadow
//    @Final
//    private static Logger LOGGER;
//
//    @Redirect(
//        method = "readWorldGenSettings",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/util/datafix/DataFixTypes;updateToCurrentVersion(Lcom/mojang/datafixers/DataFixer;Lcom/mojang/serialization/Dynamic;I)Lcom/mojang/serialization/Dynamic;"
//        )
//    )
//    private static <T> Dynamic<T> redirectUpdateToCurrentVersion(
//        DataFixTypes instance, DataFixer dataFixer, Dynamic<T> dynamic, int version
//    ) {
//        CompoundTag value = (CompoundTag) dynamic.getValue();
//        DynamicOps<T> ops = dynamic.getOps();
//
//        LOGGER.debug("Fixing dimension data {}", value);
//
//        CompoundTag dimensions = value.getCompound("dimensions");
//
//        CompoundTag vanillaDimensions = new CompoundTag();
//        CompoundTag nonVanillaDimensions = new CompoundTag();
//
//        for (String dimensionId : dimensions.getAllKeys()) {
//            Tag data = dimensions.get(dimensionId);
//            if (dimensionId.startsWith("minecraft:")) {
//                vanillaDimensions.put(dimensionId, data);
//            }
//            else {
//                nonVanillaDimensions.put(dimensionId, data);
//            }
//        }
//
//        CompoundTag newValue = value.copy();
//        newValue.put("dimensions", vanillaDimensions);
//
//        Dynamic<T> dynamicOfVanillaDimensions = (Dynamic<T>) new Dynamic<>(ops, (T) newValue);
//
//        Dynamic<T> updated = instance.updateToCurrentVersion(dataFixer, dynamicOfVanillaDimensions, version);
//
//        LOGGER.debug("Updated {}", updated.getValue());
//
//        CompoundTag updatedTag = (CompoundTag) updated.getValue();
//        CompoundTag updatedDimensions = updatedTag.getCompound("dimensions");
//
//        for (String nonVanillaDim : nonVanillaDimensions.getAllKeys()) {
//            Tag data = nonVanillaDimensions.get(nonVanillaDim);
//            updatedDimensions.put(nonVanillaDim, data);
//        }
//
//        LOGGER.debug("Fixed dimension data {}", updatedTag);
//
//        return updated;
//    }
}
