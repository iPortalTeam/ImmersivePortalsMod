package qouteall.imm_ptl.peripheral.dim_stack;

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
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.DQuaternion;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

// extending EntryListWidget.Entry is also fine
public class DimEntryWidget extends ContainerObjectSelectionList.Entry<DimEntryWidget> {
    
    public final ResourceKey<Level> dimension;
    public final DimListWidget parent;
    private final Consumer<DimEntryWidget> selectCallback;
    private final ResourceLocation dimIconPath;
    private final Component dimensionName;
    private boolean dimensionIconPresent = true;
    
    // if null, it's in select dimension screen
    // if not null, it's in dim stack screen
    @Nullable
    public final DimStackEntry entry;
    
    // updated by DimListWidget
    public boolean isFirst = false;
    public boolean isLast = false;
    
    public final static int widgetHeight = 50;
    
    @Override
    public List<? extends NarratableEntry> narratables() {
        return List.of();
    }
    
    public DimEntryWidget(
        ResourceKey<Level> dimension,
        DimListWidget parent,
        Consumer<DimEntryWidget> selectCallback,
        @Nullable DimStackEntry entry
    ) {
        this.dimension = dimension;
        this.parent = parent;
        this.selectCallback = selectCallback;
        
        this.dimIconPath = getDimensionIconPath(this.dimension);
        
        this.dimensionName = getDimensionName(dimension);
        
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(dimIconPath);
        
        if (resource.isEmpty()) {
            Helper.err("Cannot load texture " + dimIconPath);
            dimensionIconPresent = false;
        }
        
        this.entry = entry;
    }
    
    private final List<GuiEventListener> children = new ArrayList<>();
    
    @Override
    public List<? extends GuiEventListener> children() {
        return children;
    }
    
    @Override
    public void render(
        @NotNull PoseStack matrixStack,
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
            matrixStack.pushPose();
            matrixStack.translate(x, y, 0);
            
            int iconLen = widgetHeight - 4;
            
            if (entry != null && entry.flipped) {
                matrixStack.rotateAround(
                    DQuaternion.rotationByDegrees(new Vec3(0, 0, 1), 180).toMcQuaternion(),
                    iconLen / 2.0f, iconLen / 2.0f, 0
                );
            }
            
            GuiComponent.blit(
                matrixStack, 0, 0, 0.0F, 0.0F,
                iconLen, iconLen,
                iconLen, iconLen
            );
            matrixStack.popPose();
            RenderSystem.disableBlend();
        }
        
        if (entry != null) {
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
            
            if (effectivelyConnectsPrevious()) {
                matrixStack.pushPose();
                matrixStack.translate(x + rowWidth - 13, y, 0);
                matrixStack.scale(1.5f, 1.5f, 1.5f);
                client.font.draw(
                    matrixStack, Component.literal("↑"),
                    0, 0,
                    0xFF999999
                );
                matrixStack.popPose();
            }
            
            if (effectivelyConnectsNext()) {
                matrixStack.pushPose();
                matrixStack.translate(x + rowWidth - 13, y + widgetHeight - 14.5f, 0);
                matrixStack.scale(1.5f, 1.5f, 1.5f);
                client.font.draw(
                    matrixStack, Component.literal("↓"),
                    0, 0,
                    0xFF999999
                );
                matrixStack.popPose();
            }
        }
    }
    
    public boolean effectivelyConnectsPrevious() {
        // TODO refactor this
        if (isFirst && !((DimStackScreen) parent.parent).loopEnabled) {
            return false;
        }
        
        assert entry != null;
        return entry.connectsPrevious;
    }
    
    public boolean effectivelyConnectsNext() {
        if (isLast && !((DimStackScreen) parent.parent).loopEnabled) {
            return false;
        }
        
        assert entry != null;
        return entry.connectsNext;
    }
    
    public boolean hasCeilConnection() {
        assert entry != null;
        if (entry.flipped) {
            return effectivelyConnectsNext();
        }
        else {
            return effectivelyConnectsPrevious();
        }
    }
    
    public boolean hasFloorConnection() {
        assert entry != null;
        if (entry.flipped) {
            return effectivelyConnectsPrevious();
        }
        else {
            return effectivelyConnectsNext();
        }
    }
    
    public void setPossibleCeilConnection(boolean cond) {
        assert entry != null;
        if (entry.flipped) {
            entry.connectsNext = cond;
        }
        else {
            entry.connectsPrevious = cond;
        }
    }
    
    public void setPossibleFloorConnection(boolean cond) {
        assert entry != null;
        if (entry.flipped) {
            entry.connectsPrevious = cond;
        }
        else {
            entry.connectsNext = cond;
        }
    }
    
    private Component getText1() {
        MutableComponent scaleText = entry.scale != 1.0 ?
            Component.translatable("imm_ptl.scale")
                .append(Component.literal(":" + Double.toString(entry.scale)))
            : Component.literal("");
        
        return scaleText;
    }
    
    private Component getText2() {
        MutableComponent horizontalRotationText = entry.horizontalRotation != 0 ?
            Component.translatable("imm_ptl.horizontal_rotation")
                .append(Component.literal(":" + Double.toString(entry.horizontalRotation)))
                .append(Component.literal(" "))
            : Component.literal("");
        
        return horizontalRotationText;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        selectCallback.accept(this);
        super.mouseClicked(mouseX, mouseY, button);
        return true;//allow outer dragging
    }
    
    public static ResourceLocation getDimensionIconPath(ResourceKey<Level> dimension) {
        // TODO get icon from mod icon
        ResourceLocation id = dimension.location();
        return new ResourceLocation(
            id.getNamespace(),
            "textures/dimension/" + id.getPath() + ".png"
        );
    }
    
    private static Component getDimensionName(ResourceKey<Level> dimension) {
        // TODO get name from mod name
        return Component.translatable(
            "dimension." + dimension.location().getNamespace() + "."
                + dimension.location().getPath()
        );
    }
}
