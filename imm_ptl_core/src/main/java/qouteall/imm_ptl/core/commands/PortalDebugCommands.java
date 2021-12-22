package qouteall.imm_ptl.core.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.ProfilerSystem;
import net.minecraft.world.World;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.entity.EntityLookup;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.api.example.ExampleGuiPortalRendering;
import qouteall.imm_ptl.core.chunk_loading.ChunkVisibility;
import qouteall.imm_ptl.core.chunk_loading.MyLoadingTicket;
import qouteall.imm_ptl.core.chunk_loading.NewChunkTrackingGraph;
import qouteall.imm_ptl.core.ducks.IEServerEntityManager;
import qouteall.imm_ptl.core.ducks.IEServerWorld;
import qouteall.imm_ptl.core.ducks.IEWorld;
import qouteall.imm_ptl.core.mixin.common.mc_util.IESimpleEntityLookup;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.api.McRemoteProcedureCall;
import qouteall.q_misc_util.my_util.MyTaskList;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PortalDebugCommands {
    static void registerDebugCommands(
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
                        "qouteall.imm_ptl.core.render.TransformationManager.RemoteCallables.enableIsometricView",
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
                    "qouteall.imm_ptl.core.render.TransformationManager.RemoteCallables.disableIsometricView"
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
        
        builder.then(CommandManager.literal("profile")
            .then(CommandManager
                .literal("set_lag_logging_threshold")
                .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(4))
                .then(CommandManager.argument("ms", IntegerArgumentType.integer())
                    .executes(context -> {
                        int ms = IntegerArgumentType.getInteger(context, "ms");
                        ProfilerSystem.TIMEOUT_NANOSECONDS = Duration.ofMillis(ms).toNanos();
                        
                        return 0;
                    })
                )
            ).then(CommandManager
                .literal("gc")
                .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(4))
                .executes(context -> {
                    System.gc();
                    
                    long l = Runtime.getRuntime().maxMemory();
                    long m = Runtime.getRuntime().totalMemory();
                    long n = Runtime.getRuntime().freeMemory();
                    long o = m - n;
                    
                    context.getSource().sendFeedback(
                        new LiteralText(
                            String.format("Memory: % 2d%% %03d/%03dMB", o * 100L / l, toMiB(o), toMiB(l))
                        ),
                        false
                    );
                    
                    return 0;
                })
            )
        );
        
        builder.then(CommandManager
            .literal("create_command_stick")
            .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(2))
            .then(CommandManager.argument("command", StringArgumentType.string())
                .executes(context -> {
                    PortalCommand.createCommandStickCommandSignal.emit(
                        context.getSource().getPlayer(),
                        StringArgumentType.getString(context, "command")
                    );
                    return 0;
                })
            )
        );
        
        builder.then(CommandManager
            .literal("accelerate")
            .requires(PortalCommand::canUsePortalCommand)
            .then(CommandManager
                .argument("v", DoubleArgumentType.doubleArg())
                .executes(context -> {
                    double v = DoubleArgumentType.getDouble(context, "v");
                    McRemoteProcedureCall.tellClientToInvoke(
                        context.getSource().getPlayer(),
                        "qouteall.imm_ptl.core.commands.PortalCommand.RemoteCallables.clientAccelerate",
                        v
                    );
                    return 0;
                })
            )
        );
        
        builder.then(CommandManager.literal("test")
            .executes(context -> {
                return 0;
            })
        );
        
        builder.then(CommandManager
            .literal("erase_chunk")
            .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(3))
            .then(CommandManager.argument("rChunks", IntegerArgumentType.integer())
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    
                    ChunkPos center = new ChunkPos(new BlockPos(player.getPos()));
                    
                    invokeEraseChunk(
                        player.world, center,
                        IntegerArgumentType.getInteger(context, "rChunks"),
                        McHelper.getMinY(player.world), McHelper.getMaxYExclusive(player.world)
                    );
                    
                    return 0;
                })
                .then(CommandManager.argument("downY", IntegerArgumentType.integer())
                    .then(CommandManager.argument("upY", IntegerArgumentType.integer())
                        .executes(context -> {
                            
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            
                            ChunkPos center = new ChunkPos(new BlockPos(player.getPos()));
                            
                            invokeEraseChunk(
                                player.world, center,
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
        
        builder.then(CommandManager
            .literal("report_chunk_loaders")
            .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(3))
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                ChunkVisibility.getBaseChunkLoaders(
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
            .literal("report_server_entities")
            .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(3))
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
        
        builder.then(CommandManager.literal("is_chunk_loaded")
            .then(CommandManager.argument("dim", DimensionArgumentType.dimension())
                .then(CommandManager.argument("chunkX", IntegerArgumentType.integer())
                    .then(CommandManager.argument("chunkZ", IntegerArgumentType.integer())
                        .executes(context -> {
                            int chunkX = IntegerArgumentType.getInteger(context, "chunkX");
                            int chunkZ = IntegerArgumentType.getInteger(context, "chunkZ");
                            ServerWorld dim = DimensionArgumentType.getDimensionArgument(context, "dim");
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            
                            WorldChunk chunk = McHelper.getServerChunkIfPresent(
                                dim,
                                chunkX, chunkZ
                            );
                            
                            boolean loaded = chunk != null && !(chunk instanceof EmptyChunk);
                            
                            if (loaded) {
                                long longPos = ChunkPos.toLong(chunkX, chunkZ);
                                boolean shouldTickEntities =
                                    MyLoadingTicket.getTicketManager(dim).shouldTickEntities(longPos);
                                if (shouldTickEntities) {
                                    McHelper.serverLog(player, "server chunk loaded and entity tickable");
                                }
                                else {
                                    McHelper.serverLog(player, "server chunk loaded but entity not tickable");
                                }
                            }
                            else {
                                McHelper.serverLog(player, "server chunk not loaded");
                            }
                            
                            
                            McRemoteProcedureCall.tellClientToInvoke(
                                player,
                                "qouteall.imm_ptl.core.commands.ClientDebugCommand.RemoteCallables.reportClientChunkLoadStatus",
                                chunkX, chunkZ
                            );
                            
                            return 0;
                        })
                    )
                )
            )
        );
        
        builder.then(CommandManager.literal("report_player_status")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                
                CHelper.printChat(
                    String.format(
                        "On Server %s %s removal:%s added:%s age:%s",
                        player.world.getRegistryKey().getValue(),
                        player.getBlockPos(),
                        player.getRemovalReason(),
                        player.world.getEntityById(player.getId()) != null,
                        player.age
                    )
                );
                
                McRemoteProcedureCall.tellClientToInvoke(
                    player,
                    "qouteall.imm_ptl.core.commands.ClientDebugCommand.RemoteCallables.reportClientPlayerStatus"
                );
                
                return 0;
            })
        );
        
        builder.then(CommandManager.literal("list_portals")
            .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(3))
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                
                StringBuilder result = new StringBuilder();
                result.append("Server Portals\n");
                
                for (ServerWorld world : MiscHelper.getServer().getWorlds()) {
                    result.append(world.getRegistryKey().getValue().toString() + "\n");
                    for (Entity entity : world.iterateEntities()) {
                        for (Entity e : world.iterateEntities()) {
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
        
        builder.then(CommandManager.literal("report_resource_consumption")
            .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(2))
            .executes(context -> {
                StringBuilder str = new StringBuilder();
                
                str.append("Server Tracked Chunks:\n");
                MiscHelper.getServer().getWorlds().forEach(
                    world -> {
                        LongSortedSet rec = MyLoadingTicket.loadedChunkRecord.get(world);
                        EntityLookup<Entity> entityLookup = ((IEWorld) world).portal_getEntityLookup();
                        
                        str.append(String.format(
                            "%s:\nIP Tracked Chunks: %s\nIP Loading Ticket:%s\nEntities:%s Entity Sections:%s\n",
                            world.getRegistryKey().getValue(),
                            NewChunkTrackingGraph.getLoadedChunkNum(world.getRegistryKey()),
                            rec == null ? "null" : rec.size(),
                            ((IESimpleEntityLookup) entityLookup).getIndex().size(),
                            ((IESimpleEntityLookup) entityLookup).getCache().sectionCount()
                        ));
                        
                        ServerEntityManager<Entity> entityManager = ((IEServerWorld) world).ip_getEntityManager();
                        entityManager.flush();
                        str.append(String.format(
                            "Entity Manager: %s\n",
                            entityManager.getDebugString()
                        ));
                        
                        str.append("\n");
                    }
                );
                
                McHelper.serverLog(context.getSource().getPlayer(), str.toString());
                
                McRemoteProcedureCall.tellClientToInvoke(
                    context.getSource().getPlayer(),
                    "qouteall.imm_ptl.core.commands.ClientDebugCommand.RemoteCallables.reportResourceConsumption"
                );
                
                return 0;
            })
        );
        
        builder.then(CommandManager
            .literal("print_generator_config")
            .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(3))
            .executes(context -> {
                MiscHelper.getServer().getWorlds().forEach(world -> {
                    ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
                    Helper.log(world.getRegistryKey().getValue());
                    Helper.log(McHelper.serializeToJson(generator, ChunkGenerator.CODEC));
                    Helper.log(McHelper.serializeToJson(
                        world.getDimension(),
                        DimensionType.CODEC.stable()
                    ));
                });
                
                GeneratorOptions options = MiscHelper.getServer().getSaveProperties().getGeneratorOptions();
                
                Helper.log(McHelper.serializeToJson(options, GeneratorOptions.CODEC));
                
                return 0;
            })
        );
        
        builder.then(CommandManager
            .literal("nofog_enable")
            .executes(context -> {
                McRemoteProcedureCall.tellClientToInvoke(
                    context.getSource().getPlayer(),
                    "qouteall.imm_ptl.core.commands.ClientDebugCommand.RemoteCallables.setNoFog",
                    true
                );
                return 0;
            })
        );
        
        builder.then(CommandManager
            .literal("nofog_disable")
            .executes(context -> {
                McRemoteProcedureCall.tellClientToInvoke(
                    context.getSource().getPlayer(),
                    "qouteall.imm_ptl.core.commands.ClientDebugCommand.RemoteCallables.setNoFog",
                    false
                );
                return 0;
            })
        );
        
        builder.then(CommandManager
            .literal("report_air")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                BlockState blockState = player.world.getBlockState(player.getBlockPos());
                
                context.getSource().sendFeedback(
                    blockState.getBlock().getName(),
                    false
                );
                return 0;
            })
        );
    }
    
    public static long toMiB(long bytes) {
        return bytes / 1024L / 1024L;
    }
    
    public static void invokeEraseChunk(World world, ChunkPos center, int r, int downY, int upY) {
        ArrayList<ChunkPos> poses = new ArrayList<>();
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                poses.add(new ChunkPos(x + center.x, z + center.z));
            }
        }
        poses.sort(Comparator.comparingDouble(c ->
            Vec3d.of(center.getStartPos()).distanceTo(Vec3d.of(c.getStartPos()))
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
    
    
}
