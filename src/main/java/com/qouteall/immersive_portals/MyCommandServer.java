package com.qouteall.immersive_portals;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.BorderPortal;
import com.qouteall.immersive_portals.portal.global_portals.EndFloorPortal;
import com.qouteall.immersive_portals.portal.nether_portal.NewNetherPortalEntity;
import net.minecraft.command.arguments.DimensionArgumentType;
import net.minecraft.command.arguments.TextArgumentType;
import net.minecraft.command.arguments.Vec3ArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.function.Consumer;

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
            .literal("end_floor_enable")
            .executes(context -> {
                EndFloorPortal.enableFloor();
                return 0;
            })
        );
        builder.then(CommandManager
            .literal("end_floor_remove")
            .executes(context -> {
                EndFloorPortal.removeFloor();
                return 0;
            })
        );
    
        builder.then(CommandManager
            .literal("view_portal_data")
            .executes(context -> {
                return processPortalTargetedCommand(
                    context,
                    (portal) -> {
                        sendMessage(
                            context,
                            String.format(
                                "%s",
                                portal
                            )
                        );
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
            .literal("stabilize_nether_portal")
            .executes(context -> processPortalTargetedCommand(
                context,
                portal -> {
                    if (portal instanceof NewNetherPortalEntity) {
                        NewNetherPortalEntity portal1 = (NewNetherPortalEntity) portal;
                        portal1.unbreakable = true;
                        sendMessage(context, "Stabilized " + portal);
                    }
                    else {
                        sendMessage(context, "Not Nether Portal");
                    }
                }
            ))
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
        
        dispatcher.register(builder);
    }
    
    public static void sendMessage(CommandContext<ServerCommandSource> context, String message) {
        context.getSource().sendFeedback(
            new LiteralText(
                message
            ),
            true
        );
    }
    
    public static int processPortalTargetedCommand(
        CommandContext<ServerCommandSource> context,
        Consumer<Portal> processCommand
    ) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
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
    
}
