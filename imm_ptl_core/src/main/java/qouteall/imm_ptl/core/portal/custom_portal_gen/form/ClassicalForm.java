package qouteall.imm_ptl.core.portal.custom_portal_gen.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;
import qouteall.imm_ptl.core.portal.nether_portal.NetherPortalGeneration;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.Registry;

import java.util.function.Predicate;

public class ClassicalForm extends NetherPortalLikeForm {
    public static final Codec<ClassicalForm> codec = RecordCodecBuilder.create(instance -> {
        return instance.group(
            Registry.BLOCK.getCodec().fieldOf("from_frame_block").forGetter(o -> o.fromFrameBlock),
            Registry.BLOCK.getCodec().fieldOf("area_block").forGetter(o -> o.areaBlock),
            Registry.BLOCK.getCodec().fieldOf("to_frame_block").forGetter(o -> o.toFrameBlock),
            Codec.BOOL.fieldOf("generate_frame_if_not_found").forGetter(o -> o.generateFrameIfNotFound)
        ).apply(instance, instance.stable(ClassicalForm::new));
    });
    
    public final Block fromFrameBlock;
    public final Block areaBlock;
    public final Block toFrameBlock;
    
    public ClassicalForm(
        Block fromFrameBlock, Block areaBlock, Block toFrameBlock, boolean generateFrameIfNotFound
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
        return new ClassicalForm(
            toFrameBlock,
            areaBlock,
            fromFrameBlock,
            generateFrameIfNotFound
        );
    }
    
    @Override
    public void generateNewFrame(
        ServerWorld fromWorld,
        BlockPortalShape fromShape,
        ServerWorld toWorld,
        BlockPortalShape toShape
    ) {
        NetherPortalGeneration.embodyNewFrame(
            toWorld,
            toShape,
            toFrameBlock.getDefaultState()
        );
    }
    
    @Override
    public Predicate<BlockState> getOtherSideFramePredicate() {
        return blockState -> blockState.getBlock() == toFrameBlock;
    }
    
    @Override
    public Predicate<BlockState> getThisSideFramePredicate() {
        return blockState -> blockState.getBlock() == fromFrameBlock;
    }
    
    @Override
    public Predicate<BlockState> getAreaPredicate() {
        return blockState -> blockState.getBlock() == areaBlock;
    }
}
