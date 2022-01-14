package qouteall.imm_ptl.core.commands;

import com.google.common.collect.Streams;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.ColumnPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.command.argument.TextArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
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
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.imm_ptl.core.portal.GeometryPortalShape;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalExtension;
import qouteall.imm_ptl.core.portal.PortalManipulation;
import qouteall.imm_ptl.core.portal.global_portals.BorderBarrierFiller;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.imm_ptl.core.portal.global_portals.VerticalConnectingPortal;
import qouteall.imm_ptl.core.portal.global_portals.WorldWrappingPortal;
import qouteall.imm_ptl.core.portal.nether_portal.NetherPortalMatcher;
import qouteall.imm_ptl.core.teleportation.ServerTeleportationManager;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.api.McRemoteProcedureCall;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.IntBox;
import qouteall.q_misc_util.my_util.MyTaskList;
import qouteall.q_misc_util.my_util.SignalBiArged;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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
        PortalDebugCommands.registerDebugCommands(debugBuilder);
        builder.then(debugBuilder);
        
        dispatcher.register(builder);
    }
    
    public static boolean canUsePortalCommand(ServerCommandSource commandSource) {
        Entity entity = commandSource.getEntity();
        if (entity instanceof ServerPlayerEntity) {
            if (IPGlobal.easeCreativePermission) {
                if (((ServerPlayerEntity) entity).isCreative()) {
                    return true;
                }
            }
        }
        
        return commandSource.hasPermissionLevel(2);
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
                    Helper.myToString(GlobalPortalStorage.getGlobalPortals(player.world).stream())
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
                ServerPlayerEntity player = context.getSource().getPlayer();
                Portal portal = getPlayerPointingPortal(player, true);
                
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
                    portal.remove(Entity.RemovalReason.KILLED);
                }
            ))
        );
        
        builder.then(CommandManager.literal("set_portal_nbt")
            .then(CommandManager.argument("nbt", NbtCompoundArgumentType.nbtCompound())
                .executes(context -> processPortalTargetedCommand(
                    context,
                    portal -> {
                        NbtCompound newNbt = NbtCompoundArgumentType.getNbtCompound(
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
                        
                        if (newNbt.contains("dimensionTo")) {
                            context.getSource().sendError(new LiteralText(
                                "Cannot change tag dimensionTo. use command /portal set_portal_destination"
                            ));
                            return;
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
                    .executes(context -> processPortalTargetedCommand(
                        context, portal -> {
                            sendEditBiWayPortalWarning(context, portal);
                            
                            portal.dimensionTo = DimensionArgumentType.getDimensionArgument(
                                context, "dim"
                            ).getRegistryKey();
                            portal.setDestination(Vec3ArgumentType.getVec3(
                                context, "dest"
                            ));
                            
                            portal.reloadAndSyncToClient();
                            portal.rectifyClusterPortals();
                            
                            sendMessage(context, portal.toString());
                        }
                    ))
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
                    portal.remove(Entity.RemovalReason.KILLED);
                    sendMessage(context, "Deleted " + portal);
                }
            ))
        );
        
        builder.then(CommandManager.literal("eradicate_portal_cluster")
            .executes(context -> processPortalTargetedCommand(
                context,
                portal -> {
                    PortalManipulation.removeConnectedPortals(
                        portal,
                        p -> sendMessage(context, "Removed " + p)
                    );
                    portal.remove(Entity.RemovalReason.KILLED);
                    sendMessage(context, "Deleted " + portal);
                }
            ))
        );
        
        builder.then(CommandManager.literal("move_portal")
            .then(CommandManager.argument("distance", DoubleArgumentType.doubleArg())
                .executes(context -> processPortalTargetedCommand(
                    context, portal -> {
                        try {
                            sendEditBiWayPortalWarning(context, portal);
                            
                            double distance =
                                DoubleArgumentType.getDouble(context, "distance");
                            
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            Vec3d viewVector = player.getRotationVector();
                            Direction facing = Direction.getFacing(
                                viewVector.x, viewVector.y, viewVector.z
                            );
                            Vec3d offset = Vec3d.of(facing.getVector()).multiply(distance);
                            portal.setPosition(
                                portal.getX() + offset.x,
                                portal.getY() + offset.y,
                                portal.getZ() + offset.z
                            );
                            portal.reloadAndSyncToClient();
                            portal.rectifyClusterPortals();
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
                            sendEditBiWayPortalWarning(context, portal);
                            
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
                            portal.rectifyClusterPortals();
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
                    portal.rectifyClusterPortals();
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
                        portal.rectifyClusterPortals();
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
                    portal.rectifyClusterPortals();
                }))
            )
        );
        
        builder.then(CommandManager.literal("set_portal_position")
            .then(CommandManager.argument("dim", DimensionArgumentType.dimension())
                .then(CommandManager.argument("pos", Vec3ArgumentType.vec3(false))
                    .executes(context -> processPortalTargetedCommand(context, portal -> {
                        ServerWorld targetWorld =
                            DimensionArgumentType.getDimensionArgument(context, "dim");
                        
                        Vec3d pos = Vec3ArgumentType.getVec3(context, "pos");
                        
                        if (targetWorld == portal.world) {
                            portal.setOriginPos(pos);
                            portal.reloadAndSyncToClient();
                            portal.rectifyClusterPortals();
                        }
                        else {
                            ServerTeleportationManager.teleportEntityGeneral(
                                portal, pos, targetWorld
                            );
                        }
                        
                        sendMessage(context, portal.toString());
                        
                    }))
                )
            )
        );
        
        builder.then(CommandManager.literal("set_portal_position_to")
            .then(CommandManager.argument("targetEntity", EntityArgumentType.entity())
                .executes(context -> processPortalTargetedCommand(context, portal -> {
                    Entity targetEntity = EntityArgumentType.getEntity(context, "targetEntity");
                    
                    if (targetEntity.world == portal.world) {
                        portal.setOriginPos(targetEntity.getPos());
                        portal.reloadAndSyncToClient();
                        portal.rectifyClusterPortals();
                    }
                    else {
                        ServerTeleportationManager.teleportEntityGeneral(
                            portal, targetEntity.getPos(), ((ServerWorld) targetEntity.world)
                        );
                    }
                    
                    sendMessage(context, portal.toString());
                }))
            )
        );
        
        builder.then(CommandManager.literal("reset_portal_orientation")
            .executes(context -> processPortalTargetedCommand(
                context, portal -> {
                    portal.axisW = new Vec3d(1, 0, 0);
                    portal.axisH = new Vec3d(0, 1, 0);
                    portal.reloadAndSyncToClient();
                    portal.rectifyClusterPortals();
                }
            ))
        );
        
        builder.then(CommandManager.literal("relatively_move_portal")
            .then(CommandManager.argument("offset", Vec3ArgumentType.vec3(false))
                .executes(context -> processPortalTargetedCommand(context, portal -> {
                    Vec3d offset = Vec3ArgumentType.getVec3(context, "offset");
                    portal.setOriginPos(
                        portal.getOriginPos().add(
                            portal.axisW.multiply(offset.x)
                        ).add(
                            portal.axisH.multiply(offset.y)
                        ).add(
                            portal.getNormal().multiply(offset.z)
                        )
                    );
                }))
            )
        );
        
        builder.then(CommandManager.literal("relatively_move_portal_destination")
            .then(CommandManager.argument("offset", Vec3ArgumentType.vec3(false))
                .executes(context -> processPortalTargetedCommand(context, portal -> {
                    Vec3d offset = Vec3ArgumentType.getVec3(context, "offset");
                    portal.setDestination(
                        portal.getDestPos().add(
                            portal.transformLocalVec(portal.axisW).multiply(offset.x)
                        ).add(
                            portal.transformLocalVec(portal.axisH).multiply(offset.y)
                        ).add(
                            portal.transformLocalVec(portal.getNormal()).multiply(offset.z)
                        )
                    );
                    portal.reloadAndSyncToClient();
                    portal.rectifyClusterPortals();
                }))
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
                            sendEditBiWayPortalWarning(context, portal);
                            
                            Vec3d rotatingAxis = Vec3ArgumentType.getVec3(
                                context, "rotatingAxis"
                            ).normalize();
                            
                            double angleDegrees = DoubleArgumentType.getDouble(
                                context, "angleDegrees"
                            );
                            
                            Quaternion rot = angleDegrees != 0 ? new Quaternion(
                                new Vec3f(rotatingAxis),
                                (float) angleDegrees,
                                true
                            ) : null;
                            
                            func.accept(portal, rot);
                            
                            portal.reloadAndSyncToClient();
                            portal.rectifyClusterPortals();
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
                            sendEditBiWayPortalWarning(context, portal);
                            
                            Vec3f axis = new Vec3f(1, 0, 0);
                            
                            double angleDegrees =
                                DoubleArgumentType.getDouble(context, "angleDegrees");
                            
                            Quaternion rot = angleDegrees != 0 ? new Quaternion(
                                axis,
                                (float) angleDegrees,
                                true
                            ) : null;
                            
                            func.accept(portal, rot);
                            
                            portal.reloadAndSyncToClient();
                            portal.rectifyClusterPortals();
                        }
                    ))
                )
            )
            .then(CommandManager.literal("y")
                .then(CommandManager.argument("angleDegrees", DoubleArgumentType.doubleArg())
                    .executes(context -> processPortalTargetedCommand(
                        context,
                        portal -> {
                            sendEditBiWayPortalWarning(context, portal);
                            
                            Vec3f axis = new Vec3f(0, 1, 0);
                            
                            double angleDegrees =
                                DoubleArgumentType.getDouble(context, "angleDegrees");
                            
                            Quaternion rot = angleDegrees != 0 ? new Quaternion(
                                axis,
                                (float) angleDegrees,
                                true
                            ) : null;
                            
                            func.accept(portal, rot);
                            
                            portal.reloadAndSyncToClient();
                            portal.rectifyClusterPortals();
                        }
                    ))
                )
            )
            .then(CommandManager.literal("z")
                .then(CommandManager.argument("angleDegrees", DoubleArgumentType.doubleArg())
                    .executes(context -> processPortalTargetedCommand(
                        context,
                        portal -> {
                            sendEditBiWayPortalWarning(context, portal);
                            
                            Vec3f axis = new Vec3f(0, 0, 1);
                            
                            double angleDegrees =
                                DoubleArgumentType.getDouble(context, "angleDegrees");
                            
                            Quaternion rot = angleDegrees != 0 ? new Quaternion(
                                axis,
                                (float) angleDegrees,
                                true
                            ) : null;
                            
                            func.accept(portal, rot);
                            
                            portal.reloadAndSyncToClient();
                            portal.rectifyClusterPortals();
                        }
                    ))
                )
            )
        );
    }
    
    private static void setPortalRotation(Portal portal, Vec3f axis, double angleDegrees) {
        if (angleDegrees != 0) {
            portal.rotation = new Quaternion(
                axis, (float) angleDegrees, true
            );
        }
        else {
            portal.rotation = null;
        }
        
        portal.reloadAndSyncToClient();
        portal.rectifyClusterPortals();
    }
    
    // only portal custom data can be changed
    private static void setPortalNbt(Portal portal, NbtCompound newNbt) {
        NbtCompound data = portal.writePortalDataToNbt();
        
        newNbt.getKeys().forEach(
            key -> data.put(key, newNbt.get(key))
        );
        
        portal.readPortalDataFromNbt(data);
        
        portal.reloadAndSyncToClient();
        portal.rectifyClusterPortals();
    }
    
    private static void registerCBPortalCommands(
        LiteralArgumentBuilder<ServerCommandSource> builder
    ) {
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
                                
                                invokeCbMakePortal(width, height, fromEntity, toEntity, "");
                                
                                return 0;
                            })
                            .then(CommandManager.argument("portalTag", StringArgumentType.string())
                                .executes(context -> {
                                    double width = DoubleArgumentType.getDouble(context, "width");
                                    double height = DoubleArgumentType.getDouble(context, "height");
                                    
                                    Entity fromEntity = EntityArgumentType.getEntity(context, "from");
                                    Entity toEntity = EntityArgumentType.getEntity(context, "to");
                                    
                                    String portalTag = StringArgumentType.getString(context, "portalTag");
                                    
                                    invokeCbMakePortal(width, height, fromEntity, toEntity, portalTag);
                                    
                                    return 0;
                                })
                            )
                        )
                    )
                )
            )
        );
    }
    
    private static void invokeCbMakePortal(
        double width, double height, Entity fromEntity, Entity toEntity,
        String portalTag
    ) {
        Portal portal = Portal.entityType.create(fromEntity.world);
        
        portal.setPosition(fromEntity.getX(), fromEntity.getY(), fromEntity.getZ());
        
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
    }
    
    private static void registerUtilityCommands(
        LiteralArgumentBuilder<ServerCommandSource> builder
    ) {
        builder.then(CommandManager.literal("tpme")
            .then(CommandManager.argument("target", EntityArgumentType.entity())
                .executes(context -> {
                    Entity entity = EntityArgumentType.getEntity(context, "target");
                    
                    IPGlobal.serverTeleportationManager.invokeTpmeCommand(
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
                    
                    IPGlobal.serverTeleportationManager.invokeTpmeCommand(
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
                        
                        IPGlobal.serverTeleportationManager.invokeTpmeCommand(
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
                    IPGlobal.serverTeleportationManager.lastPosition.get(player);
                if (lastPos == null) {
                    sendMessage(context, "You haven't teleported");
                }
                else {
                    IPGlobal.serverTeleportationManager.invokeTpmeCommand(
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
            .literal("create_connected_rooms")
            .then(CommandManager
                .literal("roomSize")
                .then(CommandManager
                    .argument("roomSize", BlockPosArgumentType.blockPos())
                    .then(CommandManager
                        .literal("roomNumber")
                        .then(CommandManager
                            .argument("roomNumber", IntegerArgumentType.integer(2, 500))
                            .executes(context -> {
                                BlockPos roomSize =
                                    BlockPosArgumentType.getBlockPos(context, "roomSize");
                                int roomNumber = IntegerArgumentType.getInteger(context, "roomNumber");
                                
                                createConnectedRooms(
                                    context.getSource().getWorld(),
                                    new BlockPos(context.getSource().getPosition()),
                                    roomSize,
                                    roomNumber,
                                    text -> context.getSource().sendFeedback(text, false)
                                );
                                
                                return 0;
                            })
                        )
                    )
                )
            )
        );
        
        builder.then(CommandManager.literal("adjust_rotation_to_connect")
            .then(CommandManager.argument("portal1", EntityArgumentType.entity())
                .then(CommandManager.argument("portal2", EntityArgumentType.entity())
                    .executes(context -> {
                        Entity e1 = EntityArgumentType.getEntity(context, "portal1");
                        Entity e2 = EntityArgumentType.getEntity(context, "portal2");
                        
                        if (!(e1 instanceof Portal)) {
                            context.getSource().sendError(
                                new LiteralText("portal1 is not a portal entity"));
                            return 0;
                        }
                        
                        if (!(e2 instanceof Portal)) {
                            context.getSource().sendError(
                                new LiteralText("portal2 is not a portal entity"));
                            return 0;
                        }
                        
                        Portal portal1 = (Portal) e1;
                        Portal portal2 = (Portal) e2;
                        
                        portal1.setDestination(portal2.getOriginPos());
                        portal2.setDestination(portal1.getOriginPos());
                        
                        PortalManipulation.adjustRotationToConnect(portal1, portal2);
                        
                        portal1.reloadAndSyncToClient();
                        portal2.reloadAndSyncToClient();
                        
                        return 0;
                    })
                )
            )
        );
        
        builder.then(CommandManager
            .literal("dimension_stack")
            .requires(commandSource -> commandSource.hasPermissionLevel(2))
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                
                List<String> dimIdList = new ArrayList<>();
                for (ServerWorld world : MiscHelper.getServer().getWorlds()) {
                    dimIdList.add(world.getRegistryKey().getValue().toString());
                }
                
                McRemoteProcedureCall.tellClientToInvoke(
                    player,
                    "qouteall.imm_ptl.peripheral.altius_world.AltiusManagement.RemoteCallables.clientOpenScreen",
                    dimIdList
                );
                
                return 0;
            })
        );
        
        builder.then(CommandManager
            .literal("wiki")
            .executes(context -> {
                McRemoteProcedureCall.tellClientToInvoke(
                    context.getSource().getPlayer(),
                    "qouteall.imm_ptl.peripheral.guide.IPGuide.RemoteCallables.showWiki"
                );
                return 0;
            })
        );
    }
    
    private static void createConnectedRooms(
        ServerWorld world, BlockPos startingPos,
        BlockPos roomSize, int roomNumber,
        Consumer<Text> feedbackSender
    ) {
        BlockPos roomAreaSize = roomSize.add(2, 2, 2);
        
        List<IntBox> roomAreaList = new ArrayList<>();
        
        Helper.SimpleBox<BlockPos> currentSearchingCenter =
            new Helper.SimpleBox<>(startingPos);
        
        IPGlobal.serverTaskList.addTask(MyTaskList.chainTask(
            MyTaskList.repeat(
                roomNumber,
                () -> MyTaskList.withDelay(20, MyTaskList.oneShotTask(() -> {
                    
                    currentSearchingCenter.obj = currentSearchingCenter.obj.add(getRandomShift(20));
                    
                    IntBox airCube = NetherPortalMatcher.findCubeAirAreaAtAnywhere(
                        roomAreaSize.add(6, 6, 6),
                        world,
                        currentSearchingCenter.obj,
                        128
                    );
                    if (airCube == null) {
                        feedbackSender.accept(new LiteralText("Cannot find space for placing room"));
                        return;
                    }
                    airCube = airCube.getSubBoxInCenter(roomAreaSize);
                    
                    fillRoomFrame(world, airCube, getRandomBlock());
                    roomAreaList.add(airCube);
                }))
            ),
            MyTaskList.oneShotTask(() -> {
                Stream<IntBox> roomsStream = Stream.concat(
                    roomAreaList.stream(),
                    Stream.of(roomAreaList.get(0))
                );
                Helper.wrapAdjacentAndMap(
                    roomsStream, Pair::new
                ).forEach(pair -> {
                    
                    IntBox room1Area = pair.getFirst();
                    IntBox room2Area = pair.getSecond();
                    IntBox room1 = room1Area.getAdjusted(1, 1, 1, -1, -1, -1);
                    IntBox room2 = room2Area.getAdjusted(1, 1, 1, -1, -1, -1);
                    
                    Portal portal = Portal.entityType.create(world);
                    Validate.notNull(portal);
                    portal.setOriginPos(room1.getCenterVec().add(
                        roomSize.getX() / 4.0, 0, 0
                    ));
                    portal.setDestinationDimension(world.getRegistryKey());
                    portal.setDestination(room2.getCenterVec().add(
                        roomSize.getX() / 4.0, 0, 0
                    ));
                    portal.setOrientationAndSize(
                        new Vec3d(1, 0, 0),
                        new Vec3d(0, 1, 0),
                        roomSize.getX() / 2.0,
                        roomSize.getY()
                    );
                    portal.portalTag = "imm_ptl:room_connection";
                    
                    McHelper.spawnServerEntity(portal);
                    
                    Portal reversePortal = PortalAPI.createReversePortal(portal);
                    McHelper.spawnServerEntity(reversePortal);
                });
                
                feedbackSender.accept(new LiteralText("finished"));
            })
        ));
    }
    
    private static BlockState getRandomBlock() {
        Random random = new Random();
        
        for (; ; ) {
            Block block = Registry.BLOCK.getRandom(random);
            BlockState state = block.getDefaultState();
            Material material = state.getMaterial();
            if (material.blocksMovement() && material.getPistonBehavior() == PistonBehavior.NORMAL
                && !material.isLiquid()
            ) {
                return state;
            }
        }
    }
    
    private static void fillRoomFrame(
        ServerWorld world, IntBox roomArea, BlockState blockState
    ) {
        for (Direction direction : Direction.values()) {
            IntBox surface = roomArea.getSurfaceLayer(direction);
            surface.fastStream().forEach(blockPos -> {
                world.setBlockState(blockPos, blockState);
            });
        }
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
    
    private static void sendEditBiWayPortalWarning(
        CommandContext<ServerCommandSource> context, Portal portal
    ) {
        if (!PortalExtension.get(portal).bindCluster) {
            if (PortalManipulation.findReversePortal(portal) != null) {
                sendMessage(context, "You are editing a bi-way portal." +
                    " It's recommended to enable bindCluster or you will get unlinked portal entities." +
                    " Use command /portal set_portal_nbt {bindCluster:true}"
                );
            }
        }
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
            MiscHelper.getServer().getWorld(portal.dimensionTo),
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
                portal.remove(Entity.RemovalReason.KILLED);
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
                portal.writeNbt(new NbtCompound()),
                " ",
                0
            ),
            false
        );
        
        sendMessage(
            context,
            portal.toString()
        );
        
        sendMessage(context,
            String.format("Orientation: %s", PortalAPI.getPortalOrientationQuaternion(portal))
        );
        
        if (portal.getRotation() != null) {
            sendMessage(context,
                String.format("Rotating Transformation: %s",
                    DQuaternion.fromMcQuaternion(portal.getRotation())
                )
            );
        }
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
        ServerWorld targetWorld = MiscHelper.getServer().getWorld(targetDim);
        
        int numTeleported = 0;
        
        for (Entity entity : entities) {
            ServerTeleportationManager.teleportEntityGeneral(entity, targetPos, targetWorld);
            
            numTeleported++;
        }
        
        return numTeleported;
    }
    
    public static BlockPos getRandomShift(int len) {
        Random rand = new Random();
        return new BlockPos(
            (rand.nextDouble() * 2 - 1) * len,
            (rand.nextDouble() * 2 - 1) * len,
            (rand.nextDouble() * 2 - 1) * len
        );
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
        ).stream();
        if (includeGlobalPortal) {
            List<Portal> globalPortals = GlobalPortalStorage.getGlobalPortals(world);
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
        float yaw = entity.getYaw() + 90;
        float radians = -yaw * 0.017453292F;
        
        return new Vec3d(
            Math.sin(radians), 0, Math.cos(radians)
        );
    }
    
    public static class RemoteCallables {
        @Environment(EnvType.CLIENT)
        public static void clientAccelerate(double v) {
            MinecraftClient client = MinecraftClient.getInstance();
            
            ClientPlayerEntity player = client.player;
            
            McHelper.setWorldVelocity(
                player,
                player.getRotationVec(1).multiply(v / 20)
            );
        }
    }
    
}
