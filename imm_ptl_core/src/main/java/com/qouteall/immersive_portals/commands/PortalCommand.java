package com.qouteall.immersive_portals.commands;

import com.google.common.collect.Streams;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.api.example.ExampleGuiPortalRendering;
import com.qouteall.immersive_portals.my_util.IntBox;
import com.qouteall.immersive_portals.my_util.SignalBiArged;
import com.qouteall.immersive_portals.network.McRemoteProcedureCall;
import com.qouteall.immersive_portals.portal.GeometryPortalShape;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalManipulation;
import com.qouteall.immersive_portals.portal.global_portals.BorderBarrierFiller;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import com.qouteall.immersive_portals.portal.global_portals.VerticalConnectingPortal;
import com.qouteall.immersive_portals.portal.global_portals.WorldWrappingPortal;
import com.qouteall.immersive_portals.portal.nether_portal.BreakablePortalEntity;
import com.qouteall.immersive_portals.teleportation.ServerTeleportationManager;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.ColumnPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.NbtCompoundTagArgumentType;
import net.minecraft.command.argument.TextArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColumnPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.ProfilerSystem;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PortalCommand {
    // it needs to invoke the outer mod but the core does not have outer mod dependency
    public static final SignalBiArged<ServerPlayerEntity, String>
        createCommandStickCommandSignal = new SignalBiArged<>();
    
    public static void register(
        CommandDispatcher<ServerCommandSource> dispatcher
    ) {
        
        LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager
            .literal("portal")
            .requires(PortalCommand::canUsePortalCommand);
        
        registerPortalTargetedCommands(builder);
        
        registerCBPortalCommands(builder);
        
        registerUtilityCommands(builder);
        
        LiteralArgumentBuilder<ServerCommandSource> global =
            CommandManager.literal("global")
                .requires(commandSource -> commandSource.hasPermissionLevel(2));
        registerGlobalPortalCommands(global);
        builder.then(global);
        
        LiteralArgumentBuilder<ServerCommandSource> debugBuilder = CommandManager.literal("debug")
            .requires(PortalCommand::canUsePortalCommand);
        registerDebugCommands(debugBuilder);
        builder.then(debugBuilder);
        
        dispatcher.register(builder);
    }
    
    public static boolean canUsePortalCommand(ServerCommandSource commandSource) {
        Entity entity = commandSource.getEntity();
        if (entity instanceof ServerPlayerEntity) {
            if (Global.easeCreativePermission) {
                if (((ServerPlayerEntity) entity).isCreative()) {
                    return true;
                }
            }
        }
        
        return commandSource.hasPermissionLevel(2);
    }
    
    private static void registerDebugCommands(
        LiteralArgumentBuilder<ServerCommandSource> builder
    ) {
        
        builder.then(CommandManager
            .literal("gui_portal")
            .then(CommandManager.argument("dim", DimensionArgumentType.dimension())
                .then(CommandManager.argument("pos", Vec3ArgumentType.vec3(false))
                    .executes(context -> {
                        ExampleGuiPortalRendering.onCommandExecuted(
                            context.getSource().getPlayer(),
                            DimensionArgumentType.getDimensionArgument(context, "dim"),
                            Vec3ArgumentType.getVec3(context, "pos")
                        );
                        return 0;
                    })
                )
            ));
        
        builder.then(CommandManager
            .literal("isometric_enable")
            .then(CommandManager.argument("viewLength", FloatArgumentType.floatArg())
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    
                    float viewLength = FloatArgumentType.getFloat(context, "viewLength");
                    
                    McRemoteProcedureCall.tellClientToInvoke(
                        player,
                        "com.qouteall.immersive_portals.render.TransformationManager.RemoteCallables.enableIsometricView",
                        viewLength
                    );
                    
                    return 0;
                })
            )
        );
        
        builder.then(CommandManager
            .literal("isometric_disable")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                McRemoteProcedureCall.tellClientToInvoke(
                    player,
                    "com.qouteall.immersive_portals.render.TransformationManager.RemoteCallables.disableIsometricView"
                );
                return 0;
            })
        );
        
        builder.then(CommandManager
            .literal("align")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                
                Vec3d pos = player.getPos();
                
                Vec3d newPos = new Vec3d(
                    Math.round(pos.x * 2) / 2.0,
                    Math.round(pos.y * 2) / 2.0,
                    Math.round(pos.z * 2) / 2.0
                );
                
                player.networkHandler.requestTeleport(
                    newPos.x, newPos.y, newPos.z,
                    45, 30
                );
                
                return 0;
            })
        );
        
        builder.then(CommandManager
            .literal("set_profiler_logging_threshold")
            .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(4))
            .then(CommandManager.argument("ms", IntegerArgumentType.integer())
                .executes(context -> {
                    int ms = IntegerArgumentType.getInteger(context, "ms");
                    ProfilerSystem.TIMEOUT_NANOSECONDS = Duration.ofMillis(ms).toNanos();
                    
                    return 0;
                })
            )
        );
        
        builder.then(CommandManager
            .literal("create_command_stick")
            .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(2))
            .then(CommandManager.argument("command", StringArgumentType.string())
                .executes(context -> {
                    createCommandStickCommandSignal.emit(
                        context.getSource().getPlayer(),
                        StringArgumentType.getString(context, "command")
                    );
                    return 0;
                })
            )
        );
    }
    
    public static void registerClientDebugCommand(
        CommandDispatcher<ServerCommandSource> dispatcher
    ) {
        ClientDebugCommand.register(dispatcher);
    }
    
    private static void registerGlobalPortalCommands(
        LiteralArgumentBuilder<ServerCommandSource> builder
    ) {
        builder.then(CommandManager.literal("create_inward_wrapping")
            .then(CommandManager.argument("p1", ColumnPosArgumentType.columnPos())
                .then(CommandManager.argument("p2", ColumnPosArgumentType.columnPos())
                    .executes(context -> {
                        ColumnPos p1 = ColumnPosArgumentType.getColumnPos(context, "p1");
                        ColumnPos p2 = ColumnPosArgumentType.getColumnPos(context, "p2");
                        WorldWrappingPortal.invokeAddWrappingZone(
                            context.getSource().getWorld(),
                            p1.x, p1.z, p2.x, p2.z,
                            true,
                            text -> context.getSource().sendFeedback(text, false)
                        );
                        return 0;
                    })
                )
            )
        );
        
        builder.then(CommandManager.literal("create_outward_wrapping")
            .then(CommandManager.argument("p1", ColumnPosArgumentType.columnPos())
                .then(CommandManager.argument("p2", ColumnPosArgumentType.columnPos())
                    .executes(context -> {
                        ColumnPos p1 = ColumnPosArgumentType.getColumnPos(context, "p1");
                        ColumnPos p2 = ColumnPosArgumentType.getColumnPos(context, "p2");
                        WorldWrappingPortal.invokeAddWrappingZone(
                            context.getSource().getWorld(),
                            p1.x, p1.z, p2.x, p2.z,
                            false,
                            text -> context.getSource().sendFeedback(text, false)
                        );
                        return 0;
                    })
                )
            )
        );
        
        builder.then(CommandManager.literal("remove_wrapping_zone")
            .executes(context -> {
                WorldWrappingPortal.invokeRemoveWrappingZone(
                    context.getSource().getWorld(),
                    context.getSource().getPosition(),
                    text -> context.getSource().sendFeedback(text, false)
                );
                return 0;
            })
            .then(CommandManager.argument("id", IntegerArgumentType.integer())
                .executes(context -> {
                    int id = IntegerArgumentType.getInteger(context, "id");
                    WorldWrappingPortal.invokeRemoveWrappingZone(
                        context.getSource().getWorld(),
                        id,
                        text -> context.getSource().sendFeedback(text, false)
                    );
                    return 0;
                })
            )
        );
        
        builder.then(CommandManager.literal("view_wrapping_zones")
            .executes(context -> {
                WorldWrappingPortal.invokeViewWrappingZones(
                    context.getSource().getWorld(),
                    text -> context.getSource().sendFeedback(text, false)
                );
                return 0;
            })
        );
        
        builder.then(CommandManager.literal("clear_wrapping_border")
            .executes(context -> {
                BorderBarrierFiller.onCommandExecuted(
                    context.getSource().getPlayer()
                );
                return 0;
            })
            .then(CommandManager.argument("id", IntegerArgumentType.integer())
                .executes(context -> {
                    int id = IntegerArgumentType.getInteger(context, "id");
                    BorderBarrierFiller.onCommandExecuted(
                        context.getSource().getPlayer(),
                        id
                    );
                    return 0;
                })
            )
        );
        
        builder.then(CommandManager.literal("connect_floor")
            .then(CommandManager.argument("from", DimensionArgumentType.dimension())
                .then(
                    CommandManager.argument(
                        "to",
                        DimensionArgumentType.dimension()
                    ).executes(
                        context -> {
                            RegistryKey<World> from = DimensionArgumentType.getDimensionArgument(
                                context, "from"
                            ).getRegistryKey();
                            RegistryKey<World> to = DimensionArgumentType.getDimensionArgument(
                                context, "to"
                            ).getRegistryKey();
                            
                            VerticalConnectingPortal.connect(
                                from, VerticalConnectingPortal.ConnectorType.floor, to
                            );
                            return 0;
                        }
                    )
                )
            )
        );
        
        builder.then(CommandManager.literal("connect_ceil")
            .then(CommandManager.argument("from", DimensionArgumentType.dimension())
                .then(CommandManager.argument("to", DimensionArgumentType.dimension())
                    .executes(
                        context -> {
                            RegistryKey<World> from = DimensionArgumentType.getDimensionArgument(
                                context, "from"
                            ).getRegistryKey();
                            RegistryKey<World> to = DimensionArgumentType.getDimensionArgument(
                                context, "to"
                            ).getRegistryKey();
                            
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
            .then(CommandManager.argument("dim", DimensionArgumentType.dimension())
                .executes(
                    context -> {
                        RegistryKey<World> dim = DimensionArgumentType.getDimensionArgument(
                            context, "dim"
                        ).getRegistryKey();
                        
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
            .then(CommandManager.argument("dim", DimensionArgumentType.dimension())
                .executes(
                    context -> {
                        RegistryKey<World> dim = DimensionArgumentType.getDimensionArgument(
                            context, "dim"
                        ).getRegistryKey();
                        
                        VerticalConnectingPortal.removeConnectingPortal(
                            VerticalConnectingPortal.ConnectorType.ceil, dim
                        );
                        return 0;
                    }
                )
            )
        );
        
        builder.then(CommandManager.literal("view_global_portals")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                sendMessage(
                    context,
                    Helper.myToString(McHelper.getGlobalPortals(player.world).stream())
                );
                return 0;
            })
        );
        
        builder.then(CommandManager.literal("convert_normal_portal_to_global_portal")
            .executes(context -> processPortalTargetedCommand(
                context,
                GlobalPortalStorage::convertNormalPortalIntoGlobalPortal
            ))
        );
        
        builder.then(CommandManager.literal("convert_global_portal_to_normal_portal")
            .executes(context -> {
                final ServerPlayerEntity player = context.getSource().getPlayer();
                final Portal portal = getPlayerPointingPortal(player, true);
                
                if (portal == null) {
                    context.getSource().sendFeedback(
                        new LiteralText("You are not pointing to any portal"),
                        false
                    );
                    return 0;
                }
                
                if (!portal.getIsGlobal()) {
                    context.getSource().sendFeedback(
                        new LiteralText("You are not pointing to a global portal"),
                        false
                    );
                    return 0;
                }
                
                if (player.getPos().distanceTo(portal.getOriginPos()) > 64) {
                    context.getSource().sendFeedback(
                        new LiteralText("You are too far away from the portal's center " + portal),
                        false
                    );
                    return 0;
                }
                
                GlobalPortalStorage.convertGlobalPortalIntoNormalPortal(portal);
                
                return 0;
            })
        );
        
        builder.then(CommandManager.literal("delete_global_portal")
            .executes(context -> {
                final ServerPlayerEntity player = context.getSource().getPlayer();
                final Portal portal = getPlayerPointingPortal(player, true);
                
                if (portal == null) {
                    context.getSource().sendFeedback(
                        new LiteralText("You are not pointing to any portal"),
                        false
                    );
                    return 0;
                }
                
                if (!portal.getIsGlobal()) {
                    context.getSource().sendFeedback(
                        new LiteralText("You are not pointing to a global portal"),
                        false
                    );
                    return 0;
                }
                
                GlobalPortalStorage.get((ServerWorld) portal.world).removePortal(portal);
                
                return 0;
            })
        );
    }
    
    private static void registerPortalTargetedCommands(
        LiteralArgumentBuilder<ServerCommandSource> builder
    ) {
        builder.then(CommandManager.literal("view_portal_data")
            .executes(context -> processPortalTargetedCommand(
                context,
                (portal) -> {
                    sendPortalInfo(context, portal);
                }
            ))
        );
        
        builder.then(CommandManager.literal("set_portal_custom_name")
            .then(CommandManager
                .argument("name", TextArgumentType.text())
                .executes(context -> processPortalTargetedCommand(
                    context,
                    portal -> {
                        Text name = TextArgumentType.getTextArgument(context, "name");
                        portal.setCustomName(name);
                    }
                ))
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
        
        builder.then(CommandManager.literal("set_portal_nbt")
            .then(CommandManager.argument("nbt", NbtCompoundTagArgumentType.nbtCompound())
                .executes(context -> processPortalTargetedCommand(
                    context,
                    portal -> {
                        CompoundTag newNbt = NbtCompoundTagArgumentType.getCompoundTag(
                            context, "nbt"
                        );
                        
                        if (newNbt.contains("commandsOnTeleported")) {
                            if (!context.getSource().hasPermissionLevel(2)) {
                                context.getSource().sendError(new LiteralText(
                                    "You do not have the permission to set commandsOnTeleported"
                                ));
                                return;
                            }
                        }
                        
                        setPortalNbt(portal, newNbt);
                        
                        sendPortalInfo(context, portal);
                    }
                ))
            )
        );
        
        builder.then(CommandManager.literal("set_portal_destination")
            .then(CommandManager.argument("dim", DimensionArgumentType.dimension())
                .then(CommandManager.argument("dest", Vec3ArgumentType.vec3(false))
                    .executes(
                        context -> processPortalTargetedCommand(
                            context,
                            portal -> {
                                invokeSetPortalDestination(context, portal);
                            }
                        )
                    )
                )
            )
        );
        
        registerPortalTargetedCommandWithRotationArgument(
            builder, "set_portal_rotation",
            (p, r) -> {
                p.rotation = r;
            }
        );
        
        registerPortalTargetedCommandWithRotationArgument(
            builder, "rotate_portal_body",
            (p, r) -> {
                if (r != null) {
                    PortalManipulation.rotatePortalBody(p, r);
                }
            }
        );
        
        registerPortalTargetedCommandWithRotationArgument(
            builder, "rotate_portal_rotation",
            (portal, rot) -> {
                if (rot != null) {
                    if (portal.rotation == null) {
                        portal.rotation = rot;
                    }
                    else {
                        portal.rotation.hamiltonProduct(rot);
                    }
                }
            }
        );
        
        builder.then(CommandManager.literal("complete_bi_way_portal")
            .executes(context -> processPortalTargetedCommand(
                context,
                portal -> {
                    invokeCompleteBiWayPortal(context, portal);
                }
            ))
        );
        
        builder.then(CommandManager.literal("complete_bi_faced_portal")
            .executes(context -> processPortalTargetedCommand(
                context,
                portal -> {
                    invokeCompleteBiFacedPortal(context, portal);
                }
            ))
        );
        
        builder.then(CommandManager.literal("complete_bi_way_bi_faced_portal")
            .executes(context -> processPortalTargetedCommand(
                context,
                portal ->
                    invokeCompleteBiWayBiFacedPortal(context, portal)
            ))
        );
        
        builder.then(CommandManager.literal("remove_connected_portals")
            .executes(context -> processPortalTargetedCommand(
                context,
                portal -> {
                    PortalManipulation.removeConnectedPortals(
                        portal,
                        p -> sendMessage(context, "Removed " + p)
                    );
                }
            ))
        );
        
        builder.then(CommandManager.literal("eradicate_portal_clutter")
            .executes(context -> processPortalTargetedCommand(
                context,
                portal -> {
                    PortalManipulation.removeConnectedPortals(
                        portal,
                        p -> sendMessage(context, "Removed " + p)
                    );
                    portal.remove();
                    sendMessage(context, "Deleted " + portal);
                }
            ))
        );
        
        
        builder.then(CommandManager.literal("move_portal")
            .then(CommandManager.argument("distance", DoubleArgumentType.doubleArg())
                .executes(context -> processPortalTargetedCommand(
                    context, portal -> {
                        try {
                            double distance =
                                DoubleArgumentType.getDouble(context, "distance");
                            
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            Vec3d viewVector = player.getRotationVector();
                            Direction facing = Direction.getFacing(
                                viewVector.x, viewVector.y, viewVector.z
                            );
                            Vec3d offset = Vec3d.of(facing.getVector()).multiply(distance);
                            portal.updatePosition(
                                portal.getX() + offset.x,
                                portal.getY() + offset.y,
                                portal.getZ() + offset.z
                            );
                        }
                        catch (CommandSyntaxException e) {
                            sendMessage(context, "This command can only be invoked by player");
                        }
                    }
                ))
            )
        );
        
        builder.then(CommandManager.literal("move_portal_destination")
            .then(CommandManager.argument("distance", DoubleArgumentType.doubleArg())
                .executes(context -> processPortalTargetedCommand(
                    context, portal -> {
                        try {
                            double distance =
                                DoubleArgumentType.getDouble(context, "distance");
                            
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            Vec3d viewVector = player.getRotationVector();
                            Direction facing = Direction.getFacing(
                                viewVector.x, viewVector.y, viewVector.z
                            );
                            Vec3d offset = Vec3d.of(facing.getVector()).multiply(distance);
                            
                            portal.setDestination(portal.getDestPos().add(
                                portal.transformLocalVecNonScale(offset)
                            ));
                            portal.reloadAndSyncToClient();
                        }
                        catch (CommandSyntaxException e) {
                            sendMessage(context, "This command can only be invoked by player");
                        }
                    }
                ))
            )
        );
        
        
        builder.then(CommandManager.literal("set_portal_specific_accessor")
            .executes(context -> processPortalTargetedCommand(
                context,
                portal -> {
                    removeSpecificAccessor(context, portal);
                }
            ))
            .then(CommandManager.argument("player", EntityArgumentType.player())
                .executes(context -> processPortalTargetedCommand(
                    context,
                    portal -> {
                        setSpecificAccessor(context, portal,
                            EntityArgumentType.getEntity(context, "player")
                        );
                    }
                ))
            )
        );
        
        
        builder.then(CommandManager.literal("multidest")
            .then(CommandManager.argument("player", EntityArgumentType.player())
                .executes(context -> processPortalTargetedCommand(
                    context,
                    portal -> {
                        removeMultidestEntry(
                            context, portal, EntityArgumentType.getPlayer(context, "player")
                        );
                    }
                ))
                .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
                    .then(CommandManager.argument("destination", Vec3ArgumentType.vec3(false))
                        .then(CommandManager.argument("isBiFaced", BoolArgumentType.bool())
                            .then(CommandManager.argument("isBiWay", BoolArgumentType.bool())
                                .executes(context -> processPortalTargetedCommand(
                                    context,
                                    portal -> {
                                        setMultidestEntry(
                                            context,
                                            portal,
                                            EntityArgumentType.getPlayer(context, "player"),
                                            DimensionArgumentType.getDimensionArgument(
                                                context,
                                                "dimension"
                                            ).getRegistryKey(),
                                            Vec3ArgumentType.getVec3(context, "destination"),
                                            BoolArgumentType.getBool(context, "isBiFaced"),
                                            BoolArgumentType.getBool(context, "isBiWay")
                                        );
                                    }
                                ))
                            )
                        )
                    )
                )
            )
        );
        
        builder.then(CommandManager.literal("make_portal_round")
            .executes(context -> processPortalTargetedCommand(
                context,
                portal -> {
                    makePortalRound(portal);
                    portal.reloadAndSyncToClient();
                }
            ))
        );
        
        builder.then(CommandManager.literal("set_portal_scale")
            .then(CommandManager.argument("scale", DoubleArgumentType.doubleArg())
                .executes(context -> processPortalTargetedCommand(
                    context, portal -> {
                        double scale = DoubleArgumentType.getDouble(context, "scale");
                        
                        portal.scaling = scale;
                        
                        portal.reloadAndSyncToClient();
                    }
                ))
            )
        );
        
        builder.then(CommandManager.literal("set_portal_destination_to")
            .then(CommandManager.argument("entity", EntityArgumentType.entity())
                .executes(context -> processPortalTargetedCommand(context, portal -> {
                    Entity entity = EntityArgumentType.getEntity(context, "entity");
                    portal.dimensionTo = entity.world.getRegistryKey();
                    portal.setDestination(entity.getPos());
                    portal.reloadAndSyncToClient();
                }))
            )
        );
        
        builder.then(CommandManager.literal("set_portal_position")
            .then(CommandManager.argument("dim", DimensionArgumentType.dimension())
                .then(CommandManager.argument("pos", Vec3ArgumentType.vec3(false))
                    .executes(context -> processPortalTargetedCommand(context, portal -> {
                        invokeSetPortalLocation(context, portal);
                    }))
                )
            )
        );
    }
    
    private static void registerPortalTargetedCommandWithRotationArgument(
        LiteralArgumentBuilder<ServerCommandSource> builder,
        String literal,
        BiConsumer<Portal, Quaternion> func
    ) {
        builder.then(CommandManager.literal(literal)
            .then(CommandManager.argument("rotatingAxis", Vec3ArgumentType.vec3(false))
                .then(CommandManager.argument("angleDegrees", DoubleArgumentType.doubleArg())
                    .executes(context -> processPortalTargetedCommand(
                        context,
                        portal -> {
                            Vec3d rotatingAxis = Vec3ArgumentType.getVec3(
                                context, "rotatingAxis"
                            ).normalize();
                            
                            double angleDegrees = DoubleArgumentType.getDouble(
                                context, "angleDegrees"
                            );
                            
                            Quaternion rot = angleDegrees != 0 ? new Quaternion(
                                new Vector3f(rotatingAxis),
                                (float) angleDegrees,
                                true
                            ) : null;
                            
                            func.accept(portal, rot);
                            
                            portal.reloadAndSyncToClient();
                        }
                    ))
                )
            )
        );
        
        builder.then(CommandManager.literal(literal + "_along")
            .then(CommandManager.literal("x")
                .then(CommandManager.argument("angleDegrees", DoubleArgumentType.doubleArg())
                    .executes(context -> processPortalTargetedCommand(
                        context,
                        portal -> {
                            Vector3f axis = new Vector3f(1, 0, 0);
                            
                            double angleDegrees =
                                DoubleArgumentType.getDouble(context, "angleDegrees");
                            
                            Quaternion rot = angleDegrees != 0 ? new Quaternion(
                                axis,
                                (float) angleDegrees,
                                true
                            ) : null;
                            
                            func.accept(portal, rot);
                            
                            portal.reloadAndSyncToClient();
                        }
                    ))
                )
            )
            .then(CommandManager.literal("y")
                .then(CommandManager.argument("angleDegrees", DoubleArgumentType.doubleArg())
                    .executes(context -> processPortalTargetedCommand(
                        context,
                        portal -> {
                            Vector3f axis = new Vector3f(0, 1, 0);
                            
                            
                            double angleDegrees =
                                DoubleArgumentType.getDouble(context, "angleDegrees");
                            
                            Quaternion rot = angleDegrees != 0 ? new Quaternion(
                                axis,
                                (float) angleDegrees,
                                true
                            ) : null;
                            
                            func.accept(portal, rot);
                            
                            portal.reloadAndSyncToClient();
                        }
                    ))
                )
            )
            .then(CommandManager.literal("z")
                .then(CommandManager.argument("angleDegrees", DoubleArgumentType.doubleArg())
                    .executes(context -> processPortalTargetedCommand(
                        context,
                        portal -> {
                            Vector3f axis = new Vector3f(0, 0, 1);
                            
                            
                            double angleDegrees =
                                DoubleArgumentType.getDouble(context, "angleDegrees");
                            
                            Quaternion rot = angleDegrees != 0 ? new Quaternion(
                                axis,
                                (float) angleDegrees,
                                true
                            ) : null;
                            
                            func.accept(portal, rot);
                            
                            portal.reloadAndSyncToClient();
                        }
                    ))
                )
            )
        );
    }
    
    private static void setPortalRotation(Portal portal, Vector3f axis, double angleDegrees) {
        if (angleDegrees != 0) {
            portal.rotation = new Quaternion(
                axis, (float) angleDegrees, true
            );
        }
        else {
            portal.rotation = null;
        }
        
        portal.reloadAndSyncToClient();
    }
    
    private static void setPortalNbt(Portal portal, CompoundTag newNbt) {
        CompoundTag portalNbt = portal.toTag(new CompoundTag());
        
        newNbt.getKeys().forEach(
            key -> portalNbt.put(key, newNbt.get(key))
        );
        
        UUID uuid = portal.getUuid();
        portal.fromTag(portalNbt);
        portal.setUuid(uuid);
        
        portal.reloadAndSyncToClient();
    }
    
    private static void registerCBPortalCommands(
        LiteralArgumentBuilder<ServerCommandSource> builder
    ) {
        builder.then(CommandManager.literal("cb_set_portal_destination")
            .then(CommandManager.argument("portal", EntityArgumentType.entities())
                .then(CommandManager.argument("dim", DimensionArgumentType.dimension())
                    .then(CommandManager.argument("dest", Vec3ArgumentType.vec3(false))
                        .executes(
                            context -> processPortalArgumentedCBCommand(
                                context,
                                (portal) -> invokeSetPortalDestination(context, portal)
                            )
                        )
                    )
                )
            )
        );
        
        builder.then(CommandManager.literal("cb_complete_bi_way_portal")
            .then(CommandManager.argument("portal", EntityArgumentType.entities())
                .executes(context -> processPortalArgumentedCBCommand(
                    context,
                    portal -> {
                        invokeCompleteBiWayPortal(context, portal);
                    }
                ))
            )
        );
        
        
        builder.then(CommandManager.literal("cb_complete_bi_faced_portal")
            .then(CommandManager.argument("portal", EntityArgumentType.entities())
                .executes(context -> processPortalArgumentedCBCommand(
                    context,
                    portal -> {
                        invokeCompleteBiFacedPortal(context, portal);
                    }
                ))
            )
        );
        
        builder.then(CommandManager.literal("cb_complete_bi_way_bi_faced_portal")
            .then(CommandManager.argument("portal", EntityArgumentType.entities())
                .executes(context -> processPortalArgumentedCBCommand(
                    context,
                    portal -> {
                        invokeCompleteBiWayBiFacedPortal(context, portal);
                    }
                ))
            )
        );
        
        
        builder.then(CommandManager.literal("cb_remove_connected_portals")
            .then(CommandManager.argument("portal", EntityArgumentType.entities())
                .executes(context -> processPortalArgumentedCBCommand(
                    context,
                    portal -> {
                        PortalManipulation.removeConnectedPortals(
                            portal,
                            p -> sendMessage(context, "Removed " + p)
                        );
                    }
                ))
            )
        );
        
        
        builder.then(CommandManager.literal("cb_set_portal_specific_accessor")
            .then(CommandManager.argument("portal", EntityArgumentType.entities())
                .executes(context -> {
                    EntityArgumentType.getEntities(context, "portal")
                        .stream().filter(e -> e instanceof Portal)
                        .forEach(p -> removeSpecificAccessor(context, ((Portal) p)));
                    return 0;
                })
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(context -> {
                        Entity player = EntityArgumentType.getEntity(context, "player");
                        EntityArgumentType.getEntities(context, "portal")
                            .stream().filter(e -> e instanceof Portal)
                            .forEach(p -> {
                                setSpecificAccessor(context, ((Portal) p), player);
                            });
                        return 0;
                    })
                )
            )
        );
        
        builder.then(CommandManager.literal("cb_make_portal")
            .then(CommandManager.argument("width", DoubleArgumentType.doubleArg())
                .then(CommandManager.argument("height", DoubleArgumentType.doubleArg())
                    .then(CommandManager.argument("from", EntityArgumentType.entity())
                        .then(CommandManager.argument("to", EntityArgumentType.entity())
                            .executes(context -> {
                                double width = DoubleArgumentType.getDouble(context, "width");
                                double height = DoubleArgumentType.getDouble(context, "height");
                                
                                Entity fromEntity = EntityArgumentType.getEntity(context, "from");
                                Entity toEntity = EntityArgumentType.getEntity(context, "to");
                                
                                Portal portal = Portal.entityType.create(fromEntity.world);
                                
                                portal.updatePosition(fromEntity.getX(), fromEntity.getY(), fromEntity.getZ());
                                
                                portal.dimensionTo = toEntity.world.getRegistryKey();
                                portal.setDestination(toEntity.getPos());
                                portal.width = width;
                                portal.height = height;
                                
                                Vec3d normal = fromEntity.getRotationVec(1);
                                Vec3d rightVec = getRightVec(fromEntity);
                                
                                Vec3d axisH = rightVec.crossProduct(normal).normalize();
                                
                                portal.axisW = rightVec;
                                portal.axisH = axisH;
                                
                                McHelper.spawnServerEntity(portal);
                                
                                return 0;
                            })
                        )
                    )
                )
            )
        );
        builder.then(CommandManager.literal("cb_set_portal_nbt")
            .then(CommandManager.argument("portal", EntityArgumentType.entities())
                .then(CommandManager.argument("nbt", NbtCompoundTagArgumentType.nbtCompound())
                    .executes(context -> processPortalArgumentedCBCommand(
                        context,
                        (portal) -> invokeSetPortalNBT(context, portal)
                        )
                    )
                )
            )
        );
    }
    
    private static void registerUtilityCommands(
        LiteralArgumentBuilder<ServerCommandSource> builder
    ) {
        builder.then(CommandManager.literal("tpme")
            .then(CommandManager.argument("target", EntityArgumentType.entity())
                .executes(context -> {
                    Entity entity = EntityArgumentType.getEntity(context, "target");
                    
                    Global.serverTeleportationManager.invokeTpmeCommand(
                        context.getSource().getPlayer(),
                        entity.world.getRegistryKey(),
                        entity.getPos()
                    );
                    
                    context.getSource().sendFeedback(
                        new TranslatableText(
                            "imm_ptl.command.tpme.success",
                            entity.getDisplayName()
                        ),
                        true
                    );
                    
                    return 1;
                })
            )
            .then(CommandManager.argument("dest", Vec3ArgumentType.vec3())
                .executes(context -> {
                    Vec3d dest = Vec3ArgumentType.getVec3(context, "dest");
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    
                    Global.serverTeleportationManager.invokeTpmeCommand(
                        player,
                        player.world.getRegistryKey(),
                        dest
                    );
                    
                    context.getSource().sendFeedback(
                        new TranslatableText(
                            "imm_ptl.command.tpme.success",
                            dest.toString()
                        ),
                        true
                    );
                    
                    return 1;
                })
            )
            .then(CommandManager.argument("dim", DimensionArgumentType.dimension())
                .then(CommandManager.argument("dest", Vec3ArgumentType.vec3())
                    .executes(context -> {
                        RegistryKey<World> dim = DimensionArgumentType.getDimensionArgument(
                            context,
                            "dim"
                        ).getRegistryKey();
                        Vec3d dest = Vec3ArgumentType.getVec3(context, "dest");
                        
                        Global.serverTeleportationManager.invokeTpmeCommand(
                            context.getSource().getPlayer(),
                            dim,
                            dest
                        );
                        
                        context.getSource().sendFeedback(
                            new TranslatableText(
                                "imm_ptl.command.tpme.success",
                                McHelper.dimensionTypeId(dim).toString() + dest.toString()
                            ),
                            true
                        );
                        
                        return 1;
                    })
                )
            )
        );
        
        builder.then(CommandManager.literal("tp")
            .requires(commandSource -> commandSource.hasPermissionLevel(2))
            .then(CommandManager.argument("from", EntityArgumentType.entities())
                .then(CommandManager.argument("to", EntityArgumentType.entity())
                    .executes(context -> {
                        Collection<? extends Entity> entities =
                            EntityArgumentType.getEntities(context, "from");
                        Entity target = EntityArgumentType.getEntity(context, "to");
                        
                        int numTeleported = teleport(
                            entities,
                            target.world.getRegistryKey(),
                            target.getPos()
                        );
                        
                        context.getSource().sendFeedback(
                            new TranslatableText(
                                "imm_ptl.command.tp.success",
                                numTeleported,
                                target.getDisplayName()
                            ),
                            true
                        );
                        
                        return numTeleported;
                    })
                )
                .then(CommandManager.argument("dest", Vec3ArgumentType.vec3())
                    .executes(context -> {
                        Collection<? extends Entity> entities =
                            EntityArgumentType.getEntities(context, "from");
                        Vec3d dest = Vec3ArgumentType.getVec3(context, "dest");
                        
                        int numTeleported = teleport(
                            entities,
                            context.getSource().getWorld().getRegistryKey(),
                            dest
                        );
                        
                        context.getSource().sendFeedback(
                            new TranslatableText(
                                "imm_ptl.command.tp.success",
                                numTeleported,
                                dest.toString()
                            ),
                            true
                        );
                        
                        return numTeleported;
                    })
                )
                .then(CommandManager.argument("dim", DimensionArgumentType.dimension())
                    .then(CommandManager.argument("dest", Vec3ArgumentType.vec3())
                        .executes(context -> {
                            Collection<? extends Entity> entities =
                                EntityArgumentType.getEntities(context, "from");
                            RegistryKey<World> dim = DimensionArgumentType.getDimensionArgument(
                                context,
                                "dim"
                            ).getRegistryKey();
                            Vec3d dest = Vec3ArgumentType.getVec3(context, "dest");
                            
                            int numTeleported = teleport(
                                entities,
                                context.getSource().getWorld().getRegistryKey(),
                                dest
                            );
                            
                            context.getSource().sendFeedback(
                                new TranslatableText(
                                    "imm_ptl.command.tp.success",
                                    numTeleported,
                                    McHelper.dimensionTypeId(dim).toString() + dest.toString()
                                ),
                                true
                            );
                            
                            return numTeleported;
                        })
                    )
                )
            )
        );
        
        
        // By LoganDark :D
        builder.then(CommandManager.literal("make_portal")
            .then(CommandManager.argument("width", DoubleArgumentType.doubleArg())
                .then(CommandManager.argument("height", DoubleArgumentType.doubleArg())
                    .then(CommandManager.argument("to", DimensionArgumentType.dimension())
                        .then(CommandManager.argument("dest", Vec3ArgumentType.vec3(false))
                            .executes(PortalCommand::placePortalAbsolute)
                        )
                        .then(CommandManager.literal("shift")
                            .then(CommandManager.argument("dist", DoubleArgumentType.doubleArg())
                                .executes(PortalCommand::placePortalShift)
                            )
                        )
                    )
                )
            )
        );
        
        builder.then(CommandManager.literal("goback")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                net.minecraft.util.Pair<RegistryKey<World>, Vec3d> lastPos =
                    Global.serverTeleportationManager.lastPosition.get(player);
                if (lastPos == null) {
                    sendMessage(context, "You haven't teleported");
                }
                else {
                    Global.serverTeleportationManager.invokeTpmeCommand(
                        player, lastPos.getLeft(), lastPos.getRight()
                    );
                }
                return 0;
            })
        );
        
        builder.then(CommandManager.literal("create_small_inward_wrapping")
            .then(CommandManager.argument("p1", Vec3ArgumentType.vec3(false))
                .then(CommandManager.argument("p2", Vec3ArgumentType.vec3(false))
                    .executes(context -> {
                        Vec3d p1 = Vec3ArgumentType.getVec3(context, "p1");
                        Vec3d p2 = Vec3ArgumentType.getVec3(context, "p2");
                        Box box = new Box(p1, p2);
                        ServerWorld world = context.getSource().getWorld();
                        addSmallWorldWrappingPortals(box, world, true);
                        return 0;
                    })
                )
            )
        );
        
        builder.then(CommandManager.literal("create_small_outward_wrapping")
            .then(CommandManager.argument("p1", Vec3ArgumentType.vec3(false))
                .then(CommandManager.argument("p2", Vec3ArgumentType.vec3(false))
                    .executes(context -> {
                        Vec3d p1 = Vec3ArgumentType.getVec3(context, "p1");
                        Vec3d p2 = Vec3ArgumentType.getVec3(context, "p2");
                        Box box = new Box(p1, p2);
                        ServerWorld world = context.getSource().getWorld();
                        addSmallWorldWrappingPortals(box, world, false);
                        return 0;
                    })
                )
            )
        );
        
        builder.then(CommandManager.literal("create_scaled_box_view")
            .then(CommandManager.argument("p1", BlockPosArgumentType.blockPos())
                .then(CommandManager.argument("p2", BlockPosArgumentType.blockPos())
                    .then(CommandManager.argument("scale", DoubleArgumentType.doubleArg())
                        .then(CommandManager.argument("placeTargetEntity", EntityArgumentType.entity())
                            .then(CommandManager.argument("biWay", BoolArgumentType.bool())
                                .executes(context -> {
                                    invokeCreateScaledViewCommand(
                                        context,
                                        false,
                                        false,
                                        false,
                                        false,
                                        BoolArgumentType.getBool(context, "biWay")
                                    );
                                    return 0;
                                })
                                .then(CommandManager.argument("teleportChangesScale", BoolArgumentType.bool())
                                    .executes(context -> {
                                        boolean teleportChangesScale = BoolArgumentType.getBool(context, "teleportChangesScale");
                                        invokeCreateScaledViewCommand(
                                            context,
                                            teleportChangesScale,
                                            false,
                                            false,
                                            false,
                                            BoolArgumentType.getBool(context, "biWay")
                                        );
                                        return 0;
                                    })
                                )
                            )
                        )
                    )
                )
            )
        );
        
        builder.then(CommandManager.literal("create_scaled_box_view_optimized")
            .then(CommandManager.argument("p1", BlockPosArgumentType.blockPos())
                .then(CommandManager.argument("p2", BlockPosArgumentType.blockPos())
                    .then(CommandManager.argument("scale", DoubleArgumentType.doubleArg())
                        .then(CommandManager.argument("placeTargetEntity", EntityArgumentType.entity())
                            .executes(context -> {
                                invokeCreateScaledViewCommand(
                                    context,
                                    false,
                                    true,
                                    true,
                                    true,
                                    true
                                );
                                return 0;
                            })
                        )
                    )
                )
            )
        );
        
        builder.then(CommandManager
            .literal("wiki")
            .executes(context -> {
                McRemoteProcedureCall.tellClientToInvoke(
                    context.getSource().getPlayer(),
                    "com.qouteall.imm_ptl_peripheral.guide.IPGuide.RemoteCallables.showWiki"
                );
                return 0;
            })
        );
    }
    
    
    private static void invokeCreateScaledViewCommand(
        CommandContext<ServerCommandSource> context,
        boolean teleportChangesScale,
        boolean outerFuseView,
        boolean outerRenderingMergable,
        boolean innerRenderingMergable,
        boolean isBiWay
    ) throws CommandSyntaxException {
        
        BlockPos bp1 = BlockPosArgumentType.getBlockPos(context, "p1");
        BlockPos bp2 = BlockPosArgumentType.getBlockPos(context, "p2");
        IntBox intBox = new IntBox(bp1, bp2);
        
        Entity placeTargetEntity =
            EntityArgumentType.getEntity(context, "placeTargetEntity");
        
        ServerWorld boxWorld = ((ServerWorld) placeTargetEntity.world);
        Vec3d boxBottomCenter = placeTargetEntity.getPos();
        Box area = intBox.toRealNumberBox();
        ServerWorld areaWorld = context.getSource().getWorld();
        
        double scale = DoubleArgumentType.getDouble(context, "scale");
        
        
        PortalManipulation.createScaledBoxView(
            areaWorld, area, boxWorld, boxBottomCenter, scale,
            isBiWay, teleportChangesScale,
            outerFuseView, outerRenderingMergable, innerRenderingMergable,
            false
        );
    }
    
    
    private static void addSmallWorldWrappingPortals(Box box, ServerWorld world, boolean isInward) {
        for (Direction direction : Direction.values()) {
            Portal portal = Portal.entityType.create(world);
            WorldWrappingPortal.initWrappingPortal(
                world, box, direction, isInward, portal
            );
            McHelper.spawnServerEntity(portal);
        }
    }
    
    private static int processPortalArgumentedCBCommand(
        CommandContext<ServerCommandSource> context,
        PortalConsumerThrowsCommandSyntaxException invoker
    ) throws CommandSyntaxException {
        Collection<? extends Entity> entities = EntityArgumentType.getEntities(
            context, "portal"
        );
        
        for (Entity portalEntity : entities) {
            if (portalEntity instanceof Portal) {
                Portal portal = (Portal) portalEntity;
                
                invoker.accept(portal);
            }
            else {
                sendMessage(context, "The target should be portal");
            }
        }
        
        return 0;
    }
    
    private static void invokeSetPortalDestination(
        CommandContext<ServerCommandSource> context,
        Portal portal
    ) throws CommandSyntaxException {
        portal.dimensionTo = DimensionArgumentType.getDimensionArgument(
            context, "dim"
        ).getRegistryKey();
        portal.setDestination(Vec3ArgumentType.getVec3(
            context, "dest"
        ));
        
        portal.reloadAndSyncToClient();
        
        sendMessage(context, portal.toString());
        
        if (portal instanceof BreakablePortalEntity) {
            if (!((BreakablePortalEntity) portal).unbreakable) {
                sendMessage(context, "You are editing a breakable portal." +
                    " If the breakable portal entity is wrongly linked, it will automatically break." +
                    " To avoid that, use command /portal set_portal_nbt {unbreakable:true} for 4 portal entities."
                );
            }
        }
    }
    
    private static void invokeSetPortalLocation(
        CommandContext<ServerCommandSource> context,
        Portal portal
    ) throws CommandSyntaxException {
        ServerWorld targetWorld =
            DimensionArgumentType.getDimensionArgument(context, "dim");
        
        Vec3d pos = Vec3ArgumentType.getVec3(context, "pos");
        
        ServerTeleportationManager.teleportEntityGeneral(
            portal, pos, targetWorld
        );
        
        sendMessage(context, portal.toString());
        
    }
    
    private static void invokeCompleteBiWayBiFacedPortal(
        CommandContext<ServerCommandSource> context,
        Portal portal
    ) {
        PortalManipulation.completeBiWayBiFacedPortal(
            portal,
            p -> sendMessage(context, "Removed " + p),
            p -> sendMessage(context, "Added " + p), Portal.entityType
        );
    }
    
    private static void invokeCompleteBiFacedPortal(
        CommandContext<ServerCommandSource> context,
        Portal portal
    ) {
        PortalManipulation.removeOverlappedPortals(
            ((ServerWorld) portal.world),
            portal.getOriginPos(),
            portal.getNormal().multiply(-1),
            p -> Objects.equals(portal.specificPlayerId, p.specificPlayerId),
            p -> sendMessage(context, "Removed " + p)
        );
        
        Portal result = PortalManipulation.completeBiFacedPortal(
            portal,
            Portal.entityType
        );
        sendMessage(context, "Added " + result);
    }
    
    private static void invokeCompleteBiWayPortal(
        CommandContext<ServerCommandSource> context,
        Portal portal
    ) {
        PortalManipulation.removeOverlappedPortals(
            McHelper.getServer().getWorld(portal.dimensionTo),
            portal.getDestPos(),
            portal.transformLocalVecNonScale(portal.getNormal().multiply(-1)),
            p -> Objects.equals(portal.specificPlayerId, p.specificPlayerId),
            p -> sendMessage(context, "Removed " + p)
        );
        
        Portal result = PortalManipulation.completeBiWayPortal(
            portal,
            Portal.entityType
        );
        sendMessage(context, "Added " + result);
    }
    
    private static void removeSpecificAccessor(
        CommandContext<ServerCommandSource> context,
        Portal portal
    ) {
        portal.specificPlayerId = null;
        sendMessage(context, "This portal can be accessed by all players now");
        sendMessage(context, portal.toString());
    }
    
    private static void setSpecificAccessor(
        CommandContext<ServerCommandSource> context,
        Portal portal, Entity player
    ) {
        
        portal.specificPlayerId = player.getUuid();
        
        sendMessage(
            context,
            "This portal can only be accessed by " +
                player.getName().asString() + " now"
        );
        sendMessage(context, portal.toString());
    }
    
    private static void invokeSetPortalNBT(CommandContext<ServerCommandSource> context, Portal portal) throws CommandSyntaxException {
        
        CompoundTag newNbt = NbtCompoundTagArgumentType.getCompoundTag(
            context, "nbt"
        );
        
        setPortalNbt(portal, newNbt);
        
        sendPortalInfo(context, portal);
    }
    
    private static void removeMultidestEntry(
        CommandContext<ServerCommandSource> context,
        Portal pointedPortal,
        ServerPlayerEntity player
    ) {
        PortalManipulation.getPortalClutter(
            pointedPortal.world,
            pointedPortal.getOriginPos(),
            pointedPortal.getNormal(),
            p -> true
        ).stream().filter(
            portal -> player.getUuid().equals(portal.specificPlayerId) || portal.specificPlayerId == null
        ).forEach(
            portal -> {
                PortalManipulation.removeConnectedPortals(
                    portal,
                    (p) -> sendMessage(context, "removed " + p.toString())
                );
                sendMessage(context, "removed " + portal.toString());
                portal.remove();
            }
        );
    }
    
    private static void setMultidestEntry(
        CommandContext<ServerCommandSource> context,
        Portal pointedPortal,
        ServerPlayerEntity player,
        RegistryKey<World> dimension,
        Vec3d destination,
        boolean biFaced,
        boolean biWay
    ) {
        Portal newPortal = PortalManipulation.copyPortal(
            pointedPortal, Portal.entityType
        );
        
        removeMultidestEntry(context, pointedPortal, player);
        
        newPortal.dimensionTo = dimension;
        newPortal.setDestination(destination);
        newPortal.specificPlayerId = player.getUuid();
        
        McHelper.spawnServerEntity(newPortal);
        
        configureBiWayBiFaced(newPortal, biWay, biFaced);
    }
    
    private static void configureBiWayBiFaced(Portal newPortal, boolean biWay, boolean biFaced) {
        if (biFaced && biWay) {
            PortalManipulation.completeBiWayBiFacedPortal(
                newPortal,
                p -> {
                },
                p -> {
                },
                Portal.entityType
            );
        }
        else if (biFaced) {
            PortalManipulation.completeBiFacedPortal(newPortal, Portal.entityType);
        }
        else if (biWay) {
            PortalManipulation.completeBiWayPortal(newPortal, Portal.entityType);
        }
    }
    
    public static void sendPortalInfo(CommandContext<ServerCommandSource> context, Portal portal) {
        context.getSource().sendFeedback(
            McHelper.compoundTagToTextSorted(
                portal.toTag(new CompoundTag()),
                " ",
                0
            ),
            false
        );
        
        sendMessage(
            context,
            portal.toString() + "\n"
        );
    }
    
    public static void sendMessage(CommandContext<ServerCommandSource> context, String message) {
        context.getSource().sendFeedback(
            new LiteralText(message),
            false
        );
    }
    
    /**
     * Gets success message based on the portal {@code portal}.
     *
     * @param portal The portal to send the success message for.
     * @return The success message, as a {@link Text}.
     * @author LoganDark
     */
    private static Text getMakePortalSuccess(Portal portal) {
        return new TranslatableText(
            "imm_ptl.command.make_portal.success",
            Double.toString(portal.width),
            Double.toString(portal.height),
            McHelper.dimensionTypeId(portal.world.getRegistryKey()).toString(),
            portal.getOriginPos().toString(),
            McHelper.dimensionTypeId(portal.dimensionTo).toString(),
            portal.getDestPos().toString()
        );
    }
    
    // By LoganDark :D
    private static int placePortalAbsolute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        double width = DoubleArgumentType.getDouble(context, "width");
        double height = DoubleArgumentType.getDouble(context, "height");
        RegistryKey<World> to = DimensionArgumentType.getDimensionArgument(context, "to").getRegistryKey();
        Vec3d dest = Vec3ArgumentType.getVec3(context, "dest");
        
        Portal portal = PortalManipulation.placePortal(width, height, context.getSource().getPlayer());
        
        if (portal == null) {
            return 0;
        }
        
        portal.dimensionTo = to;
        portal.setDestination(dest);
        McHelper.spawnServerEntity(portal);
        
        context.getSource().sendFeedback(getMakePortalSuccess(portal), true);
        
        return 1;
    }
    
    // By LoganDark :D
    private static int placePortalShift(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        double width = DoubleArgumentType.getDouble(context, "width");
        double height = DoubleArgumentType.getDouble(context, "height");
        RegistryKey<World> to = DimensionArgumentType.getDimensionArgument(context, "to").getRegistryKey();
        double dist = DoubleArgumentType.getDouble(context, "dist");
        
        Portal portal = PortalManipulation.placePortal(width, height, context.getSource().getPlayer());
        
        if (portal == null) {
            return 0;
        }
        
        // unsafe to use getContentDirection before the destination is fully set
        portal.dimensionTo = to;
        portal.setDestination(portal.getOriginPos().add(portal.axisW.crossProduct(portal.axisH).multiply(-dist)));
        
        McHelper.spawnServerEntity(portal);
        
        context.getSource().sendFeedback(getMakePortalSuccess(portal), true);
        
        return 1;
    }
    
    public static int teleport(
        Collection<? extends Entity> entities,
        RegistryKey<World> targetDim,
        Vec3d targetPos
    ) {
        ServerWorld targetWorld = McHelper.getServer().getWorld(targetDim);
        
        int numTeleported = 0;
        
        for (Entity entity : entities) {
            ServerTeleportationManager.teleportEntityGeneral(entity, targetPos, targetWorld);
            
            numTeleported++;
        }
        
        return numTeleported;
    }
    
    public static interface PortalConsumerThrowsCommandSyntaxException {
        void accept(Portal portal) throws CommandSyntaxException;
    }
    
    public static int processPortalTargetedCommand(
        CommandContext<ServerCommandSource> context,
        PortalConsumerThrowsCommandSyntaxException processCommand
    ) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Entity entity = source.getEntity();
        
        if (entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = ((ServerPlayerEntity) entity);
            
            Portal portal = getPlayerPointingPortal(player, false);
            
            if (portal == null) {
                source.sendFeedback(
                    new LiteralText("You are not pointing to any non-global portal." +
                        " (This command cannot process global portals)"),
                    false
                );
                return 0;
            }
            else {
                processCommand.accept(portal);
            }
        }
        else if (entity instanceof Portal) {
            processCommand.accept(((Portal) entity));
        }
        else {
            source.sendFeedback(
                new LiteralText(
                    "The command executor should be either a player or a portal entity"
                ),
                false
            );
        }
        
        return 0;
    }
    
    public static Portal getPlayerPointingPortal(
        ServerPlayerEntity player, boolean includeGlobalPortal
    ) {
        return getPlayerPointingPortalRaw(player, 1, 100, includeGlobalPortal)
            .map(Pair::getFirst).orElse(null);
    }
    
    public static Optional<Pair<Portal, Vec3d>> getPlayerPointingPortalRaw(
        PlayerEntity player, float tickDelta, double maxDistance, boolean includeGlobalPortal
    ) {
        Vec3d from = player.getCameraPosVec(tickDelta);
        Vec3d to = from.add(player.getRotationVec(tickDelta).multiply(maxDistance));
        World world = player.world;
        return raytracePortals(world, from, to, includeGlobalPortal);
    }
    
    public static Optional<Pair<Portal, Vec3d>> raytracePortals(
        World world, Vec3d from, Vec3d to, boolean includeGlobalPortal
    ) {
        Stream<Portal> portalStream = McHelper.getEntitiesNearby(
            world,
            from,
            Portal.class,
            from.distanceTo(to)
        );
        if (includeGlobalPortal) {
            List<Portal> globalPortals = McHelper.getGlobalPortals(world);
            portalStream = Streams.concat(
                portalStream,
                globalPortals.stream()
            );
        }
        return portalStream.map(
            portal -> new Pair<Portal, Vec3d>(
                portal, portal.rayTrace(from, to)
            )
        ).filter(
            portalAndHitPos -> portalAndHitPos.getSecond() != null
        ).min(
            Comparator.comparingDouble(
                portalAndHitPos -> portalAndHitPos.getSecond().squaredDistanceTo(from)
            )
        );
    }
    
    private static void makePortalRound(Portal portal) {
        GeometryPortalShape shape = new GeometryPortalShape();
        final int triangleNum = 30;
        double twoPi = Math.PI * 2;
        shape.triangles = IntStream.range(0, triangleNum)
            .mapToObj(i -> new GeometryPortalShape.TriangleInPlane(
                0, 0,
                portal.width * 0.5 * Math.cos(twoPi * ((double) i) / triangleNum),
                portal.height * 0.5 * Math.sin(twoPi * ((double) i) / triangleNum),
                portal.width * 0.5 * Math.cos(twoPi * ((double) i + 1) / triangleNum),
                portal.height * 0.5 * Math.sin(twoPi * ((double) i + 1) / triangleNum)
            )).collect(Collectors.toList());
        portal.specialShape = shape;
        portal.cullableXStart = 0;
        portal.cullableXEnd = 0;
        portal.cullableYStart = 0;
        portal.cullableYEnd = 0;
    }
    
    /**
     * {@link Entity#getRotationVector()}
     */
    private static Vec3d getRightVec(Entity entity) {
        float yaw = entity.yaw + 90;
        float radians = -yaw * 0.017453292F;
        
        return new Vec3d(
            Math.sin(radians), 0, Math.cos(radians)
        );
    }
}
