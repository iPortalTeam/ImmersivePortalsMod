package qouteall.imm_ptl.peripheral.dim_stack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
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
import org.slf4j.Logger;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.DQuaternion;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

// extending EntryListWidget.Entry is also fine
public class DimEntryWidget extends ContainerObjectSelectionList.Entry<DimEntryWidget> {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static enum ArrowType {
        none, enabled, conflicting
    }
    
    public final ResourceKey<Level> dimension;
    public final DimListWidget parent;
    private final Consumer<DimEntryWidget> selectCallback;
    @Nullable
    private final ResourceLocation dimIconPath;
    private final Component dimensionName;
    
    // if null, it's in select dimension screen
    // if not null, it's in dim stack screen
    @Nullable
    public final DimStackEntry entry;
    
    public int entryIndex;
    
    ArrowType arrowToPrevious = ArrowType.none;
    ArrowType arrowToNext = ArrowType.none;
    
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
        
        if (dimIconPath != null) {
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
            
            if (arrowToPrevious != ArrowType.none) {
                matrixStack.pushPose();
                matrixStack.translate(x + rowWidth - 13, y, 0);
                matrixStack.scale(1.5f, 1.5f, 1.5f);
                client.font.draw(
                    matrixStack, Component.literal("↑"),
                    0, 0,
                    arrowToPrevious == ArrowType.enabled ? 0xFF999999 : 0xFFFF0000
                );
                matrixStack.popPose();
            }
            
            if (arrowToNext != ArrowType.none) {
                matrixStack.pushPose();
                matrixStack.translate(x + rowWidth - 13, y + widgetHeight - 14.5f, 0);
                matrixStack.scale(1.5f, 1.5f, 1.5f);
                client.font.draw(
                    matrixStack, Component.literal("↓"),
                    0, 0,
                    arrowToNext == ArrowType.enabled ? 0xFF999999 : 0xFFFF0000
                );
                matrixStack.popPose();
            }
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
    
    /**
     * Get `modid/textures/dimension/dimension_id.png` first.
     * If missing, then try to get the mod icon.
     * If still missing, return null.
     */
    @Nullable
    public static ResourceLocation getDimensionIconPath(ResourceKey<Level> dimension) {
        ResourceLocation dimensionId = dimension.location();
        
        ResourceLocation dimIconPath = new ResourceLocation(
            dimensionId.getNamespace(),
            "textures/dimension/" + dimensionId.getPath() + ".png"
        );
        
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(dimIconPath);
        
        if (resource.isEmpty()) {
            LOGGER.info("Cannot load texture {}", dimIconPath);
            
            ResourceLocation modIconLocation = O_O.getModIconLocation(dimensionId.getNamespace());
            
            if (modIconLocation == null) {
                return null;
            }
            
            ResourceLocation modIconPath = new ResourceLocation(
                modIconLocation.getNamespace(),
                modIconLocation.getPath()
            );
            
            Optional<Resource> modIconResource = Minecraft.getInstance().getResourceManager().getResource(modIconPath);
            
            if (modIconResource.isEmpty()) {
                LOGGER.info("Cannot load texture {}", modIconPath);
                return null;
            }
            
            return modIconPath;
        }
        
        return dimIconPath;
    }
    
    /**
     * Firstly try to use translatable `dimension.modid.dimension_id`.
     * If missing, try to get the mod name and use "a dimension of mod_name" or "a dimension of modid"
     */
    private static Component getDimensionName(ResourceKey<Level> dimension) {
        String namespace = dimension.location().getNamespace();
        String path = dimension.location().getPath();
        String translationkey = "dimension." + namespace + "." + path;
        MutableComponent component = Component.translatable(translationkey);
        
        if (component.getString().equals(translationkey)) {
            // no translation
            // try to get the mod name
            String modName = O_O.getModName(namespace);
            if (modName != null) {
                return Component.translatable("imm_ptl.a_dimension_of", modName);
            }
            else {
                return Component.translatable("imm_ptl.a_dimension_of", namespace);
            }
        }
        
        return component;
    }
}
