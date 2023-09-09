package qouteall.imm_ptl.peripheral;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PortalHelperItem extends BlockItem {
    
    public PortalHelperItem(Block block, Properties settings) {
        super(block, settings);
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            if (context.getPlayer() != null) {
                IPOuterClientMisc.onClientPlacePortalHelper();
            }
        }
        
        return super.useOn(context);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        super.appendHoverText(stack, world, tooltip, context);
        
        tooltip.add(Component.translatable("imm_ptl.portal_helper_tooltip"));
    }
}
