package com.qouteall.immersive_portals.portal.global_portals;

import com.qouteall.immersive_portals.MyNetwork;
import com.qouteall.immersive_portals.my_util.Helper;
import net.minecraft.client.network.packet.CustomPayloadS2CPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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
    }
    
    public static void onDataChanged(GlobalPortalStorage storage) {
        storage.setDirty(true);
        
        CustomPayloadS2CPacket packet = MyNetwork.createGlobalPortalUpdate(storage);
        Helper.getCopiedPlayerList().forEach(
            player -> player.networkHandler.sendPacket(packet)
        );
    }
    
    public static void onPlayerLoggedIn(ServerPlayerEntity player) {
        Helper.getServer().getWorlds().forEach(
            world -> player.networkHandler.sendPacket(
                MyNetwork.createGlobalPortalUpdate(
                    getFromWorld(world)
                )
            )
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
            GlobalTrackedPortal e = GlobalTrackedPortal.entityType.create(currWorld);
            e.fromTag(listTag.getCompoundTag(i));
            newData.add(e);
        }
        return newData;
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
            listTag.add(tag);
        }
        
        var1.put("data", listTag);
        
        return var1;
    }
    
    public static GlobalPortalStorage getFromWorld(
        ServerWorld world
    ) {
        return world.getPersistentStateManager().getOrCreate(
            () -> new GlobalPortalStorage("global_portal", world),
            "global_portal"
        );
    }
}
