package qouteall.imm_ptl.core.portal.global_portals;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import qouteall.imm_ptl.core.portal.Portal;

// NOTE don't use `instanceof GlobalTrackedPortal`. Use `portal.getIsGlobal()` instead
public class GlobalTrackedPortal extends Portal {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("immersive_portals", "global_tracked_portal");
    public static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, ID);
    public static final EntityType<GlobalTrackedPortal> ENTITY_TYPE =
        createPortalEntityType(KEY, GlobalTrackedPortal::new);
    
    public GlobalTrackedPortal(
        EntityType<?> entityType,
        Level world
    ) {
        super(entityType, world);
    }
    
}
