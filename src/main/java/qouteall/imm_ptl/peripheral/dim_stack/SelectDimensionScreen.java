package qouteall.imm_ptl.peripheral.dim_stack;

import com.mojang.blaze3d.vertex.PoseStack;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class SelectDimensionScreen extends Screen {
    public final DimStackScreen parent;
    private DimListWidget dimListWidget;
    private Button confirmButton;
    private final Consumer<ResourceKey<Level>> outerCallback;
    private final List<ResourceKey<Level>> dimensionList;
    
    protected SelectDimensionScreen(
        DimStackScreen parent,
        Consumer<ResourceKey<Level>> callback,
        List<ResourceKey<Level>> dimensionList
    ) {
        super(Component.translatable("imm_ptl.select_dimension"));
        this.parent = parent;
        this.outerCallback = callback;
        this.dimensionList = dimensionList;
    }
    
    @Override
    protected void init() {
        dimListWidget = new DimListWidget(
            width,
            height,
            20,
            height - 30,
            DimEntryWidget.widgetHeight,
            this,
            DimListWidget.Type.addDimensionList,
            null
        );
        addWidget(dimListWidget);
        
        Consumer<DimEntryWidget> callback = w -> dimListWidget.setSelected(w);
        
        for (ResourceKey<Level> dim : dimensionList) {
            dimListWidget.children().add(new DimEntryWidget(dim, dimListWidget, callback, new DimStackEntry(dim)));
        }
    
        confirmButton = (Button) addRenderableWidget(Button
            .builder(
                Component.translatable("imm_ptl.confirm_select_dimension"),
                (buttonWidget) -> {
                    DimEntryWidget selected = dimListWidget.getSelected();
                    if (selected == null) {
                        return;
                    }
                    Minecraft.getInstance().setScreen(parent);
                    outerCallback.accept(selected.dimension);
                }
            )
            .pos(this.width / 2 - 75, this.height - 28)
            .size(150, 20)
            .build());
    }
    
    @Override
    public void onClose() {
        // When `esc` is pressed return to the parent screen rather than setting screen to `null` which returns to the main menu.
        this.minecraft.setScreen(this.parent);
    }
    
    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrixStack);
        
        dimListWidget.render(matrixStack, mouseX, mouseY, delta);
        
        super.render(matrixStack, mouseX, mouseY, delta);
        
        
        this.drawCenteredString(
            matrixStack, this.font, this.title.getString(), this.width / 2, 10, -1
        );
    }
}
