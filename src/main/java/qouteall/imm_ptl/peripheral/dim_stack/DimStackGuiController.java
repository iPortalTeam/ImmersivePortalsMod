package qouteall.imm_ptl.peripheral.dim_stack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.portal.global_portals.VerticalConnectingPortal;
import qouteall.imm_ptl.peripheral.alternate_dimension.AlternateDimensions;
import qouteall.imm_ptl.peripheral.guide.IPOuterClientMisc;
import qouteall.q_misc_util.Helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DimStackGuiController {
    private static final int entryCountLimit = 64;
    
    private final Screen parentScreen;
    
    public final DimStackGuiModel model;
    public final DimStackScreen view;
    private boolean hasConflict = false;
    
    private final Supplier<List<ResourceKey<Level>>> dimensionListSupplier;
    private final Consumer<DimStackInfo> finishCallback;
    
    public DimStackGuiController(
        Screen parentScreen,
        Supplier<List<ResourceKey<Level>>> dimensionListSupplier,
        Consumer<DimStackInfo> finishCallback
    ) {
        this.parentScreen = parentScreen;
        this.model = new DimStackGuiModel();
        this.view = new DimStackScreen(parentScreen, this);
        this.dimensionListSupplier = dimensionListSupplier;
        this.finishCallback = finishCallback;
    }
    
    public void setEnabled(boolean enabled) {
        model.isEnabled = enabled;
        view.setEnabled(enabled);
    }
    
    private void updateViewState() {
        Map<DimStackInfo.PortalInfo, List<DimStackEntry>> portalInfoMap =
            model.dimStackInfo.getPortalInfoMap();
        
        hasConflict = false;
        for (int i = 0; i < view.dimListWidget.children().size(); i++) {
            DimEntryWidget widget = view.dimListWidget.children().get(i);
            widget.entryIndex = i;
            
            List<DimStackEntry> ceilEntries = portalInfoMap.get(
                new DimStackInfo.PortalInfo(widget.dimension, VerticalConnectingPortal.ConnectorType.ceil)
            );
            int ceilConnectionCount = ceilEntries == null ? 0 : ceilEntries.size();
            List<DimStackEntry> floorEntries = portalInfoMap.get(
                new DimStackInfo.PortalInfo(widget.dimension, VerticalConnectingPortal.ConnectorType.floor)
            );
            int floorConnectionCount = floorEntries == null ? 0 : floorEntries.size();
            
            assert widget.entry != null;
            int toPreviousConnectionCount = widget.entry.flipped ? floorConnectionCount : ceilConnectionCount;
            int toNextConnectionCount = widget.entry.flipped ? ceilConnectionCount : floorConnectionCount;
            boolean conflictsToPrevious = toPreviousConnectionCount > 1;
            boolean conflictsToNext = toNextConnectionCount > 1;
            
            widget.arrowToPrevious = model.dimStackInfo.isEffectivelyConnectingPrevious(i) ? (
                conflictsToPrevious ? DimEntryWidget.ArrowType.conflicting : DimEntryWidget.ArrowType.enabled
            ) : DimEntryWidget.ArrowType.none;
            widget.arrowToNext = model.dimStackInfo.isEffectivelyConnectionNext(i) ? (
                conflictsToNext ? DimEntryWidget.ArrowType.conflicting : DimEntryWidget.ArrowType.enabled
            ) : DimEntryWidget.ArrowType.none;
            
            if (widget.arrowToPrevious == DimEntryWidget.ArrowType.conflicting ||
                widget.arrowToNext == DimEntryWidget.ArrowType.conflicting
            ) {
                hasConflict = true;
            }
        }
    }
    
    /**
     * @return true if successful
     */
    public boolean addEntry(int index, DimStackEntry entry) {
        if (model.dimStackInfo.entries.size() >= entryCountLimit) {
            return false;
        }
        
        Validate.isTrue(index >= 0 && index <= model.dimStackInfo.entries.size());
        model.dimStackInfo.entries.add(index, entry);
        view.dimListWidget.children().add(index, view.createDimEntryWidget(entry));
        updateViewState();
        return true;
    }
    
    public void batchAddEntries(int index, List<DimStackEntry> entries) {
        Validate.isTrue(index >= 0 && index <= model.dimStackInfo.entries.size());
        List<DimStackEntry> entriesToAdd = entries;
        if (model.dimStackInfo.entries.size() + entries.size() > entryCountLimit) {
            entriesToAdd = entries.subList(0, entryCountLimit - model.dimStackInfo.entries.size());
        }
        int currentIndex = index;
        for (DimStackEntry entry : entriesToAdd) {
            model.dimStackInfo.entries.add(currentIndex, entry);
            view.dimListWidget.children().add(currentIndex, view.createDimEntryWidget(entry));
            currentIndex++;
        }
        updateViewState();
    }
    
    public void removeEntry(int index) {
        Validate.isTrue(index >= 0 && index < model.dimStackInfo.entries.size());
        model.dimStackInfo.entries.remove(index);
        view.dimListWidget.children().remove(index);
        updateViewState();
    }
    
    public void clear() {
        model.dimStackInfo.entries.clear();
        view.dimListWidget.children().clear();
        updateViewState();
    }
    
    public void editEntry(int index, DimStackEntry newEntry) {
        Validate.isTrue(index >= 0 && index < model.dimStackInfo.entries.size());
        model.dimStackInfo.entries.set(index, newEntry);
        
        // make sure that the bedrock replacement is consistent for the same dimension
        for (DimStackEntry entry : model.dimStackInfo.entries) {
            if (entry.dimensionIdStr.equals(newEntry.dimensionIdStr)) {
                entry.bedrockReplacementStr = newEntry.bedrockReplacementStr;
            }
        }
    
        DimEntryWidget newWidget = view.createDimEntryWidget(newEntry);
        view.dimListWidget.children().set(index, newWidget);
        view.dimListWidget.setSelected(newWidget);
        updateViewState();
    }
    
    public void onDragged(int selected, int mouseOver) {
        Validate.isTrue(selected >= 0 && selected < model.dimStackInfo.entries.size());
        Validate.isTrue(mouseOver >= 0 && mouseOver < model.dimStackInfo.entries.size());
        
        Helper.swapListElement(model.dimStackInfo.entries, selected, mouseOver);
        Helper.swapListElement(view.dimListWidget.children(), selected, mouseOver);
        
        updateViewState();
        view.dimListWidget.setSelected(view.dimListWidget.children().get(mouseOver));
    }
    
    public void initializeAsDefault() {
        DimStackInfo preset = IPOuterClientMisc.getDimStackPreset();
        
        if (preset != null) {
            setEnabled(true);
            
            setLoopEnabled(preset.loop);
            setGravityTransformEnabled(preset.gravityTransform);
            
            batchAddEntries(0, preset.entries);
        }
        else {
            setEnabled(false);
            setLoopEnabled(false);
            setGravityTransformEnabled(false);
            
            List<DimStackEntry> entriesToAdd = new ArrayList<>();
            
            if (IPGlobal.enableAlternateDimensions) {
                entriesToAdd.add(new DimStackEntry(AlternateDimensions.alternate5));
                entriesToAdd.add(new DimStackEntry(AlternateDimensions.alternate1));
            }
            
            entriesToAdd.add(new DimStackEntry(Level.OVERWORLD));
            entriesToAdd.add(new DimStackEntry(Level.NETHER));
            
            batchAddEntries(0, entriesToAdd);
        }
    }
    
    public List<ResourceKey<Level>> getDimensionList() {
        return dimensionListSupplier.get();
    }
    
    public void onFinish() {
        if (hasConflict) {
            showConflictingAlert();
        }
        else {
            finishCallback.accept(model.getResult());
        }
    }
    
    private void showConflictingAlert() {
        Minecraft.getInstance().setScreen(new AlertScreen(
            () -> {
                Minecraft.getInstance().setScreen(view);
            },
            Component.translatable("imm_ptl.conflicting_dim_stack")
                .withStyle(ChatFormatting.RED),
            Component.translatable("imm_ptl.conflicting_dim_stack_detail")
        ));
    }
    
    public void toggleEnabled() {
        setEnabled(!model.isEnabled);
    }
    
    public void toggleLoop() {
        boolean cond = !model.dimStackInfo.loop;
        setLoopEnabled(cond);
    }
    
    public void setLoopEnabled(boolean cond) {
        model.dimStackInfo.loop = cond;
        view.setLoopEnabled(model.dimStackInfo.loop);
        updateViewState();
    }
    
    public void toggleGravityMode() {
        boolean cond = !model.dimStackInfo.gravityTransform;
        setGravityTransformEnabled(cond);
    }
    
    public void setGravityTransformEnabled(boolean cond) {
        model.dimStackInfo.gravityTransform = cond;
        view.setGravityTransformEnabled(model.dimStackInfo.gravityTransform);
    }
    
    public void setAsDefault() {
        if (hasConflict) {
            showConflictingAlert();
        }
        else {
            IPOuterClientMisc.setDimStackPreset(model.getResult());
            
            Minecraft.getInstance().setScreen(new AlertScreen(
                () -> {
                    Minecraft.getInstance().setScreen(view);
                },
                Component.literal(""),
                Component.translatable("imm_ptl.dim_stack_default_updated")
            ));
        }
    }
}
