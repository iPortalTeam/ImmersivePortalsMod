package qouteall.imm_ptl.core.portal.global_portals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEClientWorld;
import qouteall.imm_ptl.core.platform_specific.IPNetworking;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.api.DimensionAPI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Stores global portals.
 * Also stores bedrock replacement block state for dimension stack.
 */
public class GlobalPortalStorage extends SavedData {
    public List<Portal> data;
    public final WeakReference<ServerLevel> world;
    private int version = 1;
    private boolean shouldReSync = false;
    
    @Nullable
    public BlockState bedrockReplacement;
    
    public static void init() {
        IPGlobal.postServerTickSignal.connect(() -> {
            MiscHelper.getServer().getAllLevels().forEach(world1 -> {
                GlobalPortalStorage gps = GlobalPortalStorage.get(world1);
                gps.tick();
            });
        });
        
        IPGlobal.serverCleanupSignal.connect(() -> {
            for (ServerLevel world : MiscHelper.getServer().getAllLevels()) {
                get(world).onServerClose();
            }
        });
        
        DimensionAPI.serverDimensionDynamicUpdateEvent.register(dims -> {
            for (ServerLevel world : MiscHelper.getServer().getAllLevels()) {
                GlobalPortalStorage gps = get(world);
                gps.clearAbnormalPortals();
                gps.syncToAllPlayers();
            }
        });
        
        if (!O_O.isDedicatedServer()) {
            initClient();
        }
    }
    
    public static GlobalPortalStorage get(
        ServerLevel world
    ) {
        return world.getDataStorage().computeIfAbsent(
            (nbt) -> {
                GlobalPortalStorage globalPortalStorage = new GlobalPortalStorage(world);
                globalPortalStorage.fromNbt(nbt);
                return globalPortalStorage;
            },
            () -> {
                Helper.log("Global portal storage initialized " + world.dimension().location());
                return new GlobalPortalStorage(world);
            },
            "global_portal"
        );
    }
    
    @Environment(EnvType.CLIENT)
    private static void initClient() {
        IPGlobal.clientCleanupSignal.connect(GlobalPortalStorage::onClientCleanup);
    }
    
    @Environment(EnvType.CLIENT)
    private static void onClientCleanup() {
        if (ClientWorldLoader.getIsInitialized()) {
            for (ClientLevel clientWorld : ClientWorldLoader.getClientWorlds()) {
                for (Portal globalPortal : getGlobalPortals(clientWorld)) {
                    globalPortal.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                }
            }
        }
    }
    
    public GlobalPortalStorage(ServerLevel world_) {
        world = new WeakReference<>(world_);
        data = new ArrayList<>();
    }
    
    public static void onPlayerLoggedIn(ServerPlayer player) {
        MiscHelper.getServer().getAllLevels().forEach(
            world -> {
                GlobalPortalStorage storage = get(world);
                if (!storage.data.isEmpty()) {
                    player.connection.send(
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
            player -> player.connection.send(packet)
        );
    }
    
    public void fromNbt(CompoundTag tag) {
        
        ServerLevel currWorld = world.get();
        Validate.notNull(currWorld);
        List<Portal> newData = getPortalsFromTag(tag, currWorld);
        
        data = newData;
        
        if (tag.contains("version")) {
            version = tag.getInt("version");
        }
        
        if (tag.contains("bedrockReplacement")) {
            bedrockReplacement = NbtUtils.readBlockState(
                currWorld.holderLookup(Registries.BLOCK),
                tag.getCompound("bedrockReplacement")
            );
        }
        else {
            bedrockReplacement = null;
        }
        
        clearAbnormalPortals();
    }
    
    private static List<Portal> getPortalsFromTag(
        CompoundTag tag,
        Level currWorld
    ) {
        /**{@link CompoundTag#getType()}*/
        ListTag listTag = tag.getList("data", 10);
        
        List<Portal> newData = new ArrayList<>();
        
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag compoundTag = listTag.getCompound(i);
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
    
    private static Portal readPortalFromTag(Level currWorld, CompoundTag compoundTag) {
        ResourceLocation entityId = new ResourceLocation(compoundTag.getString("entity_type"));
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityId);
        
        Entity e = entityType.create(currWorld);
        e.load(compoundTag);
        
        ((Portal) e).isGlobalPortal = true;
        
        // normal portals' bounding boxes are limited
        // update to non-limited bounding box
        ((Portal) e).updateCache();
        
        return (Portal) e;
    }
    
    @Override
    public CompoundTag save(CompoundTag tag) {
        if (data == null) {
            return tag;
        }
        
        ListTag listTag = new ListTag();
        ServerLevel currWorld = world.get();
        Validate.notNull(currWorld);
        
        for (Portal portal : data) {
            Validate.isTrue(portal.level == currWorld);
            CompoundTag portalTag = new CompoundTag();
            portal.saveWithoutId(portalTag);
            portalTag.putString(
                "entity_type",
                EntityType.getKey(portal.getType()).toString()
            );
            listTag.add(portalTag);
        }
        
        tag.put("data", listTag);
        
        tag.putInt("version", version);
        
        if (bedrockReplacement != null) {
            tag.put("bedrockReplacement", NbtUtils.writeBlockState(bedrockReplacement));
        }
        
        return tag;
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
            ResourceKey<Level> dimensionTo = ((Portal) e).dimensionTo;
            if (MiscHelper.getServer().getLevel(dimensionTo) == null) {
                Helper.err("Missing Dimension for global portal " + dimensionTo.location());
                return true;
            }
            return false;
        });
    }
    
    private static void upgradeData(ServerLevel world) {
        //removed
    }
    
    @Environment(EnvType.CLIENT)
    public static void receiveGlobalPortalSync(ResourceKey<Level> dimension, CompoundTag compoundTag) {
        ClientLevel world = ClientWorldLoader.getWorld(dimension);
        
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
        
        Helper.log("Global Portals Updated " + dimension.location());
    }
    
    public static void convertNormalPortalIntoGlobalPortal(Portal portal) {
        Validate.isTrue(!portal.getIsGlobal());
        Validate.isTrue(!portal.level.isClientSide());
        
        //global portal can only be square
        portal.specialShape = null;
        
        portal.remove(Entity.RemovalReason.KILLED);
        
        Portal newPortal = McHelper.copyEntity(portal);
        
        get(((ServerLevel) portal.level)).addPortal(newPortal);
    }
    
    public static void convertGlobalPortalIntoNormalPortal(Portal portal) {
        Validate.isTrue(portal.getIsGlobal());
        Validate.isTrue(!portal.level.isClientSide());
        
        get(((ServerLevel) portal.level)).removePortal(portal);
        
        Portal newPortal = McHelper.copyEntity(portal);
        
        McHelper.spawnServerEntity(newPortal);
    }
    
    private void onServerClose() {
        for (Portal portal : data) {
            portal.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        }
    }
    
    @Nonnull
    public static List<Portal> getGlobalPortals(Level world) {
        List<Portal> result;
        if (world.isClientSide()) {
            result = CHelper.getClientGlobalPortal(world);
        }
        else if (world instanceof ServerLevel) {
            result = get(((ServerLevel) world)).data;
        }
        else {
            result = null;
        }
        return result != null ? result : Collections.emptyList();
    }
}
