package com.qouteall.immersive_portals.alternate_dimension;

import com.qouteall.immersive_portals.ModMain;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensionType;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class AlternateDimensionInit {
    public static void initMyDimensions() {
        ModMain.alternate1 = FabricDimensionType.builder()
            .factory((world, type) -> new AlternateDimension(
                world, type, AlternateDimension::getChunkGeneratorFixedRandomBiome,
                () -> ModMain.alternate1
            ))
            .skyLight(true)
            .defaultPlacer(
                (teleported, destination, portalDir, horizontalOffset, verticalOffset) ->
                    new BlockPattern.TeleportTarget(
                        Vec3d.ZERO,
                        Vec3d.ZERO,
                        0
                    )
            )
            .buildAndRegister(new Identifier("immersive_portals", "alternate1"));
        
        ModMain.alternate2 = FabricDimensionType.builder()
            .factory((world, type) -> new AlternateDimension(
                world, type, AlternateDimension::getChunkGeneratorNormalBiome,
                () -> ModMain.alternate2
            ))
            .skyLight(true)
            .defaultPlacer(
                (teleported, destination, portalDir, horizontalOffset, verticalOffset) ->
                    new BlockPattern.TeleportTarget(
                        Vec3d.ZERO,
                        Vec3d.ZERO,
                        0
                    )
            )
            .buildAndRegister(new Identifier("immersive_portals", "alternate2"));
    
        ModMain.alternate3 = FabricDimensionType.builder()
            .factory((world, type) -> new AlternateDimension(
                world, type, AlternateDimension::getChunkGeneratorSpecial,
                () -> ModMain.alternate3
            ))
            .skyLight(true)
            .defaultPlacer(
                (teleported, destination, portalDir, horizontalOffset, verticalOffset) ->
                    new BlockPattern.TeleportTarget(
                        Vec3d.ZERO,
                        Vec3d.ZERO,
                        0
                    )
            )
            .buildAndRegister(new Identifier("immersive_portals", "alternate3"));
    }
}
