package qouteall.imm_ptl.core.commands;

import com.google.common.collect.Streams;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEClientWorld;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.ducks.IEWorldRenderer;
import qouteall.imm_ptl.core.platform_specific.IPConfig;
import qouteall.imm_ptl.core.platform_specific.IPConfigGUI;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalRenderInfo;
import qouteall.imm_ptl.core.render.MyBuiltChunkStorage;
import qouteall.imm_ptl.core.render.MyGameRenderer;
import qouteall.imm_ptl.core.render.PortalGroup;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.api.McRemoteProcedureCall;
import qouteall.q_misc_util.my_util.MyTaskList;

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
                context.getSource().getPlayer().displayClientMessage(Component.literal(str), false);
                return 0;
            })
        );
        builder = builder.then(ClientCommandManager
            .literal("get_player_colliding_portal_client")
            .executes(context -> {
                Portal collidingPortal =
                    ((IEEntity) Minecraft.getInstance().player).getCollidingPortal();
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
                Minecraft.getInstance().smartCull = true;
                return 0;
            })
        );
        builder = builder.then(ClientCommandManager
            .literal("vanilla_chunk_culling_disable")
            .executes(context -> {
                Minecraft.getInstance().smartCull = false;
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
                Minecraft client = Minecraft.getInstance();
                client.execute(() -> {
                    client.level.getChunkSource().getLightEngine().updateSectionStatus(
                        SectionPos.of(new BlockPos(client.player.position())),
                        false
                    );
                });
                return 0;
            })
        );
        builder.then(ClientCommandManager
            .literal("report_client_entities")
            .executes(context -> {
                ClientLevel world = Minecraft.getInstance().level;
                
                CHelper.printChat("client entity manager:");
                
                for (Entity entity : world.entitiesForRendering()) {
                    CHelper.printChat(entity.toString());
                }
                
                CHelper.printChat("client entity list:");
                
                EntityTickList entityList = ((IEClientWorld) world).ip_getEntityList();
                
                entityList.forEach(e -> {
                    CHelper.printChat(e.toString());
                });
                
                return 0;
            })
        );
        builder.then(ClientCommandManager
            .literal("check_server_light")
            .executes(context -> {
                MiscHelper.getServer().execute(() -> {
                    ServerPlayer player = McHelper.getRawPlayerList().get(0);
                    
                    BlockPos.betweenClosedStream(
                        player.blockPosition().offset(-2, -2, -2),
                        player.blockPosition().offset(2, 2, 2)
                    ).forEach(blockPos -> {
                        player.level.getLightEngine().checkBlock(blockPos);
                    });
                });
                return 0;
            })
        );
        builder.then(ClientCommandManager
                .literal("update_server_light")
                .executes(context -> {
                    MiscHelper.getServer().execute(() -> {
                        ServerPlayer player = McHelper.getRawPlayerList().get(0);
                        
                        ThreadedLevelLightEngine lightingProvider = (ThreadedLevelLightEngine) player.level.getLightEngine();
                        lightingProvider.lightChunk(
                            player.level.getChunk(player.blockPosition()),
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
                Minecraft.getInstance().execute(() -> {
                    ClientWorldLoader.getClientWorlds().forEach((world) -> {
                        MyBuiltChunkStorage builtChunkStorage = (MyBuiltChunkStorage) ((IEWorldRenderer)
                            ClientWorldLoader.getWorldRenderer(world.dimension()))
                            .ip_getBuiltChunkStorage();
                        CHelper.printChat(
                            world.dimension().location().toString() + builtChunkStorage.getDebugString()
                        );
                    });
                });
                
                return 0;
            })
        );
        
        builder.then(ClientCommandManager
            .literal("report_portal_groups")
            .executes(context -> {
                for (ClientLevel clientWorld : ClientWorldLoader.getClientWorlds()) {
                    Map<Optional<PortalGroup>, List<Portal>> result =
                        Streams.stream(clientWorld.entitiesForRendering())
                            .flatMap(
                                entity -> entity instanceof Portal ?
                                    Stream.of(((Portal) entity)) : Stream.empty()
                            )
                            .collect(Collectors.groupingBy(
                                p -> Optional.ofNullable(PortalRenderInfo.getGroupOf(p))
                            ));
                    
                    CHelper.printChat("\n" + clientWorld.dimension().location().toString());
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
                Minecraft.getInstance().execute(() -> {
                    LocalPlayer player = Minecraft.getInstance().player;
                    DataLayer lightSection = player.level.getLightEngine().getLayerListener(LightLayer.BLOCK).getDataLayerData(
                        SectionPos.of(player.blockPosition())
                    );
                    if (lightSection != null) {
                        boolean uninitialized = lightSection.isEmpty();
                        
                        byte[] byteArray = lightSection.getData();
                        boolean allZero = true;
                        for (byte b : byteArray) {
                            if (b != 0) {
                                allZero = false;
                                break;
                            }
                        }
                        
                        context.getSource().sendFeedback(
                            Component.literal(
                                "has light section " +
                                    (allZero ? "all zero" : "not all zero") +
                                    (uninitialized ? " uninitialized" : " fine")
                            )
                        );
                    }
                    else {
                        context.getSource().sendFeedback(
                            Component.literal("does not have light section")
                        );
                    }
                });
                return 0;
            })
        );
        
        builder.then(ClientCommandManager
            .literal("reload_world_renderer")
            .executes(context -> {
                Minecraft.getInstance().execute(() -> {
                    ClientWorldLoader.disposeRenderHelpers();
                    Minecraft.getInstance().levelRenderer.allChanged();
                });
                return 0;
            })
        );
        
        builder.then(ClientCommandManager
            .literal("config")
            .executes(context -> {
                // works without modmenu
                Minecraft client = Minecraft.getInstance();
                
                IPGlobal.clientTaskList.addTask(MyTaskList.oneShotTask(() -> {
                    client.setScreen(IPConfigGUI.createClothConfigScreen(null));
                }));
                return 0;
            })
        );
        
        builder.then(ClientCommandManager
            .literal("disable_warning")
            .executes(context -> {
                disableWarning();
                context.getSource().sendFeedback(Component.translatable("imm_ptl.warning_disabled"));
                return 0;
            })
        );
        
        builder.then(ClientCommandManager
            .literal("view_portal_data")
            .executes(context -> {
                Minecraft client = Minecraft.getInstance();
                Pair<Portal, Vec3> pair = PortalCommand.getPlayerPointingPortalRaw(
                    client.player, 0, 50, true
                ).orElse(null);
                if (pair != null) {
                    Portal portal = pair.getFirst();
                    PortalCommand.sendPortalInfo(
                        c -> {
                            context.getSource().sendFeedback(c);
                            Helper.log(c.getString());
                            // if the data is too long, the in-game message will be chopped
                            // so also print that in log
                        },
                        portal
                    );
                }
                else {
                    context.getSource().sendFeedback(Component.literal("No pointing to a portal."));
                }
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
            "early_light_update",
            cond -> IPCGlobal.lateClientLightUpdate = cond
        );
        registerSwitchCommand(
            builder,
            "early_remote_upload",
            cond -> IPCGlobal.earlyRemoteUpload = cond
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
        registerSwitchCommand(
            builder,
            "entity_pos_interpolation",
            cond -> IPGlobal.allowClientEntityPosInterpolation = cond
        );
        registerSwitchCommand(
            builder,
            "always_override_terrain_setup",
            cond -> IPGlobal.alwaysOverrideTerrainSetup = cond
        );
        registerSwitchCommand(
            builder,
            "view_bob_reduce",
            cond -> IPGlobal.viewBobbingReduce = cond
        );
        registerSwitchCommand(
            builder,
            "iris_stencil",
            cond -> IPCGlobal.debugEnableStencilWithIris = cond
        );
        registerSwitchCommand(
            builder,
            "another_stencil",
            cond -> IPCGlobal.useSeparatedStencilFormat = cond
        );
        registerSwitchCommand(
            builder,
            "experimental_iris_portal_renderer",
            cond -> IPCGlobal.experimentalIrisPortalRenderer = cond
        );
        registerSwitchCommand(
            builder,
            "portal_rendering_cave_culling",
            cond -> MyGameRenderer.enablePortalCaveCulling = cond
        );
        registerSwitchCommand(
            builder,
            "log_client_player_colliding_portal_update",
            cond -> IPGlobal.logClientPlayerCollidingPortalUpdate = cond
        );
        
        builder.then(ClientCommandManager
            .literal("print_class_path")
            .executes(context -> {
                printClassPath();
                return 0;
            })
        );
        
        builder.then(ClientCommandManager
            .literal("test_invalid_rpc")
            .executes(context -> {
                McRemoteProcedureCall.tellServerToInvoke(
                    "aaa.bbb.WrongClassRemoteCallable.method"
                );
                return 0;
            })
        );
        
        dispatcher.register(builder);
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
            ChunkAccess chunk = Minecraft.getInstance().level.getChunk(
                chunkX, chunkZ
            );
            CHelper.printChat(
                chunk != null && !(chunk instanceof EmptyLevelChunk) ?
                    "client loaded" : "client not loaded"
            );
        }
        
        public static void reportClientPlayerStatus() {
            LocalPlayer playerSP = Minecraft.getInstance().player;
            
            CHelper.printChat(
                String.format(
                    "On Client %s %s removal:%s added:%s age:%s",
                    playerSP.level.dimension().location(),
                    playerSP.blockPosition(),
                    playerSP.getRemovalReason(),
                    playerSP.level.getEntity(playerSP.getId()) != null,
                    playerSP.tickCount
                )
            );
        }
        
        public static void doListPortals() {
            StringBuilder result = new StringBuilder();
            
            result.append("Client Portals\n");
            ClientWorldLoader.getClientWorlds().forEach((world) -> {
                result.append(world.dimension().location().toString() + "\n");
                for (Entity e : world.entitiesForRendering()) {
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
                    world.dimension().location(),
                    world.getChunkSource().getLoadedChunksCount()
                ));
            });
            
            
            str.append("Chunk Mesh Sections:\n");
            ClientWorldLoader.worldRendererMap.forEach(
                (dimension, worldRenderer) -> {
                    str.append(String.format(
                        "%s %s\n",
                        dimension.location(),
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
            String str, int integer, double doubleNum, ResourceLocation identifier,
            ResourceKey<Level> dimension, ResourceKey<Biome> biomeKey,
            BlockPos blockPos, Vec3 vec3d
        ) {
            Helper.log(str + integer + doubleNum + identifier + dimension + biomeKey + blockPos + vec3d);
        }
        
        public static void clientToServer(
            ServerPlayer player,
            UUID uuid,
            Block block, BlockState blockState,
            Item item, ItemStack itemStack,
            CompoundTag compoundTag, Component text, int[] intArray
        ) {
            Helper.log(
                player.getName().getString() + uuid + block + blockState + item + itemStack
                    + compoundTag + text.getString() + Arrays.toString(intArray)
            );
        }
    }
    
    private static void testRemoteProcedureCall(ServerPlayer player) {
        Minecraft.getInstance().execute(() -> {
            CompoundTag compoundTag = new CompoundTag();
            compoundTag.put("test", IntTag.valueOf(7));
            McRemoteProcedureCall.tellServerToInvoke(
                "qouteall.imm_ptl.core.commands.ClientDebugCommand.TestRemoteCallable.clientToServer",
                new UUID(3, 3),
                Blocks.ACACIA_PLANKS,
                Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, Direction.Axis.Z),
                Items.COMPASS,
                new ItemStack(Items.ACACIA_LOG, 2),
                compoundTag,
                Component.literal("test"),
                new int[]{777, 765}
            );
        });
        
        MiscHelper.getServer().execute(() -> {
            McRemoteProcedureCall.tellClientToInvoke(
                player,
                "qouteall.imm_ptl.core.commands.ClientDebugCommand.TestRemoteCallable.serverToClient",
                "string", 2, 3.5, new ResourceLocation("imm_ptl:oops"),
                Level.NETHER, Biomes.JUNGLE,
                new BlockPos(3, 5, 4),
                new Vec3(7, 4, 1)
            );
        });
    }
    
    
    public static void disableWarning() {
        IPConfig ipConfig = IPConfig.readConfig();
        ipConfig.enableWarning = false;
        ipConfig.saveConfigFile();
    }
    
}
