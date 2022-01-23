package qouteall.imm_ptl.core.portal.nether_portal;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import qouteall.imm_ptl.core.portal.PortalPlaceholderBlock;

public class GeneralBreakablePortal extends BreakablePortalEntity {
    
    public static EntityType<GeneralBreakablePortal> entityType;
    
    public GeneralBreakablePortal(
        EntityType<?> entityType_1,
        Level world_1
    ) {
        super(entityType_1, world_1);
    }
    
    @Override
    protected boolean isPortalIntactOnThisSide() {
        return blockPortalShape.area.stream()
            .allMatch(blockPos ->
                level.getBlockState(blockPos).getBlock() == PortalPlaceholderBlock.instance
            ) &&
            blockPortalShape.frameAreaWithoutCorner.stream()
                .allMatch(blockPos -> !level.isEmptyBlock(blockPos));
    }
    
    @Override
    protected void addSoundAndParticle() {
    
    }
}
