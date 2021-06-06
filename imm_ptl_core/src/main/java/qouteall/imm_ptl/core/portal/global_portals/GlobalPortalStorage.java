package qouteall.imm_ptl.core.portal.global_portals;

import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.q_misc_util.Helper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.platform_specific.IPNetworking;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.core.ducks.IEClientWorld;
import qouteall.imm_ptl.core.portal.Portal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;
import qouteall.q_misc_util.MiscHelper;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class GlobalPortalStorage extends PersistentState {
    public List<Portal> data;
    public WeakReference<ServerWorld> world;
    private int version = 1;
    private boolean shouldReSync = false;
    
    public static void init() {
        IPGlobal.postServerTickSignal.connect(() -> {
            MiscHelper.getServer().getWorlds().forEach(world1 -> {
                GlobalPortalStorage gps = GlobalPortalStorage.get(world1);
                gps.tick();
            });
        });
        
        IPGlobal.serverCleanupSignal.connect(() -> {
            for (ServerWorld world : MiscHelper.getServer().getWorlds()) {
                get(world).onServerClose();
            }
        });
        
        if (!O_O.isDedicatedServer()) {
            initClient();
        }
    }
    
    @Environment(EnvType.CLIENT)
    private static void initClient() {
        IPGlobal.clientCleanupSignal.connect(GlobalPortalStorage::onClientCleanup);
    }
    
    @Environment(EnvType.CLIENT)
    private static void onClientCleanup() {
        if (ClientWorldLoader.getIsInitialized()) {
            for (ClientWorld clientWorld : ClientWorldLoader.getClientWorlds()) {
                for (Portal globalPortal : getGlobalPortals(clientWorld)) {
                    globalPortal.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                }
            }
        }
    }
    
    public GlobalPortalStorage(ServerWorld world_) {
        world = new WeakReference<>(world_);
        data = new ArrayList<>();
    }
    
    public static void onPlayerLoggedIn(ServerPlayerEntity player) {
        MiscHelper.getServer().getWorlds().forEach(
            world -> {
                GlobalPortalStorage storage = get(world);
                if (!storage.data.isEmpty()) {
                    player.networkHandler.sendPacket(
                        IPNetworking.createGlobalPortalUpdate(
                            storage
                        )
                    );
                }
            }
        );
        
    }
    
    public void onDataChanged() {
        setDirty(true);
        
        shouldReSync = true;
        
        
    }
    
    public void removePortal(Portal portal) {
        data.remove(portal);
        portal.remove(Entity.RemovalReason.KILLED);
        onDataChanged();
    }
    
    public void addPortal(Portal portal) {
        Validate.isTrue(!data.contains(portal));
        
        Validate.isTrue(portal.isPortalValid());
        
        portal.isGlobalPortal = true;
        portal.myUnsetRemoved();
        data.add(portal);
        onDataChanged();
    }
    
    public void removePortals(Predicate<Portal> predicate) {
        data.removeIf(portal -> {
            final boolean shouldRemove = predicate.test(portal);
            if (shouldRemove) {
                portal.remove(Entity.RemovalReason.KILLED);
            }
            return shouldRemove;
        });
        onDataChanged();
    }
    
    private void syncToAllPlayers() {
        Packet packet = IPNetworking.createGlobalPortalUpdate(this);
        McHelper.getCopiedPlayerList().forEach(
            player -> player.networkHandler.sendPacket(packet)
        );
    }
    
    public void fromNbt(NbtCompound tag) {
        
        ServerWorld currWorld = world.get();
        Validate.notNull(currWorld);
        List<Portal> newData = getPortalsFromTag(tag, currWorld);
        
        if (tag.contains("version")) {
            version = tag.getInt("version");
        }
        
        data = newData;
        
        clearAbnormalPortals();
    }
    
    private static List<Portal> getPortalsFromTag(
        NbtCompound tag,
        World currWorld
    ) {
        /**{@link CompoundTag#getType()}*/
        NbtList listTag = tag.getList("data", 10);
        
        List<Portal> newData = new ArrayList<>();
        
        for (int i = 0; i < listTag.size(); i++) {
            NbtCompound compoundTag = listTag.getCompound(i);
            Portal e = readPortalFromTag(currWorld, compoundTag);
            if (e != null) {
                newData.add(e);
            }
            else {
                Helper.err("error reading portal" + compoundTag);
            }
        }
        return newData;
    }
    
    private static Portal readPortalFromTag(World currWorld, NbtCompound compoundTag) {
        Identifier entityId = new Identifier(compoundTag.getString("entity_type"));
        EntityType<?> entityType = Registry.ENTITY_TYPE.get(entityId);
        
        Entity e = entityType.create(currWorld);
        e.readNbt(compoundTag);
        
        ((Portal) e).isGlobalPortal = true;
        
        return (Portal) e;
    }
    
    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        if (data == null) {
            return tag;
        }
        
        NbtList listTag = new NbtList();
        ServerWorld currWorld = world.get();
        Validate.notNull(currWorld);
        
        for (Portal portal : data) {
            Validate.isTrue(portal.world == currWorld);
            NbtCompound portalTag = new NbtCompound();
            portal.writeNbt(portalTag);
            portalTag.putString(
                "entity_type",
                EntityType.getId(portal.getType()).toString()
            );
            listTag.add(portalTag);
        }
        
        tag.put("data", listTag);
        
        tag.putInt("version", version);
        
        return tag;
    }
    
    public static GlobalPortalStorage get(
        ServerWorld world
    ) {
        return world.getPersistentStateManager().getOrCreate(
            (nbt) -> {
                GlobalPortalStorage globalPortalStorage = new GlobalPortalStorage(world);
                globalPortalStorage.fromNbt(nbt);
                return globalPortalStorage;
            },
            () -> {
                Helper.log("Global portal storage initialized " + world.getRegistryKey().getValue());
                return new GlobalPortalStorage(world);
            },
            "global_portal"
        );
    }
    
    public void tick() {
        if (shouldReSync) {
            syncToAllPlayers();
            shouldReSync = false;
        }
        
        if (version <= 1) {
            upgradeData(world.get());
            version = 2;
            setDirty(true);
        }
    }
    
    public void clearAbnormalPortals() {
        data.removeIf(e -> {
            RegistryKey<World> dimensionTo = ((Portal) e).dimensionTo;
            if (MiscHelper.getServer().getWorld(dimensionTo) == null) {
                Helper.err("Missing Dimension for global portal " + dimensionTo.getValue());
                return true;
            }
            return false;
        });
    }
    
    private static void upgradeData(ServerWorld world) {
        //removed
    }
    
    @Environment(EnvType.CLIENT)
    public static void receiveGlobalPortalSync(RegistryKey<World> dimension, NbtCompound compoundTag) {
        ClientWorld world = ClientWorldLoader.getWorld(dimension);
        
        List<Portal> oldGlobalPortals = ((IEClientWorld) world).getGlobalPortals();
        if (oldGlobalPortals != null) {
            for (Portal p : oldGlobalPortals) {
                p.remove(Entity.RemovalReason.KILLED);
            }
        }
        
        List<Portal> newPortals = getPortalsFromTag(compoundTag, world);
        for (Portal p : newPortals) {
            p.myUnsetRemoved();
            p.isGlobalPortal = true;
            
            Validate.isTrue(p.isPortalValid());
            
            ClientWorldLoader.getWorld(p.getDestDim());
        }
        
        ((IEClientWorld) world).setGlobalPortals(newPortals);
        
        Helper.log("Global Portals Updated " + dimension.getValue());
    }
    
    public static void convertNormalPortalIntoGlobalPortal(Portal portal) {
        Validate.isTrue(!portal.getIsGlobal());
        Validate.isTrue(!portal.world.isClient());
        
        //global portal can only be square
        portal.specialShape = null;
        
        portal.remove(Entity.RemovalReason.KILLED);
        
        Portal newPortal = McHelper.copyEntity(portal);
        
        get(((ServerWorld) portal.world)).addPortal(newPortal);
    }
    
    public static void convertGlobalPortalIntoNormalPortal(Portal portal) {
        Validate.isTrue(portal.getIsGlobal());
        Validate.isTrue(!portal.world.isClient());
        
        get(((ServerWorld) portal.world)).removePortal(portal);
        
        Portal newPortal = McHelper.copyEntity(portal);
        
        McHelper.spawnServerEntity(newPortal);
    }
    
    private void onServerClose() {
        for (Portal portal : data) {
            portal.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        }
    }
    
    @Nonnull
    public static List<Portal> getGlobalPortals(World world) {
        List<Portal> result;
        if (world.isClient()) {
            result = CHelper.getClientGlobalPortal(world);
        }
        else if (world instanceof ServerWorld) {
            result = get(((ServerWorld) world)).data;
        }
        else {
            result = null;
        }
        return result != null ? result : Collections.emptyList();
    }
}
