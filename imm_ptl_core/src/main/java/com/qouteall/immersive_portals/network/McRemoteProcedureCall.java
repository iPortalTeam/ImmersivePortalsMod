package com.qouteall.immersive_portals.network;

import com.qouteall.hiding_in_the_bushes.O_O;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * <p>
 *     Fabric provides the networking API https://fabricmc.net/wiki/tutorial:networking.
 *     If you want to add a new type of packet, you need to
 *     1. Write packet serialization/deserialization code 2. Write the packet handling code,
 *     which requires sending the task to the client/server thread to execute it 3. Give it
 *      an identifier and register it.
 * </p>
 *
 * <p>
 *     This Remote Procedure Call API provides an easier way of networking.
 *     Just write a static method, then you can remotely invoke this method.
 *     No need to register the packet, no need to write serialization/deserialization code.
 *     The arguments will be automatically serialized and deserialized.
 * </p>
 *
 * For example:
 * <pre>
 * {@code
 * public class AAARemoteCallableBBB{
 *     public static void clientMethod(int arg1, double arg2) {...}
 *     public static void serverMethod(ServerPlayerEntity player, Block arg1) {...}
 * }
 * }
 * </pre>
 * The server can send packet to client using
 * <pre>
 * {@code
 * McRemoteProcedureCall.tellClientToInvoke(
 *     player,
 *     "path.to.the_class.AAARemoteCallableBBB.clientMethod",
 *     3, 4.5
 * );
 * }
 * </pre>
 * That method will be invoked on the client thread (render thread).
 *
 * <p></p>
 *
 * The client can send packet to server using
 * <pre>
 * {@code
 * McRemoteProcedureCall.tellServerToInvoke(
 *     "path.to.the_class.AAARemoteCallableBBB.serverMethod",
 *     player,
 *     Blocks.STONE
 * );
 * }
 * </pre>
 * That method will be invoked on the server thread.
 *
 * <p>For security concerns, the class path must contain "RemoteCallable". For example,
 *      the class name can be "XXRemoteCallableYYY" or "RemoteCallables"</p>
 *
 * <p>
 *     The supported argument types are
 *     <ul>
 *         <li>The types that Gson can directly serialize/deserialize,
 *          for example {@code int,double,boolean,long,String,int[],Map<String,String>,Enums} </li>
 *         <li>{@code Identifier,RegistryKey<World>,RegistryKey<Biome>,BlockPos,Vec3d,UUID,Block,Item,
 *          BlockState,ItemStack,CompoundTag,Text}</li>
 *     </ul>
 *     Using unsupported argument types will cause serialization/deserialization issues.
 * </p>
 *
 * <p>
 *     If you are sending the packets thousands of times every second, then performance issues may arise.
 *     In this case it's not recommended to use this.
 * </p>
 */
public class McRemoteProcedureCall {
    /**
     * For example:
     * <pre>
     * {@code
     * public class AAARemoteCallableBBB{
     *     public static void clientMethod(int arg1, double arg2) {...}
     * }
     * }
     * </pre>
     * The server can send packet to client using
     * <pre>
     * {@code
     * McRemoteProcedureCall.tellClientToInvoke(
     *     player,
     *     "path.to.the_class.AAARemoteCallableBBB.clientMethod",
     *     3, 4.5
     * );
     * }
     * @param player The player that you want to send packet to
     * @param methodPath If you are using Intellij IDEA, right click on the method,
     *                   click "Copy Reference", then you get the method path
     * @param arguments The arguments. The types must match the remotely invoked method signature.
     */
    public static void tellClientToInvoke(
        ServerPlayerEntity player,
        String methodPath,
        Object... arguments
    ) {
        if (O_O.isForge()) {
            throw new RuntimeException("Not yet supported on the Forge version");
        }
        
        CustomPayloadS2CPacket packet =
            ImplRemoteProcedureCall.createS2CPacket(methodPath, arguments);
        player.networkHandler.sendPacket(packet);
    }
    
    /**
     * For example:
     * <pre>
     * {@code
     * public class AAARemoteCallableBBB{
     *     public static void serverMethod(ServerPlayerEntity player, Block arg1) {...}
     * }
     * }
     * </pre>
     * The client can send packet to server using
     * <pre>
     * {@code
     * McRemoteProcedureCall.tellServerToInvoke(
     *     "path.to.the_class.AAARemoteCallableBBB.serverMethod",
     *     Blocks.STONE
     * );
     * }
     * </pre>
     *
     * @param methodPath If you are using Intellij IDEA, right click on the method,
     *                   click "Copy Reference", then you get the method path
     * @param arguments The arguments. The types must match the remotely invoked method signature.
     *                  The remote method's first argument must be the player that's sending the packet.
     */
    @Environment(EnvType.CLIENT)
    public static void tellServerToInvoke(
        String methodPath,
        Object... arguments
    ) {
        if (O_O.isForge()) {
            throw new RuntimeException("Not yet supported on the Forge version");
        }
        
        CustomPayloadC2SPacket packet =
            ImplRemoteProcedureCall.createC2SPacket(methodPath, arguments);
        MinecraftClient.getInstance().getNetworkHandler().sendPacket(packet);
    }
}
