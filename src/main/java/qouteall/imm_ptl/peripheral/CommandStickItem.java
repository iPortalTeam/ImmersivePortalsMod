package qouteall.imm_ptl.peripheral;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.commands.PortalCommand;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class CommandStickItem extends Item {
    public record Data(
        String command, String nameTranslationKey, List<String> descriptionTranslationKeys
    ) {
        public void serialize(CompoundTag tag) {
            tag.putString("command", command);
            tag.putString("nameTranslationKey", nameTranslationKey);
            ListTag listTag = new ListTag();
            for (String descriptionTK : descriptionTranslationKeys) {
                listTag.add(StringTag.valueOf(descriptionTK));
            }
            tag.put("descriptionTranslationKeys", listTag);
        }
        
        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            serialize(tag);
            return tag;
        }
        
        public static Data deserialize(CompoundTag tag) {
            return new Data(
                tag.getString("command"),
                tag.getString("nameTranslationKey"),
                tag.getList(
                        "descriptionTranslationKeys",
                        StringTag.valueOf("").getId()
                    )
                    .stream()
                    .map(tag1 -> ((StringTag) tag1).getAsString())
                    .collect(Collectors.toList())
            );
        }
    }
    
    public static final LinkedHashMap<String, Data> BUILT_IN_COMMAND_STICK_TYPES = new LinkedHashMap<>();
    
    public static void registerBuiltInCommandStick(Data data) {
        BUILT_IN_COMMAND_STICK_TYPES.put(data.command, data);
    }
    
    public static final CommandStickItem instance = new CommandStickItem(
        new Item.Properties()
    );
    
    public CommandStickItem(Properties settings) {
        super(settings);
    }
    
    // display enchantment glint
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        doUse(player, player.getItemInHand(hand));
        return super.use(world, player, hand);
    }
    
    private void doUse(Player player, ItemStack stack) {
        if (player.level().isClientSide()) {
            return;
        }
        
        if (canUseCommand(player)) {
            Data data = Data.deserialize(stack.getOrCreateTag());
            
            CommandSourceStack commandSource = player.createCommandSourceStack().withPermission(2);
            
            Commands commandManager = player.getServer().getCommands();
            
            String command = data.command;
            
            if (command.startsWith("/")) {
                // it seems not accepting "/" in the beginning
                command = command.substring(1);
            }
            
            commandManager.performPrefixedCommand(commandSource, command);
        }
        else {
            sendMessage(player, Component.literal("No Permission"));
        }
    }
    
    private static boolean canUseCommand(Player player) {
        if (IPGlobal.easeCommandStickPermission) {
            return true;// any player regardless of gamemode can use
        }
        else {
            return player.hasPermissions(2) || player.isCreative();
        }
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        super.appendHoverText(stack, world, tooltip, context);
        
        Data data = Data.deserialize(stack.getOrCreateTag());
        
        Iterable<String> splitCommand = Splitter.fixedLength(40).split(data.command);
        
        for (String commandPortion : splitCommand) {
            tooltip.add(Component.literal(commandPortion).withStyle(ChatFormatting.GOLD));
        }
        
        for (String descriptionTranslationKey : data.descriptionTranslationKeys) {
            tooltip.add(Component.translatable(descriptionTranslationKey).withStyle(ChatFormatting.AQUA));
        }
        
        tooltip.add(Component.translatable("imm_ptl.command_stick").withStyle(ChatFormatting.GRAY));
    }
    
    @Override
    public String getDescriptionId(ItemStack stack) {
        Data data = Data.deserialize(stack.getOrCreateTag());
        return data.nameTranslationKey;
    }
    
    public static void sendMessage(Player player, Component message) {
        ((ServerPlayer) player).sendSystemMessage(message);
    }
    
    public static void init() {
        PortalCommand.createCommandStickCommandSignal.connect((player, command) -> {
            ItemStack itemStack = new ItemStack(instance, 1);
            Data data = new Data(
                command, command, new ArrayList<>()
            );
            data.serialize(itemStack.getOrCreateTag());
            
            player.getInventory().add(itemStack);
            player.inventoryMenu.broadcastChanges();
        });
        
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(
            groupEntries -> {
                for (Data data : BUILT_IN_COMMAND_STICK_TYPES.values()) {
                    ItemStack stack = new ItemStack(instance);
                    data.serialize(stack.getOrCreateTag());
                    groupEntries.accept(stack);
                }
            }
        );
    }
    
    public static void registerCommandStickTypes() {
        registerPortalSubCommandStick("delete_portal");
        registerPortalSubCommandStick("remove_connected_portals");
        registerPortalSubCommandStick("eradicate_portal_cluster");
        registerPortalSubCommandStick("complete_bi_way_bi_faced_portal");
        registerPortalSubCommandStick("complete_bi_way_portal");
        registerPortalSubCommandStick("move_portal_front", "move_portal 0.5");
        registerPortalSubCommandStick("move_portal_back", "move_portal -0.5");
        registerPortalSubCommandStick(
            "move_portal_destination_front", "move_portal_destination 0.5"
        );
        registerPortalSubCommandStick(
            "move_portal_destination_back", "move_portal_destination -0.5"
        );
        registerPortalSubCommandStick(
            "rotate_x", "rotate_portal_rotation_along x 15"
        );
        registerPortalSubCommandStick(
            "rotate_y", "rotate_portal_rotation_along y 15"
        );
        registerPortalSubCommandStick(
            "rotate_z", "rotate_portal_rotation_along z 15"
        );
        registerPortalSubCommandStick(
            "make_unbreakable", "nbt {unbreakable:true}"
        );
        registerPortalSubCommandStick(
            "make_fuse_view", "nbt {fuseView:true}"
        );
        registerPortalSubCommandStick(
            "enable_pos_adjust", "nbt {adjustPositionAfterTeleport:true}"
        );
        registerPortalSubCommandStick(
            "disable_rendering_yourself", "nbt {doRenderPlayer:false}"
        );
        registerPortalSubCommandStick(
            "enable_isometric", "debug isometric_enable 50"
        );
        registerPortalSubCommandStick(
            "disable_isometric", "debug isometric_disable"
        );
        registerPortalSubCommandStick(
            "create_5_connected_rooms", "create_connected_rooms roomSize 6 4 6 roomNumber 5"
        );
        registerPortalSubCommandStick(
            "accelerate50", "debug accelerate 50"
        );
        registerPortalSubCommandStick(
            "accelerate200", "debug accelerate 200"
        );
        registerPortalSubCommandStick(
            "reverse_accelerate50", "debug accelerate -50"
        );
        registerPortalSubCommandStick(
            "enable_gravity_change", "nbt {teleportChangesGravity:true}"
        );
        registerPortalSubCommandStick(
            "make_invisible", "nbt {isVisible:false}"
        );
        registerPortalSubCommandStick(
            "make_visible", "nbt {isVisible:true}"
        );
        registerPortalSubCommandStick(
            "disable_default_animation", "nbt {defaultAnimation:{durationTicks:0}}"
        );
        
        registerPortalSubCommandStick(
            "pause_animation", "animation pause"
        );
        registerPortalSubCommandStick(
            "resume_animation", "animation resume"
        );
        
        registerPortalSubCommandStick(
            "rotate_around_y", "animation rotate_infinitely @s 0 1 0 1.0"
        );
        registerPortalSubCommandStick(
            "rotate_randomly", "animation rotate_infinitely_random"
        );
        registerBuiltInCommandStick(
            new Data(
                "execute positioned 0.0 0.0 0.0 run portal animation rotate_infinitely @p ^0.0 ^0.0 ^1.0 1.7",
                "imm_ptl.command.rotate_around_view",
                Lists.newArrayList("imm_ptl.command_dest.rotate_around_view")
            )
        );
        registerPortalSubCommandStick(
            "expand_from_center", "animation expand_from_center 20"
        );
        registerPortalSubCommandStick(
            "clear_animation", "animation clear"
        );
        
        registerPortalSubCommandStick(
            "sculpt", "shape sculpt"
        );
        registerPortalSubCommandStick(
            "reset_shape", "shape reset"
        );
        
        registerBuiltInCommandStick(new Data(
            "/scale set pehkui:base 1",
            "imm_ptl.command.reset_scale",
            Lists.newArrayList("imm_ptl.command_desc.reset_scale")
        ));
        registerBuiltInCommandStick(new Data(
            "/scale set pehkui:reach 5",
            "imm_ptl.command.long_reach",
            Lists.newArrayList("imm_ptl.command_desc.long_reach")
        ));
        registerBuiltInCommandStick(new Data(
            "/effect give @s minecraft:night_vision 9999 1 true",
            "imm_ptl.command.night_vision",
            List.of()
        ));
        
        registerPortalSubCommandStick(
            "goback"
        );
        registerPortalSubCommandStick(
            "show_wiki", "wiki"
        );
    }
    
    private static Data registerPortalSubCommandStick(String name) {
        return registerPortalSubCommandStick(name, name);
    }
    
    private static Data registerPortalSubCommandStick(String name, String subCommand) {
        Data data = new Data(
            "/portal " + subCommand,
            "imm_ptl.command." + name,
            Lists.newArrayList("imm_ptl.command_desc." + name)
        );
        registerBuiltInCommandStick(data);
        return data;
    }
    
}
