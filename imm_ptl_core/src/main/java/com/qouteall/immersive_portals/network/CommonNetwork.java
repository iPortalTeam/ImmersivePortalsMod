package com.qouteall.immersive_portals.network;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ducks.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.ducks.IEClientWorld;
import com.qouteall.immersive_portals.ducks.IEParticleManager;
import com.qouteall.immersive_portals.my_util.LimitedLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.Packet;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public class CommonNetwork {
    
    public static final MinecraftClient client = MinecraftClient.getInstance();
    
    private static final LimitedLogger limitedLogger = new LimitedLogger(100);
    private static boolean isProcessingRedirectedMessage = false;
    
    public static boolean getIsProcessingRedirectedMessage() {
        return isProcessingRedirectedMessage;
    }
    
    public static void processRedirectedPacket(RegistryKey<World> dimension, Packet packet) {
        Runnable func = () -> {
            try {
                client.getProfiler().push("process_redirected_packet");
                
                ClientWorld packetWorld = CGlobal.clientWorldLoader.getWorld(dimension);
                
                doProcessRedirectedMessage(packetWorld, packet);
            }
            finally {
                client.getProfiler().pop();
            }
        };
        
        CHelper.executeOnRenderThread(func);
    }
    
    public static void doProcessRedirectedMessage(
        ClientWorld packetWorld,
        Packet packet
    ) {
        boolean oldIsProcessing = CommonNetwork.isProcessingRedirectedMessage;
        
        isProcessingRedirectedMessage = true;
        
        ClientPlayNetworkHandler netHandler = ((IEClientWorld) packetWorld).getNetHandler();
        
        if ((netHandler).getWorld() != packetWorld) {
            ((IEClientPlayNetworkHandler) netHandler).setWorld(packetWorld);
            Helper.err("The world field of client net handler is wrong");
        }
        
        client.getProfiler().push(() -> {
            return "handle_redirected_packet" + packetWorld.getRegistryKey() + packet;
        });
        
        try {
            withSwitchedWorld(packetWorld, () -> packet.apply(netHandler));
        }
        catch (Throwable e) {
            limitedLogger.throwException(() -> new IllegalStateException(
                "handling packet in " + packetWorld.getRegistryKey(), e
            ));
        }
        finally {
            client.getProfiler().pop();
            
            isProcessingRedirectedMessage = oldIsProcessing;
        }
    }
    
    public static void withSwitchedWorld(ClientWorld newWorld, Runnable runnable) {
        ClientWorld originalWorld = client.world;
        //some packet handling may use mc.world so switch it
        client.world = newWorld;
        ((IEParticleManager) client.particleManager).mySetWorld(newWorld);
        
        try {
            runnable.run();
        }
        finally {
            if (client.world != newWorld) {
                Helper.err("oops, respawn packet should not be redirected");
                originalWorld = client.world;
            }
            
            client.world = originalWorld;
            ((IEParticleManager) client.particleManager).mySetWorld(originalWorld);
        }
    }
}
