package com.qouteall.immersive_portals.portal.custom_portal_gen.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.my_util.IntBox;
import com.qouteall.immersive_portals.portal.custom_portal_gen.CustomPortalGeneration;
import com.qouteall.immersive_portals.portal.custom_portal_gen.SimpleBlockPredicate;
import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import com.qouteall.immersive_portals.portal.nether_portal.BlockTraverse;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ConvertConventionalPortalForm extends PortalGenForm {
    
    public static final Codec<ConvertConventionalPortalForm> codec = RecordCodecBuilder.create(instance -> {
        return instance.group(
            SimpleBlockPredicate.codec.fieldOf("portal_block").forGetter(o -> o.portalBlock)
        ).apply(instance, instance.stable(ConvertConventionalPortalForm::new));
    });
    
    public final SimpleBlockPredicate portalBlock;
    
    public ConvertConventionalPortalForm(SimpleBlockPredicate portalBlock) {
        this.portalBlock = portalBlock;
    }
    
    @Override
    public Codec<? extends PortalGenForm> getCodec() {
        return codec;
    }
    
    @Override
    public PortalGenForm getReverse() {
        return this;
    }
    
    @Override
    public boolean perform(
        CustomPortalGeneration cpg, ServerWorld fromWorld,
        BlockPos startingPos, ServerWorld toWorld,
        @Nullable Entity triggeringEntity
    ) {
        if (triggeringEntity == null) {
            Helper.err("Null triggering entity for portal conversion");
            return false;
        }
        
        if (!(triggeringEntity instanceof ServerPlayerEntity)) {
            Helper.err("Non player entity triggers portal conversion");
            return false;
        }
        
        ServerPlayerEntity player = (ServerPlayerEntity) triggeringEntity;
        Helper.log(String.format(
            "Trying to convert conventional portal %s -> %s by %s (%d %d %d)",
            fromWorld.getRegistryKey().getValue(),
            toWorld.getRegistryKey().getValue(),
            player.getName().asString(),
            (int) player.getX(), (int) player.getY(), (int) player.getZ()
        ));
        
        if (player.world != toWorld) {
            Helper.err("The player is not in the correct world " +
                player.world.getRegistryKey().getValue());
            return false;
        }
        
        BlockPos playerCurrentPos = player.getBlockPos().toImmutable();
        
        IntBox fromBox = findBlockBoxArea(fromWorld, startingPos, portalBlock);
        
        if (fromBox == null) {
            Helper.err("Cannot find the originating conventional portal");
            return false;
        }
        
        IntBox toBox = findBlockBoxArea(toWorld, playerCurrentPos, portalBlock);
        
        if (toBox == null) {
            Helper.err("Cannot find the destination conventional portal");
            return false;
        }
        
        Helper.log(fromBox + " " + toBox);
        
        BlockPortalShape fromShape = convertToPortalShape(fromBox);
        BlockPortalShape toShape = convertToPortalShape(toBox);
        
        
    }
    
    @Nullable
    public static IntBox findBlockBoxArea(
        World world, BlockPos pos, Predicate<BlockState> predicate
    ) {
        BlockPos startingPos = findBlockAround(world, pos, predicate);
        
        if (startingPos == null) {
            return null;
        }
        
        IntBox result = Helper.expandBoxArea(
            startingPos,
            p -> predicate.test(world.getBlockState(p))
        );
        
        if (result.getSize().equals(new BlockPos(1, 1, 1))) {
            return null;
        }
        
        return result;
    }
    
    @Nullable
    public static BlockPos findBlockAround(
        World world, BlockPos pos, Predicate<BlockState> predicate
    ) {
        BlockState blockState = world.getBlockState(pos);
        if (predicate.test(blockState)) {
            return pos;
        }
        
        return BlockTraverse.searchInBox(
            new IntBox(pos.add(-2, -2, -2), pos.add(2, 2, 2)),
            p -> {
                if (predicate.test(world.getBlockState(p))) {
                    return p;
                }
                return null;
            }
        );
    }
    
    @Nullable
    public static BlockPortalShape convertToPortalShape(IntBox box) {
        BlockPos size = box.getSize();
        Direction.Axis axis = null;
        if (size.getX() == 1) {
            axis = Direction.Axis.X;
        }
        else if (size.getY() == 1) {
            axis = Direction.Axis.Y;
        }
        else if (size.getZ() == 1) {
            axis = Direction.Axis.Z;
        }
        else {
            Helper.err("The box is not flat " + box);
            return null;
        }
        
        return new BlockPortalShape(
            box.stream().collect(Collectors.toSet()),
            axis
        );
    }
}
