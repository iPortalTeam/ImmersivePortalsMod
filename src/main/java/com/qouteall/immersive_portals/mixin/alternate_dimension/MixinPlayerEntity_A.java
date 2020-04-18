package com.qouteall.immersive_portals.mixin.alternate_dimension;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ducks.IEPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class MixinPlayerEntity_A implements IEPlayerEntity {
    private DimensionType portal_spawnDimension;
    
    @Inject(method = "readCustomDataFromTag", at = @At("RETURN"))
    private void onReadCustomDataFromTag(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("portal_spawnDimension")) {
            int dimIntId = tag.getInt("portal_spawnDimension");
            DimensionType dimensionType = DimensionType.byRawId(dimIntId);
            if (dimensionType == null) {
                Helper.err("Invalid spawn dimension " + dimIntId);
            }
            else {
                portal_spawnDimension = dimensionType;
            }
        }
    }
    
    @Inject(method = "writeCustomDataToTag", at = @At("RETURN"))
    private void onWriteCustomDataToTag(CompoundTag tag, CallbackInfo ci) {
        if (portal_spawnDimension != null) {
            tag.putInt("portal_spawnDimension", portal_spawnDimension.getRawId());
        }
    }
    
    @Inject(method = "setPlayerSpawn", at = @At("RETURN"))
    private void onSetPlayerSpawn(BlockPos blockPos, boolean bl, boolean bl2, CallbackInfo ci) {
        portal_spawnDimension = ((Entity) (Object) this).dimension;
    }
    
    @Override
    public DimensionType portal_getSpawnDimension() {
        return portal_spawnDimension;
    }
}
