package qouteall.imm_ptl.peripheral.wand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.GeometryPortalShape;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.IntBox;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class WandUtil {
    
    @Nullable
    @Environment(EnvType.CLIENT)
    public static Portal getClientPortalByUUID(UUID portalId) {
        LocalPlayer player = Minecraft.getInstance().player;
        
        if (player == null) {
            return null;
        }
    
        Level world = player.level();
    
        return getPortalByUUID(world, portalId);
    }
    
    @Nullable
    public static Portal getPortalByUUID(Level world, UUID portalId) {
        if (portalId == null) {
            return null;
        }
        
        Entity entity = McHelper.getEntityByUUID(world, portalId);
        
        if (entity instanceof Portal portal) {
            return portal;
        }
        else {
            return null;
        }
    }
    
    public static Vec3 alignOnBlocks(
        Level world, Vec3 vec3, int gridCount
    ) {
        if (gridCount == 0) {
            return vec3;
        }
        
        BlockPos blockPos = BlockPos.containing(vec3);
        return new IntBox(blockPos.offset(-1, -1, -1), blockPos.offset(1, 1, 1))
            .stream()
            .flatMap(
                pos -> {
                    BlockState blockState = world.getBlockState(pos);
                    VoxelShape collisionShape = blockState.getCollisionShape(world, pos)
                        .move(pos.getX(), pos.getY(), pos.getZ());
                    List<AABB> aabbs = collisionShape.toAabbs();
                    if (aabbs.size() != 1) {
                        // in the case of hopper, not all of its collision boxes are symmetric
                        // without this, the north and south side of top edge mid-point of hopper cannot be selected
                        // also make air blocks alignable
                        aabbs.add(new AABB(pos));
                    }
                    return aabbs.stream();
                }
            )
            .map(box -> Helper.alignToBoxSurface(box, vec3, gridCount))
            .min(
                Comparator.comparingDouble(
                    p -> p.distanceToSqr(vec3)
                )
            ).orElse(null);
    }
    
    @Environment(EnvType.CLIENT)
    public static void renderPortalShapeMeshDebug(
        PoseStack matrixStack, Vec3 cameraPos, VertexConsumer vertexConsumer, Portal portal
    ) {
        GeometryPortalShape shape = portal.specialShape;
        if (shape != null) {
            int triangleNum = shape.triangles.size();
            int vertexNum = triangleNum * 3;
            Vec3[] vertexes = new Vec3[vertexNum];
            double halfWidth = portal.width / 2;
            double halfHeight = portal.height / 2;
            Vec3 X = portal.axisW.scale(halfWidth);
            Vec3 Y = portal.axisH.scale(halfHeight);
            
            matrixStack.pushPose();
            
            matrixStack.translate(
                portal.getOriginPos().x - cameraPos.x,
                portal.getOriginPos().y - cameraPos.y,
                portal.getOriginPos().z - cameraPos.z
            );
            
            Matrix4f matrix = matrixStack.last().pose();
            Matrix3f normalMatrix = matrixStack.last().normal();
            
            for (int i = 0; i < shape.triangles.size(); i++) {
                GeometryPortalShape.TriangleInPlane triangleInPlane = shape.triangles.get(i);
                
                double centerX = (triangleInPlane.x1 + triangleInPlane.x2 + triangleInPlane.x3) / 3;
                double centerY = (triangleInPlane.y1 + triangleInPlane.y2 + triangleInPlane.y3) / 3;
                
                double frac = 0.97;
                double x1 = triangleInPlane.x1 * frac + centerX * (1 - frac);
                double y1 = triangleInPlane.y1 * frac + centerY * (1 - frac);
                double x2 = triangleInPlane.x2 * frac + centerX * (1 - frac);
                double y2 = triangleInPlane.y2 * frac + centerY * (1 - frac);
                double x3 = triangleInPlane.x3 * frac + centerX * (1 - frac);
                double y3 = triangleInPlane.y3 * frac + centerY * (1 - frac);
                
                WireRenderingHelper.putLine(
                    vertexConsumer, 0x80ff0000, matrix, normalMatrix,
                    X.scale(x1).add(Y.scale(y1)), X.scale(x2).add(Y.scale(y2))
                );
                
                WireRenderingHelper.putLine(
                    vertexConsumer, 0x80ff0000, matrix, normalMatrix,
                    X.scale(x2).add(Y.scale(y2)), X.scale(x3).add(Y.scale(y3))
                );
                
                WireRenderingHelper.putLine(
                    vertexConsumer, 0x80ff0000, matrix, normalMatrix,
                    X.scale(x3).add(Y.scale(y3)), X.scale(x1).add(Y.scale(y1))
                );
            }
            
            matrixStack.popPose();
        }
    }
}
