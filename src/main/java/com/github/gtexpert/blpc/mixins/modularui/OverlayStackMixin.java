package com.github.gtexpert.blpc.mixins.modularui;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.cleanroommc.modularui.overlay.OverlayStack;
import com.cleanroommc.modularui.screen.PanelManager;

/**
 * MUI's {@link OverlayStack#closeAll()} calls {@link PanelManager#dispose()}
 * without ensuring the manager has been closed first. When an overlay is
 * registered but never opened (state = INIT), dispose() throws
 * {@code IllegalStateException: Must close screen first before disposing!}.
 * This redirect ensures closeAll() is called before dispose() and silently
 * skips disposal when the manager is in an unclosable state.
 */
@Mixin(value = OverlayStack.class, remap = false)
public class OverlayStackMixin {

    @Redirect(method = "closeAll",
              at = @At(value = "INVOKE",
                       target = "Lcom/cleanroommc/modularui/screen/PanelManager;dispose()V"))
    private static void blpc$safeDispose(PanelManager pm) {
        if (pm.isDisposed()) return;
        pm.closeAll();
        try {
            pm.dispose();
        } catch (IllegalStateException ignored) {}
    }
}
