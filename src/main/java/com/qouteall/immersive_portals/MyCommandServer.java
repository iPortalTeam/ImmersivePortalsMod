package com.qouteall.immersive_portals;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.SpecialPortalShape;
import com.qouteall.immersive_portals.portal.global_portals.BorderPortal;
import com.qouteall.immersive_portals.portal.global_portals.VerticalConnectingPortal;
import net.minecraft.command.arguments.DimensionArgumentType;
import net.minecraft.command.arguments.NbtCompoundTagArgumentType;
import net.minecraft.command.arguments.TextArgumentType;
import net.minecraft.command.arguments.Vec3ArgumentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.Comparator;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MyCommandServer {
    public static void register(
        CommandDispatcher<ServerCommandSource> dispatcher
    ) {
    
        LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager
            .literal("portal")
            .requires(commandSource -> commandSource.hasPermissionLevel(2));
    
        builder.then(CommandManager
            .literal("border_set")
            .then(CommandManager
                .argument("x1", IntegerArgumentType.integer())
                .then(CommandManager
                    .argument("y1", IntegerArgumentType.integer())
                    .then(CommandManager
                        .argument("x2", IntegerArgumentType.integer())
                        .then(CommandManager
                            .argument("y2", IntegerArgumentType.integer())
                            .executes(context -> {
                                BorderPortal.setBorderPortal(
                                    context.getSource().getWorld(),
                                    IntegerArgumentType.getInteger(context, "x1"),
                                    IntegerArgumentType.getInteger(context, "y1"),
                                    IntegerArgumentType.getInteger(context, "x2"),
                                    IntegerArgumentType.getInteger(context, "y2")
                                );
                                return 0;
                            })
                        )
                    )
                )
            )
        );
        builder.then(CommandManager
            .literal("border_remove")
            .executes(context -> {
                BorderPortal.removeBorderPortal(context.getSource().getWorld());
                return 0;
            })
        );
    
        builder.then(CommandManager
            .literal("view_portal_data")
            .executes(context -> {
                return processPortalTargetedCommand(
                    context,
                    (portal) -> {
                        sendPortalInfo(context, portal);
                    }
                );
            })
        );
    
        builder.then(CommandManager
            .literal("set_portal_custom_name")
            .then(CommandManager
                .argument(
                    "name",
                    TextArgumentType.text()
                ).executes(context -> {
                    return processPortalTargetedCommand(
                        context,
                        portal -> {
                            Text name = TextArgumentType.getTextArgument(context, "name");
                            portal.setCustomName(name);
                        }
                    );
                })
            )
        );
    
        builder.then(CommandManager
            .literal("delete_portal")
            .executes(context -> processPortalTargetedCommand(
                context,
                portal -> {
                    sendMessage(context, "deleted " + portal);
                    portal.remove();
                }
            ))
        );
    
        builder.then(CommandManager
            .literal("set_portal_nbt")
            .then(CommandManager
                .argument("nbt", NbtCompoundTagArgumentType.nbtCompound())
                .executes(context -> processPortalTargetedCommand(
                    context,
                    portal -> {
                        CompoundTag newNbt = NbtCompoundTagArgumentType.getCompoundTag(
                            context, "nbt"
                        );
    
                        CompoundTag portalNbt = portal.toTag(new CompoundTag());
    
                        newNbt.getKeys().forEach(
                            key -> portalNbt.put(key, newNbt.getTag(key))
                        );
    
                        //portalNbt.copyFrom(newNbt);
                        
                        UUID uuid = portal.getUuid();
                        portal.fromTag(portalNbt);
                        portal.setUuid(uuid);
    
                        reloadPortal(portal);
    
                        sendPortalInfo(context, portal);
                    }
                ))
            )
        );
    
        builder.then(CommandManager
            .literal("set_portal_destination")
            .then(
                CommandManager.argument(
                    "dim",
                    DimensionArgumentType.dimension()
                ).then(
                    CommandManager.argument(
                        "dest",
                        Vec3ArgumentType.vec3()
                    ).executes(
                        context -> processPortalTargetedCommand(
                            context,
                            portal -> {
                                try {
                                    portal.dimensionTo = DimensionArgumentType.getDimensionArgument(
                                        context, "dim"
                                    );
                                    portal.destination = Vec3ArgumentType.getVec3(
                                        context, "dest"
                                    );
    
                                    reloadPortal(portal);
    
                                    sendMessage(context, portal.toString());
                                }
                                catch (CommandSyntaxException ignored) {
                                    ignored.printStackTrace();
                                }
                            }
                        )
                    )
                )
            )
        );
    
    
        builder.then(CommandManager
            .literal("tpme")
            .then(
                CommandManager.argument(
                    "dim",
                    DimensionArgumentType.dimension()
                ).then(
                    CommandManager.argument(
                        "dest",
                        Vec3ArgumentType.vec3()
                    ).executes(
                        context -> {
                            DimensionType dimension = DimensionArgumentType.getDimensionArgument(
                                context, "dim"
                            );
                            Vec3d pos = Vec3ArgumentType.getVec3(
                                context, "dest"
                            );
                        
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            SGlobal.serverTeleportationManager.invokeTpmeCommand(
                                player, dimension, pos
                            );
                            return 0;
                        }
                    )
                )
            )
        );
    
        builder.then(CommandManager
            .literal("complete_bi_way_portal")
            .executes(context -> processPortalTargetedCommand(
                context,
                portal -> {
                    Portal result = completeBiWayPortal(
                        portal,
                        p -> sendMessage(context, "Removed " + p)
                    );
                    sendMessage(context, "Added " + result);
                }
            ))
        );
    
        builder.then(CommandManager
            .literal("complete_bi_faced_portal")
            .executes(context -> processPortalTargetedCommand(
                context,
                portal -> {
                    Portal result = completeBiFacedPortal(
                        portal,
                        p -> sendMessage(context, "Removed " + p)
                    );
                    sendMessage(context, "Added " + result);
                }
            ))
        );
    
        builder.then(CommandManager
            .literal("complete_bi_way_bi_faced_portal")
            .executes(context -> processPortalTargetedCommand(
                context,
                portal ->
                    completeBiWayBiFacedPortal(
                        portal,
                        p -> sendMessage(context, "Removed " + p),
                        p -> sendMessage(context, "Added " + p)
                    )
            ))
        );
    
        builder.then(CommandManager
            .literal("remove_connected_portals")
            .executes(context -> processPortalTargetedCommand(
                context,
                portal -> {
                    Consumer<Portal> removalInformer = p -> sendMessage(context, "Removed " + p);
                    removeOverlappedPortals(
                        portal.world,
                        portal.getPos(),
                        portal.getNormal().multiply(-1),
                        removalInformer
                    );
                    ServerWorld toWorld = McHelper.getServer().getWorld(portal.dimensionTo);
                    removeOverlappedPortals(
                        toWorld,
                        portal.destination,
                        portal.getNormal().multiply(-1),
                        removalInformer
                    );
                    removeOverlappedPortals(
                        toWorld,
                        portal.destination,
                        portal.getNormal(),
                        removalInformer
                    );
                }
            ))
        );
    
        builder.then(CommandManager
            .literal("connect_floor")
            .then(
                CommandManager.argument(
                    "from",
                    DimensionArgumentType.dimension()
                ).then(
                    CommandManager.argument(
                        "to",
                        DimensionArgumentType.dimension()
                    ).executes(
                        context -> {
                            DimensionType from = DimensionArgumentType.getDimensionArgument(
                                context, "from"
                            );
                            DimensionType to = DimensionArgumentType.getDimensionArgument(
                                context, "to"
                            );
                        
                            VerticalConnectingPortal.connect(
                                from, VerticalConnectingPortal.ConnectorType.floor, to
                            );
                            return 0;
                        }
                    )
                )
            )
        );
    
        builder.then(CommandManager
            .literal("connect_ceil")
            .then(
                CommandManager.argument(
                    "from",
                    DimensionArgumentType.dimension()
                ).then(
                    CommandManager.argument(
                        "to",
                        DimensionArgumentType.dimension()
                    ).executes(
                        context -> {
                            DimensionType from = DimensionArgumentType.getDimensionArgument(
                                context, "from"
                            );
                            DimensionType to = DimensionArgumentType.getDimensionArgument(
                                context, "to"
                            );
                        
                            VerticalConnectingPortal.connect(
                                from, VerticalConnectingPortal.ConnectorType.ceil, to
                            );
                            return 0;
                        }
                    )
                )
            )
        );
    
        builder.then(CommandManager
            .literal("connection_floor_remove")
            .then(
                CommandManager.argument(
                    "dim",
                    DimensionArgumentType.dimension()
                ).executes(
                    context -> {
                        DimensionType dim = DimensionArgumentType.getDimensionArgument(
                            context, "dim"
                        );
                    
                        VerticalConnectingPortal.removeConnectingPortal(
                            VerticalConnectingPortal.ConnectorType.floor, dim
                        );
                        return 0;
                    }
                )
            )
        );
    
        builder.then(CommandManager
            .literal("connection_ceil_remove")
            .then(
                CommandManager.argument(
                    "dim",
                    DimensionArgumentType.dimension()
                ).executes(
                    context -> {
                        DimensionType dim = DimensionArgumentType.getDimensionArgument(
                            context, "dim"
                        );
                    
                        VerticalConnectingPortal.removeConnectingPortal(
                            VerticalConnectingPortal.ConnectorType.ceil, dim
                        );
                        return 0;
                    }
                )
            )
        );
    
        dispatcher.register(builder);
    }
    
    public static void sendPortalInfo(CommandContext<ServerCommandSource> context, Portal portal) {
        context.getSource().sendFeedback(
            portal.toTag(new CompoundTag()).toText(),
            false
        );
        
        sendMessage(
            context,
            "\n\n" + portal.toString()
        );
    }
    
    public static void reloadPortal(Portal portal) {
        portal.remove();
        
        Helper.SimpleBox<Integer> counter = new Helper.SimpleBox<>(0);
        ModMain.serverTaskList.addTask(() -> {
            if (counter.obj < 3) {
                counter.obj++;
                return false;
            }
            portal.removed = false;
            portal.world.spawnEntity(portal);
            return true;
        });
    }
    
    public static void sendMessage(CommandContext<ServerCommandSource> context, String message) {
        context.getSource().sendFeedback(
            new LiteralText(
                message
            ),
            false
        );
    }
    
    public static int processPortalTargetedCommand(
        CommandContext<ServerCommandSource> context,
        Consumer<Portal> processCommand
    ) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendFeedback(
                new LiteralText("Only player can use this command"),
                true
            );
            return 0;
        }
        
        Portal portal = getPlayerPointingPortal(player);
        
        if (portal == null) {
            source.sendFeedback(
                new LiteralText("You are not pointing to any portal"),
                true
            );
            return 0;
        }
        else {
            processCommand.accept(portal);
        }
        return 0;
    }
    
    private static Portal getPlayerPointingPortal(
        ServerPlayerEntity player
    ) {
        Vec3d from = player.getCameraPosVec(1);
        Vec3d to = from.add(player.getRotationVector().multiply(100));
        Pair<Portal, Vec3d> result = McHelper.getEntitiesNearby(
            player,
            Portal.class,
            100
        ).map(
            portal -> new Pair<Portal, Vec3d>(
                portal, portal.rayTrace(from, to)
            )
        ).filter(
            portalAndHitPos -> portalAndHitPos.getSecond() != null
        ).min(
            Comparator.comparingDouble(
                portalAndHitPos -> portalAndHitPos.getSecond().squaredDistanceTo(from)
            )
        ).orElse(null);
        if (result != null) {
            return result.getFirst();
        }
        else {
            return null;
        }
    }
    
    
    private static Portal completeBiWayPortal(
        Portal portal, Consumer<Portal> removalInformer
    ) {
        ServerWorld toWorld = McHelper.getServer().getWorld(portal.dimensionTo);
        removeOverlappedPortals(
            toWorld,
            portal.destination,
            portal.getNormal().multiply(-1),
            removalInformer
        );
        
        Portal newPortal = Portal.entityType.create(toWorld);
        newPortal.dimensionTo = portal.dimension;
        newPortal.setPosition(portal.destination.x, portal.destination.y, portal.destination.z);
        newPortal.destination = portal.getPos();
        newPortal.loadFewerChunks = portal.loadFewerChunks;
        newPortal.specificPlayer = portal.specificPlayer;
        
        newPortal.width = portal.width;
        newPortal.height = portal.height;
        newPortal.axisW = portal.axisW;
        newPortal.axisH = portal.axisH.multiply(-1);
        
        if (portal.specialShape != null) {
            newPortal.specialShape = new SpecialPortalShape();
            initFlippedShape(newPortal, portal.specialShape);
        }
        
        toWorld.spawnEntity(newPortal);
        
        return newPortal;
    }
    
    private static Portal completeBiFacedPortal(
        Portal portal, Consumer<Portal> removalInformer
    ) {
        ServerWorld world = ((ServerWorld) portal.world);
        removeOverlappedPortals(
            world,
            portal.getPos(),
            portal.getNormal().multiply(-1),
            removalInformer
        );
        
        Portal newPortal = Portal.entityType.create(world);
        newPortal.dimensionTo = portal.dimensionTo;
        newPortal.setPosition(portal.x, portal.y, portal.z);
        newPortal.destination = portal.destination;
        newPortal.loadFewerChunks = portal.loadFewerChunks;
        newPortal.specificPlayer = portal.specificPlayer;
        
        newPortal.width = portal.width;
        newPortal.height = portal.height;
        newPortal.axisW = portal.axisW;
        newPortal.axisH = portal.axisH.multiply(-1);
        
        if (portal.specialShape != null) {
            newPortal.specialShape = new SpecialPortalShape();
            initFlippedShape(newPortal, portal.specialShape);
        }
        
        world.spawnEntity(newPortal);
        
        return newPortal;
    }
    
    private static void initFlippedShape(Portal newPortal, SpecialPortalShape specialShape) {
        newPortal.specialShape.triangles = specialShape.triangles.stream()
            .map(triangle -> new SpecialPortalShape.TriangleInPlane(
                triangle.x1,
                -triangle.y1,
                triangle.x2,
                -triangle.y2,
                triangle.x3,
                -triangle.y3
            )).collect(Collectors.toList());
    }
    
    private static void completeBiWayBiFacedPortal(
        Portal portal, Consumer<Portal> removalInformer,
        Consumer<Portal> addingInformer
    ) {
        Portal oppositeFacedPortal = completeBiFacedPortal(portal, removalInformer);
        Portal r1 = completeBiWayPortal(portal, removalInformer);
        Portal r2 = completeBiWayPortal(oppositeFacedPortal, removalInformer);
        addingInformer.accept(oppositeFacedPortal);
        addingInformer.accept(r1);
        addingInformer.accept(r2);
    }
    
    private static void removeOverlappedPortals(
        World world,
        Vec3d pos,
        Vec3d normal,
        Consumer<Portal> informer
    ) {
        world.getEntities(
            Portal.class,
            new Box(
                pos.add(0.5, 0.5, 0.5),
                pos.subtract(0.5, 0.5, 0.5)
            ),
            p -> p.getNormal().dotProduct(normal) > 0.5
        ).forEach(e -> {
            e.remove();
            informer.accept(e);
        });
    }
}
