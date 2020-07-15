package com.qouteall.immersive_portals.portal.custom_portal_gen.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.portal.custom_portal_gen.CustomPortalGenManagement;
import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.RegistryTagManager;
import net.minecraft.tag.Tag;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.function.Predicate;

public class HeterogeneousForm extends NetherPortalLikeForm {
    
    public final Identifier areaBlockTagId;
    public final Identifier frameBlockTagId;
    
    public Tag<Block> areaBlockTag;
    public Tag<Block> frameBlockTag;
    
    public static final Codec<HeterogeneousForm> codec = RecordCodecBuilder.create(instance -> {
        return instance.group(
            Codec.BOOL.fieldOf("generate_frame_if_not_found").forGetter(o -> o.generateFrameIfNotFound),
            Identifier.CODEC.fieldOf("area_block_tag").forGetter(o -> o.areaBlockTagId),
            Identifier.CODEC.fieldOf("frame_block_tag").forGetter(o -> o.frameBlockTagId)
        ).apply(instance, HeterogeneousForm::new);
    });
    
    public HeterogeneousForm(
        boolean generateFrameIfNotFound, Identifier areaBlockTagId, Identifier frameBlockTagId
    ) {
        super(generateFrameIfNotFound);
        this.areaBlockTagId = areaBlockTagId;
        this.frameBlockTagId = frameBlockTagId;
    }
    
    @Override
    public boolean initAndCheck() {
        
        areaBlockTag = CustomPortalGenManagement.readBlockTag(areaBlockTagId);
        if (areaBlockTag == null) {
            return false;
        }
        
        frameBlockTag = CustomPortalGenManagement.readBlockTag(frameBlockTagId);
        if (frameBlockTag == null) {
            return false;
        }
        
        return super.initAndCheck();
    }
    
    @Override
    public void generateNewFrame(ServerWorld fromWorld, BlockPortalShape fromShape, ServerWorld toWorld, BlockPortalShape toShape) {
        //clone
        BlockPos offset = toShape.innerAreaBox.l.subtract(fromShape.innerAreaBox.l);
        fromShape.frameAreaWithoutCorner.forEach(blockPos -> {
            toWorld.setBlockState(
                blockPos.add(offset),
                fromWorld.getBlockState(blockPos)
            );
        });
    }
    
    @Override
    public Predicate<BlockState> getOtherSideFramePredicate() {
        return blockState -> blockState.isIn(frameBlockTag);
    }
    
    @Override
    public Predicate<BlockState> getThisSideFramePredicate() {
        return blockState -> blockState.isIn(frameBlockTag);
    }
    
    @Override
    public Predicate<BlockState> getAreaPredicate() {
        return blockState -> blockState.isIn(areaBlockTag);
    }
    
    @Override
    public Codec<? extends PortalGenForm> getCodec() {
        return codec;
    }
    
    @Override
    public PortalGenForm getReverse() {
        return new HeterogeneousForm(
            generateFrameIfNotFound, areaBlockTagId, frameBlockTagId
        );
    }
}
