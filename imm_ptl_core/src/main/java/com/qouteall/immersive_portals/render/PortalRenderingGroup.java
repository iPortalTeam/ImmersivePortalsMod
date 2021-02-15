package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ducks.IEWorldRendererChunkInfo;
import com.qouteall.immersive_portals.my_util.BoxPredicate;
import com.qouteall.immersive_portals.my_util.LimitedLogger;
import com.qouteall.immersive_portals.my_util.Plane;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalLike;
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

@Environment(EnvType.CLIENT)
public class PortalRenderingGroup implements PortalLike {
    
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
    
    public PortalRenderingGroup(Portal.TransformationDesc transformationDesc) {
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
                    return portals.get(0).transformPoint(pos);
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
                Portal::getExactBoundingBox
            ).reduce(Box::union).get();
        }
        return exactBoundingBox;
    }
    
    @Override
    public Vec3d transformPoint(Vec3d pos) {
        return portals.get(0).transformPoint(pos);
    }
    
    @Override
    public Vec3d transformLocalVec(Vec3d localVec) {
        return portals.get(0).transformLocalVec(localVec);
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
        return portals.get(0).world;
    }
    
    @Override
    public World getDestWorld() {
        return portals.get(0).getDestWorld();
    }
    
    @Override
    public RegistryKey<World> getDestDim() {
        return portals.get(0).getDestDim();
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
        return portals.get(0).getAdditionalCameraTransformation();
    }
    
    @Nullable
    @Override
    public UUID getDiscriminator() {
        return uuid;
    }
    
    public void purge() {
        portals.removeIf(portal -> {
            return portal.removed;
        });
    }
    
    @Override
    public boolean cannotRenderInMe(Portal portal) {
        if (isEnclosed()) {
            if (!getDestAreaBox().intersects(portal.getExactAreaBox())) {
                return true;
            }
        }
        
        return portals.stream().anyMatch(p -> p.cannotRenderInMe(portal));
    }
    
    @Environment(EnvType.CLIENT)
    @Override
    public BoxPredicate getInnerFrustumCullingFunc(
        double innerCameraX, double innerCameraY, double innerCameraZ
    ) {
        Vec3d innerCameraPos = new Vec3d(innerCameraX, innerCameraY, innerCameraZ);
        Vec3d outerCameraPos = portals.get(0).inverseTransformPoint(innerCameraPos);
        
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
        
        Box enclosedDestAreaBox = getDestAreaBox();
        
        if (enclosedDestAreaBox != null) {
            int xMin = (int) Math.round(enclosedDestAreaBox.minX / 16);
            int xMax = (int) Math.round(enclosedDestAreaBox.maxX / 16) - 1;
            int yMin = (int) Math.round(enclosedDestAreaBox.minY / 16);
            int yMax = (int) Math.round(enclosedDestAreaBox.maxY / 16) - 1;
            int zMin = (int) Math.round(enclosedDestAreaBox.minZ / 16);
            int zMax = (int) Math.round(enclosedDestAreaBox.maxZ / 16) - 1;
            
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

//                return !builtChunk.boundingBox.intersects(enclosedDestAreaBox);
            });
        }
    }
    
    @Override
    public boolean isFuseView() {
        return portals.get(0).isFuseView();
    }
    
    @Override
    public String toString() {
        return String.format("PortalRenderingGroup(%s)%s", portals.size(), portals.get(0).portalTag);
    }
    
    public boolean isEnclosed() {
        if (isEnclosedCache == null) {
            isEnclosedCache = portals.stream().allMatch(
                p -> p.getOriginPos().subtract(getOriginPos()).dotProduct(p.getNormal()) > 0.3
            );
        }
        
        return isEnclosedCache;
    }
}
