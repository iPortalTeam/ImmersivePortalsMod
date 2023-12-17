package qouteall.imm_ptl.core.mc_utils;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.ducks.IEMinecraftServer;
import qouteall.q_misc_util.my_util.MyTaskList;

public class ServerTaskList {
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            of(server).processTasks();
        });
        
        IPGlobal.SERVER_CLEANUP_EVENT.register(server -> {
            of(server).forceClearTasks();
        });
    }
    
    // the tasks are executed after ticking. will be cleared when server closes
    public static MyTaskList of(MinecraftServer server) {
        return ((IEMinecraftServer) server).ip_getServerTaskList();
    }
}
