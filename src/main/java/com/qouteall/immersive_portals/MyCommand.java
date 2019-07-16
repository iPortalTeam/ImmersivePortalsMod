package com.qouteall.immersive_portals;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.portal_entity.Portal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.dimension.DimensionType;

import java.util.List;
import java.util.function.Consumer;

public class MyCommand {
    public static void init(CommandDispatcher<ServerCommandSource> dispatcher) {
        assert dispatcher != null;
        register(dispatcher);
    }
    
    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        //for composite command arguments, put into then() 's bracket
        //for parallel command arguments, put behind then()
        
        LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager
            .literal("immersive_portals_debug")
            .requires(commandSource -> true)
            .then(CommandManager
                .literal("enable").executes(context -> enable())
            )
            .then(CommandManager
                .literal("disable").executes(context -> disable())
            )
            .then(CommandManager
                .literal("see_portal_content")
                .then(
                    CommandManager.argument("argPortalId", IntegerArgumentType.integer())
                        .executes(context -> seePortalContent(
                            IntegerArgumentType.getInteger(context, "argPortalId")
                        ))
                )
            )
            .then(CommandManager
                .literal("only_view_area")
                .executes(context -> onlyViewArea())
                .then(CommandManager
                    .argument("argPortalId_", IntegerArgumentType.integer())
                    .executes(context -> onlyViewAreaForSpecificPortal(
                        IntegerArgumentType.getInteger(context, "argPortalId_")
                    ))
                )
            )
            .then(CommandManager
                .literal("step_by_step")
                .executes(context -> stepByStep())
            )
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
            )
            .then(CommandManager
                .literal("list_all_portals")
                .executes(context -> listAllPortals(context))
            )
            .then(CommandManager
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
                            context -> {
                                int chunkX = IntegerArgumentType.getInteger(context, "chunkX");
                                int chunkZ = IntegerArgumentType.getInteger(context, "chunkZ");
                                Chunk chunk = MinecraftClient.getInstance().world.getChunk(
                                    chunkX, chunkZ
                                );
                                Helper.serverLog(
                                    context.getSource().getPlayer(),
                                    chunk != null && !(chunk instanceof EmptyChunk) ? "yes" : "no"
                                );
                                return 0;
                            }
                        )
                    )
                )
            )
            .then(CommandManager
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
                                Chunk chunk = Helper.getServer()
                                    .getWorld(player.dimension)
                                    .getChunk(
                                        chunkX, chunkZ,
                                        ChunkStatus.FULL, false
                                    );
                                Helper.serverLog(
                                    player,
                                    chunk != null && !(chunk instanceof EmptyChunk) ? "yes" : "no"
                                );
                                return 0;
                            }
                        )
                    )
                )
            )
//            .then(CommandManager
//                .literal("delete_all_portals")
//                .executes(context -> deleteAllPortals())
//            )
//            .then(CommandManager
//                .literal("delete_portal")
//                .then(CommandManager
//                    .argument(
//                        "argPortalId", IntegerArgumentType.integer()
//                    )
//                    .executes(context -> deletePortal(
//                        IntegerArgumentType.getInteger(context, "argPortalId")
//                    ))
//                )
//            )
            .then(CommandManager
                .literal("add_portal")
                .executes(context -> addPortal(context))
            )
            .then(CommandManager
                .literal("report_player_status")
                .executes(context -> reportPlayerStatus(context))
            )
//            .then(CommandManager
//                .literal("client_remote_ticking_enable")
//                .executes(context -> {
//                    Globals.portalManagerClient.worldLoader.isClientRemoteTickingEnabled = true;
//                    return 0;
//                })
//            )
//            .then(CommandManager
//                .literal("client_remote_ticking_disable")
//                .executes(context -> {
//                    Globals.portalManagerClient.worldLoader.isClientRemoteTickingEnabled = false;
//                    return 0;
//                })
//            )
//            .then(CommandManager
//                .literal("frustum_substitution_enable")
//                .executes(context -> {
//                    MyViewFrustum.enableFrustumSubstitution = true;
//                    return 0;
//                })
//            )
//            .then(CommandManager
//                .literal("frustum_substitution_disable")
//                .executes(context -> {
//                    MyViewFrustum.enableFrustumSubstitution = false;
//                    return 0;
//                })
//            )
//            .then(CommandManager
//                .literal("custom_shader_enable")
//                .executes(context -> {
//                    ShaderManager.isShaderEnabled = true;
//                    return 0;
//                })
//            )
//            .then(CommandManager
//                .literal("custom_shader_disable")
//                .executes(context -> {
//                    ShaderManager.isShaderEnabled = false;
//                    return 0;
//                })
//            )
//            .then(CommandManager
//                .literal("advanced_frustum_culling_enable")
//                .executes(context -> {
//                    Globals.gameRenderer.doUseAdvancedFrustumCulling = true;
//                    return 0;
//                })
//            )
//            .then(CommandManager
//                .literal("advanced_frustum_culling_disable")
//                .executes(context -> {
//                    Globals.gameRenderer.doUseAdvancedFrustumCulling = false;
//                    return 0;
//                })
//            )
            .then(CommandManager
                .literal("report_server_entities")
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    List<Entity> entities = player.world.getEntities(
                        Entity.class,
                        new Box(player.getBlockPos()).expand(32)
                    );
                    Helper.serverLog(player, entities.toString());
                    return 0;
                })
            );
        
        dispatcher.register(builder);
        
        Helper.log("Successfully initialized command /immersive_portals_debug");
    }
    
    private static int enable() {
        //MinecraftClient.getInstance().execute(() -> Globals.portalRenderManager.setBehaviorEnabled());
        assert false;
        return 0;
    }
    
    private static int disable() {
        //MinecraftClient.getInstance().execute(() -> Globals.portalRenderManager.setBehaviorDisabled());
        assert false;
        return 0;
    }
    
    private static int seePortalContent(int portalId) {
//        MinecraftClient.getInstance().execute(
//            () -> Globals.portalRenderManager.setBehaviorSeePortalContent(portalId));
        assert false;
        return 0;
    }
    
    private static int onlyViewArea() {
        //MinecraftClient.getInstance().execute(() -> Globals.portalRenderManager.setBehaviorOnlyViewArea());
        assert false;
        return 0;
    }
    
    private static int onlyViewAreaForSpecificPortal(int portalId) {
//        MinecraftClient.getInstance().execute(
//            () -> Globals.portalRenderManager.setBehaviorOnlyViewAreaForSpecificPortal(portalId));
        assert false;
        return 0;
    }
    
    private static int stepByStep() {
        //MinecraftClient.getInstance().execute(() -> Globals.portalRenderManager.setBehaviorStepByStep());
        assert false;
        return 0;
    }
    
    private static int setMaxPortalLayer(int m) {
//        MinecraftClient.getInstance().execute(() ->
//            Globals.portalRenderManager.setBehaviorMaxPortalLayer(m)
//        );
        assert false;
        return 0;
    }
    
    private static int listAllPortals(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity playerServer = context.getSource().getPlayer();
        ClientPlayerEntity playerClient = MinecraftClient.getInstance().player;
        
        Helper.serverLog(playerServer, "Server Portals");
        Helper.serverLog(
            playerServer,
            Helper.myToString(
                Helper.getEntitiesNearby(
                    playerServer, Portal.class, 64
                )
            )
        );
        
        Helper.serverLog(playerServer, "Client Portals");
        Helper.serverLog(
            playerServer,
            Helper.myToString(
                Helper.getEntitiesNearby(
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
            
            addPortalFunctionality = (playerEntity) -> {
                Vec3d toPos = playerEntity.getPos();
                DimensionType toDimension = player.dimension;
                
                Portal portal = new Portal(player.world);
                portal.x = fromPos.x;
                portal.y = fromPos.y;
                portal.z = fromPos.z;
                
                portal.axisH = new Vec3d(0, 1, 0);
                portal.axisW = portal.axisH.crossProduct(fromNormal).normalize();
                
                portal.dimensionTo = toDimension;
                portal.destination = toPos;
                
                portal.width = 4;
                portal.height = 4;
                
                assert portal.isPortalValid();
                
                playerEntity.world.spawnEntity(portal);
                
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
    
        Helper.serverLog(
            playerMP,
            "On Server " + playerMP.dimension + " " + playerMP.getBlockPos()
        );
        Helper.serverLog(
            playerMP,
            "On Client " + playerSP.dimension + " " + playerSP.getBlockPos()
        );
        return 0;
    }
}
