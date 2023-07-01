package qouteall.imm_ptl.core.mixin.common;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.Connection;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.q_misc_util.my_util.LimitedLogger;

@Mixin(Connection.class)
public class MixinConnection_Debug {
    @Shadow
    @Final
    private static Logger LOGGER;
    
    private static final LimitedLogger immptl_limitedLogger = new LimitedLogger(100);
    
    // avoid swallowing the exception stacktrace
    // the exception is logged in debug level, but debug level is not enabled by default
    @Inject(method = "exceptionCaught", at = @At("HEAD"))
    private void onExceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable throwable, CallbackInfo ci) {
        immptl_limitedLogger.invoke(() -> {
            LOGGER.error("[ImmPtl] Exception from connection ", throwable);
        });
    }
    
    // This will even trigger without ImmPtl
    // Vanilla seems stops entity tracking after closing the player connection
//    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/util/concurrent/GenericFutureListener;)V", at = @At("HEAD"))
//    public void send(Packet<?> packet, GenericFutureListener<? extends Future<? super Void>> genericFutureListener, CallbackInfo ci) {
//        if (channel != null && !channel.isOpen()) {
//            LOGGER.error("[ImmPtl] Packet sent after disconnect. This will cause memory leak.");
//
//            LOGGER.error("packet stack trace", new Throwable(""));
//        }
//    }
}
