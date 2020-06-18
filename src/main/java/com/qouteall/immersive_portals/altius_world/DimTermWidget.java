package com.qouteall.immersive_portals.altius_world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.function.Consumer;

public class DimTermWidget extends EntryListWidget.Entry<DimTermWidget> {
    
    public RegistryKey<World> dimension;
    public final DimListWidget parent;
    private Consumer<DimTermWidget> selectCallback;
    
    public DimTermWidget(
        RegistryKey<World> dimension,
        DimListWidget parent,
        Consumer<DimTermWidget> selectCallback
    ) {
        this.dimension = dimension;
        this.parent = parent;
        this.selectCallback = selectCallback;
    }
    
    @Override
    public void render(
        MatrixStack matrixStack,
        int y,
        int x,
        int width,
        int height,
        int mouseX,
        int mouseY,
        int i,
        boolean bl,
        float f
    ) {
        MinecraftClient.getInstance().textRenderer.draw(
            matrixStack, dimension.getValue().toString(), width + 32 + 3, (float) (x), 0xFFFFFFFF
        );
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        selectCallback.accept(this);
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
