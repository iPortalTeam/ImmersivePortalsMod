package qouteall.imm_ptl.core.portal.custom_portal_gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import qouteall.q_misc_util.MiscHelper;

import java.util.Optional;
import java.util.function.Predicate;

// it could be either specified by a block or a block tag
public class SimpleBlockPredicate implements Predicate<BlockState> {
    public static final SimpleBlockPredicate pass = new SimpleBlockPredicate();
    
    public final String name;
    private final TagKey<Block> tag;
    private final Block block;
    
    public SimpleBlockPredicate(String name, TagKey<Block> tag) {
        this.name = name;
        this.tag = tag;
        this.block = null;
    }
    
    public SimpleBlockPredicate(String name, Block block) {
        this.name = name;
        this.block = block;
        this.tag = null;
    }
    
    private SimpleBlockPredicate() {
        this.tag = null;
        this.block = null;
        this.name = "imm_ptl:pass";
    }
    
    @Override
    public boolean test(BlockState blockState) {
        if (tag != null) {
            return blockState.is(tag);
        }
        else {
            if (block != null) {
                return blockState.getBlock() == block;
            }
            else {
                return true;
            }
        }
    }
    
    private static DataResult<SimpleBlockPredicate> deserialize(String string) {
        MinecraftServer server = MiscHelper.getServer();
        
        if (server == null) {
            return DataResult.error(
                () -> "[Immersive Portals] Simple block predicate should not be deserialized in client"
            );
        }
        
        if (string.equals("minecraft:air")) {
            // to make it work for both normal air, cave air and void air
            return DataResult.success(new AirPredicate());
        }
        
        ResourceLocation id = new ResourceLocation(string);
        
        TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, id);
        
        Registry<Block> blockRegistry = server.registryAccess().registry(Registries.BLOCK).get();
    
        Optional<HolderSet.Named<Block>> tag1 = blockRegistry.getTag(tagKey);
        boolean knownTagName = tag1.isPresent();
        
        if (knownTagName) {
            return DataResult.success(new SimpleBlockPredicate(string, tagKey));
        }
        
        if (blockRegistry.keySet().contains(id)) {
            Block block = BuiltInRegistries.BLOCK.get(id);
            return DataResult.success(new SimpleBlockPredicate(string, block), Lifecycle.stable());
        }
    
        return DataResult.error(() -> "Unknown block or block tag:" + string);
    }
    
    private static String serialize(SimpleBlockPredicate predicate) {
        MinecraftServer server = MiscHelper.getServer();
        
        if (server == null) {
            throw new RuntimeException(
                "Simple block predicate should not be serialized in client"
            );
        }
        
        return predicate.name;
    }
    
    public static final Codec<SimpleBlockPredicate> codec =
        Codec.STRING.comapFlatMap(
            SimpleBlockPredicate::deserialize,
            SimpleBlockPredicate::serialize
        );
    
    // handles cave air and void air
    public static class AirPredicate extends SimpleBlockPredicate {
        @Override
        public boolean test(BlockState blockState) {
            return blockState.isAir();
        }
    }
    
}
