package com.qouteall.imm_ptl_peripheral.altius_world;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qouteall.immersive_portals.Helper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DimEntryWidget extends ElementListWidget.Entry<DimEntryWidget> {
    
    public final RegistryKey<World> dimension;
    public final DimListWidget parent;
    private final Consumer<DimEntryWidget> selectCallback;
    private final Identifier dimIconPath;
    private final Text dimensionName;
    private boolean dimensionIconPresent = true;
    private final Type type;
    public final AltiusEntry entry;
    
    public final static int widgetHeight = 50;
    
    public static enum Type {
        simple, withAdvancedOptions
    }
    
    public DimEntryWidget(
        RegistryKey<World> dimension,
        DimListWidget parent,
        Consumer<DimEntryWidget> selectCallback,
        Type type
    ) {
        this.dimension = dimension;
        this.parent = parent;
        this.selectCallback = selectCallback;
        this.type = type;
        
        this.dimIconPath = getDimensionIconPath(this.dimension);
        
        this.dimensionName = getDimensionName(dimension);
        
        try {
            MinecraftClient.getInstance().getResourceManager().getResource(dimIconPath);
        }
        catch (IOException e) {
            Helper.err("Cannot load texture " + dimIconPath);
            dimensionIconPresent = false;
        }
        
        entry = new AltiusEntry(dimension);
    }
    
    private final List<Element> children = new ArrayList<>();
    
    @Override
    public List<? extends Element> children() {
        return children;
    }
    
    @Override
    public void render(
        MatrixStack matrixStack,
        int index,
        int y,
        int x,
        int rowWidth,
        int itemHeight,
        int mouseX,
        int mouseY,
        boolean bl,
        float delta
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        client.textRenderer.draw(
            matrixStack, dimensionName.getString(),
            x + widgetHeight + 3, (float) (y),
            0xFFFFFFFF
        );
        
        client.textRenderer.draw(
            matrixStack, dimension.getValue().toString(),
            x + widgetHeight + 3, (float) (y + 10),
            0xFF999999
        );
        
        if (dimensionIconPresent) {
            client.getTextureManager().bindTexture(dimIconPath);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            
            DrawableHelper.drawTexture(
                matrixStack,
                x, y, 0, (float) 0,
                widgetHeight - 4, widgetHeight - 4,
                widgetHeight - 4, widgetHeight - 4
            );
        }
        
        if (type == Type.withAdvancedOptions) {
            client.textRenderer.draw(
                matrixStack, getText1(),
                x + widgetHeight + 3, (float) (y + 20),
                0xFF999999
            );
            client.textRenderer.draw(
                matrixStack, getText2(),
                x + widgetHeight + 3, (float) (y + 30),
                0xFF999999
            );
        }
    }
    
    private Text getText1() {
        MutableText scaleText = entry.scale != 1.0 ?
            new TranslatableText("imm_ptl.scale")
                .append(new LiteralText(" : " + Double.toString(entry.scale)))
            : new LiteralText("");
        
        BaseText loopText = getIsLoop() ?
            new TranslatableText("imm_ptl.loop") : new LiteralText("");
        
        return scaleText.append(new LiteralText(" ")).append(loopText);
    }
    
    private boolean getIsLoop() {
        AltiusScreen altiusScreen = (AltiusScreen) this.parent.parent;
        
        return altiusScreen.loopEnabled && parent.entryWidgets.get(parent.entryWidgets.size() - 1) == this;
    }
    
    private Text getText2() {
        MutableText flippedText = entry.flipped ?
            new TranslatableText("imm_ptl.flipped") : new LiteralText("");
        
        MutableText horizontalRotationText = entry.horizontalRotation != 0 ?
            new TranslatableText("imm_ptl.horizontal_rotation")
                .append(new LiteralText(":" + Double.toString(entry.horizontalRotation)))
            : new LiteralText("");
        
        return horizontalRotationText.append(new LiteralText(" ")).append(flippedText);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        selectCallback.accept(this);
        super.mouseClicked(mouseX, mouseY, button);
        return true;//allow outer dragging
        /**
         * {@link EntryListWidget#mouseClicked(double, double, int)}
         */
    }
    
    public static Identifier getDimensionIconPath(RegistryKey<World> dimension) {
        Identifier id = dimension.getValue();
        return new Identifier(
            id.getNamespace(),
            "textures/dimension/" + id.getPath() + ".png"
        );
    }
    
    private static TranslatableText getDimensionName(RegistryKey<World> dimension) {
        return new TranslatableText(
            "dimension." + dimension.getValue().getNamespace() + "."
                + dimension.getValue().getPath()
        );
    }
}
