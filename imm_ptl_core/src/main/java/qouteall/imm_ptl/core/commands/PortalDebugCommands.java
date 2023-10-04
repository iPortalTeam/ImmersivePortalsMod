package qouteall.imm_ptl.core.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.profiling.ActiveProfiler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.api.example.ExampleGuiPortalRendering;
import qouteall.imm_ptl.core.chunk_loading.ChunkVisibility;
import qouteall.imm_ptl.core.chunk_loading.ImmPtlChunkTickets;
import qouteall.imm_ptl.core.chunk_loading.ImmPtlChunkTracking;
import qouteall.imm_ptl.core.chunk_loading.PlayerChunkLoading;
import qouteall.imm_ptl.core.ducks.IEDistanceManager;
import qouteall.imm_ptl.core.ducks.IEServerChunkManager;
import qouteall.imm_ptl.core.ducks.IEServerWorld;
import qouteall.imm_ptl.core.ducks.IEWorld;
import qouteall.imm_ptl.core.mixin.common.chunk_sync.IEChunkMap_Accessor;
import qouteall.imm_ptl.core.mixin.common.mc_util.IELevelEntityGetterAdapter;
import qouteall.imm_ptl.core.portal.GeometryPortalShape;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.api.McRemoteProcedureCall;
import qouteall.q_misc_util.my_util.MyTaskList;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static qouteall.imm_ptl.core.chunk_loading.ImmPtlChunkTickets.getDistanceManager;

public class PortalDebugCommands {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    static void registerDebugCommands(
        LiteralArgumentBuilder<CommandSourceStack> builder
    ) {
        
        builder.then(Commands
            .literal("gui_portal")
            .then(Commands.argument("dim", DimensionArgument.dimension())
                .then(Commands.argument("pos", Vec3Argument.vec3(false))
                    .executes(context -> {
                        ExampleGuiPortalRendering.onCommandExecuted(
                            context.getSource().getPlayerOrException(),
                            DimensionArgument.getDimension(context, "dim"),
                            Vec3Argument.getVec3(context, "pos")
                        );
                        return 0;
                    })
                )
            ));
        
        builder.then(Commands
            .literal("isometric_enable")
            .then(Commands.argument("viewLength", FloatArgumentType.floatArg())
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    float viewLength = FloatArgumentType.getFloat(context, "viewLength");
                    
                    McRemoteProcedureCall.tellClientToInvoke(
                        player,
                        "qouteall.imm_ptl.core.render.TransformationManager.RemoteCallables.enableIsometricView",
                        viewLength
                    );
                    
                    return 0;
                })
            )
        );
        
        builder.then(Commands
            .literal("isometric_disable")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                McRemoteProcedureCall.tellClientToInvoke(
                    player,
                    "qouteall.imm_ptl.core.render.TransformationManager.RemoteCallables.disableIsometricView"
                );
                return 0;
            })
        );
        
        builder.then(Commands
            .literal("align")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                
                Vec3 pos = player.position();
                
                Vec3 newPos = new Vec3(
                    Math.round(pos.x * 2) / 2.0,
                    Math.round(pos.y * 2) / 2.0,
                    Math.round(pos.z * 2) / 2.0
                );
                
                player.connection.teleport(
                    newPos.x, newPos.y, newPos.z,
                    45, 30
                );
                
                return 0;
            })
        );
        
        builder.then(Commands.literal("profile")
            .then(Commands
                .literal("set_lag_logging_threshold")
                .requires(serverCommandSource -> serverCommandSource.hasPermission(4))
                .then(Commands.argument("ms", IntegerArgumentType.integer())
                    .executes(context -> {
                        int ms = IntegerArgumentType.getInteger(context, "ms");
                        ActiveProfiler.WARNING_TIME_NANOS = Duration.ofMillis(ms).toNanos();
                        
                        return 0;
                    })
                )
            ).then(Commands
                .literal("gc")
                .requires(serverCommandSource -> serverCommandSource.hasPermission(4))
                .executes(context -> {
                    System.gc();
                    
                    long l = Runtime.getRuntime().maxMemory();
                    long m = Runtime.getRuntime().totalMemory();
                    long n = Runtime.getRuntime().freeMemory();
                    long o = m - n;
                    
                    context.getSource().sendSuccess(
                        () -> Component.literal(
                            String.format("Memory: % 2d%% %03d/%03dMB", o * 100L / l, toMiB(o), toMiB(l))
                        ),
                        false
                    );
                    
                    return 0;
                })
            )
        );
        
        builder.then(Commands
            .literal("create_command_stick")
            .requires(serverCommandSource -> serverCommandSource.hasPermission(2))
            .then(Commands.argument("command", StringArgumentType.string())
                .executes(context -> {
                    PortalCommand.createCommandStickCommandSignal.emit(
                        context.getSource().getPlayerOrException(),
                        StringArgumentType.getString(context, "command")
                    );
                    return 0;
                })
            )
        );
        
        builder.then(Commands
            .literal("accelerate")
            .requires(PortalCommand::canUsePortalCommand)
            .then(Commands
                .argument("v", DoubleArgumentType.doubleArg())
                .executes(context -> {
                    double v = DoubleArgumentType.getDouble(context, "v");
                    ServerPlayer player = context.getSource().getPlayer();
                    Vec3 vec = player.getViewVector(1).scale(v / 20.0d);
                    McRemoteProcedureCall.tellClientToInvoke(
                        context.getSource().getPlayerOrException(),
                        "qouteall.imm_ptl.core.commands.PortalCommand.RemoteCallables.clientAccelerate",
                        vec
                    );
                    return 0;
                })
            )
        );
        
        builder.then(Commands
            .literal("accelerate_along")
            .requires(PortalCommand::canUsePortalCommand)
            .then(Commands
                .argument("vec", Vec3Argument.vec3(false))
                .executes(context -> {
                    Vec3 vec = Vec3Argument.getVec3(context, "vec");
                    McRemoteProcedureCall.tellClientToInvoke(
                        context.getSource().getPlayerOrException(),
                        "qouteall.imm_ptl.core.commands.PortalCommand.RemoteCallables.clientAccelerate",
                        vec.scale(1.0 / 20)
                    );
                    return 0;
                })
            )
        );
        
        builder.then(Commands.literal("test")
            .executes(context -> {
                return 0;
            })
        );
        
        builder.then(Commands
            .literal("erase_chunk")
            .requires(serverCommandSource -> serverCommandSource.hasPermission(3))
            .then(Commands.argument("rChunks", IntegerArgumentType.integer())
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    ChunkPos center = new ChunkPos(BlockPos.containing(player.position()));
                    
                    invokeEraseChunk(
                        player.level(), center,
                        IntegerArgumentType.getInteger(context, "rChunks"),
                        McHelper.getMinY(player.level()), McHelper.getMaxYExclusive(player.level())
                    );
                    
                    return 0;
                })
                .then(Commands.argument("downY", IntegerArgumentType.integer())
                    .then(Commands.argument("upY", IntegerArgumentType.integer())
                        .executes(context -> {
                            
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            
                            ChunkPos center = new ChunkPos(BlockPos.containing(player.position()));
                            
                            invokeEraseChunk(
                                player.level(), center,
                                IntegerArgumentType.getInteger(context, "rChunks"),
                                IntegerArgumentType.getInteger(context, "downY"),
                                IntegerArgumentType.getInteger(context, "upY")
                            );
                            return 0;
                        })
                    )
                )
            )
        );
        
        builder.then(Commands
            .literal("report_chunk_loaders")
            .requires(serverCommandSource -> serverCommandSource.hasPermission(3))
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                ChunkVisibility.foreachBaseChunkLoaders(
                    player,
                    loader -> {
                        McHelper.serverLog(
                            player, loader.toString()
                        );
                    }
                );
                return 0;
            })
        );
        
        builder.then(Commands
            .literal("report_server_entities_nearby")
            .requires(serverCommandSource -> serverCommandSource.hasPermission(3))
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                List<Entity> entities = player.level().getEntitiesOfClass(
                    Entity.class,
                    new AABB(player.position(), player.position()).inflate(32),
                    e -> true
                );
                McHelper.serverLog(player, entities.stream().map(Entity::toString)
                    .collect(Collectors.joining("\n")));
                return 0;
            })
        );
        
        builder.then(Commands
            .literal("report_loaded_portals")
            .requires(serverCommandSource -> serverCommandSource.hasPermission(3))
            .executes(context -> {
                CommandSourceStack source = context.getSource();
                MinecraftServer server = source.getServer();
                
                for (ServerLevel world : server.getAllLevels()) {
                    for (Entity entity : world.getAllEntities()) {
                        if (entity instanceof Portal portal) {
                            source.sendSuccess(
                                () -> Component.literal(entity.toString()),
                                true
                            );
                        }
                    }
                }
                
                return 0;
            })
        );
        
        builder.then(Commands.literal("is_chunk_loaded")
            .requires(serverCommandSource -> serverCommandSource.hasPermission(2))
            .then(Commands.argument("dim", DimensionArgument.dimension())
                .then(Commands.argument("chunkX", IntegerArgumentType.integer())
                    .then(Commands.argument("chunkZ", IntegerArgumentType.integer())
                        .executes(context -> {
                            int chunkX = IntegerArgumentType.getInteger(context, "chunkX");
                            int chunkZ = IntegerArgumentType.getInteger(context, "chunkZ");
                            ServerLevel world = DimensionArgument.getDimension(context, "dim");
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            
                            doReportChunkStatus(chunkX, chunkZ, world, player);
                            
                            return 0;
                        })
                    )
                )
            )
        );
        
        builder.then(Commands.literal("report_chunk_at")
            .requires(serverCommandSource -> serverCommandSource.hasPermission(2))
            .then(Commands.argument("dim", DimensionArgument.dimension())
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes(context -> {
                        ServerLevel world = DimensionArgument.getDimension(context, "dim");
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
                        ChunkPos chunkPos = new ChunkPos(pos);
                        
                        doReportChunkStatus(chunkPos.x, chunkPos.z, world, player);
                        
                        return 0;
                    })
                )
            )
        );
        
        builder.then(Commands.literal("report_player_status")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                
                CHelper.printChat(
                    String.format(
                        "On Server %s %s removal:%s added:%s age:%s",
                        player.level().dimension().location(),
                        player.blockPosition(),
                        player.getRemovalReason(),
                        player.level().getEntity(player.getId()) != null,
                        player.tickCount
                    )
                );
                
                McRemoteProcedureCall.tellClientToInvoke(
                    player,
                    "qouteall.imm_ptl.core.commands.ClientDebugCommand.RemoteCallables.reportClientPlayerStatus"
                );
                
                return 0;
            })
        );
        
        builder.then(Commands.literal("list_portals")
            .requires(serverCommandSource -> serverCommandSource.hasPermission(3))
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                
                StringBuilder result = new StringBuilder();
                result.append("Server Portals\n");
                
                for (ServerLevel world : MiscHelper.getServer().getAllLevels()) {
                    result.append(world.dimension().location().toString() + "\n");
                    for (Entity entity : world.getAllEntities()) {
                        for (Entity e : world.getAllEntities()) {
                            if (e instanceof Portal) {
                                result.append(e.toString());
                                result.append("\n");
                            }
                        }
                    }
                }
                
                McHelper.serverLog(player, result.toString());
                
                McRemoteProcedureCall.tellClientToInvoke(
                    player,
                    "qouteall.imm_ptl.core.commands.ClientDebugCommand.RemoteCallables.doListPortals"
                );
                return 0;
            })
        );
        
        builder.then(Commands.literal("report_resource_consumption")
            .requires(serverCommandSource -> serverCommandSource.hasPermission(2))
            .executes(context -> {
                StringBuilder str = new StringBuilder();
                
                str.append("Server Tracked Chunks:\n");
                MiscHelper.getServer().getAllLevels().forEach(
                    world -> {
                        str.append(getServerWorldResourceConsumption(world));
                    }
                );
                
                McHelper.serverLog(context.getSource().getPlayerOrException(), str.toString());
                
                McRemoteProcedureCall.tellClientToInvoke(
                    context.getSource().getPlayerOrException(),
                    "qouteall.imm_ptl.core.commands.ClientDebugCommand.RemoteCallables.reportResourceConsumption"
                );
                
                return 0;
            })
        );
        
        builder.then(Commands.literal("report_chunk_ticket_stat")
            .requires(serverCommandSource -> serverCommandSource.hasPermission(2))
            .executes(context -> {
                ServerLevel world = context.getSource().getLevel();
                Iterable<ChunkHolder> chunkHolders = ((IEChunkMap_Accessor) world.getChunkSource().chunkMap).ip_getChunks();
                
                Object2IntOpenHashMap<TicketType<?>> stat = new Object2IntOpenHashMap<>();
                for (ChunkHolder chunkHolder : chunkHolders) {
                    long chunkPos = chunkHolder.getPos().toLong();
                    SortedArraySet<Ticket<?>> chunkTickets =
                        ((IEDistanceManager) getDistanceManager(world))
                            .portal_getTicketSet(chunkPos);
                    
                    for (Ticket<?> ticket : chunkTickets) {
                        stat.addTo(ticket.getType(), 1);
                    }
                }
                
                context.getSource().sendSuccess(() -> Component.literal(""), false);
                for (Object2IntMap.Entry<TicketType<?>> entry : stat.object2IntEntrySet()) {
                    TicketType<?> ticketType = entry.getKey();
                    context.getSource().sendSuccess(
                        () -> Component.literal(ticketType.toString() + " " + entry.getIntValue()),
                        false
                    );
                }
                
                return 0;
            })
        );
        
        builder.then(Commands.literal("report_per_player_chunk_loading")
            .requires(serverCommandSource -> serverCommandSource.hasPermission(2))
            .executes(context -> {
                List<ServerPlayer> players = MiscHelper.getServer().getPlayerList().getPlayers();
                for (ServerPlayer player : players) {
                    PlayerChunkLoading playerInfo = ImmPtlChunkTracking.getPlayerInfo(player);
                    String text = "%s %d".formatted(player.getName().getString(), playerInfo.loadedChunks);
                    context.getSource().sendSuccess(() -> Component.literal(text), true);
                }
                return 0;
            })
        );
        
        builder.then(Commands.literal("save_all_chunks")
            .requires(serverCommandSource -> serverCommandSource.hasPermission(2))
            .executes(context -> {
                MinecraftServer server = context.getSource().getServer();
                server.saveAllChunks(true, true, false);
                return 0;
            })
        );
        
        builder.then(Commands
            .literal("simplify_portal_mesh")
            .requires(serverCommandSource -> serverCommandSource.hasPermission(2))
            .executes(context -> PortalCommand.processPortalTargetedCommand(context, portal -> {
                GeometryPortalShape shape = portal.specialShape;
                if (shape != null) {
                    shape.mesh.simplifySteps(1);
                    portal.reloadPortal();
                }
            }))
        );


//        builder.then(Commands.literal("save_all_chunks_offthread")
//            .requires(serverCommandSource -> serverCommandSource.hasPermission(2))
//            .executes(context -> {
//                MinecraftServer server = context.getSource().getServer();
//
//                new Thread(() -> {
//                    server.saveAllChunks(true, true, false);
//                    LOGGER.info("Saving finished off-thread");
//                }).start();
//
//                return 0;
//            })
//        );

//        builder.then(Commands.literal("report_chunk_level_stat")
//            .requires(serverCommandSource -> serverCommandSource.hasPermission(2))
//            .executes(context -> {
//                ServerLevel world = context.getSource().getLevel();
//                Iterable<ChunkHolder> chunkHolders = ((IEChunkMap_Accessor) world.getChunkSource().chunkMap).ip_getChunks();
//
//                Int2IntAVLTreeMap ticketLevelStat = new Int2IntAVLTreeMap();
//                Int2IntAVLTreeMap queueLevelStat = new Int2IntAVLTreeMap();
//                for (ChunkHolder chunkHolder : chunkHolders) {
//                    int ticketLevel = chunkHolder.getTicketLevel();
//                    ticketLevelStat.addTo(ticketLevel, 1);
//
//                    int queueLevel = chunkHolder.getQueueLevel();
//                    queueLevelStat.addTo(queueLevel, 1);
//                }
//
//                context.getSource().sendSuccess(() -> Component.literal("Ticket Level Stat:"), true);
//                for (Int2IntMap.Entry entry : ticketLevelStat.int2IntEntrySet()) {
//                    int level = entry.getIntKey();
//                    int count = entry.getIntValue();
//                    context.getSource().sendSuccess(
//                        () -> Component.literal(level + ": " + count),
//                        true
//                    );
//                }
//
//                context.getSource().sendSuccess(() -> Component.literal("Queue Level Stat:"), true);
//                for (Int2IntMap.Entry entry : queueLevelStat.int2IntEntrySet()) {
//                    int level = entry.getIntKey();
//                    int count = entry.getIntValue();
//                    context.getSource().sendSuccess(
//                        () -> Component.literal(level + ": " + count),
//                        true
//                    );
//                }
//
//                return 0;
//            })
//        );
        
        builder.then(Commands
            .literal("check_biome_registry")
            .requires(serverCommandSource -> serverCommandSource.hasPermission(3))
            .executes(context -> {
                RegistryAccess.Frozen registryAccess = MiscHelper.getServer().registryAccess();
                Registry<Biome> biomes = registryAccess.registryOrThrow(Registries.BIOME);
                Map<String, Integer> map = new HashMap<>();
                for (Map.Entry<ResourceKey<Biome>, Biome> entry : biomes.entrySet()) {
                    String strId = entry.getKey().location().toString();
                    int intId = biomes.getId(entry.getValue());
                    map.put(strId, intId);
                }
                McRemoteProcedureCall.tellClientToInvoke(
                    context.getSource().getPlayerOrException(),
                    "qouteall.imm_ptl.core.ClientWorldLoader.RemoteCallables.checkBiomeRegistry",
                    map
                );
                return 0;
            })
        );

//        builder.then(Commands
//            .literal("print_biome_list")
//            .requires(serverCommandSource -> serverCommandSource.hasPermission(3))
//            .executes(context -> {
//                Registry<Biome> biomes = MiscHelper.getServer().registryAccess().registryOrThrow(Registries.BIOME);
//
//                StringBuilder builder1 = new StringBuilder();
//                for (ResourceLocation resourceLocation : biomes.keySet()) {
//                    builder1.append("\"");
//                    builder1.append(resourceLocation);
//                    builder1.append("\",\n");
//                }
//
//                Helper.log(builder1.toString());
//
//                return 0;
//            })
//        );

//        builder.then(Commands
//            .literal("print_generator_config")
//            .requires(serverCommandSource -> serverCommandSource.hasPermission(3))
//            .executes(context -> {
//                MiscHelper.getServer().getAllLevels().forEach(world -> {
//                    ChunkGenerator generator = world.getChunkSource().getGenerator();
//                    Helper.log(world.dimension().location());
//                    Helper.log(McHelper.serializeToJson(generator, ChunkGenerator.CODEC));
//                    Helper.log(McHelper.serializeToJson(
//                        world.dimensionType(),
//                        DimensionType.DIRECT_CODEC.stable()
//                    ));
//                });
//
//                WorldGenSettings options = MiscHelper.getServer().getWorldData().worldGenSettings();
//
//                Helper.log(McHelper.serializeToJson(options, WorldGenSettings.CODEC));
//
//                return 0;
//            })
//        );
        
        builder.then(Commands
            .literal("nofog_enable")
            .executes(context -> {
                McRemoteProcedureCall.tellClientToInvoke(
                    context.getSource().getPlayerOrException(),
                    "qouteall.imm_ptl.core.commands.ClientDebugCommand.RemoteCallables.setNoFog",
                    true
                );
                return 0;
            })
        );
        
        builder.then(Commands
            .literal("nofog_disable")
            .executes(context -> {
                McRemoteProcedureCall.tellClientToInvoke(
                    context.getSource().getPlayerOrException(),
                    "qouteall.imm_ptl.core.commands.ClientDebugCommand.RemoteCallables.setNoFog",
                    false
                );
                return 0;
            })
        );
        
        builder.then(Commands
            .literal("report_air")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                BlockState blockState = player.level().getBlockState(player.blockPosition());
                
                context.getSource().sendSuccess(
                    () -> blockState.getBlock().getName(),
                    false
                );
                return 0;
            })
        );
        
        builder.then(Commands
            .literal("test_invalid_rpc")
            .executes(context -> {
                McRemoteProcedureCall.tellClientToInvoke(
                    context.getSource().getPlayerOrException(),
                    "qouteall.imm_ptl.core.commands.ClientDebugCommand.RemoteCallables.testInvalidRPC"
                );
                return 0;
            })
        );
    }
    
    private static void doReportChunkStatus(int chunkX, int chunkZ, ServerLevel world, ServerPlayer player) {
        LevelChunk chunk = McHelper.getServerChunkIfPresent(
            world,
            chunkX, chunkZ
        );
        
        boolean loaded = chunk != null && !(chunk instanceof EmptyLevelChunk);
        
        long longChunkPos = ChunkPos.asLong(chunkX, chunkZ);
        if (loaded) {
            boolean shouldTickEntities =
                getDistanceManager(world).inEntityTickingRange(longChunkPos);
            if (shouldTickEntities) {
                McHelper.serverLog(player, "Server chunk loaded and entity tickable");
            }
            else {
                McHelper.serverLog(player, "Server chunk loaded but entity not tickable");
            }
        }
        else {
            McHelper.serverLog(player, "Server chunk not loaded");
        }
        
        ChunkHolder chunkHolder = McHelper.getIEChunkMap(world.dimension()).ip_getChunkHolder(
            longChunkPos
        );
        
        if (chunkHolder == null) {
            McHelper.serverLog(player, "no chunk holder");
        }
        else {
            McHelper.serverLog(
                player,
                String.format(
                    "chunk holder level:%s %s",
                    chunkHolder.getTicketLevel(),
                    chunk == null ? "" : chunk.getFullStatus()
                )
            );
            
            DistanceManager distanceManager =
                ((IEServerChunkManager) world.getChunkSource()).ip_getDistanceManager();
            SortedArraySet<Ticket<?>> tickets = ((IEDistanceManager) distanceManager).portal_getTicketSet(longChunkPos);
            for (Ticket<?> ticket : tickets) {
                McHelper.serverLog(
                    player,
                    ticket.toString()
                );
            }
        }
        
        McRemoteProcedureCall.tellClientToInvoke(
            player,
            "qouteall.imm_ptl.core.commands.ClientDebugCommand.RemoteCallables.reportClientChunkLoadStatus",
            world.dimension(), chunkX, chunkZ
        );
    }
    
    public static String getServerWorldResourceConsumption(ServerLevel world) {
        StringBuilder subStr = new StringBuilder();
        
        ImmPtlChunkTickets dimTicketManager = ImmPtlChunkTickets.get(world);
        LevelEntityGetter<Entity> entityLookup = ((IEWorld) world).portal_getEntityLookup();
        
        subStr.append(String.format(
            "%s:\nImmPtl Tracked Chunks: %s\nImmPtl Loading Ticket:%s\nChunks: %s\nEntities:%s Entity Sections:%s\n",
            world.dimension().location(),
            ImmPtlChunkTracking.getLoadedChunkNum(world.dimension()),
            dimTicketManager.getLoadedChunkNum(),
            world.getChunkSource().chunkMap.size(),
            ((IELevelEntityGetterAdapter) entityLookup).getIndex().count(),
            ((IELevelEntityGetterAdapter) entityLookup).getCache().count()
        ));
        
        PersistentEntitySectionManager<Entity> entityManager = ((IEServerWorld) world).ip_getEntityManager();
        entityManager.saveAll();
        subStr.append(String.format(
            "Entity Manager: %s\n",
            entityManager.gatherStats()
        ));
        
        subStr.append("\n");
        
        String result = subStr.toString();
        return result;
    }
    
    public static long toMiB(long bytes) {
        return bytes / 1024L / 1024L;
    }
    
    public static void invokeEraseChunk(Level world, ChunkPos center, int r, int downY, int upY) {
        ArrayList<ChunkPos> poses = new ArrayList<>();
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                poses.add(new ChunkPos(x + center.x, z + center.z));
            }
        }
        poses.sort(Comparator.comparingDouble(c ->
            Vec3.atLowerCornerOf(center.getWorldPosition()).distanceTo(Vec3.atLowerCornerOf(c.getWorldPosition()))
        ));
        
        IPGlobal.serverTaskList.addTask(MyTaskList.chainTasks(
            poses.stream().map(chunkPos -> (MyTaskList.MyTask) () -> {
                eraseChunk(
                    chunkPos, world, downY, upY
                );
                return true;
            }).iterator()
        ));
    }
    
    public static void eraseChunk(ChunkPos chunkPos, Level world, int yStart, int yEnd) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = yStart; y < yEnd; y++) {
                    world.setBlockAndUpdate(
                        new BlockPos(
                            chunkPos.getMinBlockX() + x,
                            y,
                            chunkPos.getMinBlockZ() + z
                        ),
                        Blocks.AIR.defaultBlockState()
                    );
                }
            }
        }
    }
    
    
}
