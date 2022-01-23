package qouteall.imm_ptl.peripheral.altius_world;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import qouteall.q_misc_util.Helper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// extending EntryListWidget.Entry is also fine
public class DimEntryWidget extends ContainerObjectSelectionList.Entry<DimEntryWidget> {
    
    public final ResourceKey<Level> dimension;
    public final DimListWidget parent;
    private final Consumer<DimEntryWidget> selectCallback;
    private final ResourceLocation dimIconPath;
    private final Component dimensionName;
    private boolean dimensionIconPresent = true;
    private final Type type;
    public final AltiusEntry entry;
    
    public final static int widgetHeight = 50;
    
    @Override
    public List<? extends NarratableEntry> narratables() {
        return List.of();
    }
    
    public static enum Type {
        simple, withAdvancedOptions
    }
    
    public DimEntryWidget(
        ResourceKey<Level> dimension,
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
            Minecraft.getInstance().getResourceManager().getResource(dimIconPath);
        }
        catch (IOException e) {
            Helper.err("Cannot load texture " + dimIconPath);
            dimensionIconPresent = false;
        }
        
        entry = new AltiusEntry(dimension);
    }
    
    private final List<GuiEventListener> children = new ArrayList<>();
    
    @Override
    public List<? extends GuiEventListener> children() {
        return children;
    }
    
    @Override
    public void render(
        PoseStack matrixStack,
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
        Minecraft client = Minecraft.getInstance();
        
        client.font.draw(
            matrixStack, dimensionName.getString(),
            x + widgetHeight + 3, (float) (y),
            0xFFFFFFFF
        );
        
        client.font.draw(
            matrixStack, dimension.location().toString(),
            x + widgetHeight + 3, (float) (y + 10),
            0xFF999999
        );
        
        if (dimensionIconPresent) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, dimIconPath);
            RenderSystem.enableBlend();
            GuiComponent.blit(
                matrixStack, x, y, 0.0F, 0.0F,
                widgetHeight - 4, widgetHeight - 4,
                widgetHeight - 4, widgetHeight - 4
            );
            RenderSystem.disableBlend();
        }
        
        if (type == Type.withAdvancedOptions) {
            client.font.draw(
                matrixStack, getText1(),
                x + widgetHeight + 3, (float) (y + 20),
                0xFF999999
            );
            client.font.draw(
                matrixStack, getText2(),
                x + widgetHeight + 3, (float) (y + 30),
                0xFF999999
            );
        }
    }
    
    private Component getText1() {
        MutableComponent scaleText = entry.scale != 1.0 ?
            new TranslatableComponent("imm_ptl.scale")
                .append(new TextComponent(":" + Double.toString(entry.scale)))
            : new TextComponent("");
        
        return scaleText;
    }
    
    private Component getText2() {
        MutableComponent horizontalRotationText = entry.horizontalRotation != 0 ?
            new TranslatableComponent("imm_ptl.horizontal_rotation")
                .append(new TextComponent(":" + Double.toString(entry.horizontalRotation)))
                .append(new TextComponent(" "))
            : new TextComponent("");
        
        MutableComponent flippedText = entry.flipped ?
            new TranslatableComponent("imm_ptl.flipped")
            : new TextComponent("");
        
        return horizontalRotationText.append(flippedText);
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
    
    public static ResourceLocation getDimensionIconPath(ResourceKey<Level> dimension) {
        ResourceLocation id = dimension.location();
        return new ResourceLocation(
            id.getNamespace(),
            "textures/dimension/" + id.getPath() + ".png"
        );
    }
    
    private static TranslatableComponent getDimensionName(ResourceKey<Level> dimension) {
        return new TranslatableComponent(
            "dimension." + dimension.location().getNamespace() + "."
                + dimension.location().getPath()
        );
    }
}
