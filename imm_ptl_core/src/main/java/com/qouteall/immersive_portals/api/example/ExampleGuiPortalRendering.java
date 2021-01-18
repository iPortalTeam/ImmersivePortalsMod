package com.qouteall.immersive_portals.api.example;

import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.ClientWorldLoader;
import com.qouteall.immersive_portals.chunk_loading.ChunkLoader;
import com.qouteall.immersive_portals.chunk_loading.DimensionalChunkPos;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.my_util.DQuaternion;
import com.qouteall.immersive_portals.network.RemoteProcedureCall;
import com.qouteall.immersive_portals.render.GuiPortalRendering;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import com.qouteall.immersive_portals.render.context_management.WorldRenderInfo;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.WeakHashMap;

/**
 * <p>
 * Example code about how to use
 * </p>
 * <ul>
 *     <li>GUI Portal API</li>
 *     <li>Chunk Loading API</li>
 *     <li>Remote Procedure Call Utility API</li>
 * </ul>
 *
 * <p>
 *     To test this, use command /portal debug gui_portal minecraft:the_end 0 80 0
 * </p>
 * <p>It involves:</p>
 *
 * <ul>
 *     <li>The server sending the dimension and position to client</li>
 *     <li>The server adding an additional per-player chunk loader so that the chunks near that
 *     position is being generated and synchronized to that player's client</li>
 *     <li>Client opening the GUI portal screen and render the GUI portal.
 *     It controls the camera rotation transformation and camera position.</li>
 *     <li>If the client closes the screen, the server removes the additional chunk loader</li>
 * </ul>
 */
public class ExampleGuiPortalRendering {
    /**
     * The Framebuffer that the GUI portal is going to render onto
     */
    @Environment(EnvType.CLIENT)
    private static Framebuffer frameBuffer;
    
    /**
     * A weak hash map storing ChunkLoader objects for each players
     */
    private static final WeakHashMap<ServerPlayerEntity, ChunkLoader>
        chunkLoaderMap = new WeakHashMap<>();
    
    /**
     * Remove the GUI portal chunk loader for a player
     */
    private static void removeChunkLoaderFor(ServerPlayerEntity player) {
        ChunkLoader chunkLoader = chunkLoaderMap.remove(player);
        if (chunkLoader != null) {
            NewChunkTrackingGraph.removePerPlayerAdditionalChunkLoader(player, chunkLoader);
        }
    }
    
    public static void onCommandExecuted(ServerPlayerEntity player, ServerWorld world, Vec3d pos) {
        removeChunkLoaderFor(player);
        
        ChunkLoader chunkLoader = new ChunkLoader(
            new DimensionalChunkPos(
                world.getRegistryKey(), new ChunkPos(new BlockPos(pos))
            ),
            8
        );
        
        // Add the per-player additional chunk loader
        NewChunkTrackingGraph.addPerPlayerAdditionalChunkLoader(player, chunkLoader);
        chunkLoaderMap.put(player, chunkLoader);
        
        // Tell the client to open the screen
        RemoteProcedureCall.tellClientToInvoke(
            player,
            "com.qouteall.immersive_portals.api.example.ExampleGuiPortalRendering.RemoteCallables.clientActivateExampleGuiPortal",
            world.getRegistryKey(),
            pos
        );
    }
    
    public static class RemoteCallables {
        @Environment(EnvType.CLIENT)
        public static void clientActivateExampleGuiPortal(
            RegistryKey<World> dimension,
            Vec3d position
        ) {
            if (frameBuffer == null) {
                frameBuffer = new Framebuffer(1000, 1000, true, true);
            }
            
            MinecraftClient.getInstance().openScreen(new GuiPortalScreen(dimension, position));
        }
        
        public static void serverRemoveChunkLoader(ServerPlayerEntity player) {
            removeChunkLoaderFor(player);
        }
    }
    
    @Environment(EnvType.CLIENT)
    public static class GuiPortalScreen extends Screen {
        
        private final RegistryKey<World> viewingDimension;
        
        private final Vec3d viewingPosition;
        
        public GuiPortalScreen(RegistryKey<World> viewingDimension, Vec3d viewingPosition) {
            super(new LiteralText("GUI Portal Example"));
            this.viewingDimension = viewingDimension;
            this.viewingPosition = viewingPosition;
        }
        
        @Override
        public void onClose() {
            super.onClose();
            
            // Tell the server to remove the additional chunk loader
            RemoteProcedureCall.tellServerToInvoke(
                "com.qouteall.immersive_portals.api.example.ExampleGuiPortalRendering.RemoteCallables.serverRemoveChunkLoader"
            );
        }
        
        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            super.render(matrices, mouseX, mouseY, delta);
            
            double t1 = CHelper.getSmoothCycles(503);
            double t2 = CHelper.getSmoothCycles(197);
            
            // Determine the camera transformation
            Matrix4f cameraTransformation = new Matrix4f();
            cameraTransformation.loadIdentity();
            cameraTransformation.multiply(
                DQuaternion.rotationByDegrees(
                    new Vec3d(1, 1, 1).normalize(),
                    t1 * 360
                ).toMcQuaternion()
            );
            
            // Determine the camera position
            Vec3d cameraPosition = this.viewingPosition.add(
                new Vec3d(Math.cos(t2 * 2 * Math.PI), 0, Math.sin(t2 * 2 * Math.PI)).multiply(30)
            );
            
            // Create the world render info
            WorldRenderInfo worldRenderInfo = new WorldRenderInfo(
                ClientWorldLoader.getWorld(viewingDimension),// the world that it renders
                cameraPosition,// the camera position
                cameraTransformation,// the camera transformation
                true,// does not apply this transformation to the existing player camera
                null,
                client.options.viewDistance// render distance
            );
            
            // Ask it to render the world into the framebuffer the next frame
            GuiPortalRendering.submitNextFrameRendering(worldRenderInfo, frameBuffer);
            
            // Draw the framebuffer
            int h = client.getWindow().getFramebufferHeight();
            int w = client.getWindow().getFramebufferWidth();
            MyRenderHelper.drawFramebuffer(
                frameBuffer,
                false, false,
                w * 0.2f, w * 0.8f,
                h * 0.2f, h * 0.8f
            );
            
            drawCenteredText(
                matrices, this.textRenderer, this.title, this.width / 2, 70, 16777215
            );
        }
        
        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
