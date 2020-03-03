package com.qouteall.immersive_portals.portal;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public class LoadingIndicatorEntity extends Entity {
    public static EntityType<LoadingIndicatorEntity> entityType;
    
    private static final TrackedData<Text> text = DataTracker.registerData(
        LoadingIndicatorEntity.class, TrackedDataHandlerRegistry.TEXT_COMPONENT
    );
    
    public boolean isAlive = false;
    
    public LoadingIndicatorEntity(World world) {
        this(entityType, world);
    }
    
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
    
        if (!world.isClient()) {
            if (!isAlive) {
                remove();
            }
        }
    }
    
    @Override
    protected void initDataTracker() {
        getDataTracker().startTracking(text, new LiteralText("Loading..."));
    }
    
    @Override
    protected void readCustomDataFromTag(CompoundTag var1) {
    
    }
    
    @Override
    protected void writeCustomDataToTag(CompoundTag var1) {
    
    }
    
    @Override
    public Packet<?> createSpawnPacket() {
        return MyNetwork.createStcSpawnEntity(this);
    }
    
    public void setText(Text str) {
        getDataTracker().set(text, str);
    }
    
    public Text getText() {
        return getDataTracker().get(text);
    }
}
