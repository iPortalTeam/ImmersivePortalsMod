package qouteall.imm_ptl.peripheral.portal_generation;

import com.google.common.base.Splitter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.peripheral.CommandStickItem;

import javax.annotation.Nullable;
import java.util.List;

public class PortalWandItem extends Item {
    public static final PortalWandItem instance = new PortalWandItem(new Properties());
    
    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(
            groupEntries -> {
                groupEntries.accept(new ItemStack(instance));
            }
        );
        
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (player.getMainHandItem().getItem() == instance) {
                // cannot break block using the wand
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }
    
    public static void initClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                if (client.player.getMainHandItem().getItem() == instance) {
                    ClientPortalWandInteraction.updateDisplay();
                    ClientPortalWandInteraction.updateMessage();
                }
                else {
                    ClientPortalWandInteraction.clearCursorPointing();
                }
            }
        });
        
        IPGlobal.clientCleanupSignal.connect(ClientPortalWandInteraction::reset);
    }
    
    public PortalWandItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        if (world.isClientSide()) {
            if (player.getPose() == Pose.CROUCHING) {
                ClientPortalWandInteraction.showSettings(player);
            }
            else {
                ClientPortalWandInteraction.onRightClick();
            }
        }
        
        return super.use(world, player, hand);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        super.appendHoverText(stack, world, tooltip, context);
    
        tooltip.add(Component.translatable("imm_ptl.wand.item_desc_1"));
    }
}
