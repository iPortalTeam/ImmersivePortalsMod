package qouteall.imm_ptl.core.dimension_sync;

import qouteall.imm_ptl.core.platform_specific.O_O;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public class DimId {
    
    private static final boolean useIntegerId = true;
    
    public static void writeWorldId(
        PacketByteBuf buf, RegistryKey<World> dimension, boolean isClient
    ) {
        if (useIntegerId) {
            DimensionIdRecord record = isClient ?
                DimensionIdRecord.clientRecord : DimensionIdRecord.serverRecord;
            int intId = record.getIntId(dimension);
            buf.writeInt(intId);
        }
        else {
            buf.writeIdentifier(dimension.getValue());
        }
    }
    
    public static RegistryKey<World> readWorldId(PacketByteBuf buf, boolean isClient) {
        if (isClient) {
            if (O_O.isDedicatedServer()) {
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
            Identifier identifier = buf.readIdentifier();
            return idToKey(identifier);
        }
    }
    
    public static RegistryKey<World> idToKey(Identifier identifier) {
        return RegistryKey.of(Registry.WORLD_KEY, identifier);
    }
    
    public static RegistryKey<World> idToKey(String str) {
        return idToKey(new Identifier(str));
    }
    
    public static void putWorldId(NbtCompound tag, String tagName, RegistryKey<World> dim) {
        tag.putString(tagName, dim.getValue().toString());
    }
    
    public static RegistryKey<World> getWorldId(NbtCompound tag, String tagName, boolean isClient) {
        NbtElement term = tag.get(tagName);
        if (term instanceof NbtInt) {
            int intId = ((NbtInt) term).intValue();
            DimensionIdRecord record = isClient ?
                DimensionIdRecord.clientRecord : DimensionIdRecord.serverRecord;
            return record.getDim(intId);
        }
        
        if (term instanceof NbtString) {
            String id = ((NbtString) term).asString();
            return idToKey(id);
        }
        
        throw new RuntimeException(
            "Invalid Dimension Record " + term
        );
    }
}
