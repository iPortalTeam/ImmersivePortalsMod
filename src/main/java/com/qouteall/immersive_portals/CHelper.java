package com.qouteall.immersive_portals;

import com.google.common.collect.Streams;
import com.qouteall.immersive_portals.ducks.IEClientWorld;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.Quaternion;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.GL_NO_ERROR;

@Environment(EnvType.CLIENT)
public class CHelper {
    
    private static int reportedErrorNum = 0;
    
    public static PlayerListEntry getClientPlayerListEntry() {
        return MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(
            MinecraftClient.getInstance().player.getGameProfile().getId()
        );
    }
    
    //NOTE this may not be reliable
    public static DimensionType getOriginalDimension() {
        if (CGlobal.renderer.isRendering()) {
            return MyRenderHelper.originalPlayerDimension;
        }
        else {
            return MinecraftClient.getInstance().player.dimension;
        }
    }
    
    public static boolean shouldDisableFog() {
        return OFInterface.shouldDisableFog.getAsBoolean();
    }
    
    //do not inline this
    //or it will crash in server
    public static World getClientWorld(DimensionType dimension) {
        return CGlobal.clientWorldLoader.getOrCreateFakedWorld(dimension);
    }
    
    public static List<GlobalTrackedPortal> getClientGlobalPortal(World world) {
        List<GlobalTrackedPortal> globalPortals = ((IEClientWorld) world).getGlobalPortals();
        return globalPortals;
    }
    
    public static Stream<Portal> getClientNearbyPortals(double range) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        List<GlobalTrackedPortal> globalPortals = ((IEClientWorld) player.world).getGlobalPortals();
        Stream<Portal> nearbyPortals = McHelper.getEntitiesNearby(
            player,
            Portal.class,
            range
        );
        if (globalPortals == null) {
            return nearbyPortals;
        }
        else {
            return Streams.concat(
                globalPortals.stream().filter(
                    p -> p.getDistanceToNearestPointInPortal(player.getPos()) < range * 2
                ),
                nearbyPortals
            );
        }
    }
    
    public static void checkGlError() {
        if (!Global.doCheckGlError) {
            return;
        }
        if (reportedErrorNum > 100) {
            return;
        }
        int errorCode = GL11.glGetError();
        if (errorCode != GL_NO_ERROR) {
            Helper.err("OpenGL Error" + errorCode);
            new Throwable().printStackTrace();
            reportedErrorNum++;
        }
    }
    
    public static void printChat(String str) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
            new LiteralText(str)
        );
    }
    
    //NOTE this will mutate a and return a
    public static Quaternion quaternionNumAdd(Quaternion a, Quaternion b) {
        //TODO correct wrong parameter name for yarn
        a.set(
            a.getB() + b.getB(),
            a.getC() + b.getC(),
            a.getD() + b.getD(),
            a.getA() + b.getA()
        );
        return a;
    }
    
    //NOTE this will mutate a and reutrn a
    public static Quaternion quaternionScale(Quaternion a, float scale) {
        a.set(
            a.getB() * scale,
            a.getC() * scale,
            a.getD() * scale,
            a.getA() * scale
        );
        return a;
    }
    
    //NOTE parameter will be mutated
    //https://en.wikipedia.org/wiki/Slerp
    public static Quaternion interpolateQuaternion(
        Quaternion v0,
        Quaternion v1,
        float t
    ) {
        v0.normalize();
        v1.normalize();
        
        // Compute the cosine of the angle between the two vectors.
        double dot = v0.getA() * v1.getA() +
            v0.getB() * v1.getB() +
            v0.getC() * v1.getC() +
            v0.getD() * v1.getD();
        
        // If the dot product is negative, slerp won't take
        // the shorter path. Note that v1 and -v1 are equivalent when
        // the negation is applied to all four components. Fix by
        // reversing one quaternion.
        if (dot < 0.0f) {
            v1.scale(-1);
            dot = -dot;
        }
        
        double DOT_THRESHOLD = 0.9995;
        if (dot > DOT_THRESHOLD) {
            // If the inputs are too close for comfort, linearly interpolate
            // and normalize the result.
            
            //Quaternion result = v0 + t * (v1 - v0);
            
            //TODO optimize if it's invoked frequently
            Quaternion result = quaternionNumAdd(
                v0.copy(),
                quaternionScale(
                    quaternionNumAdd(
                        v1.copy(), quaternionScale(v0.copy(), -1)
                    ),
                    t
                )
            );
            result.normalize();
            return result;
        }
        
        // Since dot is in range [0, DOT_THRESHOLD], acos is safe
        double theta_0 = Math.acos(dot);        // theta_0 = angle between input vectors
        double theta = theta_0 * t;          // theta = angle between v0 and result
        double sin_theta = Math.sin(theta);     // compute this value only once
        double sin_theta_0 = Math.sin(theta_0); // compute this value only once
        
        double s0 = Math.cos(theta) - dot * sin_theta / sin_theta_0;  // == sin(theta_0 - theta) / sin(theta_0)
        double s1 = sin_theta / sin_theta_0;
        
        return quaternionNumAdd(
            quaternionScale(v0, (float) s0),
            quaternionScale(v1, (float) s1)
        );
        
        //return (s0 * v0) + (s1 * v1);
    }
}
