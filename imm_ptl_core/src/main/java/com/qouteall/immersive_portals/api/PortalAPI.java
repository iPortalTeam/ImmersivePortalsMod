package com.qouteall.immersive_portals.api;

import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.my_util.DQuaternion;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class PortalAPI {
    
    public static void setPortalPositionShape(
        Portal portal,
        Vec3d position,
        DQuaternion orientation,
        double width, double height
    ) {
        portal.setOriginPos(position);
        portal.setSquareShape(
            orientation.rotate(new Vec3d(1, 0, 0)),
            orientation.rotate(new Vec3d(0, 1, 0)),
            width, height
        );
    }
    
    public static void setPortalTransformation(
        Portal portal,
        RegistryKey<World> destinationDimension,
        Vec3d destinationPosition,
        @Nullable DQuaternion rotation,
        double scale
    ) {
        portal.setDestinationDimension(destinationDimension);
        portal.setDestination(destinationPosition);
        portal.setRotationTransformation(rotation.toMcQuaternion());
        portal.setScaleTransformation(scale);
    }
    
    public static void spawnServerEntity(Entity entity) {
        McHelper.spawnServerEntity(entity);
    }
}
