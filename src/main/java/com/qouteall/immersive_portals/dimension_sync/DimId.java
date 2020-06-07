package com.qouteall.immersive_portals.dimension_sync;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public class DimId {
    
    public static void writeWorldId(PacketByteBuf buf, RegistryKey<World> dimension) {
        buf.writeIdentifier(dimension.getValue());
    }
    
    public static RegistryKey<World> readWorldId(PacketByteBuf buf) {
        Identifier identifier = buf.readIdentifier();
        return idToKey(identifier);
    }
    
    public static RegistryKey<World> idToKey(Identifier identifier) {
        return RegistryKey.of(Registry.DIMENSION, identifier);
    }
    
    public static RegistryKey<World> idToKey(String str) {
        return idToKey(new Identifier(str));
    }
    
    public static void putWorldId(CompoundTag tag, String tagName, RegistryKey<World> dim) {
        tag.putString(tagName, dim.getValue().toString());
    }
    
    public static RegistryKey<World> getWorldId(CompoundTag tag, String tagName) {
        Tag term = tag.get(tagName);
        if (term instanceof IntTag) {
            int intId = ((IntTag) term).getInt();
            return DimensionIdRecord.clientRecord.getDim(intId);
        }
        
        if (term instanceof StringTag) {
            String id = ((StringTag) term).asString();
            return idToKey(id);
        }
        
        throw new RuntimeException(
            "Invalid Dimension Record " + term
        );
    }
}
