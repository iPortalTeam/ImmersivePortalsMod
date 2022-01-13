package qouteall.imm_ptl.core.ducks;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;

public interface IEWorldRenderer {
    EntityRenderDispatcher ip_getEntityRenderDispatcher();
    
    BuiltChunkStorage ip_getBuiltChunkStorage();
    
    ChunkBuilder getChunkBuilder();
    
    void ip_myRenderEntity(
        Entity entity,
        double cameraX,
        double cameraY,
        double cameraZ,
        float tickDelta,
        MatrixStack matrixStack,
        VertexConsumerProvider vertexConsumerProvider
    );
    
    ShaderEffect portal_getTransparencyShader();
    
    void portal_setTransparencyShader(ShaderEffect arg);
    
    BufferBuilderStorage ip_getBufferBuilderStorage();
    
    void ip_setBufferBuilderStorage(BufferBuilderStorage arg);
    
    Frustum portal_getFrustum();
    
    void portal_setFrustum(Frustum arg);
    
    void portal_fullyDispose();
    
    void portal_setChunkInfoList(ObjectArrayList<WorldRenderer.ChunkInfo> arg);
    
    ObjectArrayList<WorldRenderer.ChunkInfo> portal_getChunkInfoList();
}
