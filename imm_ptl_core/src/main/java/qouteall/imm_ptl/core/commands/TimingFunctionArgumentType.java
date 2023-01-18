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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import qouteall.imm_ptl.core.portal.animation.TimingFunction;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TimingFunctionArgumentType implements ArgumentType<TimingFunction> {
    
    public static final TimingFunctionArgumentType instance = new TimingFunctionArgumentType();
    
    public static final DynamicCommandExceptionType exceptionType =
        new DynamicCommandExceptionType(object ->
            Component.literal("Invalid Timing Function "+object)
        );
    
    public static TimingFunction get(CommandContext<CommandSourceStack> context, String timingFunction) {
        return context.getArgument(timingFunction, TimingFunction.class);
    }
    
    @Override
    public TimingFunction parse(StringReader reader) throws CommandSyntaxException {
        String s = reader.readUnquotedString();
        
        // will throw IllegalArgumentException if invalid
        return TimingFunction.valueOf(s);
    }
    
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
            Arrays.stream(TimingFunction.values())
                .map(Enum::name)
                .collect(Collectors.toList()),
            builder
        );
    }
    
    @Override
    public Collection<String> getExamples() {
        return Arrays.stream(TimingFunction.values())
            .map(Enum::toString).collect(Collectors.toList());
    }
    
    public static void init() {
        ArgumentTypeRegistry.registerArgumentType(
            new ResourceLocation("imm_ptl:timing_function_argument_type"),
            TimingFunctionArgumentType.class,
            SingletonArgumentInfo.contextFree(() -> instance)
        );
        
    }
}
