package com.qouteall.imm_ptl_peripheral;

import com.mojang.serialization.Lifecycle;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.commands.PortalCommand;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.MessageType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.Util;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandStickItem extends Item {
    
    private static RegistryKey<Registry<Data>> registryRegistryKey = RegistryKey.ofRegistry(new Identifier("immersive_portals:command_stick_type"));
    
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
        
        public void serialize(CompoundTag tag) {
            tag.putString("command", command);
            tag.putString("nameTranslationKey", nameTranslationKey);
            ListTag listTag = new ListTag();
            for (String descriptionTK : descriptionTranslationKeys) {
                listTag.add(StringTag.of(descriptionTK));
            }
            tag.put("descriptionTranslationKeys", listTag);
        }
        
        public static Data deserialize(CompoundTag tag) {
            return new Data(
                tag.getString("command"),
                tag.getString("nameTranslationKey"),
                tag.getList(
                    "descriptionTranslationKeys",
                    StringTag.of("").getType()
                )
                    .stream()
                    .map(tag1 -> ((StringTag) tag1).asString())
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
    
    public static final CommandStickItem instance = new CommandStickItem(new Item.Settings());
    
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
        
        if (player.isCreative() || player.hasPermissionLevel(2)) {
            doInvoke(player, stack);
        }
        else {
            sendMessage(player, new LiteralText("No Permission"));
        }
    }
    
    private void doInvoke(PlayerEntity player, ItemStack stack) {
        Data data = Data.deserialize(stack.getOrCreateTag());
        
        ServerCommandSource commandSource = player.getCommandSource().withLevel(2);
        
        CommandManager commandManager = McHelper.getServer().getCommandManager();
        
        commandManager.execute(commandSource, data.command);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        
        Data data = Data.deserialize(stack.getOrCreateTag());
        
        tooltip.add(new LiteralText(data.command));
        
        for (String descriptionTranslationKey : data.descriptionTranslationKeys) {
            tooltip.add(new TranslatableText(descriptionTranslationKey));
        }
        
        tooltip.add(new TranslatableText("imm_ptl.command_stick"));
    }
    
    @Override
    public void appendStacks(ItemGroup group, DefaultedList<ItemStack> stacks) {
        if (group == ItemGroup.MISC) {
            commandStickTypeRegistry.stream().forEach(data -> {
                ItemStack stack = new ItemStack(instance);
                data.serialize(stack.getOrCreateTag());
                stacks.add(stack);
            });
        }
    }
    
    @Override
    public String getTranslationKey(ItemStack stack) {
        Data data = Data.deserialize(stack.getOrCreateTag());
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
            data.serialize(itemStack.getOrCreateTag());
            
            player.inventory.insertStack(itemStack);
            player.playerScreenHandler.sendContentUpdates();
        });
    }
}
