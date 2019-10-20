package com.qouteall.immersive_portals;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_util.IntegerAABBInclusive;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.command.arguments.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class MyCommandServer {
    public static void register(
        CommandDispatcher<ServerCommandSource> dispatcher
    ) {
        LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager
            .literal("portal_wrap_wall")
            .requires(commandSource -> commandSource.hasPermissionLevel(2))
            .then(CommandManager
                .argument("from", BlockPosArgumentType.blockPos())
                .then(CommandManager
                    .argument("to", BlockPosArgumentType.blockPos())
                    .executes(context -> {
                        BlockPos from = BlockPosArgumentType.getLoadedBlockPos(context, "from");
                        BlockPos to = BlockPosArgumentType.getLoadedBlockPos(context, "to");
                        createWrappedWall(context.getSource().getWorld(), from, to);
                        return 0;
                    })
                )
            );
        
        dispatcher.register(builder);
        
        builder = CommandManager
            .literal("portal_wrap_cube")
            .requires(commandSource -> commandSource.hasPermissionLevel(2))
            .then(CommandManager
                .argument("from", BlockPosArgumentType.blockPos())
                .then(CommandManager
                    .argument("to", BlockPosArgumentType.blockPos())
                    .executes(context -> {
                        BlockPos from = BlockPosArgumentType.getLoadedBlockPos(context, "from");
                        BlockPos to = BlockPosArgumentType.getLoadedBlockPos(context, "to");
                        createWrappedCube(context.getSource().getWorld(), from, to);
                        return 0;
                    })
                )
            );
        
        dispatcher.register(builder);
    }
    
    public static void createWrappedWall(
        ServerWorld world,
        BlockPos from,
        BlockPos to
    ) {
        IntegerAABBInclusive box = new IntegerAABBInclusive(from, to).getSorted();
        Box area = box.toRealNumberBox();
        
        world.spawnEntity(createWrappingPortal(world, area, Direction.NORTH));
        world.spawnEntity(createWrappingPortal(world, area, Direction.SOUTH));
        world.spawnEntity(createWrappingPortal(world, area, Direction.WEST));
        world.spawnEntity(createWrappingPortal(world, area, Direction.EAST));
    }
    
    public static void createWrappedCube(
        ServerWorld world,
        BlockPos from,
        BlockPos to
    ) {
        IntegerAABBInclusive box = new IntegerAABBInclusive(from, to).getSorted();
        Box area = box.toRealNumberBox();
        
        world.spawnEntity(createWrappingPortal(world, area, Direction.NORTH));
        world.spawnEntity(createWrappingPortal(world, area, Direction.SOUTH));
        world.spawnEntity(createWrappingPortal(world, area, Direction.WEST));
        world.spawnEntity(createWrappingPortal(world, area, Direction.EAST));
        world.spawnEntity(createWrappingPortal(world, area, Direction.UP));
        world.spawnEntity(createWrappingPortal(world, area, Direction.DOWN));
    }
    
    public static Portal createWrappingPortal(
        ServerWorld serverWorld,
        Box area,
        Direction direction
    ) {
        Portal portal = new Portal(serverWorld);
        
        Vec3d areaSize = Helper.getBoxSize(area);
        
        Pair<Direction, Direction> axises = Helper.getPerpendicularDirections(
            direction
        );
        Box boxSurface = Helper.getBoxSurface(area, direction);
        Vec3d center = boxSurface.getCenter();
        Box oppositeSurface = Helper.getBoxSurface(area, direction.getOpposite());
        Vec3d destination = oppositeSurface.getCenter();
        portal.setPosition(center.x, center.y, center.z);
        portal.destination = destination;
        
        portal.axisW = new Vec3d(axises.getLeft().getVector());
        portal.axisH = new Vec3d(axises.getRight().getVector());
        portal.width = Helper.getCoordinate(areaSize, axises.getLeft().getAxis());
        portal.height = Helper.getCoordinate(areaSize, axises.getRight().getAxis());
        
        portal.dimensionTo = serverWorld.dimension.getType();
        
        return portal;
    }
}
