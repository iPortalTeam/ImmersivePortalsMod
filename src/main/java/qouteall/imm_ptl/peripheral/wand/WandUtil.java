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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.mc_utils.WireRenderingHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.IntBox;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class WandUtil {
    
    @Nullable
    @Environment(EnvType.CLIENT)
    @OnlyIn(Dist.CLIENT)
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
    @OnlyIn(Dist.CLIENT)
    public static void renderPortalAreaGridNew(
        VertexConsumer vertexConsumer, Vec3 cameraPos,
        ProtoPortalSide protoPortalSide,
        int color1, int color2, PoseStack matrixStack
    ) {
        int separation = 8;
        
        Vec3 leftBottom = protoPortalSide.leftBottom;
        Vec3 rightBottom = protoPortalSide.rightBottom;
        Vec3 leftTop = protoPortalSide.leftTop;
        
        Vec3 xAxis = rightBottom.subtract(leftBottom);
        Vec3 yAxis = leftTop.subtract(leftBottom);
        
        Vec3 normal = xAxis.cross(yAxis).normalize();
        
        matrixStack.pushPose();
        matrixStack.translate(
            leftBottom.x - cameraPos.x,
            leftBottom.y - cameraPos.y,
            leftBottom.z - cameraPos.z
        );
        
        Matrix4f matrix = matrixStack.last().pose();
        Matrix3f normalMatrix = matrixStack.last().normal();
        
        // render the outer frame
        WireRenderingHelper.putLine(
            vertexConsumer, color1, matrix, normalMatrix,
            Vec3.ZERO, xAxis
        );
        WireRenderingHelper.putLine(
            vertexConsumer, color1, matrix, normalMatrix,
            yAxis, yAxis.add(xAxis)
        );
        WireRenderingHelper.putLine(
            vertexConsumer, color1, matrix, normalMatrix,
            Vec3.ZERO, yAxis
        );
        WireRenderingHelper.putLine(
            vertexConsumer, color1, matrix, normalMatrix,
            xAxis, yAxis.add(xAxis)
        );
        
        // render the inner grid flow lines
        int flowCount = 3;
        Random random = new Random(color1);
        
        double scaling = 0.994;
        double offset1 = (1 - scaling) / 2;
        double offset2 = -offset1;
        
        for (int cx = 1; cx < separation; cx++) {
            double lx = ((double) cx) / separation;
            WireRenderingHelper.renderFlowLines(
                vertexConsumer,
                new Vec3[]{
                    xAxis.scale(lx * scaling + offset1),
                    xAxis.scale(lx * scaling + offset1).add(yAxis),
                },
                flowCount, color1, 1, matrixStack,
                () -> random.nextInt(10, 100)
            );
    
            WireRenderingHelper.renderFlowLines(
                vertexConsumer,
                new Vec3[]{
                    xAxis.scale(lx * scaling + offset2),
                    xAxis.scale(lx * scaling + offset2).add(yAxis),
                },
                flowCount, color2, -1, matrixStack,
                () -> random.nextInt(10, 100)
            );
        }
        
        for (int cy = 1; cy < separation; cy++) {
            double ly = ((double) cy) / separation;
            WireRenderingHelper.renderFlowLines(
                vertexConsumer,
                new Vec3[]{
                    yAxis.scale(ly * scaling + offset1),
                    yAxis.scale(ly * scaling + offset1).add(xAxis),
                },
                flowCount, color1, 1, matrixStack,
                () -> random.nextInt(10, 100)
            );
    
            WireRenderingHelper.renderFlowLines(
                vertexConsumer,
                new Vec3[]{
                    yAxis.scale(ly * scaling + offset2),
                    yAxis.scale(ly * scaling + offset2).add(xAxis),
                },
                flowCount, color2, -1, matrixStack,
                () -> random.nextInt(10, 100)
            );
        }
        
        matrixStack.popPose();
    }
    
    @Environment(EnvType.CLIENT)
    @OnlyIn(Dist.CLIENT)
    public static void renderPortalAreaGrid(
        VertexConsumer vertexConsumer, Vec3 cameraPos,
        ProtoPortalSide protoPortalSide,
        int color, PoseStack matrixStack
    ) {
        int separation = 8;
        
        Vec3 leftBottom = protoPortalSide.leftBottom;
        Vec3 rightBottom = protoPortalSide.rightBottom;
        Vec3 leftTop = protoPortalSide.leftTop;
        
        Vec3 xAxis = rightBottom.subtract(leftBottom);
        Vec3 yAxis = leftTop.subtract(leftBottom);
        
        Vec3 normal = xAxis.cross(yAxis).normalize();
        
        matrixStack.pushPose();
        matrixStack.translate(
            leftBottom.x - cameraPos.x,
            leftBottom.y - cameraPos.y,
            leftBottom.z - cameraPos.z
        );
        
        Matrix4f matrix = matrixStack.last().pose();
        Matrix3f normalMatrix = matrixStack.last().normal();
        
        for (int i = 0; i <= separation; i++) {
            double ratio = (double) i / separation;
            
            Vec3 lineStart = xAxis.scale(ratio);
            Vec3 lineEnd = xAxis.scale(ratio).add(yAxis);
            
            WireRenderingHelper.putLine(vertexConsumer, color, yAxis.normalize(), matrix, normalMatrix, lineStart, lineEnd);
        }
        
        for (int i = 0; i <= separation; i++) {
            double ratio = (double) i / separation;
            
            Vec3 lineStart = yAxis.scale(ratio);
            Vec3 lineEnd = yAxis.scale(ratio).add(xAxis);
            
            WireRenderingHelper.putLine(vertexConsumer, color, xAxis.normalize(), matrix, normalMatrix, lineStart, lineEnd);
        }
        
        matrixStack.popPose();
    }
}
