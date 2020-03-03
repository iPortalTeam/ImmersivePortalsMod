package com.qouteall.immersive_portals.portal.global_portals;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class GlobalPortalStorage extends PersistentState {
    public List<GlobalTrackedPortal> data;
    public WeakReference<ServerWorld> world;
    
    public GlobalPortalStorage(String string_1, ServerWorld world_) {
        super(string_1);
        world = new WeakReference<>(world_);
        data = new ArrayList<>();
    }
    
    public static void onPlayerLoggedIn(ServerPlayerEntity player) {
        McHelper.getServer().getWorlds().forEach(
            world -> {
                GlobalPortalStorage storage = get(world);
                if (!storage.data.isEmpty()) {
                    player.networkHandler.sendPacket(
                        MyNetwork.createGlobalPortalUpdate(
                            storage
                        )
                    );
                }
            }
        );
        NewChunkTrackingGraph.updateForPlayer(player);
    }
    
    public void onDataChanged() {
        setDirty(true);
    
        Packet packet = MyNetwork.createGlobalPortalUpdate(this);
        McHelper.getCopiedPlayerList().forEach(
            player -> player.networkHandler.sendPacket(packet)
        );
    }
    
    @Override
    public void fromTag(CompoundTag var1) {
        
        ServerWorld currWorld = world.get();
        Validate.notNull(currWorld);
        List<GlobalTrackedPortal> newData = getPortalsFromTag(var1, currWorld);
        
        data = newData;
    }
    
    public static List<GlobalTrackedPortal> getPortalsFromTag(
        CompoundTag var1,
        World currWorld
    ) {
        /**{@link CompoundTag#getType()}*/
        ListTag listTag = var1.getList("data", 10);
        
        List<GlobalTrackedPortal> newData = new ArrayList<>();
        
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag compoundTag = listTag.getCompound(i);
            GlobalTrackedPortal e = readPortalFromTag(currWorld, compoundTag);
            if (e != null) {
                newData.add(e);
            }
            else {
                Helper.err("error reading portal" + compoundTag);
            }
        }
        return newData;
    }
    
    public static GlobalTrackedPortal readPortalFromTag(World currWorld, CompoundTag compoundTag) {
        Identifier entityId = new Identifier(compoundTag.getString("entity_type"));
        EntityType<?> entityType = Registry.ENTITY_TYPE.get(entityId);
        
        Entity e = entityType.create(currWorld);
        e.fromTag(compoundTag);
        
        if (!(e instanceof GlobalTrackedPortal)) {
            return null;
        }
        
        return (GlobalTrackedPortal) e;
    }
    
    @Override
    public CompoundTag toTag(CompoundTag var1) {
        if (data == null) {
            return var1;
        }
        
        ListTag listTag = new ListTag();
        ServerWorld currWorld = world.get();
        Validate.notNull(currWorld);
        
        for (GlobalTrackedPortal portal : data) {
            Validate.isTrue(portal.world == currWorld);
            CompoundTag tag = new CompoundTag();
            portal.toTag(tag);
            tag.putString(
                "entity_type",
                EntityType.getId(portal.getType()).toString()
            );
            listTag.add(tag);
        }
        
        var1.put("data", listTag);
        
        return var1;
    }
    
    public static GlobalPortalStorage get(
        ServerWorld world
    ) {
        return world.getPersistentStateManager().getOrCreate(
            () -> new GlobalPortalStorage("global_portal", world),
            "global_portal"
        );
    }
}
