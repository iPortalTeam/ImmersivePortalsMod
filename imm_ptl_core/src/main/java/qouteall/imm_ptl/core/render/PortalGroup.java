package qouteall.imm_ptl.core.render;

import qouteall.imm_ptl.core.Helper;
import qouteall.imm_ptl.core.ducks.IEWorldRendererChunkInfo;
import qouteall.q_misc_util.my_util.BoxPredicate;
import qouteall.q_misc_util.my_util.LimitedLogger;
import qouteall.q_misc_util.my_util.Plane;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.portal.PortalRenderInfo;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

// currently only exists on client side
@Environment(EnvType.CLIENT)
public class PortalGroup implements PortalLike {
    
    private static final LimitedLogger limitedLogger = new LimitedLogger(20);
    
    public final Portal.TransformationDesc transformationDesc;
    public final List<Portal> portals = new ArrayList<>();
    
    private Box exactBoundingBox;
    private Vec3d origin;
    private Vec3d dest;
    
    @Nullable
    private Box destAreaBoxCache = null;
    
    @Nullable
    private Boolean isEnclosedCache = null;
    
    private final UUID uuid = MathHelper.randomUuid();
    
    public PortalGroup(Portal.TransformationDesc transformationDesc) {
        this.transformationDesc = transformationDesc;
    }
    
    public void addPortal(Portal portal) {
        Validate.isTrue(portal.world.isClient());
        Validate.isTrue(!portal.getIsGlobal());
        
        if (portals.contains(portal)) {
            limitedLogger.err("Adding duplicate portal into group " + portal);
            return;
        }
        
        portals.add(portal);
        updateCache();
    }
    
    public void removePortal(Portal portal) {
        portals.remove(portal);
        
        updateCache();
    }
    
    public void updateCache() {
        exactBoundingBox = null;
        origin = null;
        dest = null;
        destAreaBoxCache = null;
        isEnclosedCache = null;
    }
    
    public Box getDestAreaBox() {
        if (destAreaBoxCache == null) {
            destAreaBoxCache = (
                Helper.transformBox(getExactAreaBox(), pos -> {
                    return getFirstPortal().transformPoint(pos);
                })
            );
        }
        
        return destAreaBoxCache;
    }
    
    @Override
    public boolean isConventionalPortal() {
        return false;
    }
    
    @Override
    public Box getExactAreaBox() {
        if (exactBoundingBox == null) {
            exactBoundingBox = portals.stream().map(
                Portal::getNarrowBoundingBox
            ).reduce(Box::union).get();
        }
        return exactBoundingBox;
    }
    
    @Override
    public Vec3d transformPoint(Vec3d pos) {
        return getFirstPortal().transformPoint(pos);
    }
    
    @Override
    public Vec3d transformLocalVec(Vec3d localVec) {
        return getFirstPortal().transformLocalVec(localVec);
    }
    
    @Override
    public Vec3d inverseTransformLocalVec(Vec3d localVec) {
        return getFirstPortal().inverseTransformLocalVec(localVec);
    }
    
    @Override
    public Vec3d inverseTransformPoint(Vec3d point) {
        return getFirstPortal().inverseTransformPoint(point);
    }
    
    @Override
    public double getDistanceToNearestPointInPortal(Vec3d point) {
        return Helper.getDistanceToBox(getExactAreaBox(), point);
    }
    
    @Override
    public double getDestAreaRadiusEstimation() {
        double maxDimension = getSizeEstimation();
        return maxDimension * transformationDesc.scaling;
    }
    
    
    @Override
    public Vec3d getOriginPos() {
        if (origin == null) {
            origin = getExactAreaBox().getCenter();
        }
        
        return origin;
    }
    
    @Override
    public Vec3d getDestPos() {
        if (dest == null) {
            dest = transformPoint(getOriginPos());
        }
        
        return dest;
    }
    
    @Override
    public World getOriginWorld() {
        return getFirstPortal().world;
    }
    
    @Override
    public World getDestWorld() {
        return getFirstPortal().getDestWorld();
    }
    
    @Override
    public RegistryKey<World> getDestDim() {
        return getFirstPortal().getDestDim();
    }
    
    @Override
    public boolean isRoughlyVisibleTo(Vec3d cameraPos) {
        return true;
    }
    
    @Nullable
    @Override
    public Plane getInnerClipping() {
        return null;
    }
    
    @Override
    public boolean isInside(Vec3d entityPos, double valve) {
        if (isEnclosed()) {
            return getDestAreaBox().contains(entityPos);
        }
        return true;
    }
    
    @Nullable
    @Override
    public Quaternion getRotation() {
        return transformationDesc.rotation;
    }
    
    @Override
    public double getScale() {
        return transformationDesc.scaling;
    }
    
    @Override
    public boolean getIsGlobal() {
        return false;
    }
    
    @Nullable
    @Override
    public Vec3d[] getOuterFrustumCullingVertices() {
        return null;
    }
    
    @Environment(EnvType.CLIENT)
    @Override
    public void renderViewAreaMesh(Vec3d portalPosRelativeToCamera, Consumer<Vec3d> vertexOutput) {
        for (Portal portal : portals) {
            Vec3d relativeToGroup = portal.getOriginPos().subtract(getOriginPos());
            portal.renderViewAreaMesh(
                portalPosRelativeToCamera.add(relativeToGroup),
                vertexOutput
            );
        }
    }
    
    @Nullable
    @Override
    public Matrix4f getAdditionalCameraTransformation() {
        return getFirstPortal().getAdditionalCameraTransformation();
    }
    
    @Nullable
    @Override
    public UUID getDiscriminator() {
        return uuid;
    }
    
    public void purge() {
        portals.removeIf(portal -> {
            return portal.isRemoved();
        });
    }
    
    @Override
    public boolean cannotRenderInMe(Portal portal) {
        if (isEnclosed()) {
            if (!getDestAreaBox().intersects(portal.getExactAreaBox())) {
                return true;
            }
        }
        
        return portals.stream().filter(p -> p.cannotRenderInMe(portal)).count() >= 2;
    }
    
    @Environment(EnvType.CLIENT)
    @Override
    public BoxPredicate getInnerFrustumCullingFunc(
        double innerCameraX, double innerCameraY, double innerCameraZ
    ) {
        Vec3d innerCameraPos = new Vec3d(innerCameraX, innerCameraY, innerCameraZ);
        Vec3d outerCameraPos = getFirstPortal().inverseTransformPoint(innerCameraPos);
        
        List<BoxPredicate> funcs = portals.stream().filter(
            portal1 -> portal1.isInFrontOfPortal(outerCameraPos)
        ).map(
            portal -> portal.getInnerFrustumCullingFunc(innerCameraX, innerCameraY, innerCameraZ)
        ).collect(Collectors.toList());
        
        return (minX, minY, minZ, maxX, maxY, maxZ) -> {
            // return true if all funcs return true
            for (BoxPredicate func : funcs) {
                if (!func.test(minX, minY, minZ, maxX, maxY, maxZ)) {
                    return false;
                }
            }
            return true;
        };
    }
    
    @Environment(EnvType.CLIENT)
    @Override
    public void doAdditionalRenderingCull(ObjectList<?> visibleChunks) {
        if (!isEnclosed()) {
            return;
        }
        
        Box enclosedDestAreaBox = getDestAreaBox().contract(0.5);
        
        if (enclosedDestAreaBox != null) {
            int xMin = (int) Math.floor(enclosedDestAreaBox.minX / 16);
            int xMax = (int) Math.ceil(enclosedDestAreaBox.maxX / 16) - 1;
            int yMin = (int) Math.floor(enclosedDestAreaBox.minY / 16);
            int yMax = (int) Math.ceil(enclosedDestAreaBox.maxY / 16) - 1;
            int zMin = (int) Math.floor(enclosedDestAreaBox.minZ / 16);
            int zMax = (int) Math.ceil(enclosedDestAreaBox.maxZ / 16) - 1;
            
            Helper.removeIf(visibleChunks, (obj) -> {
                ChunkBuilder.BuiltChunk builtChunk =
                    ((IEWorldRendererChunkInfo) obj).getBuiltChunk();
                
                BlockPos origin = builtChunk.getOrigin();
                int cx = origin.getX() >> 4;
                int cy = origin.getY() >> 4;
                int cz = origin.getZ() >> 4;
                
                return !(cx >= xMin && cx <= xMax &&
                    cy >= yMin && cy <= yMax &&
                    cz >= zMin && cz <= zMax);
            });
        }
    }
    
    @Override
    public boolean isFuseView() {
        return getFirstPortal().isFuseView();
    }
    
    @Override
    public boolean getDoRenderPlayer() {
        return getFirstPortal().getDoRenderPlayer();
    }
    
    @Override
    public boolean getHasCrossPortalCollision() {
        return getFirstPortal().getHasCrossPortalCollision();
    }
    
    @Override
    public String toString() {
        return String.format("PortalRenderingGroup(%s)%s", portals.size(), getFirstPortal().portalTag);
    }
    
    public boolean isEnclosed() {
        if (isEnclosedCache == null) {
            isEnclosedCache = portals.stream().allMatch(
                p -> p.getOriginPos().subtract(getOriginPos()).dotProduct(p.getNormal()) > 0.3
            );
        }
        
        return isEnclosedCache;
    }
    
    public Portal getFirstPortal() {
        return portals.get(0);
    }
    
    // if the portal is in group, return the group, otherwise itself
    public static PortalLike getPortalUnit(Portal portal) {
        PortalGroup group = PortalRenderInfo.getGroupOf(portal);
        if (group != null) {
            return group;
        }
        else {
            return portal;
        }
    }
}
