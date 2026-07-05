package com.github.gtexpert.blpc.integration.jmap;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.github.gtexpert.blpc.common.chunk.ClientClaimCache;

@SideOnly(Side.CLIENT)
public class JMapClaimSyncHandler {

    private final Runnable listener = this::onCacheChanged;

    public void register() {
        ClientClaimCache.addChangeListener(listener);
    }

    public void unregister() {
        ClientClaimCache.removeChangeListener(listener);
    }

    private void onCacheChanged() {
        JMapPlugin plugin = JMapPlugin.getInstance();
        if (plugin == null) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;

        plugin.refreshOverlays(mc.player.dimension);
    }
}
