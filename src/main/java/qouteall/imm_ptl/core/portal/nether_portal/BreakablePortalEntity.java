package qouteall.imm_ptl.core.portal.nether_portal;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.mc_utils.ServerTaskList;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalPlaceholderBlock;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.LimitedLogger;
import qouteall.q_misc_util.my_util.MyTaskList;

import java.util.List;
import java.util.UUID;

public abstract class BreakablePortalEntity extends Portal {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static record OverlayInfo(
        BlockState blockState,
        double opacity,
        double offset,
        @Nullable DQuaternion rotation
    ) {
    }
    
    public BlockPortalShape blockPortalShape;
    public UUID reversePortalId;
    public boolean unbreakable = false;
    private boolean isNotified = true;
    private boolean shouldBreakPortal = false;
    
    @Nullable
    protected OverlayInfo overlayInfo;
    
    public BreakablePortalEntity(
        EntityType<?> entityType_1,
        Level world_1
    ) {
        super(entityType_1, world_1);
    }
    
    
    @Override
    public boolean isPortalValid() {
        if (level().isClientSide) {
            return super.isPortalValid();
        }
        return super.isPortalValid() && blockPortalShape != null && reversePortalId != null;
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        if (compoundTag.contains("netherPortalShape")) {
            blockPortalShape = new BlockPortalShape(compoundTag.getCompound("netherPortalShape"));
        }
        
        reversePortalId = Helper.getUuid(compoundTag, "reversePortalId");
        if (reversePortalId == null) {
            Helper.err("missing reverse portal id " + compoundTag);
            reversePortalId = Util.NIL_UUID;
        }
        
        unbreakable = compoundTag.getBoolean("unbreakable");
        
        if (compoundTag.contains("overlayBlockState")) {
            BlockState overlayBlockState = NbtUtils.readBlockState(
                level().holderLookup(Registries.BLOCK),
                compoundTag.getCompound("overlayBlockState")
            );
            if (overlayBlockState.isAir()) {
                overlayInfo = null;
            }
            else {
                double overlayOpacity = compoundTag.getDouble("overlayOpacity");
                if (overlayOpacity == 0) {
                    overlayOpacity = 0.5;
                }
                double overlayOffset = compoundTag.getDouble("overlayOffset");
                DQuaternion rotation = Helper.getQuaternion(compoundTag, "overlayRotation");
                
                overlayInfo = new OverlayInfo(
                    overlayBlockState, overlayOpacity, overlayOffset, rotation
                );
            }
        }
        else {
            overlayInfo = null;
        }
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        if (blockPortalShape != null) {
            compoundTag.put("netherPortalShape", blockPortalShape.toTag());
        }
        Helper.putUuid(compoundTag, "reversePortalId", reversePortalId);
        compoundTag.putBoolean("unbreakable", unbreakable);
        
        if (overlayInfo != null) {
            compoundTag.put("overlayBlockState", NbtUtils.writeBlockState(overlayInfo.blockState));
            compoundTag.putDouble("overlayOpacity", overlayInfo.opacity);
            compoundTag.putDouble("overlayOffset", overlayInfo.offset);
            Helper.putQuaternion(compoundTag, "overlayRotation", overlayInfo.rotation);
        }
    }
    
    private void breakPortalOnThisSide() {
        blockPortalShape.area.forEach(
            blockPos -> {
                if (level().getBlockState(blockPos).getBlock() == PortalPlaceholderBlock.instance) {
                    level().setBlockAndUpdate(
                        blockPos, Blocks.AIR.defaultBlockState()
                    );
                }
            }
        );
        this.remove(RemovalReason.KILLED);
        
        Helper.log("Broke " + this);
    }
    
    public void notifyPlaceholderUpdate() {
        isNotified = true;
    }
    
    private BreakablePortalEntity getReversePortal() {
        
        ServerLevel world = getServer().getLevel(getDestDim());
        Entity entity = world.getEntity(reversePortalId);
        if (entity instanceof BreakablePortalEntity) {
            return (BreakablePortalEntity) entity;
        }
        else {
            return null;
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (level().isClientSide()) {
            addSoundAndParticle();
        }
        else {
            if (!unbreakable) {
                if (isNotified || level().getGameTime() % 233 == getId() % 233) {
                    isNotified = false;
                    checkPortalIntegrity();
                }
                if (shouldBreakPortal) {
                    breakPortalOnThisSide();
                }
            }
        }
        
    }
    
    private void checkPortalIntegrity() {
        Validate.isTrue(!level().isClientSide);
        
        if (!isPortalValid()) {
            remove(RemovalReason.KILLED);
            return;
        }
        
        if (!isPortalIntactOnThisSide()) {
            markShouldBreak();
        }
        else if (!isPortalPaired()) {
            Helper.err("Break portal because of abnormal pairing");
            markShouldBreak();
        }
    }
    
    
    protected abstract boolean isPortalIntactOnThisSide();
    
    @Environment(EnvType.CLIENT)
    protected abstract void addSoundAndParticle();
    
    private static final LimitedLogger limitedLogger = new LimitedLogger(20);
    
    public boolean isPortalPaired() {
        Validate.isTrue(!level().isClientSide());
        
        if (isOneWay()) {
            return true;
        }
        
        if (!isOtherSideChunkLoaded()) {
            return true;
        }
        
        List<BreakablePortalEntity> revs = findReversePortals(this);
        if (revs.size() == 1) {
            BreakablePortalEntity reversePortal = revs.get(0);
            if (reversePortal.getDestPos().distanceToSqr(getOriginPos()) > 1) {
                return false;
            }
            else {
                return true;
            }
        }
        else if (revs.size() > 1) {
            return false;
        }
        else {
//            limitedLogger.err("Missing Reverse Portal " + this);
            return true;
        }
    }
    
    public void markShouldBreak() {
        shouldBreakPortal = true;
        
        if (isOneWay()) {
            return;
        }
        
        BreakablePortalEntity reversePortal = getReversePortal();
        if (reversePortal != null) {
            reversePortal.shouldBreakPortal = true;
        }
        else {
            ServerTaskList.of(getServer()).addTask(MyTaskList.withRetryNumberLimit(
                30,
                () -> {
                    if (this.isRemoved()) {
                        return true;
                    }
                    
                    if (!this.isOtherSideChunkLoaded()) {
                        LOGGER.info("The other side chunk is not loaded. Delay finding reverse portal of {}.", this);
                        return false;
                    }
                    
                    BreakablePortalEntity reversePortal1 = getReversePortal();
                    if (reversePortal1 != null) {
                        reversePortal1.shouldBreakPortal = true;
                    }
                    
                    return true;
                },
                () -> {}
            ));
        }
    }
    
    
    public static <T extends Portal> List<T> findReversePortals(T portal) {
        List<T> revs = McHelper.findEntitiesByBox(
            (Class<T>) portal.getClass(),
            portal.getDestinationWorld(),
            new AABB(BlockPos.containing(portal.getDestPos())),
            10,
            e -> (e.getOriginPos().distanceToSqr(portal.getDestPos()) < 0.1) &&
                e.getContentDirection().dot(portal.getNormal()) > 0.6
        );
        return revs;
    }
    
    public boolean isOneWay() {
        return reversePortalId.equals(Util.NIL_UUID);
    }
    
    public void markOneWay() {
        reversePortalId = Util.NIL_UUID;
    }
    
    public OverlayInfo getActualOverlay() {
        return overlayInfo;
    }
}
