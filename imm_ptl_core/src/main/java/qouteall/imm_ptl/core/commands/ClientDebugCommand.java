package qouteall.imm_ptl.core.commands;

import com.google.common.collect.Streams;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.q_misc_util.Helper;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.ducks.IEWorldRenderer;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.api.McRemoteProcedureCall;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalRenderInfo;
import qouteall.imm_ptl.core.render.MyBuiltChunkStorage;
import qouteall.imm_ptl.core.render.PortalGroup;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.EmptyChunk;

import java.lang.ref.Reference;
import java.net.URLClassLoader;
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
        CommandDispatcher<FabricClientCommandSource> dispatcher
    ) {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager
            .literal("imm_ptl_client_debug")
            .requires(commandSource -> true)
            .then(ClientCommandManager
                .literal("set_max_portal_layer")
                .then(ClientCommandManager
                    .argument(
                        "argMaxPortalLayer", IntegerArgumentType.integer()
                    )
                    .executes(context -> setMaxPortalLayer(
                        IntegerArgumentType.getInteger(context, "argMaxPortalLayer")
                    ))
                )
            );
        builder = builder.then(ClientCommandManager
            .literal("list_portals")
            .executes(context -> {
                RemoteCallables.doListPortals();
                return 0;
            })
        );
        builder = builder.then(ClientCommandManager
            .literal("is_client_chunk_loaded")
            .then(ClientCommandManager
                .argument(
                    "chunkX", IntegerArgumentType.integer()
                )
                .then(ClientCommandManager
                    .argument(
                        "chunkZ", IntegerArgumentType.integer()
                    )
                    .executes(
                        ClientDebugCommand::isClientChunkLoaded
                    )
                )
            )
        );
        
        builder = builder.then(ClientCommandManager
            .literal("report_player_status")
            .executes(context -> {
                RemoteCallables.reportClientPlayerStatus();
                return 0;
            })
        );
        builder = builder.then(ClientCommandManager
            .literal("client_remote_ticking_enable")
            .executes(context -> {
                IPCGlobal.isClientRemoteTickingEnabled = true;
                return 0;
            })
        );
        builder = builder.then(ClientCommandManager
            .literal("client_remote_ticking_disable")
            .executes(context -> {
                IPCGlobal.isClientRemoteTickingEnabled = false;
                return 0;
            })
        );
        builder = builder.then(ClientCommandManager
            .literal("advanced_frustum_culling_enable")
            .executes(context -> {
                IPCGlobal.doUseAdvancedFrustumCulling = true;
                return 0;
            })
        );
        builder = builder.then(ClientCommandManager
            .literal("advanced_frustum_culling_disable")
            .executes(context -> {
                IPCGlobal.doUseAdvancedFrustumCulling = false;
                return 0;
            })
        );
//        builder = builder.then(ClientCommandManager
//            .literal("hacked_chunk_render_dispatcher_enable")
//            .executes(context -> {
//                IPCGlobal.useHackedChunkRenderDispatcher = true;
//                return 0;
//            })
//        );
//        builder = builder.then(ClientCommandManager
//            .literal("hacked_chunk_render_dispatcher_disable")
//            .executes(context -> {
//                IPCGlobal.useHackedChunkRenderDispatcher = false;
//                return 0;
//            })
//        );
        builder = builder.then(ClientCommandManager
            .literal("report_resource_consumption")
            .executes(context1 -> {
                RemoteCallables.reportResourceConsumption();
                
                return 0;
            })
        );
        builder = builder.then(ClientCommandManager
            .literal("report_render_info_num")
            .executes(context -> {
                String str = Helper.myToString(IPCGlobal.renderInfoNumMap.entrySet().stream());
                context.getSource().getPlayer().sendMessage(new LiteralText(str), false);
                return 0;
            })
        );
        builder = builder.then(ClientCommandManager
            .literal("get_player_colliding_portal_client")
            .executes(context -> {
                Portal collidingPortal =
                    ((IEEntity) MinecraftClient.getInstance().player).getCollidingPortal();
                CHelper.printChat(
                    collidingPortal != null ? collidingPortal.toString() : "null"
                );
                return 0;
            })
        );
        builder = builder.then(ClientCommandManager
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
                CHelper.printChat(str);
                return 0;
            })
        );
        builder = builder.then(ClientCommandManager
            .literal("vanilla_chunk_culling_enable")
            .executes(context -> {
                MinecraftClient.getInstance().chunkCullingEnabled = true;
                return 0;
            })
        );
        builder = builder.then(ClientCommandManager
            .literal("vanilla_chunk_culling_disable")
            .executes(context -> {
                MinecraftClient.getInstance().chunkCullingEnabled = false;
                return 0;
            })
        );
        builder = builder.then(ClientCommandManager
            .literal("render_mode_normal")
            .executes(context -> {
                IPGlobal.renderMode = IPGlobal.RenderMode.normal;
                return 0;
            })
        );
        builder = builder.then(ClientCommandManager
            .literal("render_mode_compatibility")
            .executes(context -> {
                IPGlobal.renderMode = IPGlobal.RenderMode.compatibility;
                return 0;
            })
        );
        builder = builder.then(ClientCommandManager
            .literal("render_mode_debug")
            .executes(context -> {
                IPGlobal.renderMode = IPGlobal.RenderMode.debug;
                return 0;
            })
        );
        builder = builder.then(ClientCommandManager
            .literal("render_mode_none")
            .executes(context -> {
                IPGlobal.renderMode = IPGlobal.RenderMode.none;
                return 0;
            })
        );
        
        builder.then(ClientCommandManager
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
        builder.then(ClientCommandManager
            .literal("check_server_light")
            .executes(context -> {
                MiscHelper.getServer().execute(() -> {
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
        builder.then(ClientCommandManager
                .literal("update_server_light")
                .executes(context -> {
                    MiscHelper.getServer().execute(() -> {
                        ServerPlayerEntity player = McHelper.getRawPlayerList().get(0);
                        
                        ServerLightingProvider lightingProvider = (ServerLightingProvider) player.world.getLightingProvider();
                        lightingProvider.light(
                            player.world.getChunk(player.getBlockPos()),
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
        
        builder.then(ClientCommandManager
            .literal("report_rebuild_status")
            .executes(context -> {
                MinecraftClient.getInstance().execute(() -> {
                    ClientWorldLoader.getClientWorlds().forEach((world) -> {
                        MyBuiltChunkStorage builtChunkStorage = (MyBuiltChunkStorage) ((IEWorldRenderer)
                            ClientWorldLoader.getWorldRenderer(world.getRegistryKey()))
                            .ip_getBuiltChunkStorage();
                        CHelper.printChat(
                            world.getRegistryKey().getValue().toString() + builtChunkStorage.getDebugString()
                        );
                    });
                });
                
                return 0;
            })
        );
        
        builder.then(ClientCommandManager
            .literal("report_portal_groups")
            .executes(context -> {
                for (ClientWorld clientWorld : ClientWorldLoader.getClientWorlds()) {
                    Map<Optional<PortalGroup>, List<Portal>> result =
                        Streams.stream(clientWorld.getEntities())
                            .flatMap(
                                entity -> entity instanceof Portal ?
                                    Stream.of(((Portal) entity)) : Stream.empty()
                            )
                            .collect(Collectors.groupingBy(
                                p -> Optional.ofNullable(PortalRenderInfo.getGroupOf(p))
                            ));
                    
                    CHelper.printChat("\n" + clientWorld.getRegistryKey().getValue().toString());
                    result.forEach((g, l) -> {
                        CHelper.printChat("\n" + g.toString());
                        CHelper.printChat(l.stream()
                            .map(Portal::toString).collect(Collectors.joining("\n"))
                        );
                    });
                }
                return 0;
            })
        );
        builder.then(ClientCommandManager
            .literal("report_client_light_status")
            .executes(context -> {
                MinecraftClient.getInstance().execute(() -> {
                    ClientPlayerEntity player = MinecraftClient.getInstance().player;
                    ChunkNibbleArray lightSection = player.world.getLightingProvider().get(LightType.BLOCK).getLightSection(
                        ChunkSectionPos.from(player.getBlockPos())
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
                            )
                        );
                    }
                    else {
                        context.getSource().sendFeedback(
                            new LiteralText("does not have light section")
                        );
                    }
                });
                return 0;
            })
        );
        
        builder.then(ClientCommandManager
            .literal("reload_world_renderer")
            .executes(context -> {
                MinecraftClient.getInstance().execute(() -> {
                    ClientWorldLoader.disposeRenderHelpers();
                    MinecraftClient.getInstance().worldRenderer.reload();
                });
                return 0;
            })
        );
        
        registerSwitchCommand(
            builder,
            "front_clipping",
            cond -> IPCGlobal.useFrontClipping = cond
        );
        registerSwitchCommand(
            builder,
            "gl_check_error",
            cond -> IPGlobal.doCheckGlError = cond
        );
        registerSwitchCommand(
            builder,
            "smooth_chunk_unload",
            cond -> IPCGlobal.smoothChunkUnload = cond
        );
        
        registerSwitchCommand(
            builder,
            "early_light_update",
            cond -> IPCGlobal.earlyClientLightUpdate = cond
        );
        registerSwitchCommand(
            builder,
            "super_advanced_frustum_culling",
            cond -> IPCGlobal.useSuperAdvancedFrustumCulling = cond
        );
        registerSwitchCommand(
            builder,
            "cross_portal_entity_rendering",
            cond -> IPGlobal.correctCrossPortalEntityRendering = cond
        );
        registerSwitchCommand(
            builder,
            "loose_visible_chunk_iteration",
            cond -> IPGlobal.looseVisibleChunkIteration = cond
        );
        registerSwitchCommand(
            builder,
            "early_cull_portal",
            cond -> IPCGlobal.earlyFrustumCullingPortal = cond
        );
        registerSwitchCommand(
            builder,
            "cache_gl_buffer",
            cond -> IPGlobal.cacheGlBuffer = cond
        );
        registerSwitchCommand(
            builder,
            "secondary_vertex_consumer",
            cond -> IPGlobal.useSecondaryEntityVertexConsumer = cond
        );
        registerSwitchCommand(
            builder,
            "cull_sections_behind",
            cond -> IPGlobal.cullSectionsBehind = cond
        );
        registerSwitchCommand(
            builder,
            "offset_occlusion_query",
            cond -> IPGlobal.offsetOcclusionQuery = cond
        );
        registerSwitchCommand(
            builder,
            "cloud_optimization",
            cond -> IPGlobal.cloudOptimization = cond
        );
        registerSwitchCommand(
            builder,
            "cross_portal_collision",
            cond -> IPGlobal.crossPortalCollision = cond
        );
        registerSwitchCommand(
            builder,
            "nofog",
            cond -> IPGlobal.debugDisableFog = cond
        );
        registerSwitchCommand(
            builder,
            "depth_clamp_for_portal_rendering",
            cond -> IPGlobal.enableDepthClampForPortalRendering = cond
        );
        registerSwitchCommand(
            builder,
            "shared_block_mesh_builder",
            cond -> IPGlobal.enableSharedBlockMeshBuffers = cond
        );
        
        builder.then(ClientCommandManager
            .literal("print_class_path")
            .executes(context -> {
                printClassPath();
                return 0;
            })
        );
        
        dispatcher.register(builder);
        
        Helper.log("Successfully initialized command /immersive_portals_debug");
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
        LiteralArgumentBuilder<FabricClientCommandSource> builder,
        String name,
        Consumer<Boolean> setFunction
    ) {
        builder = builder.then(ClientCommandManager
            .literal(name + "_enable")
            .executes(context -> {
                setFunction.accept(true);
                return 0;
            })
        );
        builder = builder.then(ClientCommandManager
            .literal(name + "_disable")
            .executes(context -> {
                setFunction.accept(false);
                return 0;
            })
        );
    }
    
    private static int isClientChunkLoaded(CommandContext<FabricClientCommandSource> context) throws CommandSyntaxException {
        int chunkX = IntegerArgumentType.getInteger(context, "chunkX");
        int chunkZ = IntegerArgumentType.getInteger(context, "chunkZ");
        RemoteCallables.reportClientChunkLoadStatus(chunkX, chunkZ);
        return 0;
    }
    
    public static class RemoteCallables {
        public static void reportClientChunkLoadStatus(int chunkX, int chunkZ) {
            Chunk chunk = MinecraftClient.getInstance().world.getChunk(
                chunkX, chunkZ
            );
            CHelper.printChat(chunk != null && !(chunk instanceof EmptyChunk) ? "yes" : "no");
        }
        
        public static void reportClientPlayerStatus() {
            ClientPlayerEntity playerSP = MinecraftClient.getInstance().player;
            
            CHelper.printChat(
                String.format(
                    "On Client %s %s removal:%s added:%s age:%s",
                    playerSP.world.getRegistryKey().getValue(),
                    playerSP.getBlockPos(),
                    playerSP.getRemovalReason(),
                    playerSP.world.getEntityById(playerSP.getId()) != null,
                    playerSP.age
                )
            );
        }
        
        public static void doListPortals() {
            StringBuilder result = new StringBuilder();
            
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
            
            CHelper.printChat(result.toString());
        }
        
        public static void reportResourceConsumption() {
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
                            .ip_getBuiltChunkStorage()
                        ).getManagedSectionNum()
                    ));
                }
            );
            
            String result = str.toString();
            
            CHelper.printChat(result);
        }
        
        public static void setNoFog(boolean cond) {
            IPGlobal.debugDisableFog = cond;
        }
    }
    
    private static int setMaxPortalLayer(int m) {
        IPGlobal.maxPortalLayer = m;
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
            NbtCompound compoundTag, Text text, int[] intArray
        ) {
            Helper.log(
                player.getName().asString() + uuid + block + blockState + item + itemStack
                    + compoundTag + text + Arrays.toString(intArray)
            );
        }
    }
    
    private static void testRemoteProcedureCall(ServerPlayerEntity player) {
        MinecraftClient.getInstance().execute(() -> {
            NbtCompound compoundTag = new NbtCompound();
            compoundTag.put("test", NbtInt.of(7));
            McRemoteProcedureCall.tellServerToInvoke(
                "qouteall.imm_ptl.core.commands.ClientDebugCommand.TestRemoteCallable.clientToServer",
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
        
        MiscHelper.getServer().execute(() -> {
            McRemoteProcedureCall.tellClientToInvoke(
                player,
                "qouteall.imm_ptl.core.commands.ClientDebugCommand.TestRemoteCallable.serverToClient",
                "string", 2, 3.5, new Identifier("imm_ptl:oops"),
                World.NETHER, BiomeKeys.JUNGLE,
                new BlockPos(3, 5, 4),
                new Vec3d(7, 4, 1)
            );
        });
    }
}
