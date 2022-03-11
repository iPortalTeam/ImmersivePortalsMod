package qouteall.q_misc_util.dimension;

import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;

public class DimId {
    
    private static final boolean useIntegerId = true;
    
    public static void writeWorldId(
        FriendlyByteBuf buf, ResourceKey<Level> dimension, boolean isClient
    ) {
        if (useIntegerId) {
            DimensionIdRecord record = isClient ?
                DimensionIdRecord.clientRecord : DimensionIdRecord.serverRecord;
            int intId = record.getIntId(dimension);
            buf.writeInt(intId);
        }
        else {
            buf.writeResourceLocation(dimension.location());
        }
    }
    
    public static ResourceKey<Level> readWorldId(FriendlyByteBuf buf, boolean isClient) {
        if (isClient) {
            if (MiscHelper.isDedicatedServer()) {
                throw new IllegalStateException("oops");
            }
        }
        
        if (useIntegerId) {
            DimensionIdRecord record = isClient ?
                DimensionIdRecord.clientRecord : DimensionIdRecord.serverRecord;
            int intId = buf.readInt();
            return record.getDim(intId);
        }
        else {
            ResourceLocation identifier = buf.readResourceLocation();
            return idToKey(identifier);
        }
    }
    
    // NOTE Minecraft use a global map to store these
    // that map can only grow but cannot be cleaned.
    // so a malicious client may make the server memory leak by that
    public static ResourceKey<Level> idToKey(ResourceLocation identifier) {
        return ResourceKey.create(Registry.DIMENSION_REGISTRY, identifier);
    }
    
    public static ResourceKey<Level> idToKey(String str) {
        return idToKey(new ResourceLocation(str));
    }
    
    public static void putWorldId(CompoundTag tag, String tagName, ResourceKey<Level> dim) {
        tag.putString(tagName, dim.location().toString());
    }
    
    public static ResourceKey<Level> getWorldId(CompoundTag tag, String tagName, boolean isClient) {
        Tag term = tag.get(tagName);
        if (term instanceof IntTag) {
            int intId = ((IntTag) term).getAsInt();
            DimensionIdRecord record = isClient ?
                DimensionIdRecord.clientRecord : DimensionIdRecord.serverRecord;
            ResourceKey<Level> result = record.getDimFromIntOptional(intId);
            if (result == null) {
                Helper.err("unknown dimension id " + intId);
                return Level.OVERWORLD;
            }
            return result;
        }
        
        if (term instanceof StringTag) {
            String id = ((StringTag) term).getAsString();
            return idToKey(id);
        }
        
        throw new RuntimeException(
            "Invalid Dimension Record " + term
        );
    }
}
