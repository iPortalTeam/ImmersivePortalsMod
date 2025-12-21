package qouteall.imm_ptl.core.miscellaneous;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import qouteall.imm_ptl.core.platform_specific.IPConfig;

public class IPortalInitialScreen extends Screen {
    private static final int PAGE_NUM = 4;
    
    private final Runnable onClose;
    
    private final Button prevButton;
    private final StringWidget pageNumberWidget;
    private final Button iKnowButton;
    
    private final ImageWidget iconWidget;
    private final StringWidget titleWidget;
    
    private final MultiLineTextWidget contentWidget;
    
    private int currentPageIndex = 0;
    
    public IPortalInitialScreen(Runnable onClose) {
        super(Component.empty());
        this.onClose = onClose;
        
        this.minecraft = Minecraft.getInstance();
        this.font = minecraft.font;
        
        prevButton = Button.builder(
            Component.translatable("iportal.initial_screen.prev"),
            button -> onPrevious()
        ).build();
        
        pageNumberWidget = new StringWidget(
            Component.empty(),
            font
        );
        
        iKnowButton = Button.builder(
            Component.translatable("iportal.initial_screen.i_know"),
            button -> onIKnow()
        ).build();
        
        iconWidget = ImageWidget.texture(
            30, 30,
            ResourceLocation.fromNamespaceAndPath("immersive_portals", "icon.png"),
            30, 30
        );
        
        titleWidget = new StringWidget(
            Component.translatable("iportal.initial_screen.title"),
            font
        ).alignCenter();
        
        contentWidget = new MultiLineTextWidget(
            Component.empty(),
            font
        );
    }
    
    private void onPrevious() {
        if (currentPageIndex > 0) {
            currentPageIndex--;
            updateUiStatus(currentPageIndex);
        }
    }
    
    private void onIKnow() {
        if (currentPageIndex == PAGE_NUM - 1) {
            IPConfig config = IPConfig.getConfig();
            config.initialScreenShown = true;
            config.saveConfigFile();
            
            onClose();
        }
        else {
            currentPageIndex++;
            updateUiStatus(currentPageIndex);
        }
    }
    
    private void updateUiStatus(int newPageIndex) {
        this.currentPageIndex = newPageIndex;
        
        prevButton.visible = newPageIndex > 0;
        
        pageNumberWidget.setMessage(
            Component.literal("%d / %d".formatted(newPageIndex + 1, PAGE_NUM))
        );
        
        contentWidget.setMessage(
            Component.translatable(
                "iportal.initial_screen.content." + newPageIndex
            )
        );
    }
    
    @Override
    public void onClose() {
        onClose.run();
    }
    
    @Override
    public void init() {
        contentWidget.setMaxWidth(this.width - 40);
        pageNumberWidget.setWidth(50);
        pageNumberWidget.setHeight(iKnowButton.getHeight());
        pageNumberWidget.alignCenter();
        titleWidget.setHeight(iconWidget.getHeight());
        
        addRenderableWidget(prevButton);
        addRenderableWidget(pageNumberWidget);
        addRenderableWidget(iKnowButton);
        addRenderableWidget(iconWidget);
        addRenderableWidget(titleWidget);
        addRenderableWidget(contentWidget);
        
        HeaderAndFooterLayout layout = new HeaderAndFooterLayout(
            this, 60, 33
        );
        
        LinearLayout headerLayout = layout.addToHeader(LinearLayout.horizontal());
        headerLayout.addChild(iconWidget, headerLayout.defaultCellSetting().padding(5));
        headerLayout.addChild(titleWidget, headerLayout.defaultCellSetting().padding(5));
        
        LinearLayout contentLayout = layout.addToContents(LinearLayout.vertical());
        contentLayout.addChild(contentWidget, headerLayout.defaultCellSetting().padding(5));
        
        LinearLayout footerLayout = layout.addToFooter(LinearLayout.horizontal());
        footerLayout.addChild(prevButton, footerLayout.defaultCellSetting().padding(5));
        footerLayout.addChild(pageNumberWidget, footerLayout.defaultCellSetting().padding(5));
        footerLayout.addChild(iKnowButton, footerLayout.defaultCellSetting().padding(5));
        
        layout.arrangeElements();
        
        updateUiStatus(currentPageIndex);
    }
}
