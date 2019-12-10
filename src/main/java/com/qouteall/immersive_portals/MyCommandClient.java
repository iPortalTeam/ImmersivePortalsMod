package com.qouteall.immersive_portals;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.qouteall.immersive_portals.chunk_loading.ChunkVisibilityManager;
import com.qouteall.immersive_portals.chunk_loading.MyClientChunkManager;
import com.qouteall.immersive_portals.ducks.IEEntity;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.MyBuiltChunkStorage;
import com.qouteall.immersive_portals.render.RenderHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.dimension.DimensionType;

import java.lang.ref.Reference;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class MyCommandClient {
    
    public static void register(
        CommandDispatcher<ServerCommandSource> dispatcher
    ) {
        //for composite command arguments, put into then() 's bracket
        //for parallel command arguments, put behind then()
        
        LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager
            .literal("immersive_portals_debug")
            .requires(commandSource -> true)
            .then(CommandManager
                .literal("set_max_portal_layer")
                .then(CommandManager
                    .argument(
                        "argMaxPortalLayer", IntegerArgumentType.integer()
                    )
                    .executes(context -> setMaxPortalLayer(
                        IntegerArgumentType.getInteger(context, "argMaxPortalLayer")
                    ))
                )
            );
        builder = builder.then(CommandManager
            .literal("list_nearby_portals")
            .executes(context -> listNearbyPortals(context))
        );
        builder = builder.then(CommandManager
            .literal("is_client_chunk_loaded")
            .then(CommandManager
                .argument(
                    "chunkX", IntegerArgumentType.integer()
                )
                .then(CommandManager
                    .argument(
                        "chunkZ", IntegerArgumentType.integer()
                    )
                    .executes(
                        MyCommandClient::isClientChunkLoaded
                    )
                )
            )
        );
        builder = builder.then(CommandManager
            .literal("is_server_chunk_loaded")
            .then(CommandManager
                .argument(
                    "chunkX", IntegerArgumentType.integer()
                )
                .then(CommandManager
                    .argument(
                        "chunkZ", IntegerArgumentType.integer()
                    )
                    .executes(
                        context -> {
                            int chunkX = IntegerArgumentType.getInteger(context, "chunkX");
                            int chunkZ = IntegerArgumentType.getInteger(context, "chunkZ");
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            Chunk chunk = McHelper.getServer()
                                .getWorld(player.dimension)
                                .getChunk(
                                    chunkX, chunkZ,
                                    ChunkStatus.FULL, false
                                );
                            McHelper.serverLog(
                                player,
                                chunk != null && !(chunk instanceof EmptyChunk) ? "yes" : "no"
                            );
                            return 0;
                        }
                    )
                )
            )
        );
        builder = builder.then(CommandManager
            .literal("add_portal")
            .executes(context -> addPortal(context))
        );
        builder = builder.then(CommandManager
            .literal("report_player_status")
            .executes(context -> reportPlayerStatus(context))
        );
        builder = builder.then(CommandManager
            .literal("client_remote_ticking_enable")
            .executes(context -> {
                CGlobal.isClientRemoteTickingEnabled = true;
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("client_remote_ticking_disable")
            .executes(context -> {
                CGlobal.isClientRemoteTickingEnabled = false;
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("advanced_frustum_culling_enable")
            .executes(context -> {
                CGlobal.doUseAdvancedFrustumCulling = true;
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("advanced_frustum_culling_disable")
            .executes(context -> {
                CGlobal.doUseAdvancedFrustumCulling = false;
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("multithreaded_chunk_loading_enable")
            .executes(context -> {
                SGlobal.isChunkLoadingMultiThreaded = true;
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("multithreaded_chunk_loading_disable")
            .executes(context -> {
                SGlobal.isChunkLoadingMultiThreaded = false;
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("hacked_chunk_render_dispatcher_enable")
            .executes(context -> {
                CGlobal.useHackedChunkRenderDispatcher = true;
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("hacked_chunk_render_dispatcher_disable")
            .executes(context -> {
                CGlobal.useHackedChunkRenderDispatcher = false;
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("front_culling_enable")
            .executes(context -> {
                CGlobal.useFrontCulling = true;
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("front_culling_disable")
            .executes(context -> {
                CGlobal.useFrontCulling = false;
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("switch_to_normal_renderer")
            .executes(context -> {
                MinecraftClient.getInstance().execute(() -> {
                    CGlobal.useCompatibilityRenderer = false;
                });
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("switch_to_compatibility_renderer")
            .executes(context -> {
                MinecraftClient.getInstance().execute(() -> {
                    CGlobal.useCompatibilityRenderer = true;
                });
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("report_server_entities")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                List<Entity> entities = player.world.getEntities(
                    Entity.class,
                    new Box(player.getBlockPos()).expand(32),
                    e -> true
                );
                McHelper.serverLog(player, entities.toString());
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("report_resource_consumption")
            .executes(MyCommandClient::reportResourceConsumption)
        );
        builder = builder.then(CommandManager
            .literal("report_render_info_num")
            .executes(context -> {
                String str = Helper.myToString(CGlobal.renderInfoNumMap.entrySet().stream());
                context.getSource().getPlayer().sendMessage(new LiteralText(str));
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("shader_debug_enable")
            .executes(context -> {
                CGlobal.isRenderDebugMode = true;
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("shader_debug_disable")
            .executes(context -> {
                CGlobal.isRenderDebugMode = false;
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("get_player_colliding_portal_client")
            .executes(context -> {
                Portal collidingPortal =
                    ((IEEntity) MinecraftClient.getInstance().player).getCollidingPortal();
                McHelper.serverLog(context.getSource().getPlayer(), collidingPortal.toString());
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("report_rendering")
            .executes(context -> {
                String str = RenderHelper.lastPortalRenderInfos
                    .stream()
                    .map(
                        list -> list.stream()
                            .map(Reference::get)
                            .collect(Collectors.toList())
                    )
                    .collect(Collectors.toList())
                    .toString();
                McHelper.serverLog(context.getSource().getPlayer(), str);
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("debug_mirror_mode_enable")
            .executes(context -> {
                CGlobal.debugMirrorMode = true;
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("debug_mirror_mode_disable")
            .executes(context -> {
                CGlobal.debugMirrorMode = false;
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("teleport_on_rendering_enable")
            .executes(context -> {
                CGlobal.teleportOnRendering = true;
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("teleport_on_rendering_disable")
            .executes(context -> {
                CGlobal.teleportOnRendering = false;
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("vanilla_chunk_culling_enable")
            .executes(context -> {
                MinecraftClient.getInstance().chunkCullingEnabled = true;
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("vanilla_chunk_culling_disable")
            .executes(context -> {
                MinecraftClient.getInstance().chunkCullingEnabled = false;
                return 0;
            })
        );
        builder.then(CommandManager
            .literal("report_chunk_loaders")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                ChunkVisibilityManager.getChunkLoaders(
                    player
                ).forEach(
                    loader -> McHelper.serverLog(
                        player, loader.toString()
                    )
                );
                return 0;
            })
        );
    
    
        dispatcher.register(builder);
        
        Helper.log("Successfully initialized command /immersive_portals_debug");
    }
    
    private static int reportResourceConsumption(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        StringBuilder str = new StringBuilder();
        
        str.append("Client Chunk:\n");
        CGlobal.clientWorldLoader.clientWorldMap.values().forEach(world -> {
            str.append(String.format(
                "%s %s\n",
                world.dimension.getType(),
                ((MyClientChunkManager) world.getChunkManager()).getChunkNum()
            ));
        });
        
        
        str.append("Chunk Renderers:\n");
        CGlobal.clientWorldLoader.worldRendererMap.forEach(
            (dimension, worldRenderer) -> {
                str.append(String.format(
                    "%s %s\n",
                    dimension,
                    ((MyBuiltChunkStorage) ((IEWorldRenderer) worldRenderer)
                        .getBuiltChunkStorage()
                    ).getManagedChunkNum()
                ));
            }
        );
    
        str.append("Server Chunks:\n");
        McHelper.getServer().getWorlds().forEach(
            world -> {
                str.append(String.format(
                    "%s %s\n",
                    world.dimension.getType(),
                    world.getForcedChunks().size()
                ));
            }
        );
        
        String result = str.toString();
        
        Helper.log(str);
        
        context.getSource().getPlayer().sendMessage(new LiteralText(result));
        
        return 0;
    }
    
    private static int isClientChunkLoaded(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int chunkX = IntegerArgumentType.getInteger(context, "chunkX");
        int chunkZ = IntegerArgumentType.getInteger(context, "chunkZ");
        Chunk chunk = MinecraftClient.getInstance().world.getChunk(
            chunkX, chunkZ
        );
        McHelper.serverLog(
            context.getSource().getPlayer(),
            chunk != null && !(chunk instanceof EmptyChunk) ? "yes" : "no"
        );
        return 0;
    }
    
    private static int setMaxPortalLayer(int m) {
        CGlobal.maxPortalLayer = m;
        return 0;
    }
    
    private static int listNearbyPortals(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity playerServer = context.getSource().getPlayer();
        ClientPlayerEntity playerClient = MinecraftClient.getInstance().player;
    
        McHelper.serverLog(playerServer, "Server Portals");
        McHelper.serverLog(
            playerServer,
            Helper.myToString(
                McHelper.getEntitiesNearby(
                    playerServer, Portal.class, 64
                )
            )
        );
    
        McHelper.serverLog(playerServer, "Client Portals");
        McHelper.serverLog(
            playerServer,
            Helper.myToString(
                McHelper.getEntitiesNearby(
                    playerClient, Portal.class, 64
                )
            )
        );
        
        return 0;
    }
    
    private static Consumer<ServerPlayerEntity> originalAddPortalFunctionality;
    private static Consumer<ServerPlayerEntity> addPortalFunctionality;
    
    static {
        originalAddPortalFunctionality = (player) -> {
            Vec3d fromPos = player.getPos();
            Vec3d fromNormal = player.getRotationVector().multiply(-1);
            ServerWorld fromWorld = ((ServerWorld) player.world);
            
            addPortalFunctionality = (playerEntity) -> {
                Vec3d toPos = playerEntity.getPos();
                DimensionType toDimension = player.dimension;
                
                Portal portal = new Portal(fromWorld);
                portal.setPos(fromPos.x, fromPos.y, fromPos.z);
                
                portal.axisH = new Vec3d(0, 1, 0);
                portal.axisW = portal.axisH.crossProduct(fromNormal).normalize();
                
                portal.dimensionTo = toDimension;
                portal.destination = toPos;
                
                portal.width = 4;
                portal.height = 4;
                
                assert portal.isPortalValid();
                
                fromWorld.spawnEntity(portal);
                
                addPortalFunctionality = originalAddPortalFunctionality;
            };
        };
        
        addPortalFunctionality = originalAddPortalFunctionality;
    }
    
    private static int addPortal(CommandContext<ServerCommandSource> context) {
        try {
            addPortalFunctionality.accept(context.getSource().getPlayer());
        }
        catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    private static int reportPlayerStatus(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        //only invoked on single player
        
        ServerPlayerEntity playerMP = context.getSource().getPlayer();
        ClientPlayerEntity playerSP = MinecraftClient.getInstance().player;
    
        McHelper.serverLog(
            playerMP,
            "On Server " + playerMP.dimension + " " + playerMP.getBlockPos()
        );
        McHelper.serverLog(
            playerMP,
            "On Client " + playerSP.dimension + " " + playerSP.getBlockPos()
        );
        return 0;
    }
}
