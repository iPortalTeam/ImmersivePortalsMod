package qouteall.imm_ptl.peripheral.wand;

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
import qouteall.imm_ptl.core.McHelper;
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
}
