package qouteall.imm_ptl.peripheral.dim_stack;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.WorldOptions;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class DimensionStackAPI {
    public static interface DimensionCollectionCallback {
        Collection<ResourceKey<Level>> getExtraDimensionKeys(
            RegistryAccess.Frozen registryAccess,
            WorldOptions options
        );
    }
    
    public static final Event<DimensionCollectionCallback> DIMENSION_STACK_CANDIDATE_COLLECTION_EVENT =
        EventFactory.createArrayBacked(
            DimensionCollectionCallback.class,
            (listeners) -> ((registryAccess, options) -> {
                Set<ResourceKey<Level>> totalResult = new LinkedHashSet<>();
                for (DimensionCollectionCallback listener : listeners) {
                    totalResult.addAll(
                        listener.getExtraDimensionKeys(registryAccess, options)
                    );
                }
                return totalResult;
            })
        );
}
