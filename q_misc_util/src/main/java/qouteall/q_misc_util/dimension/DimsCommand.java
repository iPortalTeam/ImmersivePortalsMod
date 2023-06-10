package qouteall.q_misc_util.dimension;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import qouteall.q_misc_util.api.DimensionAPI;

import java.util.Optional;

public class DimsCommand {
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
                        
//                        context.getSource().sendSuccess(
//                            Component.literal("Warning: the dynamic dimension feature is not yet stable now"),
//                            false
//                        );
    
                        context.getSource().sendSuccess(() -> Component.literal(
                            "Dynamically added dimension %s".formatted(newDimensionId)
                        ), true);
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
                    
                    DimensionAPI.deleteDimensionConfiguration(dimension.dimension());
                    
//                    context.getSource().sendSuccess(
//                        Component.literal("Warning: the dynamic dimension feature is not yet stable now"),
//                        false
//                    );
    
                    context.getSource().sendSuccess(() -> Component.literal(
                        "Dynamically removed dimension %s . Its world file is not yet deleted."
                            .formatted(dimension.dimension().location())
                    ), true);
                    
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
        
        DimensionAPI.addDimensionDynamically(
            newDimId,
            new LevelStem(
                templateDimension.dimensionTypeRegistration(),
                generator
            )
        );
        
        DimensionAPI.saveDimensionConfiguration(DimId.idToKey(newDimId));
    }
    
}
