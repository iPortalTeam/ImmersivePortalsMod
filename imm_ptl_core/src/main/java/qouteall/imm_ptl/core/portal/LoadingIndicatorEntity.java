package qouteall.imm_ptl.core.portal;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.network.IPNetworking;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;
import qouteall.q_misc_util.my_util.IntBox;

public class LoadingIndicatorEntity extends Entity {
    public static EntityType<LoadingIndicatorEntity> entityType;
    
    private static final EntityDataAccessor<Component> text = SynchedEntityData.defineId(
        LoadingIndicatorEntity.class, EntityDataSerializers.COMPONENT
    );
    
    public boolean isValid = false;
    
    public BlockPortalShape portalShape;
    
    public LoadingIndicatorEntity(EntityType type, Level world) {
        super(type, world);
    }
    
    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return null;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (level.isClientSide()) {
            tickClient();
        }
        else {
            // remove after quitting server and restarting
            if (!isValid) {
                remove(RemovalReason.KILLED);
            }
        }
    }
    
    @Environment(EnvType.CLIENT)
    private void tickClient() {
        addParticles();
        
        if (tickCount > 40) {
            showMessageClient();
        }
    }
    
    @Environment(EnvType.CLIENT)
    private void addParticles() {
        int num = tickCount < 100 ? 50 : 20;
        
        if (portalShape != null) {
            IntBox box = portalShape.innerAreaBox;
            BlockPos size = box.getSize();
            RandomSource random = level.getRandom();
            
            for (int i = 0; i < num; i++) {
                Vec3 p = new Vec3(
                    random.nextDouble(), random.nextDouble(), random.nextDouble()
                ).multiply(Vec3.atLowerCornerOf(size)).add(Vec3.atLowerCornerOf(box.l));
                
                double speedMultiplier = 20;
                
                double vx = speedMultiplier * ((double) random.nextFloat() - 0.5D) * 0.5D;
                double vy = speedMultiplier * ((double) random.nextFloat() - 0.5D) * 0.5D;
                double vz = speedMultiplier * ((double) random.nextFloat() - 0.5D) * 0.5D;
                
                level.addParticle(
                    ParticleTypes.PORTAL,
                    p.x, p.y, p.z,
                    vx, vy, vz
                );
            }
        }
    }
    
    @Override
    protected void defineSynchedData() {
        getEntityData().define(text, Component.literal("Loading..."));
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("shape")) {
            portalShape = new BlockPortalShape(tag.getCompound("shape"));
        }
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (portalShape != null) {
            tag.put("shape", portalShape.toTag());
        }
    }
    
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return IPNetworking.createStcSpawnEntity(this);
    }
    
    public void inform(Component str) {
        setText(str);
    }
    
    public void setText(Component str) {
        getEntityData().set(text, str);
    }
    
    public Component getText() {
        return getEntityData().get(text);
    }
    
    @Environment(EnvType.CLIENT)
    private void showMessageClient() {
        Gui inGameHud = Minecraft.getInstance().gui;
        inGameHud.setOverlayMessage(
            getText(), false
        );
    }
}
