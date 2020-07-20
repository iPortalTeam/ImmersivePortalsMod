package com.qouteall.immersive_portals.portal;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.my_util.IntBox;
import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.MessageType;
import net.minecraft.network.Packet;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class LoadingIndicatorEntity extends Entity {
    public static EntityType<LoadingIndicatorEntity> entityType;
    
    private static final TrackedData<Text> text = DataTracker.registerData(
        LoadingIndicatorEntity.class, TrackedDataHandlerRegistry.TEXT_COMPONENT
    );
    
    public boolean isValid = false;
    
    public BlockPortalShape portalShape;
    
    public LoadingIndicatorEntity(EntityType type, World world) {
        super(type, world);
    }
    
    @Override
    public Iterable<ItemStack> getArmorItems() {
        return null;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (world.isClient()) {
            tickClient();
        }
        else {
            // remove after quitting server and restarting
            if (!isValid) {
                remove();
            }
        }
    }
    
    @Environment(EnvType.CLIENT)
    private void tickClient() {
        addParticles();
        
        if (age > 40) {
            showMessageClient();
        }
    }
    
    @Environment(EnvType.CLIENT)
    private void addParticles() {
        int num = age < 100 ? 50 : 20;
        
        if (portalShape != null) {
            IntBox box = portalShape.innerAreaBox;
            BlockPos size = box.getSize();
            Random random = world.getRandom();
            
            for (int i = 0; i < num; i++) {
                Vec3d p = new Vec3d(
                    random.nextDouble(), random.nextDouble(), random.nextDouble()
                ).multiply(Vec3d.of(size)).add(Vec3d.of(box.l));
                
                double speedMultiplier = 20;
                
                double vx = speedMultiplier * ((double) random.nextFloat() - 0.5D) * 0.5D;
                double vy = speedMultiplier * ((double) random.nextFloat() - 0.5D) * 0.5D;
                double vz = speedMultiplier * ((double) random.nextFloat() - 0.5D) * 0.5D;
                
                world.addParticle(
                    ParticleTypes.PORTAL,
                    p.x, p.y, p.z,
                    vx, vy, vz
                );
            }
        }
    }
    
    @Override
    protected void initDataTracker() {
        getDataTracker().startTracking(text, new LiteralText("Loading..."));
    }
    
    @Override
    protected void readCustomDataFromTag(CompoundTag tag) {
        if (tag.contains("shape")) {
            portalShape = new BlockPortalShape(tag.getCompound("shape"));
        }
    }
    
    @Override
    protected void writeCustomDataToTag(CompoundTag tag) {
        if (portalShape != null) {
            tag.put("shape", portalShape.toTag());
        }
    }
    
    @Override
    public Packet<?> createSpawnPacket() {
        return MyNetwork.createStcSpawnEntity(this);
    }
    
    public void inform(Text str) {
        setText(str);
    }
    
    public void setText(Text str) {
        getDataTracker().set(text, str);
    }
    
    public Text getText() {
        return getDataTracker().get(text);
    }
    
    @Environment(EnvType.CLIENT)
    private void showMessageClient() {
        InGameHud inGameHud = MinecraftClient.getInstance().inGameHud;
        inGameHud.addChatMessage(
            MessageType.GAME_INFO,
            getText(),
            Util.NIL_UUID
        );
    }
}
