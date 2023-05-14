package qouteall.q_misc_util.platform_specific;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import qouteall.q_misc_util.MiscGlobals;
import qouteall.q_misc_util.dimension.DimensionMisc;
import qouteall.q_misc_util.dimension.DimsCommand;
import qouteall.q_misc_util.dimension.DynamicDimensionsImpl;
import qouteall.q_misc_util.dimension.ExtraDimensionStorage;

public class MiscUtilModEntry implements ModInitializer {
    @Override
    public void onInitialize() {
        DimensionMisc.init();
        
        ExtraDimensionStorage.init();
        
        DynamicDimensionsImpl.init();
        
        MiscNetworking.init();
        
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            MiscGlobals.serverTaskList.processTasks();
        });
        
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            DimsCommand.register(dispatcher);
        });
    }
}
