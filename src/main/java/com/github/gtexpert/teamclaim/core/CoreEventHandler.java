package com.github.gtexpert.teamclaim.core;

import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.github.gtexpert.teamclaim.common.TeamClaimSaveHandler;

public class CoreEventHandler {

    @SubscribeEvent
    public static void onWorldSave(WorldEvent.Save event) {
        if (!event.getWorld().isRemote && event.getWorld().provider.getDimension() == 0) {
            TeamClaimSaveHandler.INSTANCE.saveAll();
        }
    }
}
