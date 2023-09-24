package qouteall.q_misc_util.api;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import qouteall.q_misc_util.ImplRemoteProcedureCall;

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
        ServerPlayer player,
        String methodPath,
        Object... arguments
    ) {
        ClientboundCustomPayloadPacket packet = createPacketToSendToClient(methodPath, arguments);
        player.connection.send(packet);
    }
    
    /**
     * Same as the above, but only creates packet and does not send.
     */
    public static ClientboundCustomPayloadPacket createPacketToSendToClient(
        String methodPath, Object... arguments
    ) {
        return ImplRemoteProcedureCall.createS2CPacket(methodPath, arguments);
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
        ServerboundCustomPayloadPacket packet = createPacketToSendToServer(methodPath, arguments);
        Minecraft.getInstance().getConnection().send(packet);
    }
    
    public static ServerboundCustomPayloadPacket createPacketToSendToServer(String methodPath, Object... arguments) {
        return ImplRemoteProcedureCall.createC2SPacket(methodPath, arguments);
    }
}
