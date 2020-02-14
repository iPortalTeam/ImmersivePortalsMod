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
import net.minecraft.client.render.chunk.ChunkBuilder;
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
        if (!CGlobal.doCheckGlError) {
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
    
    public static void whatever(
        int north,
        int south,
        int west,
        int east,
        ChunkBuilder.BuiltChunk[] renderChunkNeighbours
    ) {
        if (renderChunkNeighbours[north] == null ||
            renderChunkNeighbours[south] == null ||
            renderChunkNeighbours[west] == null ||
            renderChunkNeighbours[east] == null
        ) {
            Helper.log("ouch");
        }
    }
}
