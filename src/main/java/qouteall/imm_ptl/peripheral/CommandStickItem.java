package qouteall.imm_ptl.peripheral;

import com.mojang.serialization.Lifecycle;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.MessageType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.Util;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.World;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.commands.PortalCommand;
import qouteall.q_misc_util.MiscHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandStickItem extends Item {
    
    private static final RegistryKey<Registry<Data>> registryRegistryKey =
        RegistryKey.ofRegistry(new Identifier("immersive_portals:command_stick_type"));
    
    public static class Data {
        public final String command;
        public final String nameTranslationKey;
        public final List<String> descriptionTranslationKeys;
        
        public Data(
            String command, String nameTranslationKey, List<String> descriptionTranslationKeys
        ) {
            this.command = command;
            this.nameTranslationKey = nameTranslationKey;
            this.descriptionTranslationKeys = descriptionTranslationKeys;
        }
        
        public void serialize(NbtCompound tag) {
            tag.putString("command", command);
            tag.putString("nameTranslationKey", nameTranslationKey);
            NbtList listTag = new NbtList();
            for (String descriptionTK : descriptionTranslationKeys) {
                listTag.add(NbtString.of(descriptionTK));
            }
            tag.put("descriptionTranslationKeys", listTag);
        }
        
        public static Data deserialize(NbtCompound tag) {
            return new Data(
                tag.getString("command"),
                tag.getString("nameTranslationKey"),
                tag.getList(
                    "descriptionTranslationKeys",
                    NbtString.of("").getType()
                )
                    .stream()
                    .map(tag1 -> ((NbtString) tag1).asString())
                    .collect(Collectors.toList())
            );
        }
    }
    
    public static final SimpleRegistry<Data> commandStickTypeRegistry = new SimpleRegistry<>(
        registryRegistryKey,
        Lifecycle.stable()
    );
    
    public static void registerType(String id, Data data) {
        commandStickTypeRegistry.add(
            RegistryKey.of(
                registryRegistryKey, new Identifier(id)
            ),
            data,
            Lifecycle.stable()
        );
    }
    
    public static final CommandStickItem instance = new CommandStickItem(
        new Item.Settings().group(ItemGroup.MISC)
    );
    
    public CommandStickItem(Settings settings) {
        super(settings);
    }
    
    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        doUse(player, player.getStackInHand(hand));
        return super.use(world, player, hand);
    }
    
    private void doUse(PlayerEntity player, ItemStack stack) {
        if (player.world.isClient()) {
            return;
        }
        
        if (canUseCommand(player)) {
            Data data = Data.deserialize(stack.getOrCreateNbt());
            
            ServerCommandSource commandSource = player.getCommandSource().withLevel(2);
            
            CommandManager commandManager = MiscHelper.getServer().getCommandManager();
            
            commandManager.execute(commandSource, data.command);
        }
        else {
            sendMessage(player, new LiteralText("No Permission"));
        }
    }
    
    private static boolean canUseCommand(PlayerEntity player) {
        if (IPGlobal.easeCommandStickPermission) {
            return true;// any player regardless of gamemode can use
        }
        else {
            return player.hasPermissionLevel(2) || player.isCreative();
        }
    }
    
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        
        Data data = Data.deserialize(stack.getOrCreateNbt());
        
        tooltip.add(new LiteralText(data.command).formatted(Formatting.GOLD));
        
        for (String descriptionTranslationKey : data.descriptionTranslationKeys) {
            tooltip.add(new TranslatableText(descriptionTranslationKey).formatted(Formatting.AQUA));
        }
        
        tooltip.add(new TranslatableText("imm_ptl.command_stick").formatted(Formatting.GRAY));
    }
    
    @Override
    public void appendStacks(ItemGroup group, DefaultedList<ItemStack> stacks) {
        if (isIn(group)) {
            commandStickTypeRegistry.stream().forEach(data -> {
                ItemStack stack = new ItemStack(instance);
                data.serialize(stack.getOrCreateNbt());
                stacks.add(stack);
            });
        }
    }
    
    @Override
    public String getTranslationKey(ItemStack stack) {
        Data data = Data.deserialize(stack.getOrCreateNbt());
        return data.nameTranslationKey;
    }
    
    public static void sendMessage(PlayerEntity player, Text message) {
        ((ServerPlayerEntity) player).sendMessage(message, MessageType.GAME_INFO, Util.NIL_UUID);
    }
    
    public static void init() {
        PortalCommand.createCommandStickCommandSignal.connect((player, command) -> {
            ItemStack itemStack = new ItemStack(instance, 1);
            Data data = new Data(
                command,
                command, new ArrayList<>()
            );
            data.serialize(itemStack.getOrCreateNbt());
            
            player.getInventory().insertStack(itemStack);
            player.playerScreenHandler.sendContentUpdates();
        });
    }
}
