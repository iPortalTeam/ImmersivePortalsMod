package qouteall.imm_ptl.core.portal.nether_portal;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import qouteall.imm_ptl.core.portal.PortalPlaceholderBlock;

public class GeneralBreakablePortal extends BreakablePortalEntity {
    
    public static final Identifier ID = Identifier.fromNamespaceAndPath("immersive_portals", "general_breakable_portal");
    public static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, ID);
    public static final EntityType<GeneralBreakablePortal> ENTITY_TYPE =
        createPortalEntityType(KEY, GeneralBreakablePortal::new);
    
    public GeneralBreakablePortal(EntityType<?> entityType, Level world) {
        super(entityType, world);
    }
    
    @Override
    protected boolean isPortalIntactOnThisSide() {
        boolean areaIntact = blockPortalShape.area.stream()
            .allMatch(blockPos ->
                level().getBlockState(blockPos).getBlock() == PortalPlaceholderBlock.instance
            );
        boolean frameIntact = blockPortalShape.frameAreaWithoutCorner.stream()
            .allMatch(blockPos -> !level().isEmptyBlock(blockPos));
        return areaIntact && frameIntact;
    }
    
    @Override
    protected void addSoundAndParticle() {
    
    }
}
