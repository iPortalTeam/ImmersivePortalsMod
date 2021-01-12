package com.qouteall.hiding_in_the_bushes.util.networking;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class RemoteProcedureCall {
    public static void sendToClient(
        String methodPath,
        Object... arguments
    ) {
    
    }
    
    @Environment(EnvType.CLIENT)
    public static void sendToServer(
        String methodPath,
        Object... arguments
    ) {
    
    }
}
