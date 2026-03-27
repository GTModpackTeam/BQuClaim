package com.github.gtexpert.blpc.integration.jmap;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class JMapClaimSyncHandler {

    private static final int UPDATE_INTERVAL_TICKS = 20;
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;

        tickCounter++;
        if (tickCounter < UPDATE_INTERVAL_TICKS) return;
        tickCounter = 0;

        BLPCJourneyMapPlugin plugin = BLPCJourneyMapPlugin.getInstance();
        if (plugin == null) return;

        plugin.refreshOverlays(mc.player.dimension);
    }
}
