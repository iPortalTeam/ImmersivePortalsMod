package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.my_util.Plane;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Consumer;

public interface PortalLike {
    boolean isConventionalPortal();
    
    // bounding box
    Box getExactAreaBox();
    
    Vec3d transformPoint(Vec3d pos);
    
    Vec3d transformLocalVec(Vec3d localVec);
    
    // TODO remove this and use the area box
    double getDistanceToNearestPointInPortal(
        Vec3d point
    );
    
    // TODO remove this and use the area box
    double getDestAreaRadiusEstimation();
    
    Vec3d getOriginPos();
    
    Vec3d getDestPos();
    
    World getOriginWorld();
    
    World getDestWorld();
    
    RegistryKey<World> getDestDim();
    
    boolean isRoughlyVisibleTo(Vec3d cameraPos);
    
    @Nullable
    Plane getInnerClipping();
    
    @Nullable
    Quaternion getRotation();
    
    double getScale();
    
    boolean getIsGlobal();
    
    // used for advanced frustum culling
    @Nullable
    Vec3d[] getInnerFrustumCullingVertices();
    
    // used for super advanced frustum culling
    @Nullable
    Vec3d[] getOuterFrustumCullingVertices();
    
    @Environment(EnvType.CLIENT)
    void renderViewAreaMesh(Vec3d posInPlayerCoordinate, Consumer<Vec3d> vertexOutput);
    
    // Scaling does not interfere camera transformation
    @Nullable
    Matrix4f getAdditionalCameraTransformation();
    
    @Nullable
    UUID getDiscriminator();
    
    default boolean hasScaling() {
        return getScale() != 1.0;
    }
    
    default RegistryKey<World> getOriginDim() {
        return getOriginWorld().getRegistryKey();
    }
    
    default boolean isInside(Vec3d entityPos, double valve) {
        Plane innerClipping = getInnerClipping();
        
        if (innerClipping == null) {
            return true;
        }
        
        double v = entityPos.subtract(innerClipping.pos).dotProduct(innerClipping.normal);
        return v > valve;
    }
}
