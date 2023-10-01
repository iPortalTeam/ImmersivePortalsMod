package qouteall.q_misc_util.dimension;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldOptions;
import qouteall.q_misc_util.api.DimensionAPI;
import qouteall.q_misc_util.mixin.dimension.IEMappedRegistry;

public class DimensionImpl {
    public static void fireDimensionLoadEvent(
        MinecraftServer server,
        WorldOptions worldOptions,
        RegistryAccess registryAccess
    ) {
        MappedRegistry<LevelStem> levelStems = (MappedRegistry<LevelStem>)
            registryAccess.registryOrThrow(Registries.LEVEL_STEM);
        
        // the vanilla freezing mechanism is used for validating dangling object references
        // for this API, that thing won't happen
        boolean oldIsFrozen = ((IEMappedRegistry) levelStems).ip_getIsFrozen();
        ((IEMappedRegistry) levelStems).ip_setIsFrozen(false);
        
        DimensionAPI.SEVER_DIMENSIONS_LOAD_EVENT.invoker().run(
            server, worldOptions, levelStems, registryAccess
        );
        
        ((IEMappedRegistry) levelStems).ip_setIsFrozen(oldIsFrozen);
    }
}
