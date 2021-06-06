package qouteall.imm_ptl.peripheral.altius_world;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.GameRenderer;
import qouteall.q_misc_util.Helper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.util.math.MatrixStack;
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

// extending EntryListWidget.Entry is also fine
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
    
    @Override
    public List<? extends Selectable> method_37025() {
        return List.of();
    }
    
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
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, dimIconPath);
            RenderSystem.enableBlend();
            DrawableHelper.drawTexture(
                matrixStack, x, y, 0.0F, 0.0F,
                widgetHeight - 4, widgetHeight - 4,
                widgetHeight - 4, widgetHeight - 4
            );
            RenderSystem.disableBlend();
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
                .append(new LiteralText(":" + Double.toString(entry.scale)))
            : new LiteralText("");
        
        return scaleText;
    }
    
    private Text getText2() {
        MutableText horizontalRotationText = entry.horizontalRotation != 0 ?
            new TranslatableText("imm_ptl.horizontal_rotation")
                .append(new LiteralText(":" + Double.toString(entry.horizontalRotation)))
                .append(new LiteralText(" "))
            : new LiteralText("");
        
        MutableText flippedText = entry.flipped ?
            new TranslatableText("imm_ptl.flipped")
            : new LiteralText("");
        
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
