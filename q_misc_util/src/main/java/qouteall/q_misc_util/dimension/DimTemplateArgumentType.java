package qouteall.q_misc_util.dimension;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class DimTemplateArgumentType implements ArgumentType<DimensionTemplate> {
    
    public static final DimTemplateArgumentType INSTANCE = new DimTemplateArgumentType();
    
    private static final DynamicCommandExceptionType EXCEPTION_TYPE =
        new DynamicCommandExceptionType(object ->
            Component.literal("Invalid Dim Template " + object)
        );
    
    public static DimensionTemplate getDimTemplate(CommandContext<?> context, String argName) {
        return context.getArgument(argName, DimensionTemplate.class);
    }
    
    public static void init() {
        ArgumentTypeRegistry.registerArgumentType(
            new ResourceLocation("q_misc_util:dim_template"),
            DimTemplateArgumentType.class,
            SingletonArgumentInfo.contextFree(() -> INSTANCE)
        );
    }
    
    @Override
    public DimensionTemplate parse(StringReader reader) throws CommandSyntaxException {
        String s = reader.readUnquotedString();
        
        DimensionTemplate r = DimensionTemplate.DIMENSION_TEMPLATES.get(s);
        
        if (r == null) {
            throw EXCEPTION_TYPE.createWithContext(reader, s);
        }
        
        return r;
    }
    
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(
        CommandContext<S> context, SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggest(
            DimensionTemplate.DIMENSION_TEMPLATES.keySet(),
            builder
        );
    }
    
    @Override
    public Collection<String> getExamples() {
        return DimensionTemplate.DIMENSION_TEMPLATES.keySet();
    }
}
