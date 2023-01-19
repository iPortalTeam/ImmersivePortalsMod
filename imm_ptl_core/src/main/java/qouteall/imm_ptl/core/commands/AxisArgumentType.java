package qouteall.imm_ptl.core.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import qouteall.imm_ptl.core.portal.animation.TimingFunction;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AxisArgumentType implements ArgumentType<Direction.Axis> {
    
    public static final AxisArgumentType instance = new AxisArgumentType();
    
    public static final DynamicCommandExceptionType exceptionType =
        new DynamicCommandExceptionType(object ->
            Component.literal("Invalid Axis " + object)
        );
    
    public static Direction.Axis getAxis(CommandContext<CommandSourceStack> context, String axis) {
        return context.getArgument(axis, Direction.Axis.class);
    }
    
    @Override
    public Direction.Axis parse(StringReader reader) throws CommandSyntaxException {
        String s = reader.readUnquotedString();
        return switch (s) {
            case "x", "X" -> Direction.Axis.X;
            case "y", "Y" -> Direction.Axis.Y;
            case "z", "Z" -> Direction.Axis.Z;
            default -> throw exceptionType.createWithContext(reader, s);
        };
    }
    
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
            Arrays.stream(Direction.Axis.values())
                .map(Enum::name)
                .collect(Collectors.toList()),
            builder
        );
    }
    
    @Override
    public Collection<String> getExamples() {
        return Arrays.stream(Direction.Axis.values())
            .map(Enum::toString).collect(Collectors.toList());
    }
    
    public static void init() {
        ArgumentTypeRegistry.registerArgumentType(
            new ResourceLocation("imm_ptl:axis"),
            AxisArgumentType.class,
            SingletonArgumentInfo.contextFree(() -> AxisArgumentType.instance)
        );
    }
}
