package com.qouteall.immersive_portals;

import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

public class MyNetworkServer {
    public static final Identifier id_ctsTeleport =
        new Identifier("immersive_portals", "teleport");
    
    static void processCtsTeleport(PacketContext context, PacketByteBuf buf) {
        DimensionType dimensionBefore = DimensionType.byRawId(buf.readInt());
        Vec3d posBefore = new Vec3d(
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble()
        );
        int portalEntityId = buf.readInt();
        
        ModMain.serverTaskList.addTask(() -> {
            Globals.serverTeleportationManager.onPlayerTeleportedInClient(
                (ServerPlayerEntity) context.getPlayer(),
                dimensionBefore,
                posBefore,
                portalEntityId
            );
            
            return true;
        });
    }
    
    public static void init() {
        ServerSidePacketRegistry.INSTANCE.register(
            id_ctsTeleport,
            MyNetworkServer::processCtsTeleport
        );
    }
}
