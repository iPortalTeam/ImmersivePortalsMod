package qouteall.imm_ptl.core.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.portal.PortalRenderInfo;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.BoxPredicate;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.LimitedLogger;
import qouteall.q_misc_util.my_util.Plane;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// currently only exists on client side
// TODO remove this in 1.21
@Environment(EnvType.CLIENT)
public class PortalGroup implements PortalLike {
    
    private static final LimitedLogger limitedLogger = new LimitedLogger(20);
    
    public final Portal.TransformationDesc transformationDesc;
    public final List<Portal> portals = new ArrayList<>();
    
    private AABB exactBoundingBox;
    private Vec3 origin;
    private Vec3 dest;
    
    @Nullable
    private AABB destAreaBoxCache = null;
    
    @Nullable
    private Boolean isEnclosedCache = null;
    
    private final UUID uuid = Mth.createInsecureUUID();
    
    public PortalGroup(Portal.TransformationDesc transformationDesc) {
        this.transformationDesc = transformationDesc;
    }
    
    public void addPortal(Portal portal) {
        Validate.isTrue(portal.level().isClientSide());
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
    
    public AABB getDestAreaBox() {
        if (destAreaBoxCache == null) {
            destAreaBoxCache = (
                Helper.transformBox(getThinBoundingBox(), pos -> {
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
    public AABB getThinBoundingBox() {
        if (exactBoundingBox == null) {
            exactBoundingBox = portals.stream().map(
                Portal::getThinBoundingBox
            ).reduce(AABB::minmax).get();
        }
        return exactBoundingBox;
    }
    
    @Override
    public Vec3 transformPoint(Vec3 pos) {
        return getFirstPortal().transformPoint(pos);
    }
    
    @Override
    public Vec3 transformLocalVec(Vec3 localVec) {
        return getFirstPortal().transformLocalVec(localVec);
    }
    
    @Override
    public Vec3 transformLocalVecNonScale(Vec3 localVec) {
        return getFirstPortal().transformLocalVecNonScale(localVec);
    }
    
    @Override
    public Vec3 inverseTransformLocalVec(Vec3 localVec) {
        return getFirstPortal().inverseTransformLocalVec(localVec);
    }
    
    @Override
    public Vec3 inverseTransformPoint(Vec3 point) {
        return getFirstPortal().inverseTransformPoint(point);
    }
    
    @Override
    public double getDistanceToNearestPointInPortal(Vec3 point) {
        return Helper.getDistanceToBox(getThinBoundingBox(), point);
    }
    
    @Override
    public double getDestAreaRadiusEstimation() {
        double maxDimension = getSizeEstimation();
        return maxDimension * transformationDesc.scaling();
    }
    
    
    @Override
    public Vec3 getOriginPos() {
        if (origin == null) {
            origin = getThinBoundingBox().getCenter();
        }
        
        return origin;
    }
    
    @Override
    public Vec3 getDestPos() {
        if (dest == null) {
            dest = transformPoint(getOriginPos());
        }
        
        return dest;
    }
    
    @Override
    public Level getOriginWorld() {
        return getFirstPortal().level();
    }
    
    @Override
    public Level getDestWorld() {
        return getFirstPortal().getDestWorld();
    }
    
    @Override
    public ResourceKey<Level> getDestDim() {
        return getFirstPortal().getDestDim();
    }
    
    @Environment(EnvType.CLIENT)
    @Override
    public boolean isRoughlyVisibleTo(Vec3 cameraPos) {
        return true;
    }
    
    @Nullable
    @Override
    public Plane getInnerClipping() {
        return null;
    }
    
    @Override
    public boolean isOnDestinationSide(Vec3 entityPos, double valve) {
        if (isEnclosed()) {
            return getDestAreaBox().contains(entityPos);
        }
        return true;
    }
    
    @Nullable
    @Override
    public DQuaternion getRotation() {
        return transformationDesc.rotation();
    }
    
    @Override
    public double getScale() {
        return transformationDesc.scaling();
    }
    
    @Override
    public boolean getIsGlobal() {
        return false;
    }
    
    @Override
    public boolean isVisible() {
        return true;
    }
    
    @Nullable
    @Override
    public Vec3[] getOuterFrustumCullingVertices() {
        return null;
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
            if (!getDestAreaBox().intersects(portal.getThinBoundingBox())) {
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
        Vec3 innerCameraPos = new Vec3(innerCameraX, innerCameraY, innerCameraZ);
        Vec3 outerCameraPos = getFirstPortal().inverseTransformPoint(innerCameraPos);
        
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
        return String.format(
            "PortalRenderingGroup(%s)(first:%s)",
            portals.size(), getFirstPortal()
        );
    }
    
    public boolean isEnclosed() {
        if (isEnclosedCache == null) {
            isEnclosedCache = portals.stream().allMatch(
                p -> p.getOriginPos().subtract(getOriginPos()).dot(p.getNormal()) > 0.3
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
    
    public boolean removeFromGroupIfNecessary(Portal portal) {
        if (portal == getFirstPortal()) {
            return false;
        }
        
        Portal.TransformationDesc desc = portal.getTransformationDesc();
        
        if (!Portal.TransformationDesc.isRoughlyEqual(transformationDesc, desc)) {
            removePortal(portal);
            return true;
        }
        
        return false;
    }
}
