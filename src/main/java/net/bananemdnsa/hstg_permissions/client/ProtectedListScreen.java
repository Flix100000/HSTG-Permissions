package net.bananemdnsa.hstg_permissions.client;

import java.util.function.Consumer;

import net.bananemdnsa.hstg_permissions.editor.ProtectedSelection;
import net.bananemdnsa.hstg_permissions.rules.ProtectedListValue;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Hosts the {@link ProtectedListPicker} over the config editor. Only "Done" hands the list back;
 * Esc or a click beside the window leaves it as it was.
 */
public final class ProtectedListScreen extends Screen {

    // Above the depth the parent's items render at, so the backdrop covers them.
    private static final int OVERLAY_Z = 400;

    private final Screen parent;
    private final Consumer<String> onDone;
    private final ProtectedSelection selection;
    private final ProtectedListPicker picker;

    public ProtectedListScreen(Screen parent, String currentValue, Consumer<String> onDone) {
        super(Component.translatable("config.hstg_permissions.settings.protectedCommands"));
        this.parent = parent;
        this.onDone = onDone;
        this.selection = new ProtectedSelection(ProtectedListValue.split(currentValue));
        this.picker = new ProtectedListPicker(selection, this::done);
        CatalogRequester.request();
    }

    @Override
    protected void init() {
        // Also after a resize: show() recentres the panel, and the selection lives out here.
        picker.show(this.width / 2, this.height / 2, this.width);
    }

    private void done() {
        onDone.accept(ProtectedListValue.join(selection.entries()));
        this.minecraft.setScreen(parent);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // render() draws its own backdrop; this keeps 1.21's menu blur off the parent.
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (parent != null) {
            parent.render(g, -1, -1, partialTick);
            // The parent batches its text and items and would otherwise paint them over us.
            g.flush();
        }
        g.pose().pushPose();
        g.pose().translate(0, 0, OVERLAY_Z);
        g.fill(0, 0, this.width, this.height, 0xC0000000);
        picker.render(g, this.font, mouseX, mouseY);
        g.pose().popPose();
        // Esc or a click beside the window hid it: leave without storing anything.
        if (!picker.isVisible() && this.minecraft.screen == this) this.minecraft.setScreen(parent);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) picker.mouseClicked(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        picker.mouseDragged(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        picker.mouseReleased();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        picker.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (picker.keyPressed(keyCode)) return true;
        // ENTER keeps the list, as it did when this was a dialog.
        if (keyCode == 257 || keyCode == 335) {
            done();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return picker.charTyped(codePoint) || super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // Esc belongs to the picker, which hides itself; render() then goes back.
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
