package qouteall.q_misc_util.dimension;

import com.google.gson.JsonElement;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.core.MappedRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import org.slf4j.Logger;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.api.DimensionAPI;

public class DimsCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands
            .literal("dims")
            .requires(source -> source.hasPermission(2));
        
        builder.then(Commands
            .literal("clone_dimension")
            .then(Commands.argument("templateDimension", DimensionArgument.dimension())
                .then(Commands.argument("newDimensionID", StringArgumentType.string())
                    .executes(context -> {
                        ServerLevel templateDimension =
                            DimensionArgument.getDimension(context, "templateDimension");
                        String newDimensionId = StringArgumentType.getString(context, "newDimensionID");
                        
                        ResourceLocation newDimId = new ResourceLocation(newDimensionId);
                        
                        if (newDimId.getNamespace().equals("minecraft")) {
                            context.getSource().sendFailure(Component.literal("Invalid namespace"));
                            return 0;
                        }
                        
                        cloneDimension(
                            templateDimension, newDimId
                        );
                        
                        context.getSource().sendSuccess(() -> Component.literal(
                            "Dynamically added dimension %s".formatted(newDimensionId)
                        ), true);
                        
                        context.getSource().sendSuccess(
                            () -> Component.literal("Warning: In the current version, dynamic dimension feature is still experimental."),
                            false
                        );
                        
                        return 0;
                    })
                )
            )
        );
        
        builder.then(Commands.literal("add_dimension")
            .then(Commands.argument("newDimensionId", StringArgumentType.string())
                .then(Commands.argument("template", DimTemplateArgumentType.INSTANCE)
                    .executes(context -> {
                        String newDimensionId = StringArgumentType.getString(
                            context, "newDimensionId"
                        );
                        
                        ResourceLocation newDimId = new ResourceLocation(newDimensionId);
                        
                        DimensionTemplate template =
                            DimTemplateArgumentType.getDimTemplate(context, "template");
                        
                        MinecraftServer server = context.getSource().getServer();
                        
                        if (DimensionAPI.dimensionExists(server, newDimId)) {
                            context.getSource().sendFailure(
                                Component.literal("Dimension" + newDimId + " already exists")
                            );
                            return 0;
                        }
                        
                        DimensionAPI.addDimensionDynamically(
                            server,
                            newDimId,
                            template.createLevelStem(server)
                        );
                        
                        context.getSource().sendSuccess(
                            () -> Component.literal("Warning: In the current version, dynamic dimension feature is still experimental."),
                            false
                        );
                        
                        return 0;
                    })
                )
            )
        );
        
        builder.then(Commands
            .literal("remove_dimension")
            .then(Commands.argument("dimension", DimensionArgument.dimension())
                .executes(context -> {
                    ServerLevel dimension =
                        DimensionArgument.getDimension(context, "dimension");
                    
                    DimensionAPI.removeDimensionDynamically(dimension);
                    
                    context.getSource().sendSuccess(() -> Component.literal(
                        "Dynamically removed dimension %s . Its world file is not yet deleted."
                            .formatted(dimension.dimension().location())
                    ), true);
                    
                    context.getSource().sendSuccess(
                        () -> Component.literal("Warning: In the current version, dynamic dimension feature is still experimental."),
                        false
                    );
                    
                    return 0;
                })
            
            )
        );
        
        builder.then(Commands.literal("view_dim_config")
            .then(Commands.argument("dim", DimensionArgument.dimension())
                .executes(context -> {
                    ServerLevel world =
                        DimensionArgument.getDimension(context, "dim");
                    
                    MappedRegistry<LevelStem> dimensionRegistry =
                        DimensionImpl.getDimensionRegistry(world.getServer());
                    
                    LevelStem levelStem = dimensionRegistry.get(world.dimension().location());
                    
                    if (levelStem == null) {
                        context.getSource().sendFailure(
                            Component.literal("Dimension config not found")
                        );
                        return 0;
                    }
                    
                    DataResult<JsonElement> encoded = LevelStem.CODEC.encodeStart(
                        RegistryOps.create(JsonOps.INSTANCE, world.registryAccess()),
                        levelStem
                    );
                    
                    if (encoded.result().isPresent()) {
                        String jsonStr = MiscHelper.gson.toJson(encoded.result().get());
                        
                        context.getSource().sendSuccess(
                            () -> Component.literal(jsonStr),
                            true
                        );
                    }
                    else {
                        context.getSource().sendFailure(Component.literal(
                            encoded.error().toString()
                        ));
                    }
                    
                    return 0;
                })
            )
        );
        
        dispatcher.register(builder);
    }
    
    private static void cloneDimension(
        ServerLevel templateDimension, ResourceLocation newDimId
    ) {
        // may throw exception here
        
        ChunkGenerator generator = templateDimension.getChunkSource().getGenerator();
        
        DimensionAPI.addDimension(
            templateDimension.getServer(),
            newDimId,
            new LevelStem(
                templateDimension.dimensionTypeRegistration(),
                generator
            )
        );
    }
    
}
