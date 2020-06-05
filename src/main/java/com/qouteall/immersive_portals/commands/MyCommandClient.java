package com.qouteall.immersive_portals.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.chunk_loading.ChunkVisibilityManager;
import com.qouteall.immersive_portals.chunk_loading.MyClientChunkManager;
import com.qouteall.immersive_portals.ducks.IEEntity;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.far_scenery.FSRenderingContext;
import com.qouteall.immersive_portals.far_scenery.FarSceneryRenderer;
import com.qouteall.immersive_portals.optifine_compatibility.UniformReport;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.MyBuiltChunkStorage;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.MessageType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.dimension.DimensionType;

import java.lang.ref.Reference;
import java.net.URLClassLoader;
import java.util.Arrays;
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
            .literal("report_server_entities")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                List<Entity> entities = player.world.getEntities(
                    Entity.class,
                    new Box(player.getPos(),player.getPos()).expand(32),
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
                context.getSource().getPlayer().sendMessage(new LiteralText(str), MessageType.SYSTEM);
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("get_player_colliding_portal_client")
            .executes(context -> {
                Portal collidingPortal =
                    ((IEEntity) MinecraftClient.getInstance().player).getCollidingPortal();
                McHelper.serverLog(
                    context.getSource().getPlayer(),
                    collidingPortal != null ? collidingPortal.toString() : "null"
                );
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("report_rendering")
            .executes(context -> {
                String str = MyRenderHelper.lastPortalRenderInfos
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
        builder = builder.then(CommandManager
            .literal("render_mode_normal")
            .executes(context -> {
                Global.renderMode = Global.RenderMode.normal;
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("render_mode_compatibility")
            .executes(context -> {
                Global.renderMode = Global.RenderMode.compatibility;
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("render_mode_debug")
            .executes(context -> {
                Global.renderMode = Global.RenderMode.debug;
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("render_mode_none")
            .executes(context -> {
                Global.renderMode = Global.RenderMode.none;
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
        builder.then(CommandManager
            .literal("check_light")
            .executes(context -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                mc.execute(() -> {
                    mc.world.getChunkManager().getLightingProvider().updateSectionStatus(
                        ChunkSectionPos.from(new BlockPos(mc.player.getPos())),
                        false
                    );
                });
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("uniform_report_textured")
            .executes(context -> {
                UniformReport.launchUniformReport(
                    new String[]{
                        "gbuffers_textured", "gbuffers_textured_lit"
                    },
                    s -> context.getSource().sendFeedback(
                        new LiteralText(s), true
                    )
                );
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("uniform_report_terrain")
            .executes(context -> {
                UniformReport.launchUniformReport(
                    new String[]{
                        "gbuffers_terrain", "gbuffers_terrain_solid"
                    },
                    s -> context.getSource().sendFeedback(
                        new LiteralText(s), true
                    )
                );
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("uniform_report_shadow")
            .executes(context -> {
                UniformReport.launchUniformReport(
                    new String[]{
                        "shadow_solid", "shadow"
                    },
                    s -> context.getSource().sendFeedback(
                        new LiteralText(s), true
                    )
                );
                return 0;
            })
        );
        builder.then(CommandManager
            .literal("erase_chunk")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
    
                eraseChunk(new ChunkPos(new BlockPos(player.getPos())), player.world, 0, 256);
    
                return 0;
            })
        );
        builder.then(CommandManager
            .literal("erase_chunk_large")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
    
                ChunkPos center = new ChunkPos(new BlockPos(player.getPos()));
    
                for (int dx = -4; dx <= 4; dx++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        eraseChunk(
                            new ChunkPos(
                                player.chunkX + dx,
                                player.chunkZ + dz
                            ),
                            player.world, 0, 256
                        );
                    }
                }
                
                return 0;
            })
        );
        builder.then(CommandManager
            .literal("erase_chunk_large_middle")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
            
                ChunkPos center = new ChunkPos(new BlockPos(player.getPos()));
            
                for (int dx = -4; dx <= 4; dx++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        eraseChunk(
                            new ChunkPos(
                                player.chunkX + dx,
                                player.chunkZ + dz
                            ),
                            player.world, 64, 128
                        );
                    }
                }
                
                return 0;
            })
        );
        registerSwitchCommand(
            builder,
            "render_fewer_on_fast_graphic",
            cond -> CGlobal.renderFewerInFastGraphic = cond
        );
        registerSwitchCommand(
            builder,
            "gl_check_error",
            cond -> Global.doCheckGlError = cond
        );
        registerSwitchCommand(
            builder,
            "far_scenery",
            cond -> FSRenderingContext.isFarSceneryEnabled = cond
        );
        registerSwitchCommand(
            builder,
            "smooth_chunk_unload",
            cond -> CGlobal.smoothChunkUnload = cond
        );
        registerSwitchCommand(
            builder,
            "update_far_scenery",
            cond -> FarSceneryRenderer.shouldUpdateFarScenery = cond
        );
        registerSwitchCommand(
            builder,
            "early_light_update",
            cond -> CGlobal.earlyClientLightUpdate = cond
        );
        registerSwitchCommand(
            builder,
            "super_advanced_frustum_culling",
            cond -> CGlobal.useSuperAdvancedFrustumCulling = cond
        );
    
        registerSwitchCommand(
            builder,
            "teleportation_debug",
            cond -> Global.teleportationDebugEnabled = cond
        );
        registerSwitchCommand(
            builder,
            "cross_portal_entity_rendering",
            cond -> Global.correctCrossPortalEntityRendering = cond
        );
        registerSwitchCommand(
            builder,
            "loose_visible_chunk_iteration",
            cond -> Global.looseVisibleChunkIteration = cond
        );
        registerSwitchCommand(
            builder,
            "portal_placeholder_passthrough",
            cond -> Global.portalPlaceholderPassthrough = cond
        );
        
        builder.then(CommandManager
            .literal("print_class_path")
            .executes(context -> {
                printClassPath();
                return 0;
            })
        );
        
        dispatcher.register(builder);
        
        Helper.log("Successfully initialized command /immersive_portals_debug");
    }
    
    public static void eraseChunk(ChunkPos chunkPos, World world, int yStart, int yEnd) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = yStart; y < yEnd; y++) {
                    world.setBlockState(
                        chunkPos.toBlockPos(
                            x, y, z
                        ),
                        Blocks.AIR.getDefaultState()
                    );
                }
            }
        }
    }
    
    private static void printClassPath() {
        System.out.println(
            Arrays.stream(
                ((URLClassLoader) ClassLoader.getSystemClassLoader()).getURLs()
            ).map(
                url -> "\"" + url.getFile().substring(1).replace("%20", " ") + "\""
            ).collect(Collectors.joining(",\n"))
        );
    }
    
    private static void registerSwitchCommand(
        LiteralArgumentBuilder<ServerCommandSource> builder,
        String name,
        Consumer<Boolean> setFunction
    ) {
        builder = builder.then(CommandManager
            .literal(name + "_enable")
            .executes(context -> {
                setFunction.accept(true);
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal(name + "_disable")
            .executes(context -> {
                setFunction.accept(false);
                return 0;
            })
        );
    }
    
    private static int reportResourceConsumption(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        StringBuilder str = new StringBuilder();
        
        str.append("Client Chunk:\n");
        CGlobal.clientWorldLoader.clientWorldMap.values().forEach(world -> {
            str.append(String.format(
                "%s %s\n",
                world.getDimension().getType(),
                ((MyClientChunkManager) world.getChunkManager()).getLoadedChunkCount()
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
                    world.getDimension().getType(),
                    world.getForcedChunks().size()
                ));
            }
        );
        
        String result = str.toString();
        
        Helper.log(str);
        
        context.getSource().getPlayer().sendMessage(new LiteralText(result), MessageType.SYSTEM);
        
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
        Global.maxPortalLayer = m;
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
                
                Portal portal = new Portal(Portal.entityType, fromWorld);
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
            "On Server " + playerMP.dimension + " " + playerMP.getPos()
        );
        McHelper.serverLog(
            playerMP,
            "On Client " + playerSP.dimension + " " + playerSP.getPos()
        );
        return 0;
    }
    
}
