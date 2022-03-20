package qouteall.q_misc_util.dimension;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import qouteall.q_misc_util.MiscHelper;
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
                        
                        cloneDimension(templateDimension, newDimensionId, Optional.empty());
                        
                        context.getSource().sendSuccess(
                            new TextComponent("Warning: the dynamic dimension feature is not yet stable now"),
                            false
                        );
                        
                        context.getSource().sendSuccess(new TextComponent(
                            "Dynamically added dimension %s".formatted(newDimensionId)
                        ), true);
                        return 0;
                    })
                    .then(Commands
                        .argument("newSeed", LongArgumentType.longArg())
                        .executes(context -> {
                            ServerLevel templateDimension =
                                DimensionArgument.getDimension(context, "templateDimension");
                            String newDimensionId = StringArgumentType.getString(context, "newDimensionID");
                            long newSeed = LongArgumentType.getLong(context, "newSeed");
                            
                            cloneDimension(templateDimension, newDimensionId, Optional.of(newSeed));
                            
                            context.getSource().sendSuccess(
                                new TextComponent("Warning: the dynamic dimension feature is not yet stable now"),
                                false
                            );
                            
                            context.getSource().sendSuccess(new TextComponent(
                                "Dynamically added dimension %s with seed %s"
                                    .formatted(newDimensionId, newSeed)
                            ), true);
                            
                            return 0;
                        })
                    )
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
                    
                    context.getSource().sendSuccess(
                        new TextComponent("Warning: the dynamic dimension feature is not yet stable now"),
                        false
                    );
                    
                    context.getSource().sendSuccess(new TextComponent(
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
        ServerLevel templateDimension, String newDimensionId,
        Optional<Long> newSeed
    ) {
        // may throw exception here
        ResourceLocation newDimId = new ResourceLocation(newDimensionId);
        
        ChunkGenerator generator = templateDimension.getChunkSource().getGenerator();
        if (newSeed.isPresent()) {
            generator = generator.withSeed(newSeed.get());
        }
        
        DimensionAPI.addDimensionDynamically(
            newDimId,
            new LevelStem(
                templateDimension.dimensionTypeRegistration(),
                generator
            )
        );
        
        DimensionAPI.saveDimensionConfiguration(DimId.idToKey(newDimensionId));
        
        
    }
    
}
