package qouteall.imm_ptl.core.portal.custom_portal_gen.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;

import java.util.function.Predicate;

public class DiligentForm extends AbstractDiligentForm {
    public static final Codec<DiligentForm> codec = RecordCodecBuilder.create(instance -> {
        return instance.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("from_frame_block").forGetter(o -> o.fromFrameBlock),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("area_block").forGetter(o -> o.areaBlock),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("to_frame_block").forGetter(o -> o.toFrameBlock),
            Codec.BOOL.fieldOf("generate_frame_if_not_found").forGetter(o -> o.generateFrameIfNotFound)
        ).apply(instance, instance.stable(DiligentForm::new));
    });
    
    public final Block fromFrameBlock;
    public final Block areaBlock;
    public final Block toFrameBlock;
    
    public DiligentForm(
        Block fromFrameBlock, Block areaBlock, Block toFrameBlock,
        boolean generateFrameIfNotFound
    ) {
        super(generateFrameIfNotFound);
        this.fromFrameBlock = fromFrameBlock;
        this.areaBlock = areaBlock;
        this.toFrameBlock = toFrameBlock;
    }
    
    @Override
    public Codec<? extends PortalGenForm> getCodec() {
        return codec;
    }
    
    @Override
    public PortalGenForm getReverse() {
        return new DiligentForm(
            toFrameBlock, areaBlock, fromFrameBlock, generateFrameIfNotFound
        );
    }
    
    @Override
    public void generateNewFrame(ServerLevel fromWorld, BlockPortalShape fromShape, ServerLevel toWorld, BlockPortalShape toShape) {
        for (BlockPos blockPos : toShape.frameAreaWithCorner) {
            toWorld.setBlockAndUpdate(blockPos, toFrameBlock.defaultBlockState());
        }
    }
    
    @Override
    public Predicate<BlockState> getOtherSideFramePredicate() {
        return s -> s.getBlock() == toFrameBlock;
    }
    
    @Override
    public Predicate<BlockState> getThisSideFramePredicate() {
        return s -> s.getBlock() == fromFrameBlock;
    }
    
    @Override
    public Predicate<BlockState> getAreaPredicate() {
        return s -> s.getBlock() == areaBlock;
    }
}
