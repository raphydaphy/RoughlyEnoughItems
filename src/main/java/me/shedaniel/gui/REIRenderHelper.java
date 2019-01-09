package me.shedaniel.gui;

import me.shedaniel.ClientListener;
import me.shedaniel.gui.widget.Control;
import me.shedaniel.gui.widget.IFocusable;
import me.shedaniel.gui.widget.REISlot;
import me.shedaniel.impl.REIRecipeManager;
import me.shedaniel.listenerdefinitions.IMixinGuiContainer;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.dimdev.riftloader.RiftLoader;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * Created by James on 7/28/2018.
 */
public class REIRenderHelper {
    
    static Point mouseLoc;
    static public GuiItemList reiGui;
    static GuiContainer overlayedGui;
    static List<TooltipData> tooltipsToRender = new ArrayList<>();
    
    public static void setMouseLoc(int x, int y) {
        mouseLoc = new Point(x, y);
    }
    
    static public IFocusable focusedControl;
    
    public static Point getMouseLoc() {
        return mouseLoc;
    }
    
    public static MainWindow getResolution() {
        return Minecraft.getInstance().mainWindow;
    }
    
    public static String tryGettingModName(String modid) {
        if (modid.equalsIgnoreCase("minecraft"))
            return "Minecraft";
        return RiftLoader.instance.getMods().stream()
                .filter(modInfo -> modInfo.id.equals(modid) || (modInfo.name != null && modInfo.name.equals(modid)))
                .findFirst().map(modInfo -> {
                    if (modInfo.name != null)
                        return modInfo.name;
                    return modid;
                }).orElse(modid);
    }
    
    public static void drawREI(GuiContainer overlayedGui) {
        REIRenderHelper.overlayedGui = overlayedGui;
        if (reiGui == null) {
            reiGui = new GuiItemList(overlayedGui);
        }
        reiGui.draw();
        renderTooltips();
    }
    
    public static void resize(int scaledWidth, int scaledHeight) {
        if (reiGui != null) {
            reiGui.resize();
        }
        if (overlayedGui instanceof RecipeGui) {
            overlayedGui.onResize(Minecraft.getInstance(), scaledWidth, scaledHeight);
        }
    }
    
    public static ItemRenderer getItemRender() {
        return Minecraft.getInstance().getItemRenderer();
    }
    
    public static FontRenderer getFontRenderer() {
        return Minecraft.getInstance().fontRenderer;
    }
    
    public static GuiContainer getOverlayedGui() {
        if (overlayedGui instanceof GuiContainer)
            return overlayedGui;
        return null;
    }
    
    public static void addToolTip(List<String> text, int x, int y) {
        tooltipsToRender.add(new TooltipData(text, x, y));
    }
    
    
    private static void renderTooltips() {
        tooltipsToRender.forEach(tooltipData -> getOverlayedGui().drawHoveringText(tooltipData.text, tooltipData.x, tooltipData.y));
        tooltipsToRender.clear();
    }
    
    public static boolean mouseClick(int x, int y, int button) {
        if (reiGui.visible) {
            for(Control control : reiGui.controls) {
                if (control.isHighlighted() && control.isEnabled() && control.onClick != null) {
                    if (focusedControl != null)
                        focusedControl.setFocused(false);
                    if (control instanceof IFocusable) {
                        focusedControl = (IFocusable) control;
                        ((IFocusable) control).setFocused(true);
                    }
                    return control.onClick.apply(button);
                }
            }
            if (focusedControl != null) {
                focusedControl.setFocused(false);
                focusedControl = null;
            }
        }
        if (overlayedGui instanceof RecipeGui) {
            List<Control> controls = new LinkedList<>(((RecipeGui) overlayedGui).controls);
            if (((RecipeGui) overlayedGui).slots != null)
                controls.addAll(((RecipeGui) overlayedGui).slots);
            controls.addAll(reiGui.controls.stream().filter(control -> {
                return control instanceof REISlot;
            }).collect(Collectors.toList()));
            Optional<Control> ctrl = controls.stream().filter(Control::isHighlighted).filter(Control::isEnabled).findFirst();
            if (ctrl.isPresent()) {
                try {
                    return ctrl.get().onClick.apply(button);
                } catch (Exception e) {
                }
            }
        }
        return false;
    }
    
    public static boolean isGuiVisible() {
        return reiGui != null && reiGui.visible;
    }
    
    public static boolean keyDown(int typedChar, int keyCode, int unknown) {
        boolean handled = false;
        if (focusedControl != null && focusedControl instanceof Control) {
            Control control = (Control) focusedControl;
            if (control.onKeyDown != null) {
                handled = control.onKeyDown.accept(typedChar, keyCode, unknown);
            }
            if (control.charPressed != null)
                if (typedChar == 256) {
                    ((IFocusable) control).setFocused(false);
                    focusedControl = null;
                }
            handled = true;
        }
        if (!handled) {
            return ClientListener.processGuiKeybinds(typedChar);
        }
        return handled;
    }
    
    public static boolean charInput(long num, int keyCode, int unknown) {
        if (focusedControl != null && focusedControl instanceof Control) {
            Control control = (Control) focusedControl;
            if (control.charPressed != null) {
                int numChars = Character.charCount(keyCode);
                if (num == numChars)
                    control.charPressed.accept((char) keyCode, unknown);
                else {
                    char[] chars = Character.toChars(keyCode);
                    for(int x = 0; x < chars.length; x++) {
                        control.charPressed.accept(chars[x], unknown);
                    }
                }
                return true;
            }
        }
        return false;
    }
    
    public static boolean mouseScrolled(double direction) {
        if (!reiGui.visible)
            return false;
        if (Minecraft.getInstance().currentScreen instanceof RecipeGui) {
            MainWindow window = REIRenderHelper.getResolution();
            Point mouse = new Point((int) Minecraft.getInstance().mouseHelper.getMouseX(), (int) Minecraft.getInstance().mouseHelper.getMouseY());
            int mouseX = (int) (mouse.x * (double) window.getScaledWidth() / (double) window.getWidth());
            int mouseY = (int) (mouse.y * (double) window.getScaledHeight() / (double) window.getHeight());
            mouse = new Point(mouseX, mouseY);
            
            RecipeGui recipeGui = (RecipeGui) Minecraft.getInstance().currentScreen;
            if (mouse.getX() < window.getScaledWidth() / 2 + recipeGui.guiWidth / 2 && mouse.getX() > window.getScaledWidth() / 2 - recipeGui.guiWidth / 2 &&
                    mouse.getY() < window.getScaledHeight() / 2 + recipeGui.guiHeight / 2 && mouse.getY() > window.getScaledHeight() / 2 - recipeGui.guiHeight / 2 &&
                    recipeGui.recipes.get(recipeGui.selectedCategory).size() > 2) {
                boolean failed = false;
                if (direction > 0 && recipeGui.btnRecipeLeft.isEnabled())
                    recipeGui.btnRecipeLeft.onClick.apply(0);
                else if (direction < 0 && recipeGui.btnRecipeRight.isEnabled())
                    recipeGui.btnRecipeRight.onClick.apply(0);
                else failed = true;
                if (!failed)
                    return true;
            }
        }
        if (direction > 0 && reiGui.buttonLeft.isEnabled())
            reiGui.buttonLeft.onClick.apply(0);
        else if (direction < 0 && reiGui.buttonRight.isEnabled())
            reiGui.buttonRight.onClick.apply(0);
        return true;
    }
    
    private static class TooltipData {
        
        private final List<String> text;
        private final int x;
        private final int y;
        
        public TooltipData(List<String> text, int x, int y) {
            this.text = text;
            this.x = x;
            this.y = y;
        }
    }
    
    public static void updateSearch() {
        reiGui.updateView();
    }
    
    public static void tick() {
        if (reiGui != null && Minecraft.getInstance().currentScreen == overlayedGui)
            reiGui.tick();
    }
    
    public static boolean recipeKeyBind() {
        if (!(Minecraft.getInstance().currentScreen instanceof GuiContainer))
            return false;
        Control control = reiGui.getLastHovered();
        if (control != null && control.isHighlighted() && control instanceof REISlot) {
            REISlot slot = (REISlot) control;
            REIRecipeManager.instance().displayRecipesFor(slot.getStack());
            return true;
        }
        if (((IMixinGuiContainer) overlayedGui).getHoveredSlot() != null) {
            ItemStack stack = ((IMixinGuiContainer) overlayedGui).getHoveredSlot().getStack();
            REIRecipeManager.instance().displayRecipesFor(stack);
            return true;
        }
        return false;
    }
    
    public static boolean useKeyBind() {
        if (!(Minecraft.getInstance().currentScreen instanceof GuiContainer))
            return false;
        Control control = reiGui.getLastHovered();
        if (control != null && control.isHighlighted() && control instanceof REISlot) {
            REISlot slot = (REISlot) control;
            REIRecipeManager.instance().displayUsesFor(slot.getStack());
            return true;
        }
        if (((IMixinGuiContainer) overlayedGui).getHoveredSlot() != null) {
            ItemStack stack = ((IMixinGuiContainer) overlayedGui).getHoveredSlot().getStack();
            REIRecipeManager.instance().displayUsesFor(stack);
            return true;
        }
        return false;
    }
    
    public static boolean hideKeyBind() {
        if (Minecraft.getInstance().currentScreen == overlayedGui && reiGui != null) {
            reiGui.visible = !reiGui.visible;
            return true;
        }
        return true;
    }
}
