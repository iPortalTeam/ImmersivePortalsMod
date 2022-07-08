package qouteall.imm_ptl.peripheral.mixin.common.dim_stack;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelVersion;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.peripheral.dim_stack.DimStackGameRule;

@Mixin(PrimaryLevelData.class)
public class MixinLevelProperties {
    
   
    
    @Shadow
    @Final
    private WorldGenSettings worldGenSettings;
    
    // used for upgrading legacy dimension stack
    @Inject(
        method = "Lnet/minecraft/world/level/storage/PrimaryLevelData;parse(Lcom/mojang/serialization/Dynamic;Lcom/mojang/datafixers/DataFixer;ILnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/LevelSettings;Lnet/minecraft/world/level/storage/LevelVersion;Lnet/minecraft/world/level/levelgen/WorldGenSettings;Lcom/mojang/serialization/Lifecycle;)Lnet/minecraft/world/level/storage/PrimaryLevelData;",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void onReadDataFromTag(
        Dynamic<Tag> dynamic,
        DataFixer dataFixer,
        int i,
        CompoundTag playerTag,
        LevelSettings levelInfo,
        LevelVersion saveVersionInfo,
        WorldGenSettings generatorOptions,
        Lifecycle lifecycle,
        CallbackInfoReturnable<PrimaryLevelData> cir
    ) {
        PrimaryLevelData levelProperties = cir.getReturnValue();
        
        MixinLevelProperties this_ = (MixinLevelProperties) (Object) levelProperties;
        
        Tag altiusTag = dynamic.getElement("altius", null);
        if (altiusTag != null) {
            DimStackGameRule.upgradeOldDimensionStack();
        }
    }
    
   
}
