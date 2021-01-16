package com.qouteall.immersive_portals.commands;

import com.google.common.collect.Streams;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ClientWorldLoader;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.chunk_loading.ChunkVisibilityManager;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.ducks.IEEntity;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.network.RemoteProcedureCall;
import com.qouteall.immersive_portals.optifine_compatibility.UniformReport;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.MyBuiltChunkStorage;
import com.qouteall.immersive_portals.render.PortalRenderInfo;
import com.qouteall.immersive_portals.render.PortalRenderingGroup;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.ProfilerSystem;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import java.lang.ref.Reference;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
public class ClientDebugCommand {
    
    public static void register(
        CommandDispatcher<ServerCommandSource> dispatcher
    ) {
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
            .literal("list_portals")
            .executes(context -> listPortals(context))
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
                        ClientDebugCommand::isClientChunkLoaded
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
                                .getWorld(player.world.getRegistryKey())
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
            .literal("report_server_entities")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                List<Entity> entities = player.world.getEntitiesByClass(
                    Entity.class,
                    new Box(player.getPos(), player.getPos()).expand(32),
                    e -> true
                );
                McHelper.serverLog(player, entities.toString());
                return 0;
            })
        );
        builder = builder.then(CommandManager
            .literal("report_resource_consumption")
            .executes(ClientDebugCommand::reportResourceConsumption)
        );
        builder = builder.then(CommandManager
            .literal("report_render_info_num")
            .executes(context -> {
                String str = Helper.myToString(CGlobal.renderInfoNumMap.entrySet().stream());
                context.getSource().getPlayer().sendMessage(new LiteralText(str), false);
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
                String str = RenderStates.lastPortalRenderInfos
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
                ChunkVisibilityManager.getBaseChunkLoaders(
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
            .literal("check_client_light")
            .executes(context -> {
                MinecraftClient client = MinecraftClient.getInstance();
                client.execute(() -> {
                    client.world.getChunkManager().getLightingProvider().setSectionStatus(
                        ChunkSectionPos.from(new BlockPos(client.player.getPos())),
                        false
                    );
                });
                return 0;
            })
        );
        builder.then(CommandManager
            .literal("check_server_light")
            .executes(context -> {
                McHelper.getServer().execute(() -> {
                    ServerPlayerEntity player = McHelper.getRawPlayerList().get(0);
                    
                    BlockPos.stream(
                        player.getBlockPos().add(-2, -2, -2),
                        player.getBlockPos().add(2, 2, 2)
                    ).forEach(blockPos -> {
                        player.world.getLightingProvider().checkBlock(blockPos);
                    });
                });
                return 0;
            })
        );
        builder.then(CommandManager
                .literal("update_server_light")
                .executes(context -> {
                    McHelper.getServer().execute(() -> {
                        ServerPlayerEntity player = McHelper.getRawPlayerList().get(0);
                        
                        ServerLightingProvider lightingProvider = (ServerLightingProvider) player.world.getLightingProvider();
                        lightingProvider.light(
                            player.world.getChunk(player.chunkX, player.chunkZ),
                            false
                        );
//                    lightingProvider.light(
//                        player.world.getChunk(player.chunkX, player.chunkZ),
//                        true
//                    );
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
        builder.then(CommandManager
            .literal("report_rebuild_status")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                MinecraftClient.getInstance().execute(() -> {
                    ClientWorldLoader.getClientWorlds().forEach((world) -> {
                        MyBuiltChunkStorage builtChunkStorage = (MyBuiltChunkStorage) ((IEWorldRenderer)
                            ClientWorldLoader.getWorldRenderer(world.getRegistryKey()))
                            .getBuiltChunkStorage();
                        McHelper.serverLog(
                            player,
                            world.getRegistryKey().getValue().toString() + builtChunkStorage.getDebugString()
                        );
                    });
                });
                
                return 0;
            })
        );
        builder.then(CommandManager
            .literal("is_altius")
            .executes(context -> {
                
                Object obj = Helper.noError(
                    () -> {
                        Class<?> cls = Class.forName("com.qouteall.imm_ptl_peripheral.altius_world.AltiusInfo");
                        Method method = cls.getDeclaredMethod("isAltius");
                        return method.invoke(null);
                    }
                );
                
                boolean altius = ((Boolean) obj);
                
                if (altius) {
                    context.getSource().sendFeedback(
                        new LiteralText("yes"),
                        false
                    );
                }
                else {
                    context.getSource().sendFeedback(
                        new LiteralText("no"),
                        false
                    );
                }
                
                return 0;
            })
        );
        builder.then(CommandManager
            .literal("print_generator_config")
            .executes(context -> {
                McHelper.getServer().getWorlds().forEach(world -> {
                    ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
                    Helper.log(world.getRegistryKey().getValue());
                    Helper.log(McHelper.serializeToJson(generator, ChunkGenerator.CODEC));
                    Helper.log(McHelper.serializeToJson(
                        world.getDimension(),
                        DimensionType.CODEC.stable()
                    ));
                });
                
                GeneratorOptions options = McHelper.getServer().getSaveProperties().getGeneratorOptions();
                
                Helper.log(McHelper.serializeToJson(options, GeneratorOptions.CODEC));
                
                return 0;
            })
        );
        builder.then(CommandManager
            .literal("set_profiler_logging_threshold")
            .then(CommandManager.argument("ms", IntegerArgumentType.integer())
                .executes(context -> {
                    int ms = IntegerArgumentType.getInteger(context, "ms");
                    ProfilerSystem.TIMEOUT_NANOSECONDS = Duration.ofMillis(ms).toNanos();
                    
                    return 0;
                })
            )
        );
        builder.then(CommandManager
            .literal("report_portal_groups")
            .executes(context -> {
                for (ClientWorld clientWorld : ClientWorldLoader.getClientWorlds()) {
                    Map<Optional<PortalRenderingGroup>, List<Portal>> result =
                        Streams.stream(clientWorld.getEntities())
                            .flatMap(
                                entity -> entity instanceof Portal ?
                                    Stream.of(((Portal) entity)) : Stream.empty()
                            )
                            .collect(Collectors.groupingBy(
                                p -> Optional.ofNullable(PortalRenderInfo.getGroupOf(p))
                            ));
                    final ServerPlayerEntity player = context.getSource().getPlayer();
                    McHelper.serverLog(player, "\n" + clientWorld.getRegistryKey().getValue().toString());
                    result.forEach((g, l) -> {
                        McHelper.serverLog(player, "\n" + g.toString());
                        McHelper.serverLog(player, l.stream()
                            .map(Portal::toString).collect(Collectors.joining("\n"))
                        );
                    });
                }
                return 0;
            })
        );
        builder.then(CommandManager
            .literal("gui_portal_test")
            .executes(context -> {
                MinecraftClient.getInstance().execute(() -> {
                    try {
                        Class.forName("com.qouteall.imm_ptl_peripheral.test.ExampleGuiPortalRendering")
                            .getDeclaredMethod("open")
                            .invoke(null);
                    }
                    catch (Throwable e) {
                        e.printStackTrace();
                    }
                });
                
                return 0;
            })
        );
        builder.then(CommandManager
            .literal("report_client_light_status")
            .executes(context -> {
                MinecraftClient.getInstance().execute(() -> {
                    ClientPlayerEntity player = MinecraftClient.getInstance().player;
                    ChunkNibbleArray lightSection = player.world.getLightingProvider().get(LightType.BLOCK).getLightSection(
                        ChunkSectionPos.from(player.chunkX, player.chunkY, player.chunkZ)
                    );
                    if (lightSection != null) {
                        boolean uninitialized = lightSection.isUninitialized();
    
                        byte[] byteArray = lightSection.asByteArray();
                        boolean allZero = true;
                        for (byte b : byteArray) {
                            if (b != 0) {
                                allZero = false;
                                break;
                            }
                        }
                        
                        context.getSource().sendFeedback(
                            new LiteralText(
                                "has light section " +
                                    (allZero ? "all zero" : "not all zero") +
                                    (uninitialized ? " uninitialized" : " fine")
                            ),
                            false
                        );
                    }
                    else {
                        context.getSource().sendFeedback(
                            new LiteralText("does not have light section"), false
                        );
                    }
                });
                return 0;
            })
        );
        builder.then(CommandManager
            .literal("remote_procedure_call_test")
            .executes(context -> {
                testRemoteProcedureCall(context.getSource().getPlayer());
                return 0;
            })
        );
        registerSwitchCommand(
            builder,
            "front_clipping",
            cond -> CGlobal.useFrontClipping = cond
        );
        registerSwitchCommand(
            builder,
            "gl_check_error",
            cond -> Global.doCheckGlError = cond
        );
        registerSwitchCommand(
            builder,
            "smooth_chunk_unload",
            cond -> CGlobal.smoothChunkUnload = cond
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
        registerSwitchCommand(
            builder,
            "early_cull_portal",
            cond -> CGlobal.earlyFrustumCullingPortal = cond
        );
        registerSwitchCommand(
            builder,
            "cache_gl_buffer",
            cond -> Global.cacheGlBuffer = cond
        );
        registerSwitchCommand(
            builder,
            "add_custom_ticket_for_direct_loading_delayed",
            cond -> NewChunkTrackingGraph.addCustomTicketForDirectLoadingDelayed = cond
        );
        registerSwitchCommand(
            builder,
            "server_smooth_loading",
            cond -> Global.serverSmoothLoading = cond
        );
        registerSwitchCommand(
            builder,
            "secondary_vertex_consumer",
            cond -> Global.useSecondaryEntityVertexConsumer = cond
        );
        registerSwitchCommand(
            builder,
            "cull_sections_behind",
            cond -> Global.cullSectionsBehind = cond
        );
        registerSwitchCommand(
            builder,
            "offset_occlusion_query",
            cond -> Global.offsetOcclusionQuery = cond
        );
        registerSwitchCommand(
            builder,
            "cloud_optimization",
            cond -> Global.cloudOptimization = cond
        );
        registerSwitchCommand(
            builder,
            "cross_portal_collision",
            cond -> Global.crossPortalCollision = cond
        );
        registerSwitchCommand(
            builder,
            "light_logging",
            cond -> Global.lightLogging = cond
        );
        registerSwitchCommand(
            builder,
            "flush_light_tasks_before_sending_packet",
            cond -> Global.flushLightTasksBeforeSendingPacket = cond
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
                        new BlockPos(
                            chunkPos.getStartX() + x,
                            y,
                            chunkPos.getStartZ() + z
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
        ClientWorldLoader.getClientWorlds().forEach(world -> {
            str.append(String.format(
                "%s %s\n",
                world.getRegistryKey().getValue(),
                world.getChunkManager().getLoadedChunkCount()
            ));
        });
        
        
        str.append("Chunk Mesh Sections:\n");
        ClientWorldLoader.worldRendererMap.forEach(
            (dimension, worldRenderer) -> {
                str.append(String.format(
                    "%s %s\n",
                    dimension.getValue(),
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
                    world.getRegistryKey().getValue(),
                    NewChunkTrackingGraph.getLoadedChunkNum(world.getRegistryKey())
                ));
            }
        );
        
        String result = str.toString();
        
        Helper.log(str);
        
        context.getSource().getPlayer().sendMessage(new LiteralText(result), false);
        
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
    
    private static int listPortals(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity playerServer = context.getSource().getPlayer();
        ClientPlayerEntity playerClient = MinecraftClient.getInstance().player;
        
        StringBuilder result = new StringBuilder();
        
        result.append("Server Portals\n");
        
        playerServer.getServer().getWorlds().forEach(world -> {
            result.append(world.getRegistryKey().getValue().toString() + "\n");
            for (Entity e : world.iterateEntities()) {
                if (e instanceof Portal) {
                    result.append(e.toString());
                    result.append("\n");
                }
            }
        });
        
        result.append("Client Portals\n");
        ClientWorldLoader.getClientWorlds().forEach((world) -> {
            result.append(world.getRegistryKey().getValue().toString() + "\n");
            for (Entity e : world.getEntities()) {
                if (e instanceof Portal) {
                    result.append(e.toString());
                    result.append("\n");
                }
            }
        });
        
        McHelper.serverLog(playerServer, result.toString());
        
        return 0;
    }
    
    private static int reportPlayerStatus(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        //only invoked on single player
        
        ServerPlayerEntity playerMP = context.getSource().getPlayer();
        ClientPlayerEntity playerSP = MinecraftClient.getInstance().player;
        
        McHelper.serverLog(
            playerMP,
            String.format(
                "On Server %s %s removed:%s added:%s age:%s chunk:%s %s",
                playerMP.world.getRegistryKey().getValue(),
                playerMP.getPos(),
                playerMP.removed,
                playerMP.world.getEntityById(playerMP.getEntityId()) != null,
                playerMP.age,
                playerMP.chunkX, playerMP.chunkZ
            )
        );
        
        McHelper.serverLog(
            playerMP,
            String.format(
                "On Client %s %s removed:%s added:%s age:%s chunk:%s %s",
                playerSP.world.getRegistryKey().getValue(),
                playerSP.getPos(),
                playerSP.removed,
                playerSP.world.getEntityById(playerSP.getEntityId()) != null,
                playerSP.age,
                playerSP.chunkX, playerSP.chunkZ
            )
        );
        return 0;
    }
    
    public static class TestRemoteCallable {
        public static void serverToClient(
            String str, int integer, double doubleNum, Identifier identifier,
            RegistryKey<World> dimension, RegistryKey<Biome> biomeKey,
            BlockPos blockPos, Vec3d vec3d
        ) {
            Helper.log(str + integer + doubleNum + identifier + dimension + biomeKey + blockPos + vec3d);
        }
        
        public static void clientToServer(
            ServerPlayerEntity player,
            UUID uuid,
            Block block, BlockState blockState,
            Item item, ItemStack itemStack,
            CompoundTag compoundTag, Text text, int[] intArray
        ) {
            Helper.log(
                player.getName().asString() + uuid + block + blockState + item + itemStack
                    + compoundTag + text + Arrays.toString(intArray)
            );
        }
    }
    
    private static void testRemoteProcedureCall(ServerPlayerEntity player) {
        MinecraftClient.getInstance().execute(() -> {
            CompoundTag compoundTag = new CompoundTag();
            compoundTag.put("test", IntTag.of(7));
            RemoteProcedureCall.tellServerToInvoke(
                "com.qouteall.immersive_portals.commands.ClientDebugCommand.TestRemoteCallable.clientToServer",
                new UUID(3, 3),
                Blocks.ACACIA_PLANKS,
                Blocks.NETHER_PORTAL.getDefaultState().with(NetherPortalBlock.AXIS, Direction.Axis.Z),
                Items.COMPASS,
                new ItemStack(Items.ACACIA_LOG, 2),
                compoundTag,
                new LiteralText("test"),
                new int[]{777, 765}
            );
        });
        
        McHelper.getServer().execute(() -> {
            RemoteProcedureCall.tellClientToInvoke(
                player,
                "com.qouteall.immersive_portals.commands.ClientDebugCommand.TestRemoteCallable.serverToClient",
                "string", 2, 3.5, new Identifier("imm_ptl:oops"),
                World.NETHER, BiomeKeys.JUNGLE,
                new BlockPos(3, 5, 4),
                new Vec3d(7, 4, 1)
            );
        });
    }
}
